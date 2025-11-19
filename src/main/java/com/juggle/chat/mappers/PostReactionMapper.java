package com.juggle.chat.mappers;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.juggle.chat.models.PostReaction;

@Mapper
public interface PostReactionMapper {
    int upsert(PostReaction reaction);
    int delete(@Param("appkey")String appkey,
               @Param("busId")String busId,
               @Param("busType")int busType,
               @Param("key")String key,
               @Param("userId")String userId);
    List<PostReaction> qryReactions(@Param("appkey")String appkey,
                                    @Param("busId")String busId,
                                    @Param("busType")int busType,
                                    @Param("limit")long limit);
}
