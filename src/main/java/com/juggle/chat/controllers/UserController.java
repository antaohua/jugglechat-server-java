package com.juggle.chat.controllers;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import jakarta.annotation.Resource;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.juggle.chat.apimodels.BlockUsersReq;
import com.juggle.chat.apimodels.BindEmailReq;
import com.juggle.chat.apimodels.BindPhoneReq;
import com.juggle.chat.apimodels.BlockUsers;
import com.juggle.chat.apimodels.QrCode;
import com.juggle.chat.apimodels.Result;
import com.juggle.chat.apimodels.SearchReq;
import com.juggle.chat.apimodels.SetUserAccountReq;
import com.juggle.chat.apimodels.UpdUserPassReq;
import com.juggle.chat.apimodels.UserIds;
import com.juggle.chat.apimodels.UserInfo;
import com.juggle.chat.apimodels.UserInfos;
import com.juggle.chat.apimodels.UserSettings;
import com.juggle.chat.exceptions.JimException;
import com.juggle.chat.interceptors.RequestContext;
import com.juggle.chat.services.ImSdkService;
import com.juggle.chat.services.UserService;
import com.juggle.chat.utils.CommonUtil;
import com.juggle.chat.exceptions.JimErrorCode;
import com.juggle.im.JuggleIm;

@RestController
@RequestMapping("/jim/users")
public class UserController {
    @Resource
    private UserService userService;

    @PostMapping("/update")
    public Result updateUser(@RequestBody UserInfo user)throws JimException{
        userService.updateUser(user);
        return new Result(0, "");
    }

    @PostMapping("/updpass")
    public Result updatePass(@RequestBody UpdUserPassReq req)throws JimException{
        this.userService.updatePass(req);
        return Result.success(null);
    }

    @PostMapping("/updsettings")
    public Result updateUserSettings(@RequestBody UserSettings settings)throws JimException{
        this.userService.updateUserSettings(settings);
        return new Result(0, "");
    }

    @PostMapping("/search")
    public Result searchUsers(@RequestBody SearchReq req)throws JimException{
        if(req==null||((req.getPhone()==null||req.getPhone().isEmpty())
                && (req.getKeyword()==null||req.getKeyword().isEmpty()))){
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        UserInfos users;
        if(req.getPhone()!=null && !req.getPhone().isEmpty()){
            users = this.userService.searchByPhone(req.getPhone());
        }else{
            users = this.userService.searchByKeyword(req);
        }
        return Result.success(users);
    }

    @GetMapping("/info")
    public Result qryUserInfo(@RequestParam("user_id") String userId){
        UserInfo user = this.userService.qryUserInfo(userId);
        return Result.success(user);
    }

    @GetMapping("/qrcode")
    public Result qryUserQrCode(){
        String userId = RequestContext.getCurrentUserIdFromCtx();
        QrCode ret = new QrCode();
        Map<String,String> qrContent = new HashMap<>();
        qrContent.put("action", "add_friend");
        qrContent.put("user_id", userId);
        String content = CommonUtil.toJson(qrContent);
        String format = "png";
        try{
            BitMatrix bitMatrix = new MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE,400,400);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, format, baos);
            byte[] imgBs = baos.toByteArray();
            ret.setQrCode(Base64.getUrlEncoder().encodeToString(imgBs));
        }catch(Exception e){
            e.printStackTrace();
        }
        return Result.success(ret);
    }

    @PostMapping("/setaccount")
    public Result setLoginAccount(@RequestBody SetUserAccountReq req)throws JimException{
        this.userService.setLoginAccount(req);
        return Result.success(null);
    }

    @PostMapping("/bindemail/send")
    public Result bindEmailSend(@RequestBody BindEmailReq req)throws JimException{
        this.userService.bindEmailSend(req);
        return Result.success(null);
    }

    @PostMapping("/bindemail")
    public Result bindEmail(@RequestBody BindEmailReq req)throws JimException{
        this.userService.bindEmail(req);
        return Result.success(null);
    }

    @PostMapping("/bindphone/send")
    public Result bindPhoneSend(@RequestBody BindPhoneReq req)throws JimException{
        this.userService.bindPhoneSend(req);
        return Result.success(null);
    }

    @PostMapping("/bindphone")
    public Result bindPhone(@RequestBody BindPhoneReq req)throws JimException{
        this.userService.bindPhone(req);
        return Result.success(null);
    }

    @PostMapping("/onlinestatus")
    public Result qryUsersOnlineStatus(@RequestBody UserIds req){
        if(req==null||req.getUserIds()==null||req.getUserIds().isEmpty()){
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        JuggleIm sdk = ImSdkService.getJimSdk(RequestContext.getAppkeyFromCtx());
        if(sdk==null){
            throw new JimException(JimErrorCode.ErrorCode_APP_NOT_EXISTED);
        }
        try {
            Object resp = sdk.user.queryOnlineStatus(req.getUserIds());
            return Result.success(resp);
        } catch (Exception e) {
            throw new JimException(JimErrorCode.ErrorCode_APP_INTERNAL_TIMEOUT);
        }
    }

    @PostMapping("/blockusers/add")
    public Result blockUsers(@RequestBody BlockUsersReq req){
        if(req==null||req.getBlockUserIds()==null||req.getBlockUserIds().isEmpty()){
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        this.userService.blockUsers(req);
        return Result.success(null);
    }

    @PostMapping("/blockusers/del")
    public Result unBlockUsers(@RequestBody BlockUsersReq req){
        if(req==null||req.getBlockUserIds()==null||req.getBlockUserIds().isEmpty()){
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        this.userService.unBlockUsers(req);
        return Result.success(null);
    }

    @GetMapping("/blockusers/list")
    public Result qryBlockUsers(@RequestParam(value = "offset", required = false) String offset,
            @RequestParam(value = "count", required = false) Integer count){
        BlockUsers users = this.userService.qryBlockUsers(offset, count);
        return Result.success(users);
    }
}
