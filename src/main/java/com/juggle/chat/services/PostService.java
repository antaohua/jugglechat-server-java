package com.juggle.chat.services;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.Resource;

import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.alibaba.fastjson.JSON;
import com.juggle.chat.apimodels.PostContent;
import com.juggle.chat.apimodels.PostIds;
import com.juggle.chat.apimodels.Posts;
import com.juggle.chat.exceptions.JimErrorCode;
import com.juggle.chat.exceptions.JimException;
import com.juggle.chat.interceptors.RequestContext;
import com.juggle.chat.mappers.PostFeedMapper;
import com.juggle.chat.mappers.PostMapper;
import com.juggle.chat.utils.CommonUtil;

@Service
public class PostService {
    private static final long DEFAULT_LIMIT = 20L;
    private static final long MAX_LIMIT = 100L;
    private static final int TOP_COMMENT_LIMIT = 10;
    private static final int TOP_REACTION_LIMIT = 100;

    @Resource
    private PostMapper postMapper;

    @Resource
    private PostFeedMapper postFeedMapper;

    @Resource
    private PostFeedService postFeedService;

    @Resource
    private PostCommentService postCommentService;

    @Resource
    private PostReactionService postReactionService;

    @Resource
    private UserService userService;

    @Resource
    private AppSettingService appSettingService;

    public com.juggle.chat.apimodels.Post addPost(com.juggle.chat.apimodels.Post req) {
        if (req == null || req.getContent() == null) {
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        String appkey = RequestContext.getAppkeyFromCtx();
        String userId = RequestContext.getCurrentUserIdFromCtx();
        if (!StringUtils.hasText(appkey) || !StringUtils.hasText(userId)) {
            throw new JimException(JimErrorCode.ErrorCode_APP_APPKEY_REQUIRED);
        }
        String postId = CommonUtil.generateUuid();
        long now = System.currentTimeMillis();
        com.juggle.chat.models.Post entity = new com.juggle.chat.models.Post();
        entity.setPostId(postId);
        entity.setContent(CommonUtil.toJson(req.getContent()).getBytes(StandardCharsets.UTF_8));
        entity.setUserId(userId);
        entity.setAppkey(appkey);
        entity.setCreatedTime(now);
        entity.setUpdatedTime(new Timestamp(now));
        entity.setIsDelete(0);
        entity.setStatus(0);
        int inserted = postMapper.create(entity);
        if (inserted <= 0) {
            throw new JimException(JimErrorCode.ErrorCode_APP_POST_DEFAULT);
        }
        postFeedService.appendPostFeed(entity);
        com.juggle.chat.apimodels.Post resp = new com.juggle.chat.apimodels.Post();
        resp.setPostId(postId);
        resp.setCreatedTime(now);
        return resp;
    }

    public void updatePost(com.juggle.chat.apimodels.Post req) {
        if (req == null || !StringUtils.hasText(req.getPostId()) || req.getContent() == null) {
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        String appkey = RequestContext.getAppkeyFromCtx();
        String userId = RequestContext.getCurrentUserIdFromCtx();
        com.juggle.chat.models.Post dbPost = postMapper.findById(appkey, req.getPostId());
        if (dbPost == null) {
            throw new JimException(JimErrorCode.ErrorCode_APP_POST_NOTEXISTED);
        }
        if (!userId.equals(dbPost.getUserId())) {
            throw new JimException(JimErrorCode.ErrorCode_APP_POST_NORIGHT);
        }
        String content = CommonUtil.toJson(req.getContent());
        int updated = postMapper.updateContent(appkey, req.getPostId(), content);
        if (updated <= 0) {
            throw new JimException(JimErrorCode.ErrorCode_APP_POST_DEFAULT);
        }
    }

    public void deletePosts(PostIds ids) {
        if (ids == null || CollectionUtils.isEmpty(ids.getPostIds())) {
            return;
        }
        String appkey = RequestContext.getAppkeyFromCtx();
        for (String postId : ids.getPostIds()) {
            if (!StringUtils.hasText(postId)) {
                continue;
            }
            postMapper.delete(appkey, postId);
        }
    }

    public Posts listPosts(Long start, Long limit, boolean isPositive) {
        long pageSize = (limit == null || limit <= 0) ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
        long startTime = start == null ? 0L : start;
        if (!isPositive && startTime <= 0) {
            startTime = System.currentTimeMillis();
        }
        long queryLimit = pageSize + 1;
        String appkey = RequestContext.getAppkeyFromCtx();
        String userId = RequestContext.getCurrentUserIdFromCtx();
        boolean friendMode = appSettingService.isFriendPostMode(appkey);
        List<com.juggle.chat.models.Post> dbPosts = new ArrayList<>();
        if (friendMode) {
            List<com.juggle.chat.models.Post> feeds = postFeedMapper.qryPosts(appkey, userId, startTime, queryLimit,
                    isPositive);
            if (!CollectionUtils.isEmpty(feeds)) {
                dbPosts.addAll(feeds);
            }
        } else {
            List<com.juggle.chat.models.Post> posts = postMapper.qryPosts(appkey, startTime, queryLimit, isPositive);
            if (!CollectionUtils.isEmpty(posts)) {
                dbPosts.addAll(posts);
            }
        }
        List<com.juggle.chat.apimodels.Post> items = new ArrayList<>();
        if (!CollectionUtils.isEmpty(dbPosts)) {
            for (com.juggle.chat.models.Post post : dbPosts) {
                if (post != null) {
                    items.add(toApiPost(post));
                }
            }
        }
        boolean finished = true;
        if (items.size() > pageSize) {
            items = new ArrayList<>(items.subList(0, (int) pageSize));
            finished = false;
        }
        Posts resp = new Posts();
        resp.setItems(items);
        resp.setFinished(finished);
        return resp;
    }

    public com.juggle.chat.apimodels.Post getPostInfo(String postId) {
        if (!StringUtils.hasText(postId)) {
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        String appkey = RequestContext.getAppkeyFromCtx();
        com.juggle.chat.models.Post dbPost = postMapper.findById(appkey, postId);
        if (dbPost == null) {
            return new com.juggle.chat.apimodels.Post();
        }
        return toApiPost(dbPost);
    }

    private com.juggle.chat.apimodels.Post toApiPost(com.juggle.chat.models.Post post) {
        com.juggle.chat.apimodels.Post resp = new com.juggle.chat.apimodels.Post();
        resp.setPostId(post.getPostId());
        resp.setContent(parseContent(post.getContent()));
        resp.setUserInfo(userService.getUserInfo(post.getUserId()));
        resp.setCreatedTime(post.getCreatedTime() == null ? 0 : post.getCreatedTime());
        resp.setUpdatedTime(post.getUpdatedTime() == null ? 0 : post.getUpdatedTime().getTime());
        resp.setTopComments(postCommentService.topComments(post.getPostId(), TOP_COMMENT_LIMIT));
        resp.setReactions(postReactionService.topReactions(post.getPostId(), TOP_REACTION_LIMIT));
        return resp;
    }

    private PostContent parseContent(byte[] content) {
        if (content == null || content.length == 0) {
            return null;
        }
        try {
            return JSON.parseObject(new String(content, StandardCharsets.UTF_8), PostContent.class);
        } catch (Exception ex) {
            return null;
        }
    }
}
