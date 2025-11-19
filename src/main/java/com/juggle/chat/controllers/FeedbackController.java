package com.juggle.chat.controllers;

import jakarta.annotation.Resource;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.juggle.chat.apimodels.Feedback;
import com.juggle.chat.apimodels.Result;
import com.juggle.chat.services.FeedbackService;

@RestController
@RequestMapping("/jim/feedbacks")
public class FeedbackController {
    @Resource
    private FeedbackService feedbackService;

    @PostMapping("/add")
    public Result addFeedback(@RequestBody Feedback feedback) {
        this.feedbackService.addFeedback(feedback);
        return Result.success(null);
    }
}
