package com.juggle.chat.services;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.Resource;

import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.juggle.chat.apimodels.PostComment;
import com.juggle.chat.apimodels.PostCommentIds;
import com.juggle.chat.apimodels.PostComments;
import com.juggle.chat.exceptions.JimErrorCode;
import com.juggle.chat.exceptions.JimException;
import com.juggle.chat.interceptors.RequestContext;
import com.juggle.chat.mappers.PostCommentFeedMapper;
import com.juggle.chat.mappers.PostCommentMapper;
import com.juggle.chat.utils.CommonUtil;

@Service
public class PostCommentService {
    private static final long DEFAULT_LIMIT = 20L;
    private static final long MAX_LIMIT = 100L;

    @Resource
    private PostCommentMapper postCommentMapper;

    @Resource
    private PostCommentFeedMapper postCommentFeedMapper;

    @Resource
    private PostFeedService postFeedService;

    @Resource
    private UserService userService;

    @Resource
    private AppSettingService appSettingService;

    public PostComment addComment(PostComment req) {
        if (req == null || !StringUtils.hasText(req.getPostId())) {
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        String appkey = RequestContext.getAppkeyFromCtx();
        String userId = RequestContext.getCurrentUserIdFromCtx();
        String parentUserId = req.getParentUserId();
        if (!StringUtils.hasText(parentUserId) && req.getParentUserInfo() != null) {
            parentUserId = req.getParentUserInfo().getUserId();
        }
        long now = System.currentTimeMillis();
        com.juggle.chat.models.PostComment entity = new com.juggle.chat.models.PostComment();
        entity.setCommentId(CommonUtil.generateUuid());
        entity.setPostId(req.getPostId());
        entity.setParentCommentId(req.getParentCommentId());
        entity.setParentUserId(parentUserId);
        entity.setText(req.getText());
        entity.setUserId(userId);
        entity.setAppkey(appkey);
        entity.setCreatedTime(now);
        entity.setUpdatedTime(new Timestamp(now));
        entity.setIsDelete(0);
        entity.setStatus(0);
        int inserted = postCommentMapper.create(entity);
        if (inserted <= 0) {
            throw new JimException(JimErrorCode.ErrorCode_APP_POST_DEFAULT);
        }
        postFeedService.appendCommentFeed(entity);
        PostComment resp = new PostComment();
        resp.setCommentId(entity.getCommentId());
        resp.setPostId(entity.getPostId());
        resp.setCreatedTime(now);
        return resp;
    }

    public void updateComment(PostComment req) {
        if (req == null || !StringUtils.hasText(req.getCommentId()) || !StringUtils.hasText(req.getText())) {
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        String appkey = RequestContext.getAppkeyFromCtx();
        String userId = RequestContext.getCurrentUserIdFromCtx();
        com.juggle.chat.models.PostComment dbComment = postCommentMapper.findById(appkey, req.getCommentId());
        if (dbComment == null) {
            throw new JimException(JimErrorCode.ErrorCode_APP_POST_NOTEXISTED);
        }
        if (!userId.equals(dbComment.getUserId())) {
            throw new JimException(JimErrorCode.ErrorCode_APP_POST_NORIGHT);
        }
        int updated = postCommentMapper.updateComment(appkey, req.getCommentId(), req.getText());
        if (updated <= 0) {
            throw new JimException(JimErrorCode.ErrorCode_APP_POST_DEFAULT);
        }
    }

    public void deleteComments(PostCommentIds ids) {
        if (ids == null || CollectionUtils.isEmpty(ids.getCommentIds())) {
            return;
        }
        String appkey = RequestContext.getAppkeyFromCtx();
        for (String commentId : ids.getCommentIds()) {
            if (!StringUtils.hasText(commentId)) {
                continue;
            }
            postCommentMapper.delete(appkey, commentId);
        }
    }

    public PostComments listComments(String postId, Long start, Long limit, boolean isPositive) {
        if (!StringUtils.hasText(postId)) {
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        long pageSize = (limit == null || limit <= 0) ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
        long startTime = normalizeStart(start == null ? 0L : start, isPositive);
        long queryLimit = pageSize + 1;
        String appkey = RequestContext.getAppkeyFromCtx();
        String userId = RequestContext.getCurrentUserIdFromCtx();
        boolean friendMode = appSettingService.isFriendPostMode(appkey);
        List<com.juggle.chat.models.PostComment> records;
        if (friendMode) {
            records = postCommentFeedMapper.qryPostComments(appkey, userId, postId, startTime, queryLimit, isPositive);
        } else {
            records = postCommentMapper.qryPostComments(appkey, postId, startTime, queryLimit, isPositive);
        }
        List<PostComment> items = new ArrayList<>();
        if (!CollectionUtils.isEmpty(records)) {
            for (com.juggle.chat.models.PostComment record : records) {
                items.add(buildApiComment(record));
            }
        }
        boolean finished = true;
        if (items.size() > pageSize) {
            items = new ArrayList<>(items.subList(0, (int) pageSize));
            finished = false;
        }
        PostComments resp = new PostComments();
        resp.setItems(items);
        resp.setFinished(finished);
        return resp;
    }

    public List<PostComment> topComments(String postId, int limit) {
        if (!StringUtils.hasText(postId)) {
            return new ArrayList<>();
        }
        long size = limit <= 0 ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
        String appkey = RequestContext.getAppkeyFromCtx();
        String userId = RequestContext.getCurrentUserIdFromCtx();
        boolean friendMode = appSettingService.isFriendPostMode(appkey);
        List<com.juggle.chat.models.PostComment> records;
        long startTime = normalizeStart(0L, false);
        if (friendMode) {
            records = postCommentFeedMapper.qryPostComments(appkey, userId, postId, startTime, size, false);
        } else {
            records = postCommentMapper.qryPostComments(appkey, postId, startTime, size, false);
        }
        List<PostComment> items = new ArrayList<>();
        if (!CollectionUtils.isEmpty(records)) {
            for (com.juggle.chat.models.PostComment record : records) {
                items.add(buildApiComment(record));
            }
        }
        return items;
    }

    private PostComment buildApiComment(com.juggle.chat.models.PostComment comment) {
        PostComment resp = new PostComment();
        resp.setCommentId(comment.getCommentId());
        resp.setPostId(comment.getPostId());
        resp.setParentCommentId(comment.getParentCommentId());
        resp.setParentUserId(comment.getParentUserId());
        resp.setText(comment.getText());
        if (StringUtils.hasText(comment.getParentUserId())) {
            resp.setParentUserInfo(userService.getUserInfo(comment.getParentUserId()));
        }
        resp.setUserInfo(userService.getUserInfo(comment.getUserId()));
        resp.setCreatedTime(comment.getCreatedTime() == null ? 0 : comment.getCreatedTime());
        resp.setUpdatedTime(comment.getUpdatedTime() == null ? 0 : comment.getUpdatedTime().getTime());
        return resp;
    }

    private long normalizeStart(long start, boolean isPositive) {
        if (isPositive) {
            return Math.max(0L, start);
        }
        return start > 0 ? start : System.currentTimeMillis();
    }
}
