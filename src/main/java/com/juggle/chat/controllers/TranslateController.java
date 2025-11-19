package com.juggle.chat.controllers;

import javax.annotation.Resource;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.juggle.chat.apimodels.Result;
import com.juggle.chat.apimodels.TransReq;
import com.juggle.chat.exceptions.JimException;
import com.juggle.chat.services.TranslateService;

@RestController
@RequestMapping("/jim")
public class TranslateController {
    @Resource
    private TranslateService translateService;

    @PostMapping("/translate")
    public Result translate(@RequestBody TransReq req) throws JimException {
        return Result.success(this.translateService.translate(req));
    }
}
