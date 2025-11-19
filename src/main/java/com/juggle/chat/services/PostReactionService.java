package com.juggle.chat.services;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.annotation.Resource;

import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.juggle.chat.apimodels.PostReaction;
import com.juggle.chat.apimodels.PostReactions;
import com.juggle.chat.exceptions.JimErrorCode;
import com.juggle.chat.exceptions.JimException;
import com.juggle.chat.interceptors.RequestContext;
import com.juggle.chat.mappers.PostReactionMapper;
import com.juggle.chat.models.PostBusType;

@Service
public class PostReactionService {
    private static final int DEFAULT_LIMIT = 100;

    @Resource
    private PostReactionMapper postReactionMapper;

    @Resource
    private UserService userService;

    public void addReaction(PostReaction req) {
        if (req == null || !StringUtils.hasText(req.getPostId()) || !StringUtils.hasText(req.getKey())) {
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        String appkey = RequestContext.getAppkeyFromCtx();
        String userId = RequestContext.getCurrentUserIdFromCtx();
        com.juggle.chat.models.PostReaction reaction = new com.juggle.chat.models.PostReaction();
        reaction.setAppkey(appkey);
        reaction.setBusId(req.getPostId());
        reaction.setBusType(PostBusType.POST.getCode());
        reaction.setItemKey(req.getKey());
        reaction.setItemValue(req.getValue());
        reaction.setUserId(userId);
        reaction.setCreatedTime(System.currentTimeMillis());
        int updated = postReactionMapper.upsert(reaction);
        if (updated <= 0) {
            throw new JimException(JimErrorCode.ErrorCode_APP_POST_DEFAULT);
        }
    }

    public void deleteReaction(PostReaction req) {
        if (req == null || !StringUtils.hasText(req.getPostId()) || !StringUtils.hasText(req.getKey())) {
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        String appkey = RequestContext.getAppkeyFromCtx();
        String userId = RequestContext.getCurrentUserIdFromCtx();
        postReactionMapper.delete(appkey, req.getPostId(), PostBusType.POST.getCode(), req.getKey(), userId);
    }

    public PostReactions listReactions(String postId) {
        if (!StringUtils.hasText(postId)) {
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        String appkey = RequestContext.getAppkeyFromCtx();
        List<com.juggle.chat.models.PostReaction> records = postReactionMapper.qryReactions(appkey, postId,
                PostBusType.POST.getCode(), DEFAULT_LIMIT);
        List<PostReaction> items = new ArrayList<>();
        if (!CollectionUtils.isEmpty(records)) {
            for (com.juggle.chat.models.PostReaction record : records) {
                items.add(buildApiReaction(record));
            }
        }
        PostReactions resp = new PostReactions();
        resp.setReactions(items);
        return resp;
    }

    public Map<String, List<PostReaction>> topReactions(String postId, int limit) {
        Map<String, List<PostReaction>> result = new LinkedHashMap<>();
        if (!StringUtils.hasText(postId)) {
            return result;
        }
        String appkey = RequestContext.getAppkeyFromCtx();
        int size = limit <= 0 ? DEFAULT_LIMIT : Math.min(limit, DEFAULT_LIMIT);
        List<com.juggle.chat.models.PostReaction> records = postReactionMapper.qryReactions(appkey, postId,
                PostBusType.POST.getCode(), size);
        if (!CollectionUtils.isEmpty(records)) {
            for (com.juggle.chat.models.PostReaction record : records) {
                PostReaction api = buildApiReaction(record);
                result.computeIfAbsent(record.getItemKey(), key -> new ArrayList<>()).add(api);
            }
        }
        return result;
    }

    private PostReaction buildApiReaction(com.juggle.chat.models.PostReaction reaction) {
        PostReaction api = new PostReaction();
        api.setPostId(reaction.getBusId());
        api.setKey(reaction.getItemKey());
        api.setValue(reaction.getItemValue());
        api.setTimestamp(reaction.getCreatedTime() == null ? 0 : reaction.getCreatedTime());
        api.setUserInfo(userService.getUserInfo(reaction.getUserId()));
        return api;
    }
}
