package com.juggle.chat.mappers;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.juggle.chat.models.Post;
import com.juggle.chat.models.PostFeed;

@Mapper
public interface PostFeedMapper {
    int insert(PostFeed feed);
    int batchInsert(@Param("feeds")List<PostFeed> feeds);
    List<Post> qryPosts(@Param("appkey")String appkey,
                        @Param("userId")String userId,
                        @Param("startTime")long startTime,
                        @Param("limit")long limit,
                        @Param("isPositive")boolean isPositive);
}
