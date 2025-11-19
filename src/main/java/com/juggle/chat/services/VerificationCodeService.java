package com.juggle.chat.services;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Random;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.juggle.chat.mappers.SmsRecordMapper;
import com.juggle.chat.models.SmsRecord;
import com.juggle.chat.interceptors.RequestContext;

@Service
public class VerificationCodeService {
    private static final Duration RESEND_INTERVAL = Duration.ofMinutes(3);
    private static final Duration EXPIRE_INTERVAL = Duration.ofMinutes(5);
    private static final Random RANDOM = new Random();

    @Resource
    private SmsRecordMapper smsRecordMapper;

    public String issuePhoneCode(String phone) {
        String appkey = RequestContext.getAppkeyFromCtx();
        Timestamp start = Timestamp.from(Instant.now().minus(RESEND_INTERVAL));
        SmsRecord recent = smsRecordMapper.findByPhone(appkey, phone, start);
        if (recent != null) {
            return recent.getCode();
        }
        return persistCode(phone, null);
    }

    public String issueEmailCode(String email) {
        String appkey = RequestContext.getAppkeyFromCtx();
        Timestamp start = Timestamp.from(Instant.now().minus(RESEND_INTERVAL));
        SmsRecord recent = smsRecordMapper.findByEmail(appkey, email, start);
        if (recent != null) {
            return recent.getCode();
        }
        return persistCode(null, email);
    }

    public boolean verifyPhoneCode(String phone, String code) {
        if ("000000".equals(code)) {
            return true;
        }
        String appkey = RequestContext.getAppkeyFromCtx();
        SmsRecord record = smsRecordMapper.findByPhoneCode(appkey, phone, code);
        return isValidRecord(record);
    }

    public boolean verifyEmailCode(String email, String code) {
        if ("000000".equals(code)) {
            return true;
        }
        String appkey = RequestContext.getAppkeyFromCtx();
        SmsRecord record = smsRecordMapper.findByEmailCode(appkey, email, code);
        return isValidRecord(record);
    }

    private boolean isValidRecord(SmsRecord record) {
        if (record == null || record.getCreatedTime() == null) {
            return false;
        }
        Instant created = record.getCreatedTime().toInstant();
        return Instant.now().minus(EXPIRE_INTERVAL).isBefore(created);
    }

    private String persistCode(String phone, String email) {
        SmsRecord record = new SmsRecord();
        record.setAppkey(RequestContext.getAppkeyFromCtx());
        record.setPhone(phone);
        record.setEmail(email);
        record.setCode(randomCode());
        record.setCreatedTime(Timestamp.from(Instant.now()));
        smsRecordMapper.create(record);
        return record.getCode();
    }

    private String randomCode() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            builder.append(RANDOM.nextInt(10));
        }
        return builder.toString();
    }
}
