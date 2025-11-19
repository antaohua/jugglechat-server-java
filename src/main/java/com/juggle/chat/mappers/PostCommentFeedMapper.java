package com.juggle.chat.mappers;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.juggle.chat.models.PostComment;
import com.juggle.chat.models.PostCommentFeed;

@Mapper
public interface PostCommentFeedMapper {
    int insert(PostCommentFeed feed);
    int batchInsert(@Param("feeds")List<PostCommentFeed> feeds);
    List<PostComment> qryPostComments(@Param("appkey")String appkey,
                                      @Param("userId")String userId,
                                      @Param("postId")String postId,
                                      @Param("startTime")long startTime,
                                      @Param("limit")long limit,
                                      @Param("isPositive")boolean isPositive);
}
