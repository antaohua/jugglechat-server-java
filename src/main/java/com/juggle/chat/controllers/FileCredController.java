package com.juggle.chat.controllers;

import javax.annotation.Resource;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.juggle.chat.apimodels.QryFileCredReq;
import com.juggle.chat.apimodels.Result;
import com.juggle.chat.exceptions.JimErrorCode;
import com.juggle.chat.exceptions.JimException;
import com.juggle.chat.services.FileService;

@RestController
@RequestMapping("/jim")
public class FileCredController {
    @Resource
    private FileService fileService;

    @PostMapping("/file_cred")
    public Result getFileCred(@RequestBody(required = false) QryFileCredReq req) {
        if (req == null) {
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        return Result.success(this.fileService.getFileCredential(req));
    }
}
