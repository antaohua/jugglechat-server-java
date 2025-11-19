package com.juggle.chat.controllers;

import jakarta.annotation.Resource;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.juggle.chat.apimodels.PostIds;
import com.juggle.chat.apimodels.PostReaction;
import com.juggle.chat.apimodels.Result;
import com.juggle.chat.exceptions.JimErrorCode;
import com.juggle.chat.exceptions.JimException;
import com.juggle.chat.services.PostReactionService;
import com.juggle.chat.services.PostService;

@RestController
@RequestMapping("/jim/posts")
public class PostController {
    @Resource
    private PostService postService;

    @Resource
    private PostReactionService postReactionService;

    @GetMapping("/list")
    public Result qryPosts(@RequestParam(value = "start", required = false) Long start,
            @RequestParam(value = "limit", required = false) Long limit,
            @RequestParam(value = "order", required = false) Integer order) {
        boolean isPositive = order != null && order == 1;
        return Result.success(this.postService.listPosts(start, limit, isPositive));
    }

    @GetMapping("/info")
    public Result qryPostInfo(@RequestParam("post_id") String postId) {
        if (postId == null || postId.isEmpty()) {
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        return Result.success(this.postService.getPostInfo(postId));
    }

    @PostMapping("/add")
    public Result postAdd(@RequestBody(required = false) com.juggle.chat.apimodels.Post req) {
        if (req == null) {
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        return Result.success(this.postService.addPost(req));
    }

    @PostMapping("/update")
    public Result postUpdate(@RequestBody(required = false) com.juggle.chat.apimodels.Post req) {
        if (req == null) {
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        this.postService.updatePost(req);
        return Result.success(null);
    }

    @PostMapping("/del")
    public Result postDel(@RequestBody(required = false) PostIds req) {
        if (req == null) {
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        this.postService.deletePosts(req);
        return Result.success(null);
    }

    @PostMapping("/reactions/add")
    public Result addPostReaction(@RequestBody(required = false) PostReaction req) {
        if (req == null) {
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        this.postReactionService.addReaction(req);
        return Result.success(null);
    }

    @PostMapping("/reactions/del")
    public Result delPostReaction(@RequestBody(required = false) PostReaction req) {
        if (req == null) {
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        this.postReactionService.deleteReaction(req);
        return Result.success(null);
    }

    @GetMapping("/reactions/list")
    public Result qryPostReactions(@RequestParam("post_id") String postId) {
        if (postId == null || postId.isEmpty()) {
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        return Result.success(this.postReactionService.listReactions(postId));
    }
}
