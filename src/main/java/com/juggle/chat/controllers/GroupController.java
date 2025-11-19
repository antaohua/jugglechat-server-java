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
import com.juggle.chat.apimodels.CheckGroupMembersReq;
import com.juggle.chat.apimodels.CheckGroupMembersResp;
import com.juggle.chat.apimodels.GroupAnnouncement;
import com.juggle.chat.apimodels.GroupConfirm;
import com.juggle.chat.apimodels.GroupInfo;
import com.juggle.chat.apimodels.GroupInvite;
import com.juggle.chat.apimodels.GroupInviteResp;
import com.juggle.chat.apimodels.GroupMemberInfos;
import com.juggle.chat.apimodels.GroupMembersReq;
import com.juggle.chat.apimodels.Groups;
import com.juggle.chat.apimodels.QrCode;
import com.juggle.chat.apimodels.QryGrpApplicationsResp;
import com.juggle.chat.apimodels.Result;
import com.juggle.chat.apimodels.SearchGroupMembersReq;
import com.juggle.chat.apimodels.SearchReq;
import com.juggle.chat.apimodels.SetGroupDisplayNameReq;
import com.juggle.chat.exceptions.JimErrorCode;
import com.juggle.chat.exceptions.JimException;
import com.juggle.chat.interceptors.RequestContext;
import com.juggle.chat.services.GroupService;
import com.juggle.chat.utils.CommonUtil;

@RestController
@RequestMapping("/jim/groups")
public class GroupController {
    @Resource
    private GroupService grpService;

    @PostMapping(value={"/add","/create"})
    public Result createGroup(@RequestBody GroupInfo grpInfo)throws JimException{
        if(grpInfo==null||grpInfo.getGroupId().isEmpty()||grpInfo.getMemberIds()==null){
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        this.grpService.createGroup(grpInfo);
        return new Result(0, "");
    }

    @PostMapping("/update")
    public Result updateGroup(@RequestBody GroupInfo grpInfo)throws JimException{
        if(grpInfo==null||grpInfo.getGroupId()==null){
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        this.grpService.updateGroup(grpInfo);
        return new Result(0, "");
    }

    @PostMapping("/dissolve")
    public Result dissolveGroup(@RequestBody GroupInfo grpInfo)throws JimException{
        if(grpInfo==null||grpInfo.getGroupId()==null){
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        this.grpService.dissolveGroup(grpInfo.getGroupId());
        return new Result(0, "");
    }

    @GetMapping("/info")
    public Result qryGroupInfo(@RequestParam("group_id") String groupId){
        if(groupId.isEmpty()){
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        return Result.success(this.grpService.qryGroupInfo(groupId));
    }

    @GetMapping("/qrcode")
    public Result qryGrpQrCode(@RequestParam("group_id") String groupId)throws JimException{
        if(groupId==null||groupId.isEmpty()){
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        QrCode ret = new QrCode();
        String userId = RequestContext.getCurrentUserIdFromCtx();
        Map<String,String> qrContent = new HashMap<>();
        qrContent.put("action", "join_group");
        qrContent.put("group_id", groupId);
        qrContent.put("user_id", userId);
        String content = CommonUtil.toJson(qrContent);
        int width = 400;
        int height = 400;
        String format = "png";
        try {
            BitMatrix bitMatrix = new MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, width, height);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, format, baos);
            byte[] imgBs = baos.toByteArray();
            ret.setQrCode(Base64.getUrlEncoder().encodeToString(imgBs));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Result.success(ret);
    }

    @PostMapping("/apply")
    public Result groupApply(@RequestBody GroupInvite grpInvite)throws JimException{
        if(grpInvite==null||grpInvite.getGroupId()==null||grpInvite.getGroupId().isEmpty()){
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        this.grpService.grpJoinApply(grpInvite.getGroupId());
        return new Result(0, "");
    }

    @PostMapping("/invite")
    public Result groupInvite(@RequestBody GroupInvite grpInvite){
        if(grpInvite==null||grpInvite.getGroupId()==null||grpInvite.getGroupId().isEmpty()||grpInvite.getMemberIds()==null||grpInvite.getMemberIds().size()<=0){
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        GroupInviteResp resp = this.grpService.grpInviteMembers(grpInvite.getGroupId(), grpInvite.getMemberIds());
        return Result.success(resp);
    }

    @PostMapping("/quit")
    public Result quitGroup(@RequestBody GroupInfo grpInfo){
        if(grpInfo==null||grpInfo.getGroupId()==null||grpInfo.getGroupId().isEmpty()){
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        this.grpService.quitGroup(grpInfo.getGroupId());
        return Result.success(null);
    }

    @PostMapping("/members/add")
    public Result addGrpMembers(@RequestBody GroupMembersReq req){
        if(req==null||req.getGroupId()==null||req.getGroupId().isEmpty()
                || req.getMemberIds()==null||req.getMemberIds().isEmpty()){
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        this.grpService.addGrpMembers(req);
        return Result.success(null);
    }

    @PostMapping("/members/del")
    public Result delGrpMembers(@RequestBody GroupMembersReq req){
        if(req==null||req.getGroupId()==null||req.getGroupId().isEmpty()
                || req.getMemberIds()==null||req.getMemberIds().isEmpty()){
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        this.grpService.delGrpMembers(req);
        return Result.success(null);
    }

    @GetMapping("/members/list")
    public Result qryGrpMembers(@RequestParam("group_id") String groupId,
            @RequestParam(value = "offset", required = false) String offset,
            @RequestParam(value = "limit", required = false) Integer limit){
        if(groupId==null||groupId.isEmpty()){
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        GroupMemberInfos resp = this.grpService.qryGrpMembers(groupId, offset, limit);
        return Result.success(resp);
    }

    @PostMapping("/members/check")
    public Result checkGroupMembers(@RequestBody CheckGroupMembersReq req){
        if(req==null||req.getGroupId()==null||req.getGroupId().isEmpty()
                || req.getMemberIds()==null||req.getMemberIds().isEmpty()){
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        CheckGroupMembersResp resp = this.grpService.checkGroupMembers(req);
        return Result.success(resp);
    }

    @PostMapping("/members/search")
    public Result searchGroupMembers(@RequestBody SearchGroupMembersReq req){
        if(req==null||req.getGroupId()==null||req.getGroupId().isEmpty()
                || req.getKey()==null||req.getKey().isEmpty()){
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        GroupMemberInfos resp = this.grpService.searchGroupMembers(req);
        return Result.success(resp);
    }

    @PostMapping("/setgrpannouncement")
    public Result setGrpAnnouncement(@RequestBody GroupAnnouncement announcement){
        if(announcement==null||announcement.getGroupId()==null||announcement.getGroupId().isEmpty()){
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        this.grpService.setGrpAnnouncement(announcement);
        return Result.success(null);
    }

    @GetMapping("/getgrpannouncement")
    public Result getGrpAnnouncement(@RequestParam("group_id") String groupId){
        if(groupId==null||groupId.isEmpty()){
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        return Result.success(this.grpService.getGrpAnnouncement(groupId));
    }

    @PostMapping("/setdisplayname")
    public Result setGrpDisplayName(@RequestBody SetGroupDisplayNameReq req){
        if(req==null||req.getGroupId()==null||req.getGroupId().isEmpty()){
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        this.grpService.setGrpDisplayName(req);
        return Result.success(null);
    }

    @GetMapping("/mygroups")
    public Result qryMyGroups(@RequestParam(value = "offset", required = false) String offset,
            @RequestParam(value = "count", required = false) Integer count){
        Groups resp = this.grpService.qryMyGroups(offset, count);
        return Result.success(resp);
    }

    @PostMapping("/mygroups/search")
    public Result searchMyGroups(@RequestBody SearchReq req){
        if(req==null||req.getKeyword()==null||req.getKeyword().isEmpty()){
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        if(req.getLimit()<=0){
            req.setLimit(100);
        }
        return Result.success(this.grpService.searchMyGroups(req));
    }

    @GetMapping("/myapplications")
    public Result qryMyGrpApplications(@RequestParam("start") long start,
            @RequestParam(value = "count", required = false) Integer count,
            @RequestParam(value = "order", required = false) Integer order){
        int safeOrder = normalizeOrder(order);
        long safeStart = normalizeStart(start, safeOrder);
        int safeCount = normalizeCount(count);
        return Result.success(this.grpService.qryMyGrpApplications(safeStart, safeCount, safeOrder));
    }

    @GetMapping("/mypendinginvitations")
    public Result qryMyPendingGrpInvitations(@RequestParam("start") long start,
            @RequestParam(value = "count", required = false) Integer count,
            @RequestParam(value = "order", required = false) Integer order){
        int safeOrder = normalizeOrder(order);
        long safeStart = normalizeStart(start, safeOrder);
        int safeCount = normalizeCount(count);
        return Result.success(this.grpService.qryMyPendingGrpInvitations(safeStart, safeCount, safeOrder));
    }

    @GetMapping("/grpinvitations")
    public Result qryGrpInvitations(@RequestParam("group_id") String groupId,
            @RequestParam("start") long start,
            @RequestParam(value = "count", required = false) Integer count,
            @RequestParam(value = "order", required = false) Integer order){
        if(groupId==null||groupId.isEmpty()){
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        int safeOrder = normalizeOrder(order);
        long safeStart = normalizeStart(start, safeOrder);
        int safeCount = normalizeCount(count);
        return Result.success(this.grpService.qryGrpInvitations(groupId, safeStart, safeCount, safeOrder));
    }

    @GetMapping("/grppendingapplications")
    public Result qryGrpPendingApplications(@RequestParam("group_id") String groupId,
            @RequestParam("start") long start,
            @RequestParam(value = "count", required = false) Integer count,
            @RequestParam(value = "order", required = false) Integer order){
        if(groupId==null||groupId.isEmpty()){
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        int safeOrder = normalizeOrder(order);
        long safeStart = normalizeStart(start, safeOrder);
        int safeCount = normalizeCount(count);
        return Result.success(this.grpService.qryGrpPendingApplications(groupId, safeStart, safeCount, safeOrder));
    }

    @GetMapping("/grpapplications")
    public Result qryGrpApplications(@RequestParam("group_id") String groupId,
            @RequestParam("start") long start,
            @RequestParam(value = "count", required = false) Integer count,
            @RequestParam(value = "order", required = false) Integer order){
        if(groupId==null||groupId.isEmpty()){
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        int safeOrder = normalizeOrder(order);
        long safeStart = normalizeStart(start, safeOrder);
        int safeCount = normalizeCount(count);
        return Result.success(this.grpService.qryGrpApplications(groupId, safeStart, safeCount, safeOrder));
    }

    @PostMapping("/grpapplications/confirm")
    public Result groupConfirm(@RequestBody GroupConfirm confirm){
        if(confirm==null||confirm.getApplicationId()==null||confirm.getApplicationId().isEmpty()){
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        this.grpService.groupConfirm(confirm);
        return Result.success(null);
    }

    private int normalizeCount(Integer count){
        if(count==null){
            return 20;
        }
        if(count<=0||count>50){
            return 20;
        }
        return count;
    }

    private int normalizeOrder(Integer order){
        if(order==null){
            return 0;
        }
        return (order>1||order<0)?0:order;
    }

    private long normalizeStart(long start, int order){
        if(order==0 && start<=0){
            return System.currentTimeMillis();
        }
        return start;
    }
}
