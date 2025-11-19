package com.juggle.chat.controllers;

import javax.annotation.Resource;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.juggle.chat.apimodels.GroupAdministratorsReq;
import com.juggle.chat.apimodels.GroupManagement;
import com.juggle.chat.apimodels.GroupOwnerChgReq;
import com.juggle.chat.apimodels.Result;
import com.juggle.chat.apimodels.SetGroupHisMsgVisibleReq;
import com.juggle.chat.apimodels.SetGroupMemberMuteReq;
import com.juggle.chat.apimodels.SetGroupMuteReq;
import com.juggle.chat.apimodels.SetGroupVerifyTypeReq;
import com.juggle.chat.exceptions.JimErrorCode;
import com.juggle.chat.exceptions.JimException;
import com.juggle.chat.services.GroupService;

@RestController
@RequestMapping("/jim/groups/management")
public class GroupManageController {
    @Resource
    private GroupService grpService;

    @PostMapping("/chgowner")
    public Result chgGroupOwner(@RequestBody GroupOwnerChgReq req){
        if(req==null||req.getGroupId()==null||req.getGroupId().isEmpty()
                || req.getOwnerId()==null||req.getOwnerId().isEmpty()){
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        this.grpService.chgGroupOwner(req);
        return Result.success(null);
    }

    @PostMapping("/setmute")
    public Result setGroupMute(@RequestBody SetGroupMuteReq req){
        if(req==null||req.getGroupId()==null||req.getGroupId().isEmpty()){
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        this.grpService.setGroupMute(req);
        return Result.success(null);
    }

    @PostMapping("/setgrpmembersmute")
    public Result setGroupMembersMute(@RequestBody SetGroupMemberMuteReq req){
        if(req==null||req.getGroupId()==null||req.getGroupId().isEmpty()
                || req.getMemberIds()==null||req.getMemberIds().isEmpty()){
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        this.grpService.setGroupMembersMute(req);
        return Result.success(null);
    }

    @PostMapping("/setgrpverifytype")
    public Result setGrpVerifyType(@RequestBody SetGroupVerifyTypeReq req){
        if(req==null||req.getGroupId()==null||req.getGroupId().isEmpty()){
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        this.grpService.setGrpVerifyType(req);
        return Result.success(null);
    }

    @PostMapping("/sethismsgvisible")
    public Result setGrpHisMsgVisible(@RequestBody SetGroupHisMsgVisibleReq req){
        if(req==null||req.getGroupId()==null||req.getGroupId().isEmpty()){
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        this.grpService.setGrpHisMsgVisible(req);
        return Result.success(null);
    }

    @PostMapping("/administrators/add")
    public Result addGrpAdministrator(@RequestBody GroupAdministratorsReq req){
        if(req==null||req.getGroupId()==null||req.getGroupId().isEmpty()
                || req.getAdminIds()==null||req.getAdminIds().isEmpty()){
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        this.grpService.addGrpAdministrator(req);
        return Result.success(null);
    }

    @PostMapping("/administrators/del")
    public Result delGrpAdministrator(@RequestBody GroupAdministratorsReq req){
        if(req==null||req.getGroupId()==null||req.getGroupId().isEmpty()
                || req.getAdminIds()==null||req.getAdminIds().isEmpty()){
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        this.grpService.delGrpAdministrator(req);
        return Result.success(null);
    }

    @GetMapping("/administrators/list")
    public Result qryGrpAdministrator(@RequestParam("group_id") String groupId){
        if(groupId==null||groupId.isEmpty()){
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        return Result.success(this.grpService.qryGrpAdministrator(groupId));
    }

    @PostMapping("/set")
    public Result setGrpManagementConfs(@RequestBody GroupManagement management){
        if(management==null||management.getGroupId()==null||management.getGroupId().isEmpty()){
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        this.grpService.setGrpManagementConfs(management);
        return Result.success(null);
    }
}
