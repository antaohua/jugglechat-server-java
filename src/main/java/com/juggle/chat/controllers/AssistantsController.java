package com.juggle.chat.controllers;

import javax.annotation.Resource;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.juggle.chat.apimodels.AssistantAnswerReq;
import com.juggle.chat.apimodels.AssistantPromptIds;
import com.juggle.chat.apimodels.AssistantPromptReq;
import com.juggle.chat.apimodels.Result;
import com.juggle.chat.exceptions.JimErrorCode;
import com.juggle.chat.exceptions.JimException;
import com.juggle.chat.services.AssistantService;

@RestController
@RequestMapping("/jim/assistants")
public class AssistantsController {
    @Resource
    private AssistantService assistantService;

    @PostMapping("/answer")
    public Result assistantAnswer(@RequestBody(required = false) AssistantAnswerReq req) {
        if (req == null) {
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        return Result.success(this.assistantService.answer(req));
    }

    @PostMapping("/prompts/add")
    public Result promptAdd(@RequestBody(required = false) AssistantPromptReq req) {
        if (req == null) {
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        return Result.success(this.assistantService.addPrompt(req));
    }

    @PostMapping("/prompts/update")
    public Result promptUpdate(@RequestBody(required = false) AssistantPromptReq req) {
        if (req == null) {
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        return Result.success(this.assistantService.updatePrompt(req));
    }

    @PostMapping("/prompts/del")
    public Result promptDel(@RequestBody(required = false) AssistantPromptReq req) {
        if (req == null) {
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        this.assistantService.deletePrompt(req);
        return Result.success(null);
    }

    @PostMapping("/prompts/batchdel")
    public Result promptBatchDel(@RequestBody(required = false) AssistantPromptIds req) {
        if (req == null) {
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        this.assistantService.batchDelete(req);
        return Result.success(null);
    }

    @GetMapping("/prompts/list")
    public Result qryPrompts(@RequestParam(value = "offset", required = false) String offset,
            @RequestParam(value = "count", required = false) Integer count) {
        return Result.success(this.assistantService.listPrompts(offset, count));
    }
}
