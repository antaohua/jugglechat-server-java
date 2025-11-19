package com.juggle.chat.services;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.juggle.chat.mappers.FriendRelMapper;
import com.juggle.chat.mappers.PostCommentFeedMapper;
import com.juggle.chat.mappers.PostFeedMapper;
import com.juggle.chat.models.FriendRel;
import com.juggle.chat.models.Post;
import com.juggle.chat.models.PostBusType;
import com.juggle.chat.models.PostComment;
import com.juggle.chat.models.PostCommentFeed;
import com.juggle.chat.models.PostFeed;

@Service
public class PostFeedService {
    private static final int FRIEND_BATCH_SIZE = 200;

    @Resource
    private PostFeedMapper postFeedMapper;

    @Resource
    private PostCommentFeedMapper postCommentFeedMapper;

    @Resource
    private FriendRelMapper friendRelMapper;

    @Resource
    private AppSettingService appSettingService;

    @Resource
    private PostNotifyService postNotifyService;

    public void appendPostFeed(Post post) {
        if (post == null) {
            return;
        }
        PostFeed self = new PostFeed();
        self.setAppkey(post.getAppkey());
        self.setUserId(post.getUserId());
        self.setPostId(post.getPostId());
        self.setFeedTime(post.getCreatedTime());
        postFeedMapper.insert(self);
        boolean friendMode = appSettingService.isFriendPostMode(post.getAppkey());
        List<String> notifiedTargets = new ArrayList<>();
        foreachFriends(post.getAppkey(), post.getUserId(), friendIds -> {
            List<PostFeed> feeds = friendIds.stream().map(friendId -> {
                PostFeed feed = new PostFeed();
                feed.setAppkey(post.getAppkey());
                feed.setUserId(friendId);
                feed.setPostId(post.getPostId());
                feed.setFeedTime(post.getCreatedTime());
                return feed;
            }).collect(Collectors.toList());
            if (!feeds.isEmpty()) {
                postFeedMapper.batchInsert(feeds);
            }
            if (friendMode) {
                notifiedTargets.addAll(friendIds);
            }
        });
        if (friendMode && !notifiedTargets.isEmpty()) {
            postNotifyService.notifyTargets(post.getAppkey(), post.getUserId(), PostBusType.POST, notifiedTargets);
        }
    }

    public void appendCommentFeed(PostComment comment) {
        if (comment == null) {
            return;
        }
        String appkey = comment.getAppkey();
        PostCommentFeed self = new PostCommentFeed();
        self.setAppkey(appkey);
        self.setUserId(comment.getUserId());
        self.setCommentId(comment.getCommentId());
        self.setPostId(comment.getPostId());
        self.setFeedTime(comment.getCreatedTime());
        postCommentFeedMapper.insert(self);
        boolean friendMode = appSettingService.isFriendPostMode(appkey);
        List<String> notifiedTargets = new ArrayList<>();
        foreachFriends(appkey, comment.getUserId(), friendIds -> {
            List<PostCommentFeed> feeds = friendIds.stream().map(friendId -> {
                PostCommentFeed feed = new PostCommentFeed();
                feed.setAppkey(appkey);
                feed.setUserId(friendId);
                feed.setCommentId(comment.getCommentId());
                feed.setPostId(comment.getPostId());
                feed.setFeedTime(comment.getCreatedTime());
                return feed;
            }).collect(Collectors.toList());
            if (!feeds.isEmpty()) {
                postCommentFeedMapper.batchInsert(feeds);
            }
            if (friendMode) {
                notifiedTargets.addAll(friendIds);
            }
        });
        if (friendMode && !notifiedTargets.isEmpty()) {
            postNotifyService.notifyTargets(appkey, comment.getUserId(), PostBusType.COMMENT,
                    notifiedTargets);
        }
    }

    private void foreachFriends(String appkey, String userId, Consumer<List<String>> consumer) {
        long startId = 0;
        while (true) {
            List<FriendRel> rels = friendRelMapper.queryFriendRels(appkey, userId, startId, FRIEND_BATCH_SIZE);
            if (CollectionUtils.isEmpty(rels)) {
                break;
            }
            List<String> friendIds = rels.stream()
                    .map(FriendRel::getFriendId)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toList());
            if (!friendIds.isEmpty()) {
                consumer.accept(friendIds);
            }
            FriendRel last = rels.get(rels.size() - 1);
            startId = last.getId() == null ? startId : last.getId();
            if (rels.size() < FRIEND_BATCH_SIZE) {
                break;
            }
        }
    }
}
