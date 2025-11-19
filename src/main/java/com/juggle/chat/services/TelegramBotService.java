package com.juggle.chat.services;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import jakarta.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import com.google.protobuf.ServiceException;
import com.juggle.chat.apimodels.TelegramBot;
import com.juggle.chat.apimodels.TelegramBotIds;
import com.juggle.chat.apimodels.TelegramBots;
import com.juggle.chat.config.BotConnectorProperties;
import com.juggle.chat.exceptions.JimErrorCode;
import com.juggle.chat.exceptions.JimException;
import com.juggle.chat.interceptors.RequestContext;
import com.juggle.chat.mappers.TeleBotMapper;
import com.juggle.chat.models.AppInfo;
import com.juggle.chat.models.TeleBot;
import com.juggle.chat.services.AppInfoCache;
import com.juggle.chat.utils.N3d;

@Service
public class TelegramBotService {
    private static final Logger log = LoggerFactory.getLogger(TelegramBotService.class);

    @Resource
    private TeleBotMapper teleBotMapper;

    @Resource
    private BotConnectorProperties botConnectorProperties;

    private final RestTemplate restTemplate = new RestTemplate();

    public TelegramBot addBot(TelegramBot req) {
        if (req == null || !StringUtils.hasText(req.getBotToken()) || !StringUtils.hasText(req.getBotName())) {
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        String appkey = RequestContext.getAppkeyFromCtx();
        String userId = RequestContext.getCurrentUserIdFromCtx();
        TeleBot bot = new TeleBot();
        bot.setAppkey(appkey);
        bot.setUserId(userId);
        bot.setBotName(req.getBotName());
        bot.setBotToken(req.getBotToken());
        bot.setStatus(1);
        teleBotMapper.create(bot);
        activate(bot);
        return toApi(bot);
    }

    public void deleteBot(TelegramBot req) {
        if (req == null || !StringUtils.hasText(req.getBotId())) {
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        TelegramBotIds ids = new TelegramBotIds();
        ids.setBotIds(List.of(req.getBotId()));
        batchDelete(ids);
    }

    public void batchDelete(TelegramBotIds ids) {
        if (ids == null || ids.getBotIds() == null || ids.getBotIds().isEmpty()) {
            return;
        }
        String appkey = RequestContext.getAppkeyFromCtx();
        String userId = RequestContext.getCurrentUserIdFromCtx();
        List<Long> decodedIds = new ArrayList<>();
        for (String id : ids.getBotIds()) {
            if (StringUtils.hasText(id)) {
                decodedIds.add(decode(id));
            }
        }
        if (decodedIds.isEmpty()) {
            return;
        }
        for (Long id : decodedIds) {
            TeleBot bot = teleBotMapper.findById(id, appkey, userId);
            if (bot != null) {
                deactivate(bot);
            }
        }
        teleBotMapper.batchDelete(appkey, userId, decodedIds);
    }

    public TelegramBots listBots(int count, String offset) {
        int pageSize = Math.min(Math.max(count, 1), 100);
        long startId = 0;
        if (StringUtils.hasText(offset)) {
            startId = decode(offset);
        }
        List<TeleBot> bots = teleBotMapper.qryTeleBots(RequestContext.getAppkeyFromCtx(),
                RequestContext.getCurrentUserIdFromCtx(), startId, pageSize);
        TelegramBots resp = new TelegramBots();
        List<TelegramBot> items = new ArrayList<>();
        String nextOffset = null;
        if (bots != null) {
            for (TeleBot bot : bots) {
                items.add(toApi(bot));
                nextOffset = encode(bot.getId());
            }
        }
        resp.setItems(items);
        resp.setOffset(nextOffset);
        return resp;
    }

    private TelegramBot toApi(TeleBot bot) {
        TelegramBot resp = new TelegramBot();
        resp.setBotId(encode(bot.getId()));
        resp.setBotName(bot.getBotName());
        resp.setBotToken(bot.getBotToken());
        resp.setCreatedTime(bot.getCreatedTime() == null ? 0 : bot.getCreatedTime().getTime());
        return resp;
    }

    private void activate(TeleBot bot) {
        sendConnectorRequest("/bot-connector/telebot/add", bot);
    }

    private void deactivate(TeleBot bot) {
        sendConnectorRequest("/bot-connector/telebot/del", bot);
    }

    private void sendConnectorRequest(String path, TeleBot bot) {
        if (!StringUtils.hasText(botConnectorProperties.getDomain())) {
            return;
        }
        String url = botConnectorProperties.getDomain() + path;
        TeleBotRel rel = new TeleBotRel();
        rel.setAppKey(bot.getAppkey());
        rel.setUserId(bot.getUserId());
        rel.setBotToken(bot.getBotToken());
        rel.setTeleBotId(extractBotId(bot));
        HttpHeaders headers = buildHeaders(bot.getAppkey());
        HttpEntity<String> entity = new HttpEntity<>(com.juggle.chat.utils.CommonUtil.toJson(rel), headers);
        try {
            ResponseEntity<String> resp = restTemplate.postForEntity(url, entity, String.class);
            log.debug("telebot connector resp: {} {}", resp.getStatusCode(), resp.getBody());
        } catch (Exception ex) {
            log.warn("telebot connector call failed: {}", ex.getMessage());
        }
    }

    private HttpHeaders buildHeaders(String appkey) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("appkey", appkey);
        AppInfo appInfo = AppInfoCache.getAppInfo(appkey);
        if (appInfo != null && StringUtils.hasText(appInfo.getAppSecret())) {
            String nonce = String.valueOf(ThreadLocalRandom.current().nextInt(100000));
            String timestamp = String.valueOf(System.currentTimeMillis());
            String signature = sha1(appInfo.getAppSecret() + nonce + timestamp);
            headers.add("nonce", nonce);
            headers.add("timestamp", timestamp);
            headers.add("signature", signature);
        }
        return headers;
    }

    private String sha1(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] bs = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(bs);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 missing", e);
        }
    }

    private String extractBotId(TeleBot bot) {
        if (StringUtils.hasText(bot.getBotToken()) && bot.getBotToken().contains(":")) {
            return bot.getBotToken().split(":", 2)[0];
        }
        return encode(bot.getId());
    }

    private long decode(String value) {
        try {
            return N3d.decode(value);
        } catch (ServiceException e) {
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
    }

    private String encode(Long id) {
        if (id == null || id <= 0) {
            return null;
        }
        try {
            return N3d.encode(id);
        } catch (ServiceException e) {
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
    }

    private static class TeleBotRel {
        private String appKey;
        private String teleBotId;
        private String userId;
        private String botToken;

        public String getAppKey() {
            return appKey;
        }

        public void setAppKey(String appKey) {
            this.appKey = appKey;
        }

        public String getTeleBotId() {
            return teleBotId;
        }

        public void setTeleBotId(String teleBotId) {
            this.teleBotId = teleBotId;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getBotToken() {
            return botToken;
        }

        public void setBotToken(String botToken) {
            this.botToken = botToken;
        }
    }
}
