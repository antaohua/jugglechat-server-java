package com.juggle.chat.services;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.google.protobuf.ServiceException;
import com.juggle.chat.apimodels.AssistantAnswerReq;
import com.juggle.chat.apimodels.AssistantAnswerResp;
import com.juggle.chat.apimodels.AssistantPrompt;
import com.juggle.chat.apimodels.AssistantPromptIds;
import com.juggle.chat.apimodels.AssistantPromptReq;
import com.juggle.chat.apimodels.AssistantPrompts;
import com.juggle.chat.exceptions.JimErrorCode;
import com.juggle.chat.exceptions.JimException;
import com.juggle.chat.interceptors.RequestContext;
import com.juggle.chat.mappers.PromptMapper;
import com.juggle.chat.models.Prompt;
import com.juggle.chat.utils.N3d;

@Service
public class AssistantService {
    private static final int DEFAULT_PAGE_SIZE = 20;

    @Resource
    private PromptMapper promptMapper;

    public AssistantPrompt addPrompt(AssistantPromptReq req) {
        if (req == null || !StringUtils.hasText(req.getPrompts())) {
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        String appkey = RequestContext.getAppkeyFromCtx();
        String userId = RequestContext.getCurrentUserIdFromCtx();
        Prompt prompt = new Prompt();
        prompt.setAppkey(appkey);
        prompt.setUserId(userId);
        prompt.setPrompts(req.getPrompts());
        promptMapper.create(prompt);
        return toApi(prompt);
    }

    public AssistantPrompt updatePrompt(AssistantPromptReq req) {
        if (req == null || !StringUtils.hasText(req.getPromptId()) || !StringUtils.hasText(req.getPrompts())) {
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        String appkey = RequestContext.getAppkeyFromCtx();
        String userId = RequestContext.getCurrentUserIdFromCtx();
        long id = decode(req.getPromptId());
        Prompt dbPrompt = promptMapper.findPrompt(appkey, id, userId);
        if (dbPrompt == null) {
            throw new JimException(JimErrorCode.ErrorCode_APP_ASSISTANT_PROMPT_DBERROR);
        }
        promptMapper.updatePrompt(appkey, id, userId, req.getPrompts());
        dbPrompt.setPrompts(req.getPrompts());
        return toApi(dbPrompt);
    }

    public void deletePrompt(AssistantPromptReq req) {
        if (req == null || !StringUtils.hasText(req.getPromptId())) {
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        String appkey = RequestContext.getAppkeyFromCtx();
        String userId = RequestContext.getCurrentUserIdFromCtx();
        long id = decode(req.getPromptId());
        promptMapper.deletePrompt(appkey, id, userId);
    }

    public void batchDelete(AssistantPromptIds ids) {
        if (ids == null || ids.getPromptIds() == null || ids.getPromptIds().isEmpty()) {
            return;
        }
        List<Long> idList = new ArrayList<>();
        for (String pid : ids.getPromptIds()) {
            if (StringUtils.hasText(pid)) {
                idList.add(decode(pid));
            }
        }
        if (idList.isEmpty()) {
            return;
        }
        promptMapper.batchDeletePrompts(RequestContext.getAppkeyFromCtx(), RequestContext.getCurrentUserIdFromCtx(), idList);
    }

    public AssistantPrompts listPrompts(String offset, Integer count) {
        int size = (count == null || count <= 0) ? DEFAULT_PAGE_SIZE : Math.min(count, 100);
        long startId = Long.MAX_VALUE;
        if (StringUtils.hasText(offset)) {
            startId = decode(offset);
        }
        List<Prompt> prompts = promptMapper.qryPrompts(RequestContext.getAppkeyFromCtx(),
                RequestContext.getCurrentUserIdFromCtx(), startId, size);
        List<AssistantPrompt> items = new ArrayList<>();
        String nextOffset = null;
        if (prompts != null) {
            for (Prompt prompt : prompts) {
                items.add(toApi(prompt));
                nextOffset = encode(prompt.getId());
            }
        }
        AssistantPrompts resp = new AssistantPrompts();
        resp.setItems(items);
        resp.setOffset(nextOffset);
        return resp;
    }

    public AssistantAnswerResp answer(AssistantAnswerReq req) {
        if (req == null || !StringUtils.hasText(req.getQuestion())) {
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        List<Prompt> prompts = promptMapper.qryPrompts(RequestContext.getAppkeyFromCtx(),
                RequestContext.getCurrentUserIdFromCtx(), Long.MAX_VALUE, 5);
        StringBuilder builder = new StringBuilder();
        if (prompts != null) {
            for (Prompt prompt : prompts) {
                if (StringUtils.hasText(prompt.getPrompts())) {
                    builder.append(prompt.getPrompts()).append('\n');
                }
            }
        }
        if (req.getContexts() != null) {
            for (String ctx : req.getContexts()) {
                if (StringUtils.hasText(ctx)) {
                    builder.append(ctx).append('\n');
                }
            }
        }
        builder.append(req.getQuestion());
        AssistantAnswerResp resp = new AssistantAnswerResp();
        resp.setAnswer(builder.toString());
        return resp;
    }

    private AssistantPrompt toApi(Prompt prompt) {
        AssistantPrompt resp = new AssistantPrompt();
        resp.setPromptId(encode(prompt.getId()));
        resp.setPrompts(prompt.getPrompts());
        resp.setCreatedTime(prompt.getCreatedTime() == null ? 0 : prompt.getCreatedTime().getTime());
        return resp;
    }

    private long decode(String value) {
        try {
            return N3d.decode(value);
        } catch (ServiceException e) {
            throw new JimException(JimErrorCode.ErrorCode_APP_ASSISTANT_PROMPT_DBERROR);
        }
    }

    private String encode(Long id) {
        if (id == null || id <= 0) {
            return null;
        }
        try {
            return N3d.encode(id);
        } catch (ServiceException e) {
            throw new JimException(JimErrorCode.ErrorCode_APP_ASSISTANT_PROMPT_DBERROR);
        }
    }
}
