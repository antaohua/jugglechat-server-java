package com.juggle.chat.services;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import jakarta.annotation.Resource;

import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.juggle.chat.mappers.AppInfoExtMapper;
import com.juggle.chat.models.AppInfoExt;

@Service
public class AppSettingService {
    private static final List<String> POST_KEYS = Arrays.asList("post_mode", "jchat_post_mod");

    @Resource
    private AppInfoExtMapper appInfoExtMapper;

    private final LoadingCache<String, Boolean> postModeCache = Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(Duration.ofMinutes(10))
            .build(this::loadPostMode);

    public boolean isFriendPostMode(String appkey) {
        if (appkey == null) {
            return false;
        }
        return postModeCache.get(appkey);
    }

    private boolean loadPostMode(String appkey) {
        List<AppInfoExt> exts = appInfoExtMapper.findByItemKeys(appkey, POST_KEYS);
        if (CollectionUtils.isEmpty(exts)) {
            return false;
        }
        for (AppInfoExt ext : exts) {
            if ("post_mode".equals(ext.getAppItemKey()) && "1".equals(ext.getAppItemValue())) {
                return true;
            }
            if ("jchat_post_mod".equals(ext.getAppItemKey()) && "friend".equalsIgnoreCase(ext.getAppItemValue())) {
                return true;
            }
        }
        return false;
    }
}
