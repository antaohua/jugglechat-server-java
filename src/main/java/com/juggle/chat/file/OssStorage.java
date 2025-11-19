package com.juggle.chat.file;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.alibaba.fastjson.JSON;
import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;

public class OssStorage {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
            .withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter EXPIRATION = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .withZone(ZoneOffset.UTC);

    private final OssConfig config;

    public OssStorage(OssConfig config) {
        this.config = config;
    }

    public String preSignedPutUrl(String objectKey) {
        OSS client = new OSSClientBuilder().build(config.getEndpoint(), config.getAccessKey(), config.getSecretKey());
        try {
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(config.getBucket(), objectKey);
            request.setMethod(HttpMethod.PUT);
            request.setExpiration(new Date(System.currentTimeMillis() + 15 * 60 * 1000));
            URL url = client.generatePresignedUrl(request);
            return url.toString();
        } finally {
            client.shutdown();
        }
    }

    public OssPostPolicy createPostPolicy(String objectKey) {
        OssPostPolicy policy = new OssPostPolicy();
        policy.setObjKey(objectKey);
        Instant now = Instant.now();
        String date = DATE.format(now);
        String iso8601 = EXPIRATION.format(now.plusSeconds(3600));
        Map<String, Object> policyMap = new LinkedHashMap<>();
        policyMap.put("expiration", iso8601);
        List<Object> conditions = new ArrayList<>();
        Map<String, String> bucketCondition = new LinkedHashMap<>();
        bucketCondition.put("bucket", config.getBucket());
        conditions.add(bucketCondition);
        conditions.add(new Object[] {"content-length-range", 1, 1024 * 1024 * 1024});
        policyMap.put("conditions", conditions);
        String policyJson = JSON.toJSONString(policyMap);
        String encodedPolicy = Base64.getEncoder().encodeToString(policyJson.getBytes(StandardCharsets.UTF_8));
        policy.setPolicy(encodedPolicy);
        String credential = String.format("%s/%s/%s/oss/aliyun_v4_request", config.getAccessKey(), date, config.getRegion());
        policy.setCredential(credential);
        policy.setSignVersion("OSS4-HMAC-SHA256");
        policy.setDate(DATE_TIME.format(now));
        policy.setSignature(sign(encodedPolicy, date));
        return policy;
    }

    private String sign(String encodedPolicy, String date) {
        try {
            byte[] kDate = hmac("aliyun_v4" + config.getSecretKey(), date);
            byte[] kRegion = hmac(kDate, config.getRegion());
            byte[] kService = hmac(kRegion, "oss");
            byte[] kSigning = hmac(kService, "aliyun_v4_request");
            byte[] signature = hmac(kSigning, encodedPolicy);
            StringBuilder sb = new StringBuilder();
            for (byte b : signature) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Failed to sign OSS policy", e);
        }
    }

    private byte[] hmac(String key, String data) throws NoSuchAlgorithmException, InvalidKeyException {
        return hmac(key.getBytes(StandardCharsets.UTF_8), data);
    }

    private byte[] hmac(byte[] key, String data) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }
}
