package com.juggle.chat.services;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.juggle.chat.models.PostBusType;

@Service
public class PostNotifyService {
    private static final Logger log = LoggerFactory.getLogger(PostNotifyService.class);

    public void notifyTargets(String appkey, String sponsorId, PostBusType type, List<String> targetIds) {
        if (CollectionUtils.isEmpty(targetIds)) {
            return;
        }
        // The IM SDK dependency is not available, so we log the intent for now.
        log.debug("post notify appkey={} sponsor={} type={} targets={}", appkey, sponsorId, type, targetIds.size());
    }
}
