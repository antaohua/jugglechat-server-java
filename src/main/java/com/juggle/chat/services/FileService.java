package com.juggle.chat.services;

import java.time.Duration;
import java.util.Locale;
import java.util.Optional;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.juggle.chat.apimodels.FileType;
import com.juggle.chat.apimodels.OssType;
import com.juggle.chat.apimodels.PreSignResp;
import com.juggle.chat.apimodels.QiNiuCredResp;
import com.juggle.chat.apimodels.QryFileCredReq;
import com.juggle.chat.apimodels.QryFileCredResp;
import com.juggle.chat.exceptions.JimErrorCode;
import com.juggle.chat.exceptions.JimException;
import com.juggle.chat.file.MinioConfig;
import com.juggle.chat.file.MinioStorage;
import com.juggle.chat.file.OssConfig;
import com.juggle.chat.file.OssPostPolicy;
import com.juggle.chat.file.OssStorage;
import com.juggle.chat.file.QiNiuConfig;
import com.juggle.chat.file.QiNiuStorage;
import com.juggle.chat.file.S3Config;
import com.juggle.chat.file.S3Storage;
import com.juggle.chat.interceptors.RequestContext;
import com.juggle.chat.mappers.FileConfMapper;
import com.juggle.chat.models.FileConf;
import com.juggle.chat.utils.CommonUtil;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.ServerException;
import io.minio.errors.XmlParserException;

@Service
public class FileService {
    private static final FileConfItem NOT_EXIST = new FileConfItem();

    @Resource
    private FileConfMapper fileConfMapper;

    private final LoadingCache<String, FileConfItem> cache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(5))
            .build(this::loadFileConf);

    public QryFileCredResp getFileCredential(QryFileCredReq req) {
        if (req == null) {
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        String appkey = RequestContext.getAppkeyFromCtx();
        if (!StringUtils.hasText(appkey)) {
            throw new JimException(JimErrorCode.ErrorCode_APP_APPKEY_REQUIRED);
        }
        FileConfItem item = cache.get(appkey);
        if (item == null || item == NOT_EXIST) {
            throw new JimException(JimErrorCode.ErrorCode_APP_FILE_NOOSS);
        }
        String ext = sanitizeExt(req.getExt());
        FileType fileType = FileType.fromCode(req.getFileType());
        String dir = fileTypeToDir(fileType);
        String objectKey = dir + "/" + CommonUtil.generageShortUuid() + "." + ext;
        switch (item.channel) {
            case "qiniu":
                return buildQiNiuResp(objectKey, item);
            case "minio":
                return buildMinioResp(objectKey, item);
            case "aws":
                return buildS3Resp(objectKey, item);
            case "oss":
                return buildOssResp(objectKey, item);
            default:
                throw new JimException(JimErrorCode.ErrorCode_APP_FILE_NOOSS);
        }
    }

    private QryFileCredResp buildQiNiuResp(String objectKey, FileConfItem item) {
        if (item.qiNiuStorage == null) {
            throw new JimException(JimErrorCode.ErrorCode_APP_FILE_NOOSS);
        }
        QryFileCredResp resp = new QryFileCredResp();
        resp.setOssType(OssType.QINIU.getCode());
        QiNiuCredResp qi = new QiNiuCredResp();
        qi.setDomain(item.qiNiuStorage.getDomain());
        qi.setToken(item.qiNiuStorage.createUploadToken());
        resp.setQiNiuCredResp(qi);
        return resp;
    }

    private QryFileCredResp buildMinioResp(String objectKey, FileConfItem item) {
        if (item.minioStorage == null) {
            throw new JimException(JimErrorCode.ErrorCode_APP_FILE_NOOSS);
        }
        try {
            String url = item.minioStorage.preSignedPutUrl(objectKey);
            QryFileCredResp resp = new QryFileCredResp();
            resp.setOssType(OssType.MINIO.getCode());
            PreSignResp preSign = new PreSignResp();
            preSign.setUrl(url);
            preSign.setObjKey(objectKey);
            resp.setPreSignResp(preSign);
            return resp;
        } catch (ErrorResponseException | InsufficientDataException | InternalException | InvalidResponseException
                | ServerException | XmlParserException e) {
            throw new JimException(JimErrorCode.ErrorCode_APP_FILE_SIGNERR, e.getMessage());
        }
    }

    private QryFileCredResp buildS3Resp(String objectKey, FileConfItem item) {
        if (item.s3Storage == null) {
            throw new JimException(JimErrorCode.ErrorCode_APP_FILE_NOOSS);
        }
        String url = item.s3Storage.preSignedPutUrl(objectKey);
        QryFileCredResp resp = new QryFileCredResp();
        resp.setOssType(OssType.S3.getCode());
        PreSignResp preSign = new PreSignResp();
        preSign.setUrl(url);
        preSign.setObjKey(objectKey);
        resp.setPreSignResp(preSign);
        return resp;
    }

    private QryFileCredResp buildOssResp(String objectKey, FileConfItem item) {
        if (item.ossStorage == null) {
            throw new JimException(JimErrorCode.ErrorCode_APP_FILE_NOOSS);
        }
        QryFileCredResp resp = new QryFileCredResp();
        resp.setOssType(OssType.OSS.getCode());
        String url = item.ossStorage.preSignedPutUrl(objectKey);
        OssPostPolicy policy = item.ossStorage.createPostPolicy(objectKey);
        PreSignResp preSign = new PreSignResp();
        preSign.setUrl(url);
        preSign.setObjKey(policy.getObjKey());
        preSign.setPolicy(policy.getPolicy());
        preSign.setCredential(policy.getCredential());
        preSign.setSignVersion(policy.getSignVersion());
        preSign.setDate(policy.getDate());
        preSign.setSignature(policy.getSignature());
        resp.setPreSignResp(preSign);
        return resp;
    }

    private FileConfItem loadFileConf(String appkey) {
        FileConf conf = fileConfMapper.findEnableFileConf(appkey);
        if (conf == null) {
            return NOT_EXIST;
        }
        FileConfItem item = new FileConfItem();
        item.channel = Optional.ofNullable(conf.getChannel()).map(s -> s.toLowerCase(Locale.ROOT)).orElse("");
        JSONObject json = JSON.parseObject(conf.getConf());
        switch (item.channel) {
            case "qiniu":
                item.qiNiuStorage = new QiNiuStorage(json.toJavaObject(QiNiuConfig.class));
                break;
            case "minio":
                item.minioStorage = new MinioStorage(json.toJavaObject(MinioConfig.class));
                break;
            case "aws":
                item.s3Storage = new S3Storage(json.toJavaObject(S3Config.class));
                break;
            case "oss":
                item.ossStorage = new OssStorage(json.toJavaObject(OssConfig.class));
                break;
            default:
                return NOT_EXIST;
        }
        return item;
    }

    private String sanitizeExt(String ext) {
        if (!StringUtils.hasText(ext)) {
            return "bin";
        }
        String clean = ext.trim();
        if (clean.startsWith(".")) {
            clean = clean.substring(1);
        }
        if (!StringUtils.hasText(clean)) {
            return "bin";
        }
        return clean.toLowerCase(Locale.ROOT);
    }

    private String fileTypeToDir(FileType type) {
        switch (type) {
            case IMAGE:
                return "images";
            case VIDEO:
                return "videos";
            case AUDIO:
                return "audios";
            case FILE:
                return "files";
            case LOG:
                return "logs";
            default:
                return "files";
        }
    }

    private static class FileConfItem {
        private String channel;
        private QiNiuStorage qiNiuStorage;
        private MinioStorage minioStorage;
        private S3Storage s3Storage;
        private OssStorage ossStorage;
    }
}
