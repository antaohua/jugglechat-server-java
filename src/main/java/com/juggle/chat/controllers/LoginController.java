package com.juggle.chat.controllers;

import jakarta.annotation.Resource;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.juggle.chat.apimodels.EmailLoginReq;
import com.juggle.chat.apimodels.LoginReq;
import com.juggle.chat.apimodels.LoginUserResp;
import com.juggle.chat.apimodels.QrCode;
import com.juggle.chat.apimodels.QrCodeReq;
import com.juggle.chat.apimodels.RegisterReq;
import com.juggle.chat.apimodels.Result;
import com.juggle.chat.apimodels.SmsLoginReq;
import com.juggle.chat.exceptions.JimErrorCode;
import com.juggle.chat.exceptions.JimException;
import com.juggle.chat.services.LoginService;

@RestController
@RequestMapping("/admingateway")
public class LoginController {
    @Resource
    private LoginService loginService;


    @PostMapping(path = "/login",consumes = { MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE })
    public Result login(@RequestBody LoginReq req) throws JimException {
        LoginUserResp resp = loginService.login(req);
        return Result.success(resp);
    }

    @PostMapping("/register")
    public Result register(@RequestBody RegisterReq req) {
        loginService.register(req);
        return Result.success(null);
    }

    @GetMapping("/login/qrcode")
    public Result generateQrCode() {
        QrCode qrCode = loginService.generateQrCode();
        return Result.success(qrCode);
    }

    @PostMapping("/login/qrcode/check")
    public Result checkQrCode(@RequestBody QrCodeReq req) {
        LoginUserResp resp = loginService.checkQrCode(req);
        return Result.success(resp);
    }

    @PostMapping("/sms/send")
    public Result smsSend(@RequestBody SmsLoginReq req) {
        loginService.smsSend(req);
        return Result.success(null);
    }

    @PostMapping(value = {"/sms/login","/sms_login"})
    public Result smsLogin(@RequestBody SmsLoginReq req)throws JimException{
        if(req==null||req.getPhone().isEmpty()||req.getCode().isEmpty()){
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL, "");
        }
        LoginUserResp resp = loginService.smsLogin(req.getPhone(), req.getCode());
        return Result.success(resp);
    }

    @PostMapping("/email/send")
    public Result emailSend(@RequestBody EmailLoginReq req) {
        loginService.emailSend(req);
        return Result.success(null);
    }

    @PostMapping("/email/login")
    public Result emailLogin(@RequestBody EmailLoginReq req) {
        LoginUserResp resp = loginService.emailLogin(req);
        return Result.success(resp);
    }

    @PostMapping("/login/qrcode/confirm")
    public Result confirmQrCode(@RequestBody QrCodeReq req) {
        loginService.confirmQrCode(req);
        return Result.success(null);
    }
}
