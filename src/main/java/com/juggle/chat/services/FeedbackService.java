package com.juggle.chat.services;

import jakarta.annotation.Resource;

import org.springframework.stereotype.Service;

import com.juggle.chat.apimodels.Feedback;
import com.juggle.chat.interceptors.RequestContext;
import com.juggle.chat.mappers.FeedbackMapper;
import com.juggle.chat.services.Appbus.FeedbackContent;

@Service
public class FeedbackService {
    @Resource
    private FeedbackMapper feedbackMapper;

    public void addFeedback(Feedback feedback){
        String appkey = RequestContext.getAppkeyFromCtx();
        String userId = RequestContext.getCurrentUserIdFromCtx();
        FeedbackContent content = FeedbackContent.newBuilder()
                .setText(feedback.getText()==null?"":feedback.getText())
                .addAllImages(feedback.getImages()==null?java.util.List.of():feedback.getImages())
                .addAllVideos(feedback.getVideos()==null?java.util.List.of():feedback.getVideos())
                .build();
        com.juggle.chat.models.Feedback record = new com.juggle.chat.models.Feedback();
        record.setAppkey(appkey);
        record.setUserId(userId);
        record.setCategory(feedback.getCategory());
        record.setContent(content.toByteArray());
        this.feedbackMapper.create(record);
    }
}
