package com.juggle.chat.controllers;

import javax.annotation.Resource;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.juggle.chat.apimodels.DelHisMsgsReq;
import com.juggle.chat.apimodels.RecallMsgReq;
import com.juggle.chat.apimodels.Result;
import com.juggle.chat.exceptions.JimException;
import com.juggle.chat.services.MessageService;

@RestController
@RequestMapping("/jim/messages")
public class MessageController {
    @Resource
    private MessageService messageService;

    @PostMapping("/recall")
    public Result recallMessage(@RequestBody RecallMsgReq req) throws JimException {
        this.messageService.recallMessage(req);
        return Result.success(null);
    }

    @PostMapping("/del")
    public Result deleteMessages(@RequestBody DelHisMsgsReq req) throws JimException {
        this.messageService.deleteMessages(req);
        return Result.success(null);
    }
}
