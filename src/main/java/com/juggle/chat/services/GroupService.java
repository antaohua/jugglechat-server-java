package com.juggle.chat.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.google.protobuf.ServiceException;
import com.juggle.chat.apimodels.CheckGroupMembersReq;
import com.juggle.chat.apimodels.CheckGroupMembersResp;
import com.juggle.chat.apimodels.GroupAnnouncement;
import com.juggle.chat.apimodels.GroupAdministratorsReq;
import com.juggle.chat.apimodels.GroupAdministratorsResp;
import com.juggle.chat.apimodels.GroupConfirm;
import com.juggle.chat.apimodels.GroupInfo;
import com.juggle.chat.apimodels.GroupInviteResp;
import com.juggle.chat.apimodels.GroupManagement;
import com.juggle.chat.apimodels.GroupMemberInfo;
import com.juggle.chat.apimodels.GroupMemberInfos;
import com.juggle.chat.apimodels.GroupMembersReq;
import com.juggle.chat.apimodels.GroupOwnerChgReq;
import com.juggle.chat.apimodels.GrpApplicationItem;
import com.juggle.chat.apimodels.Groups;
import com.juggle.chat.apimodels.QryGrpApplicationsResp;
import com.juggle.chat.apimodels.SearchGroupMembersReq;
import com.juggle.chat.apimodels.SearchReq;
import com.juggle.chat.apimodels.SetGroupDisplayNameReq;
import com.juggle.chat.apimodels.SetGroupHisMsgVisibleReq;
import com.juggle.chat.apimodels.SetGroupMemberMuteReq;
import com.juggle.chat.apimodels.SetGroupMuteReq;
import com.juggle.chat.apimodels.SetGroupVerifyTypeReq;
import com.juggle.chat.apimodels.UserInfo;
import com.juggle.chat.apimodels.UserSettings;
import com.juggle.chat.exceptions.JimErrorCode;
import com.juggle.chat.exceptions.JimException;
import com.juggle.chat.interceptors.RequestContext;
import com.juggle.chat.mappers.GroupAdminMapper;
import com.juggle.chat.mappers.GroupExtMapper;
import com.juggle.chat.mappers.GroupMapper;
import com.juggle.chat.mappers.GroupMemberMapper;
import com.juggle.chat.mappers.GrpApplicationMapper;
import com.juggle.chat.models.Group;
import com.juggle.chat.models.GroupAdmin;
import com.juggle.chat.models.GroupExt;
import com.juggle.chat.models.GroupExtKeys;
import com.juggle.chat.models.GroupMember;
import com.juggle.chat.models.GrpApplication;
import com.juggle.chat.models.UserExtKeys;
import com.juggle.chat.utils.CommonUtil;
import com.juggle.chat.utils.N3d;

@Service
public class GroupService {
    private static final int DEFAULT_PAGE_LIMIT = 20;
    private static final int DEFAULT_MEMBER_LIMIT = 100;
    private static final int DEFAULT_ADMIN_LIMIT = 10;

    @Resource
    private GroupMapper grpMapper;
    @Resource
    private GroupMemberMapper grpMemberMapper;
    @Resource
    private GroupExtMapper grpExtMapper;
    @Resource
    private GroupAdminMapper grpAdminMapper;
    @Resource
    private UserService userService;
    @Resource
    private GrpApplicationMapper grpApplicationMapper;

    public void createGroup(GroupInfo grpInfo)throws JimException{
        String appkey = RequestContext.getAppkeyFromCtx();
        String userId = RequestContext.getCurrentUserIdFromCtx();
        if(CollectionUtils.isEmpty(grpInfo.getMemberIds())){
            grpInfo.setMemberIds(new ArrayList<>());
        }
        Set<String> memberIds = new HashSet<>();
        memberIds.add(userId);
        memberIds.addAll(grpInfo.getMemberIds());
        Group group =  new Group();
        group.setAppkey(appkey);
        group.setGroupId(grpInfo.getGroupId());
        group.setGroupName(grpInfo.getGroupName());
        group.setGroupPortrait(grpInfo.getGroupPortrait());
        group.setCreatorId(userId);
        this.grpMapper.create(group);

        List<GroupMember> grpMembers = new ArrayList<>();
        for (String memberId : memberIds) {
            GroupMember member = new GroupMember();
            member.setGroupId(group.getGroupId());
            member.setMemberId(memberId);
            member.setAppkey(appkey);
            grpMembers.add(member);
        }
        this.grpMemberMapper.batchCreate(grpMembers);
        // TODO sync to imserver & send notifications when SDK becomes available
    }

    public void updateGroup(GroupInfo grp)throws JimException{
        String appkey = RequestContext.getAppkeyFromCtx();
        int ok = this.grpMapper.updateGrpName(appkey, grp.getGroupId(), grp.getGroupName(), grp.getGroupPortrait());
        if(ok>0){
            // TODO sync to imserver & send notify msg
        }
    }

    public void dissolveGroup(String groupId)throws JimException{
        String appkey = RequestContext.getAppkeyFromCtx();
        String requester = RequestContext.getCurrentUserIdFromCtx();
        Group group = this.grpMapper.findById(appkey, groupId);
        if(group==null || !requester.equals(group.getCreatorId())){
            throw new JimException(JimErrorCode.ErrorCode_APP_GROUP_DEFAULT);
        }
        int ok = this.grpMapper.delete(appkey, groupId);
        if(ok>0){
            this.grpMemberMapper.deleteByGroupId(appkey, groupId);
            // TODO send notify msg & sync to imserver
        }
    }

    public void quitGroup(String groupId)throws JimException{
        String appkey = RequestContext.getAppkeyFromCtx();
        String userId = RequestContext.getCurrentUserIdFromCtx();
        this.grpMemberMapper.batchDelete(appkey, groupId, List.of(userId));
        // TODO sync to imserver
    }

    public GroupInfo qryGroupInfo(String groupId)throws JimException{
        if(groupId==null||groupId.isEmpty()){
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        String appkey = RequestContext.getAppkeyFromCtx();
        Group group = this.grpMapper.findById(appkey, groupId);
        if(group==null){
            throw new JimException(JimErrorCode.ErrorCode_APP_GROUP_DEFAULT);
        }
        GroupInfo grpInfo = new GroupInfo();
        grpInfo.setGroupId(groupId);
        grpInfo.setGroupName(group.getGroupName());
        grpInfo.setGroupPortrait(group.getGroupPortrait());
        grpInfo.setMemberCount(this.grpMemberMapper.countByGroup(appkey, groupId));
        Map<String,Boolean> administrators = buildAdministrators(appkey, groupId);
        grpInfo.setGrpManagement(buildGroupManagement(appkey, group, administrators.size()));

        String requesterId = RequestContext.getCurrentUserIdFromCtx();
        GroupMember selfMember = this.grpMemberMapper.find(appkey, groupId, requesterId);
        if(selfMember==null){
            grpInfo.setMyRole(GroupMember.GrpMemberRole_GrpNotMember);
            return grpInfo;
        }
        grpInfo.setGrpDisplayName(selfMember.getGrpDisplayName());
        grpInfo.setMyRole(resolveRole(requesterId, group.getCreatorId(), administrators));
        if(group.getCreatorId()!=null && !group.getCreatorId().isEmpty()){
            grpInfo.setOwner(buildOwnerInfo(group.getCreatorId()));
        }
        List<GroupMember> members = this.grpMemberMapper.queryMembers(appkey, groupId, 0L, 20L);
        if(members!=null && !members.isEmpty()){
            List<GroupMemberInfo> memberInfos = new ArrayList<>();
            for (GroupMember member : members) {
                GroupMemberInfo info = toMemberInfo(member, group.getCreatorId(), administrators);
                memberInfos.add(info);
                if(member.getId()!=null){
                    try {
                        grpInfo.setMemberOffset(N3d.encode(member.getId()));
                    } catch (ServiceException ignored) {
                    }
                }
            }
            grpInfo.setMembers(memberInfos);
        }
        return grpInfo;
    }

    public void grpJoinApply(String groupId)throws JimException{
        String appkey = RequestContext.getAppkeyFromCtx();
        String userId = RequestContext.getCurrentUserIdFromCtx();
        GroupMember member = this.grpMemberMapper.find(appkey, groupId, userId);
        if(member!=null){
            throw new JimException(JimErrorCode.ErrorCode_APP_GROUP_MEMBEREXISTED);
        }
        member = new GroupMember();
        member.setGroupId(groupId);
        member.setMemberId(userId);
        member.setAppkey(appkey);
        this.grpMemberMapper.create(member);
        // TODO sync & notify
    }

    public GroupInviteResp grpInviteMembers(String groupId, List<String> memberIds)throws JimException{
        if(memberIds==null||memberIds.isEmpty()){
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        String appkey = RequestContext.getAppkeyFromCtx();
        Group group = requireGroup(appkey, groupId);
        Map<String,Boolean> admins = buildAdministrators(appkey, groupId);
        GroupInviteResp resp = new GroupInviteResp();
        resp.setResults(new HashMap<>());
        resp.setReason(GroupInviteResp.GrpInviteReason_InviteSucc);
        GroupManagement settings = buildGroupManagement(appkey, group, admins.size());
        String requesterId = RequestContext.getCurrentUserIdFromCtx();
        long now = System.currentTimeMillis();
        if(settings.getGroupVerifyType() == UserExtKeys.GrpVerifyType_DeclineGroup){
            resp.setReason(GroupInviteResp.GrpInviteReason_InviteDecline);
            return resp;
        }
        List<String> directAddMemberIds = new ArrayList<>();
        if(settings.getGroupVerifyType() == UserExtKeys.GrpVerifyType_NeedGrpVerify){
            resp.setReason(GroupInviteResp.GrpInviteReason_InviteSendOut);
            for (String memberId : memberIds) {
                createInviteApplication(appkey, groupId, memberId, requesterId, now);
                resp.getResults().put(memberId, GroupInviteResp.GrpInviteReason_InviteSendOut);
            }
            return resp;
        }
        for (String memberId : memberIds) {
            UserSettings targetSettings = this.userService.getUserSettings(memberId);
            int verifyType = targetSettings.getGrpVerifyType();
            if(verifyType == UserExtKeys.GrpVerifyType_DeclineGroup){
                resp.getResults().put(memberId, GroupInviteResp.GrpInviteReason_InviteDecline);
            }else if(verifyType == UserExtKeys.GrpVerifyType_NeedGrpVerify){
                createInviteApplication(appkey, groupId, memberId, requesterId, now);
                resp.getResults().put(memberId, GroupInviteResp.GrpInviteReason_InviteSendOut);
                resp.setReason(GroupInviteResp.GrpInviteReason_InviteSendOut);
            }else{
                directAddMemberIds.add(memberId);
                resp.getResults().put(memberId, GroupInviteResp.GrpInviteReason_InviteSucc);
            }
        }
        if(!directAddMemberIds.isEmpty()){
            List<String> toCreate = filterExistingMembers(appkey, groupId, directAddMemberIds);
            if(!toCreate.isEmpty()){
                List<GroupMember> grpMembers = new ArrayList<>();
                for (String memberId : toCreate) {
                    GroupMember grpMember = new GroupMember();
                    grpMember.setGroupId(groupId);
                    grpMember.setMemberId(memberId);
                    grpMember.setAppkey(appkey);
                    grpMembers.add(grpMember);
                }
                this.grpMemberMapper.batchCreate(grpMembers);
                // TODO sync to imserver & send notify msg
            }
        }
        return resp;
    }

    public void addGrpMembers(GroupMembersReq req)throws JimException{
        if(req==null||CollectionUtils.isEmpty(req.getMemberIds())){
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        String appkey = RequestContext.getAppkeyFromCtx();
        List<String> toCreate = filterExistingMembers(appkey, req.getGroupId(), req.getMemberIds());
        if(toCreate.isEmpty()){
            return;
        }
        List<GroupMember> grpMembers = new ArrayList<>();
        for (String memberId : toCreate) {
            GroupMember grpMember = new GroupMember();
            grpMember.setGroupId(req.getGroupId());
            grpMember.setMemberId(memberId);
            grpMember.setAppkey(appkey);
            grpMembers.add(grpMember);
        }
        this.grpMemberMapper.batchCreate(grpMembers);
        // TODO sync & notify
    }

    public void delGrpMembers(GroupMembersReq req)throws JimException{
        if(req==null||CollectionUtils.isEmpty(req.getMemberIds())){
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        this.grpMemberMapper.batchDelete(RequestContext.getAppkeyFromCtx(), req.getGroupId(), req.getMemberIds());
        // TODO sync & notify
    }

    public GroupMemberInfos qryGrpMembers(String groupId, String offset, Integer limit){
        String appkey = RequestContext.getAppkeyFromCtx();
        long startId = decodeOffset(offset);
        long fetchLimit = limit!=null && limit>0 ? limit : DEFAULT_MEMBER_LIMIT;
        List<GroupMember> members = this.grpMemberMapper.queryMembers(appkey, groupId, startId, fetchLimit);
        Map<String,Boolean> admins = buildAdministrators(appkey, groupId);
        Group group = this.grpMapper.findById(appkey, groupId);
        GroupMemberInfos infos = new GroupMemberInfos();
        if(members!=null){
            for (GroupMember member : members) {
                GroupMemberInfo info = toMemberInfo(member, group!=null?group.getCreatorId():null, admins);
                infos.addMember(info);
                if(member.getId()!=null){
                    try {
                        infos.setOffset(N3d.encode(member.getId()));
                    } catch (ServiceException ignored) {
                    }
                }
            }
        }
        return infos;
    }

    public CheckGroupMembersResp checkGroupMembers(CheckGroupMembersReq req){
        String appkey = RequestContext.getAppkeyFromCtx();
        CheckGroupMembersResp resp = new CheckGroupMembersResp();
        resp.setGroupId(req.getGroupId());
        if(CollectionUtils.isEmpty(req.getMemberIds())){
            return resp;
        }
        for (String memberId : req.getMemberIds()) {
            resp.putExist(memberId, false);
        }
        List<GroupMember> members = this.grpMemberMapper.findByMemberIds(appkey, req.getGroupId(), req.getMemberIds());
        if(members!=null){
            for (GroupMember member : members) {
                resp.putExist(member.getMemberId(), true);
            }
        }
        return resp;
    }

    public GroupMemberInfos searchGroupMembers(SearchGroupMembersReq req){
        String appkey = RequestContext.getAppkeyFromCtx();
        long startId = decodeOffset(req.getOffset());
        long fetchLimit = req.getLimit()>0 ? req.getLimit() : DEFAULT_MEMBER_LIMIT;
        List<GroupMember> members = this.grpMemberMapper.searchMembersByName(appkey, req.getGroupId(), req.getKey(), startId, fetchLimit);
        Map<String,Boolean> admins = buildAdministrators(appkey, req.getGroupId());
        Group group = this.grpMapper.findById(appkey, req.getGroupId());
        GroupMemberInfos infos = new GroupMemberInfos();
        if(members!=null){
            for (GroupMember member : members) {
                GroupMemberInfo info = toMemberInfo(member, group!=null?group.getCreatorId():null, admins);
                infos.addMember(info);
                if(member.getId()!=null){
                    try {
                        infos.setOffset(N3d.encode(member.getId()));
                    } catch (ServiceException ignored) {
                    }
                }
            }
        }
        return infos;
    }

    public void setGrpAnnouncement(GroupAnnouncement announcement){
        GroupExt ext = new GroupExt();
        ext.setAppkey(RequestContext.getAppkeyFromCtx());
        ext.setGroupId(announcement.getGroupId());
        ext.setItemKey(GroupExtKeys.GrpExtKey_GrpAnnouncement);
        ext.setItemValue(announcement.getContent());
        ext.setItemType(1);
        this.grpExtMapper.upsert(ext);
        // TODO send announcement message via IM SDK when available
    }

    public GroupAnnouncement getGrpAnnouncement(String groupId){
        GroupAnnouncement announcement = new GroupAnnouncement();
        announcement.setGroupId(groupId);
        GroupExt ext = this.grpExtMapper.find(RequestContext.getAppkeyFromCtx(), groupId, GroupExtKeys.GrpExtKey_GrpAnnouncement);
        if(ext!=null){
            announcement.setContent(ext.getItemValue());
        }
        return announcement;
    }

    public void setGrpDisplayName(SetGroupDisplayNameReq req){
        this.grpMemberMapper.updateGrpDisplayName(RequestContext.getAppkeyFromCtx(), req.getGroupId(),
                RequestContext.getCurrentUserIdFromCtx(), req.getGrpDisplayName());
    }

    public Groups qryMyGroups(String offset, Integer count){
        String appkey = RequestContext.getAppkeyFromCtx();
        String memberId = RequestContext.getCurrentUserIdFromCtx();
        long startId = decodeOffset(offset);
        long limit = count!=null && count>0 ? count : DEFAULT_PAGE_LIMIT;
        List<GroupMember> groups = this.grpMemberMapper.queryGroupsByMemberId(appkey, memberId, startId, limit);
        return buildGroupSummaries(groups);
    }

    public Groups searchMyGroups(SearchReq req){
        String appkey = RequestContext.getAppkeyFromCtx();
        String memberId = RequestContext.getCurrentUserIdFromCtx();
        if(req==null||req.getKeyword()==null||req.getKeyword().isEmpty()){
            return new Groups();
        }
        long startId = decodeOffset(req.getOffset());
        long limit = req.getLimit()>0 ? req.getLimit() : 100L;
        List<GroupMember> groups = this.grpMemberMapper.searchGroupsByMemberId(appkey, memberId, req.getKeyword(), startId, limit);
        return buildGroupSummaries(groups);
    }

    public QryGrpApplicationsResp qryMyGrpApplications(long start, Integer count, Integer order){
        return buildApplicationResp(this.grpApplicationMapper.queryMyGrpApplications(RequestContext.getAppkeyFromCtx(),
                RequestContext.getCurrentUserIdFromCtx(), adjustStart(start), adjustCount(count), isPositive(order)), null);
    }

    public QryGrpApplicationsResp qryMyPendingGrpInvitations(long start, Integer count, Integer order){
        return buildApplicationResp(this.grpApplicationMapper.queryMyPendingGrpInvitations(RequestContext.getAppkeyFromCtx(),
                RequestContext.getCurrentUserIdFromCtx(), adjustStart(start), adjustCount(count), isPositive(order)), null);
    }

    public QryGrpApplicationsResp qryGrpInvitations(String groupId, long start, Integer count, Integer order){
        return buildApplicationResp(this.grpApplicationMapper.queryGrpInvitations(RequestContext.getAppkeyFromCtx(), groupId,
                adjustStart(start), adjustCount(count), isPositive(order)), null);
    }

    public QryGrpApplicationsResp qryGrpPendingApplications(String groupId, long start, Integer count, Integer order){
        return buildApplicationResp(this.grpApplicationMapper.queryGrpPendingApplications(RequestContext.getAppkeyFromCtx(), groupId,
                adjustStart(start), adjustCount(count), isPositive(order)), null);
    }

    public QryGrpApplicationsResp qryGrpApplications(String groupId, long start, Integer count, Integer order){
        GroupInfo grpInfo = new GroupInfo();
        grpInfo.setGroupId(groupId);
        Group group = this.grpMapper.findById(RequestContext.getAppkeyFromCtx(), groupId);
        if(group!=null){
            grpInfo.setGroupName(group.getGroupName());
            grpInfo.setGroupPortrait(group.getGroupPortrait());
        }
        return buildApplicationResp(this.grpApplicationMapper.queryGrpApplications(RequestContext.getAppkeyFromCtx(), groupId,
                adjustStart(start), adjustCount(count), isPositive(order)), grpInfo);
    }

    public void groupConfirm(GroupConfirm confirm)throws JimException{
        String appkey = RequestContext.getAppkeyFromCtx();
        long id = decodeOffset(confirm.getApplicationId());
        if(id<=0){
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        GrpApplication application = this.grpApplicationMapper.findById(appkey, id);
        if(application==null){
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        if(application.getApplyType()!=null && application.getApplyType()==GrpApplication.GrpApplicationType_Invite){
            if(confirm.isAgree()){
                this.grpApplicationMapper.updateStatus(id, GrpApplication.GrpApplicationStatus_AgreeInvite);
                GroupMembersReq req = new GroupMembersReq();
                req.setGroupId(application.getGroupId());
                req.setMemberIds(List.of(application.getRecipientId()));
                addGrpMembers(req);
            }else{
                this.grpApplicationMapper.updateStatus(id, GrpApplication.GrpApplicationStatus_DeclineInvite);
            }
        }
    }

    public void chgGroupOwner(GroupOwnerChgReq req){
        this.grpMapper.updateCreatorId(RequestContext.getAppkeyFromCtx(), req.getGroupId(), req.getOwnerId());
        // TODO notify members
    }

    public void setGroupMute(SetGroupMuteReq req){
        this.grpMapper.updateGroupMuteStatus(RequestContext.getAppkeyFromCtx(), req.getGroupId(), req.getMute());
        // TODO sync to imserver
    }

    public void setGroupMembersMute(SetGroupMemberMuteReq req){
        this.grpMemberMapper.updateMute(RequestContext.getAppkeyFromCtx(), req.getGroupId(), req.getMute(), req.getMemberIds(), 0);
        // TODO sync to imserver
    }

    public void setGrpVerifyType(SetGroupVerifyTypeReq req){
        GroupExt ext = new GroupExt();
        ext.setAppkey(RequestContext.getAppkeyFromCtx());
        ext.setGroupId(req.getGroupId());
        ext.setItemKey(GroupExtKeys.GrpExtKey_GrpVerifyType);
        ext.setItemValue(CommonUtil.int2String(req.getVerifyType()));
        ext.setItemType(1);
        this.grpExtMapper.upsert(ext);
    }

    public void setGrpHisMsgVisible(SetGroupHisMsgVisibleReq req){
        GroupExt ext = new GroupExt();
        ext.setAppkey(RequestContext.getAppkeyFromCtx());
        ext.setGroupId(req.getGroupId());
        ext.setItemKey(GroupExtKeys.GrpExtKey_HideGrpMsg);
        ext.setItemValue(req.getGroupHisMsgVisible()>0 ? "0" : "1");
        ext.setItemType(1);
        this.grpExtMapper.upsert(ext);
        // TODO sync to imserver when SDK available
    }

    public void setGrpManagementConfs(GroupManagement management){
        List<GroupExt> exts = new ArrayList<>();
        String appkey = RequestContext.getAppkeyFromCtx();
        if(management.getGroupEditMsgRight()!=null){
            exts.add(buildSettingExt(appkey, management.getGroupId(), GroupExtKeys.GrpExtKey_GrpEditMsgRight,
                    management.getGroupEditMsgRight()));
        }
        if(management.getGroupAddMemberRight()!=null){
            exts.add(buildSettingExt(appkey, management.getGroupId(), GroupExtKeys.GrpExtKey_AddMemberRight,
                    management.getGroupAddMemberRight()));
        }
        if(management.getGroupMentionAllRight()!=null){
            exts.add(buildSettingExt(appkey, management.getGroupId(), GroupExtKeys.GrpExtKey_MentionAllRight,
                    management.getGroupMentionAllRight()));
        }
        if(management.getGroupTopMsgRight()!=null){
            exts.add(buildSettingExt(appkey, management.getGroupId(), GroupExtKeys.GrpExtKey_TopMsgRight,
                    management.getGroupTopMsgRight()));
        }
        if(management.getGroupSendMsgRight()!=null){
            exts.add(buildSettingExt(appkey, management.getGroupId(), GroupExtKeys.GrpExtKey_SendMsgRight,
                    management.getGroupSendMsgRight()));
        }
        if(management.getGroupSetMsgLifeRight()!=null){
            exts.add(buildSettingExt(appkey, management.getGroupId(), GroupExtKeys.GrpExtKey_SetMsgLifeRight,
                    management.getGroupSetMsgLifeRight()));
        }
        if(!exts.isEmpty()){
            this.grpExtMapper.batchUpsert(exts);
        }
    }

    public void addGrpAdministrator(GroupAdministratorsReq req){
        String appkey = RequestContext.getAppkeyFromCtx();
        if(req.getAdminIds()!=null){
            for (String adminId : req.getAdminIds()) {
                GroupAdmin admin = new GroupAdmin();
                admin.setAppkey(appkey);
                admin.setGroupId(req.getGroupId());
                admin.setAdminId(adminId);
                this.grpAdminMapper.upsert(admin);
            }
        }
    }

    public void delGrpAdministrator(GroupAdministratorsReq req){
        if(req.getAdminIds()!=null && !req.getAdminIds().isEmpty()){
            this.grpAdminMapper.batchDel(RequestContext.getAppkeyFromCtx(), req.getGroupId(), req.getAdminIds());
        }
    }

    public GroupAdministratorsResp qryGrpAdministrator(String groupId){
        String appkey = RequestContext.getAppkeyFromCtx();
        GroupAdministratorsResp resp = new GroupAdministratorsResp();
        resp.setGroupId(groupId);
        List<GroupAdmin> admins = this.grpAdminMapper.qryAdmins(appkey, groupId);
        if(admins!=null){
            for (GroupAdmin admin : admins) {
                GroupMemberInfo info = new GroupMemberInfo();
                info.setUserId(admin.getAdminId());
                info.setRole(GroupMember.GrpMemberRole_GrpAdmin);
                UserInfo user = this.userService.getUserInfo(admin.getAdminId());
                info.setNickname(user.getNickname());
                info.setAvatar(user.getAvatar());
                info.setMemberType(user.getUserType());
                resp.addAdministrator(info);
            }
        }
        return resp;
    }

    private Group requireGroup(String appkey, String groupId)throws JimException{
        Group group = this.grpMapper.findById(appkey, groupId);
        if(group==null){
            throw new JimException(JimErrorCode.ErrorCode_APP_GROUP_DEFAULT);
        }
        return group;
    }

    private Map<String,Boolean> buildAdministrators(String appkey, String groupId){
        Map<String,Boolean> administrators = new HashMap<>();
        List<GroupAdmin> admins = this.grpAdminMapper.qryAdmins(appkey, groupId);
        if(admins!=null){
            for (GroupAdmin admin : admins) {
                administrators.put(admin.getAdminId(), true);
            }
        }
        return administrators;
    }

    private GroupManagement buildGroupManagement(String appkey, Group group, int adminCount){
        GroupManagement management = new GroupManagement();
        management.setGroupId(group.getGroupId());
        management.setGroupMute(group.getIsMute()==null?0:group.getIsMute());
        management.setMaxAdminCount(DEFAULT_ADMIN_LIMIT);
        management.setAdminCount(adminCount);
        management.setGroupVerifyType(0);
        management.setGroupHisMsgVisible(1);
        management.setGroupEditMsgRight(7);
        management.setGroupAddMemberRight(7);
        management.setGroupMentionAllRight(7);
        management.setGroupTopMsgRight(7);
        management.setGroupSendMsgRight(7);
        management.setGroupSetMsgLifeRight(7);
        List<GroupExt> exts = this.grpExtMapper.qryExtFields(appkey, group.getGroupId());
        if(exts!=null){
            for (GroupExt ext : exts) {
                String key = ext.getItemKey();
                String value = ext.getItemValue();
                if(GroupExtKeys.GrpExtKey_GrpVerifyType.equals(key)){
                    if(value!=null && !value.isEmpty()){
                        management.setGroupVerifyType(CommonUtil.string2Int(value));
                    }
                }else if(GroupExtKeys.GrpExtKey_HideGrpMsg.equals(key)){
                    management.setGroupHisMsgVisible("0".equals(value)?1:0);
                }else if(GroupExtKeys.GrpExtKey_GrpEditMsgRight.equals(key)){
                    if(value!=null && !value.isEmpty()){
                        management.setGroupEditMsgRight(CommonUtil.string2Int(value));
                    }
                }else if(GroupExtKeys.GrpExtKey_AddMemberRight.equals(key)){
                    if(value!=null && !value.isEmpty()){
                        management.setGroupAddMemberRight(CommonUtil.string2Int(value));
                    }
                }else if(GroupExtKeys.GrpExtKey_MentionAllRight.equals(key)){
                    if(value!=null && !value.isEmpty()){
                        management.setGroupMentionAllRight(CommonUtil.string2Int(value));
                    }
                }else if(GroupExtKeys.GrpExtKey_TopMsgRight.equals(key)){
                    if(value!=null && !value.isEmpty()){
                        management.setGroupTopMsgRight(CommonUtil.string2Int(value));
                    }
                }else if(GroupExtKeys.GrpExtKey_SendMsgRight.equals(key)){
                    if(value!=null && !value.isEmpty()){
                        management.setGroupSendMsgRight(CommonUtil.string2Int(value));
                    }
                }else if(GroupExtKeys.GrpExtKey_SetMsgLifeRight.equals(key)){
                    if(value!=null && !value.isEmpty()){
                        management.setGroupSetMsgLifeRight(CommonUtil.string2Int(value));
                    }
                }
            }
        }
        return management;
    }

    private GroupMemberInfo buildOwnerInfo(String userId){
        UserInfo owner = this.userService.getUserInfo(userId);
        GroupMemberInfo info = new GroupMemberInfo();
        info.setUserId(owner.getUserId());
        info.setNickname(owner.getNickname());
        info.setAvatar(owner.getAvatar());
        info.setMemberType(owner.getUserType());
        info.setRole(GroupMember.GrpMemberRole_GrpCreator);
        return info;
    }

    private GroupMemberInfo toMemberInfo(GroupMember member, String creatorId, Map<String,Boolean> admins){
        GroupMemberInfo info = new GroupMemberInfo();
        info.setUserId(member.getMemberId());
        info.setNickname(member.getNickname());
        info.setAvatar(member.getUserPortrait());
        if(member.getMemberType()!=null){
            info.setMemberType(member.getMemberType());
        }
        info.setIsMute(member.getIsMute());
        info.setRole(resolveRole(member.getMemberId(), creatorId, admins));
        return info;
    }

    private int resolveRole(String memberId, String creatorId, Map<String,Boolean> admins){
        if(memberId!=null && memberId.equals(creatorId)){
            return GroupMember.GrpMemberRole_GrpCreator;
        }
        if(memberId!=null && Boolean.TRUE.equals(admins.get(memberId))){
            return GroupMember.GrpMemberRole_GrpAdmin;
        }
        return GroupMember.GrpMemberRole_GrpMember;
    }

    private long decodeOffset(String offset){
        if(offset==null||offset.isEmpty()){
            return 0L;
        }
        try {
            return N3d.decode(offset);
        } catch (ServiceException e) {
            try {
                return Long.parseLong(offset);
            } catch (NumberFormatException ex) {
                return 0L;
            }
        }
    }

    private long adjustStart(long start){
        if(start<=0){
            return System.currentTimeMillis();
        }
        return start;
    }

    private long adjustCount(Integer count){
        if(count==null||count<=0){
            return DEFAULT_PAGE_LIMIT;
        }
        return count;
    }

    private boolean isPositive(Integer order){
        return order!=null && order>0;
    }

    private Groups buildGroupSummaries(List<GroupMember> groups){
        Groups resp = new Groups();
        if(groups!=null){
            for (GroupMember groupMember : groups) {
                GroupInfo info = new GroupInfo();
                info.setGroupId(groupMember.getGroupId());
                info.setGroupName(groupMember.getGroupName());
                info.setGroupPortrait(groupMember.getGroupPortrait());
                resp.addGroup(info);
                if(groupMember.getId()!=null){
                    try {
                        resp.setOffset(N3d.encode(groupMember.getId()));
                    } catch (ServiceException ignored) {
                    }
                }
            }
        }
        return resp;
    }

    private QryGrpApplicationsResp buildApplicationResp(List<GrpApplication> applications, GroupInfo grpInfo){
        QryGrpApplicationsResp resp = new QryGrpApplicationsResp();
        if(applications!=null){
            for (GrpApplication application : applications) {
                GrpApplicationItem item = new GrpApplicationItem();
                if(grpInfo!=null){
                    item.setGrpInfo(grpInfo);
                }else{
                    GroupInfo info = new GroupInfo();
                    info.setGroupId(application.getGroupId());
                    item.setGrpInfo(info);
                }
                item.setApplyType(application.getApplyType()==null?0:application.getApplyType());
                item.setApplyTime(application.getApplyTime()==null?0L:application.getApplyTime());
                item.setStatus(application.getStatus()==null?0:application.getStatus());
                if(application.getId()!=null){
                    try {
                        item.setApplicationId(N3d.encode(application.getId()));
                    } catch (ServiceException ignored) {
                    }
                }
                if(application.getSponsorId()!=null){
                    item.setSponsor(this.userService.getUserInfo(application.getSponsorId()));
                }
                if(application.getRecipientId()!=null){
                    item.setRecipient(this.userService.getUserInfo(application.getRecipientId()));
                }
                if(application.getInviterId()!=null){
                    item.setInviter(this.userService.getUserInfo(application.getInviterId()));
                }
                if(application.getOperatorId()!=null){
                    item.setOperator(this.userService.getUserInfo(application.getOperatorId()));
                }
                resp.addItem(item);
            }
        }
        return resp;
    }

    private GroupExt buildSettingExt(String appkey, String groupId, String key, Integer value){
        GroupExt ext = new GroupExt();
        ext.setAppkey(appkey);
        ext.setGroupId(groupId);
        ext.setItemKey(key);
        ext.setItemValue(CommonUtil.int2String(value));
        ext.setItemType(1);
        return ext;
    }

    private void createInviteApplication(String appkey, String groupId, String memberId, String inviterId, long applyTime){
        GrpApplication grpApp = new GrpApplication();
        grpApp.setAppkey(appkey);
        grpApp.setGroupId(groupId);
        grpApp.setApplyType(GrpApplication.GrpApplicationType_Invite);
        grpApp.setRecipientId(memberId);
        grpApp.setInviterId(inviterId);
        grpApp.setApplyTime(applyTime);
        grpApp.setStatus(GrpApplication.GrpApplicationStatus_Invite);
        this.grpApplicationMapper.inviteUpsert(grpApp);
    }

    private List<String> filterExistingMembers(String appkey, String groupId, List<String> memberIds){
        if(CollectionUtils.isEmpty(memberIds)){
            return List.of();
        }
        List<GroupMember> existing = this.grpMemberMapper.findByMemberIds(appkey, groupId, memberIds);
        if(existing==null || existing.isEmpty()){
            return memberIds;
        }
        Set<String> existSet = new HashSet<>();
        for (GroupMember member : existing) {
            existSet.add(member.getMemberId());
        }
        List<String> filtered = new ArrayList<>();
        for (String memberId : memberIds) {
            if(!existSet.contains(memberId)){
                filtered.add(memberId);
            }
        }
        return filtered;
    }
}
