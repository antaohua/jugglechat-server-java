package com.juggle.chat.controllers;

import jakarta.annotation.Resource;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.juggle.chat.apimodels.PostComment;
import com.juggle.chat.apimodels.PostCommentIds;
import com.juggle.chat.apimodels.Result;
import com.juggle.chat.exceptions.JimErrorCode;
import com.juggle.chat.exceptions.JimException;
import com.juggle.chat.services.PostCommentService;

@RestController
@RequestMapping("/jim/posts/comments")
public class PostCommentController {
    @Resource
    private PostCommentService postCommentService;

    @GetMapping("/list")
    public Result qryPostComments(@RequestParam("post_id") String postId,
            @RequestParam(value = "start", required = false) Long start,
            @RequestParam(value = "limit", required = false) Long limit,
            @RequestParam(value = "order", required = false) Integer order) {
        if (postId == null || postId.isEmpty()) {
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        boolean isPositive = order != null && order == 1;
        return Result.success(this.postCommentService.listComments(postId, start, limit, isPositive));
    }

    @PostMapping("/add")
    public Result addPostComment(@RequestBody(required = false) PostComment req) {
        if (req == null) {
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        return Result.success(this.postCommentService.addComment(req));
    }

    @PostMapping("/update")
    public Result updatePostComment(@RequestBody(required = false) PostComment req) {
        if (req == null) {
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        this.postCommentService.updateComment(req);
        return Result.success(null);
    }

    @PostMapping("/del")
    public Result delPostComment(@RequestBody(required = false) PostCommentIds req) {
        if (req == null) {
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        this.postCommentService.deleteComments(req);
        return Result.success(null);
    }
}
