package com.juggle.chat.controllers;

import jakarta.annotation.Resource;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.juggle.chat.apimodels.Result;
import com.juggle.chat.apimodels.TelegramBot;
import com.juggle.chat.apimodels.TelegramBotIds;
import com.juggle.chat.exceptions.JimErrorCode;
import com.juggle.chat.exceptions.JimException;
import com.juggle.chat.services.TelegramBotService;

@RestController
@RequestMapping("/jim/telegrambots")
public class TelegramBotController {
    @Resource
    private TelegramBotService telegramBotService;

    @PostMapping("/add")
    public Result telegramBotAdd(@RequestBody(required = false) TelegramBot req) {
        if (req == null) {
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        return Result.success(this.telegramBotService.addBot(req));
    }

    @PostMapping("/del")
    public Result telegramBotDel(@RequestBody(required = false) TelegramBot req) {
        if (req == null) {
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        this.telegramBotService.deleteBot(req);
        return Result.success(null);
    }

    @PostMapping("/batchdel")
    public Result telegramBotBatchDel(@RequestBody(required = false) TelegramBotIds req) {
        if (req == null) {
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        this.telegramBotService.batchDelete(req);
        return Result.success(null);
    }

    @GetMapping("/list")
    public Result telegramBotList(@RequestParam(value = "count", required = false) Integer count,
            @RequestParam(value = "offset", required = false) String offset) {
        int pageSize = (count == null || count <= 0) ? 20 : count;
        return Result.success(this.telegramBotService.listBots(pageSize, offset));
    }
}
