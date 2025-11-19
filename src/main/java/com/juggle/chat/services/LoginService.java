package com.juggle.chat.services;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.regex.Pattern;

import jakarta.annotation.Resource;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.juggle.chat.apimodels.EmailLoginReq;
import com.juggle.chat.apimodels.LoginReq;
import com.juggle.chat.apimodels.LoginUserResp;
import com.juggle.chat.apimodels.QrCode;
import com.juggle.chat.apimodels.QrCodeReq;
import com.juggle.chat.apimodels.RegisterReq;
import com.juggle.chat.apimodels.SmsLoginReq;
import com.juggle.chat.exceptions.JimErrorCode;
import com.juggle.chat.exceptions.JimException;
import com.juggle.chat.interceptors.RequestContext;
import com.juggle.chat.mappers.QrCodeRecordMapper;
import com.juggle.chat.mappers.UserExtMapper;
import com.juggle.chat.mappers.UserMapper;
import com.juggle.chat.models.AppInfo;
import com.juggle.chat.models.AuthToken;
import com.juggle.chat.models.QrCodeRecord;
import com.juggle.chat.models.User;
import com.juggle.chat.models.UserExt;
import com.juggle.chat.models.UserExtKeys;
import com.juggle.chat.utils.CommonUtil;
import com.juggle.im.JuggleIm;
import com.juggle.im.models.user.UserInfo;
import com.juggle.im.models.user.UserTokenResult;

@Service
public class LoginService {
    private static final Pattern ACCOUNT_PATTERN = Pattern.compile("^[A-Za-z0-9]{6,20}$");

    @Resource
    private UserMapper userMapper;
    @Resource
    private UserExtMapper userExtMapper;
    @Resource
    private QrCodeRecordMapper qrCodeRecordMapper;
    @Resource
    private VerificationCodeService verificationCodeService;

    public LoginUserResp login(LoginReq req) {
        if (req == null || !StringUtils.hasText(req.getPassword())
                || (!StringUtils.hasText(req.getAccount()) && !StringUtils.hasText(req.getPhone())
                        && !StringUtils.hasText(req.getEmail()))) {
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        String appkey = RequestContext.getAppkeyFromCtx();
        User user = null;
        if (StringUtils.hasText(req.getAccount())) {
            user = userMapper.findByAccount(appkey, req.getAccount());
        } else if (StringUtils.hasText(req.getPhone())) {
            user = userMapper.findByPhone(appkey, req.getPhone());
        } else {
            user = userMapper.findByEmail(appkey, req.getEmail());
        }
        if (user == null) {
            throw new JimException(JimErrorCode.ErrorCode_APP_USER_NOT_EXIST);
        }
        String hashed = CommonUtil.sha1(req.getPassword());
        if (user.getLoginPass() == null || !user.getLoginPass().equals(hashed)) {
            throw new JimException(JimErrorCode.ErrorCode_APP_LOGIN_ERR_PASS);
        }
        return buildLoginResp(appkey, user);
    }

    public void register(RegisterReq req) {
        if (req == null || !StringUtils.hasText(req.getPassword())
                || (!StringUtils.hasText(req.getAccount()) && !StringUtils.hasText(req.getPhone())
                        && !StringUtils.hasText(req.getEmail()))) {
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        String appkey = RequestContext.getAppkeyFromCtx();
        User user = new User();
        user.setUserId(CommonUtil.generageShortUuid());
        user.setNickname(String.format("user%05d", CommonUtil.randomInt()));
        user.setAppkey(appkey);
        user.setLoginPass(CommonUtil.sha1(req.getPassword()));
        if (StringUtils.hasText(req.getAccount())) {
            validateAccount(req.getAccount());
            User existed = userMapper.findByAccount(appkey, req.getAccount());
            if (existed != null) {
                throw new JimException(JimErrorCode.ErrorCode_APP_USER_EXISTED);
            }
            user.setLoginAccount(req.getAccount());
        } else if (StringUtils.hasText(req.getPhone())) {
            if (!verificationCodeService.verifyPhoneCode(req.getPhone(), req.getCode())) {
                throw new JimException(JimErrorCode.ErrorCode_APP_SMS_CODE_EXPIRED);
            }
            if (userMapper.findByPhone(appkey, req.getPhone()) != null) {
                throw new JimException(JimErrorCode.ErrorCode_APP_PHONE_EXISTED);
            }
            user.setPhone(req.getPhone());
        } else if (StringUtils.hasText(req.getEmail())) {
            if (!verificationCodeService.verifyEmailCode(req.getEmail(), req.getCode())) {
                throw new JimException(JimErrorCode.ErrorCode_APP_SMS_CODE_EXPIRED);
            }
            if (userMapper.findByEmail(appkey, req.getEmail()) != null) {
                throw new JimException(JimErrorCode.ErrorCode_APP_EMAIL_EXIST);
            }
            user.setEmail(req.getEmail());
        }
        try {
            userMapper.create(user);
        } catch (DuplicateKeyException ex) {
            throw new JimException(JimErrorCode.ErrorCode_APP_USER_EXISTED);
        }
        ensureDefaultSettings(appkey, user.getUserId());
    }

    public void smsSend(SmsLoginReq req) {
        if (req == null || !StringUtils.hasText(req.getPhone())) {
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        verificationCodeService.issuePhoneCode(req.getPhone());
    }

    public LoginUserResp smsLogin(String phone, String code) {
        if (!verificationCodeService.verifyPhoneCode(phone, code)) {
            throw new JimException(JimErrorCode.ErrorCode_APP_SMS_CODE_EXPIRED);
        }
        String appkey = RequestContext.getAppkeyFromCtx();
        User user = userMapper.findByPhone(appkey, phone);
        if (user == null) {
            user = new User();
            user.setPhone(phone);
            user.setAppkey(appkey);
            user.setUserId(CommonUtil.generageShortUuid());
            user.setNickname("user" + CommonUtil.randomInt());
            userMapper.upsert(user);
            ensureDefaultSettings(appkey, user.getUserId());
        }
        return buildLoginResp(appkey, user);
    }

    public void emailSend(EmailLoginReq req) {
        if (req == null || !StringUtils.hasText(req.getEmail())) {
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        if (userMapper.findByEmail(RequestContext.getAppkeyFromCtx(), req.getEmail()) != null) {
            throw new JimException(JimErrorCode.ErrorCode_APP_EMAIL_EXIST);
        }
        verificationCodeService.issueEmailCode(req.getEmail());
    }

    public LoginUserResp emailLogin(EmailLoginReq req) {
        if (req == null || !StringUtils.hasText(req.getEmail())) {
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        if (!verificationCodeService.verifyEmailCode(req.getEmail(), req.getCode())) {
            throw new JimException(JimErrorCode.ErrorCode_APP_SMS_CODE_EXPIRED);
        }
        String appkey = RequestContext.getAppkeyFromCtx();
        User user = userMapper.findByEmail(appkey, req.getEmail());
        if (user == null) {
            user = new User();
            user.setAppkey(appkey);
            user.setUserId(CommonUtil.generageShortUuid());
            user.setNickname("user" + CommonUtil.randomInt());
            user.setEmail(req.getEmail());
            userMapper.upsert(user);
            ensureDefaultSettings(appkey, user.getUserId());
        }
        return buildLoginResp(appkey, user);
    }

    public QrCode generateQrCode() {
        String codeId = CommonUtil.generateUuid();
        QrCodeRecord record = new QrCodeRecord();
        record.setAppkey(RequestContext.getAppkeyFromCtx());
        record.setCodeId(codeId);
        record.setStatus(QrCodeRecord.STATUS_DEFAULT);
        record.setCreatedTime(System.currentTimeMillis());
        qrCodeRecordMapper.create(record);
        try {
            BitMatrix matrix = new MultiFormatWriter().encode(
                    CommonUtil.toJson(java.util.Map.of("action", "login", "code", codeId)),
                    BarcodeFormat.QR_CODE, 400, 400);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "png", baos);
            QrCode qrCode = new QrCode();
            qrCode.setId(codeId);
            qrCode.setQrCode(Base64.getEncoder().encodeToString(baos.toByteArray()));
            return qrCode;
        } catch (Exception ex) {
            throw new JimException(JimErrorCode.ErrorCode_AppDefault);
        }
    }

    public LoginUserResp checkQrCode(QrCodeReq req) {
        if (req == null || !StringUtils.hasText(req.getId())) {
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        String appkey = RequestContext.getAppkeyFromCtx();
        QrCodeRecord record = qrCodeRecordMapper.findById(appkey, req.getId());
        if (record == null || record.getCreatedTime() == null
                || System.currentTimeMillis() - record.getCreatedTime() > 10 * 60 * 1000L) {
            throw new JimException(JimErrorCode.ErrorCode_APP_QRCODE_EXPIRED);
        }
        if (record.getStatus() == QrCodeRecord.STATUS_OK) {
            User user = userMapper.findByUserId(appkey, record.getUserId());
            if (user == null) {
                throw new JimException(JimErrorCode.ErrorCode_APP_USER_NOT_EXIST);
            }
            return buildLoginResp(appkey, user);
        }
        throw new JimException(JimErrorCode.ErrorCode_APP_CONTINUE);
    }

    public void confirmQrCode(QrCodeReq req) {
        if (req == null || !StringUtils.hasText(req.getId())) {
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        String appkey = RequestContext.getAppkeyFromCtx();
        String userId = RequestContext.getCurrentUserIdFromCtx();
        int updated = qrCodeRecordMapper.updateStatus(appkey, req.getId(), QrCodeRecord.STATUS_OK, userId);
        if (updated <= 0) {
            throw new JimException(JimErrorCode.ErrorCode_AppDefault);
        }
    }

    private void validateAccount(String account) {
        if (!ACCOUNT_PATTERN.matcher(account).matches()) {
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
    }

    private void ensureDefaultSettings(String appkey, String userId) {
        userExtMapper.upsert(new UserExt(appkey, userId, UserExtKeys.UserExtKey_FriendVerifyType,
                CommonUtil.int2String(UserExtKeys.FriendVerifyType_Need), UserExtKeys.AttItemType_Setting));
    }

    private LoginUserResp buildLoginResp(String appkey, User user) {
        LoginUserResp resp = new LoginUserResp();
        resp.setUserId(user.getUserId());
        resp.setNickname(user.getNickname());
        resp.setAvatar(user.getUserPortrait());
        resp.setStatus(0);
        resp.setAuthorization(generateAuthToken(appkey, user.getUserId()));
        JuggleIm imSdk = ImSdkService.getJimSdk(appkey);
        if (imSdk == null) {
            throw new JimException(JimErrorCode.ErrorCode_APP_NOT_EXISTED);
        }
        try {
            UserTokenResult result = imSdk.user
                    .register(new UserInfo(user.getUserId(), user.getNickname(), user.getUserPortrait()));
            resp.setImToken(result.getUserToken().getToken());
        } catch (Exception e) {
            throw new JimException(JimErrorCode.ErrorCode_APP_INTERNAL_TIMEOUT);
        }
        return resp;
    }

    public String generateAuthToken(String appkey, String userId) {
        AuthToken aToken = new AuthToken(appkey, userId, "", System.currentTimeMillis());
        AppInfo appInfo = AppInfoCache.getAppInfo(appkey);
        if (appInfo != null) {
            try {
                return AuthTokenService.toTokenString(aToken, appInfo.getAppSecureKey());
            } catch (Exception e) {
                throw new JimException(JimErrorCode.ErrorCode_APP_INTERNAL_TIMEOUT);
            }
        }
        throw new JimException(JimErrorCode.ErrorCode_APP_NOT_EXISTED);
    }
}
