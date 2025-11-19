package com.juggle.chat.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.juggle.chat.apimodels.FriendApplicationItem;
import com.juggle.chat.apimodels.FriendApplications;
import com.juggle.chat.apimodels.FriendIds;
import com.juggle.chat.apimodels.UserInfo;
import com.juggle.chat.apimodels.UserInfos;
import com.juggle.chat.apimodels.SearchFriendsReq;
import com.juggle.chat.apimodels.UserSettings;
import com.juggle.chat.exceptions.JimErrorCode;
import com.juggle.chat.exceptions.JimException;
import com.juggle.chat.interceptors.RequestContext;
import com.juggle.chat.mappers.FriendApplicationMapper;
import com.juggle.chat.mappers.FriendRelMapper;
import com.juggle.chat.mappers.UserMapper;
import com.juggle.chat.models.FriendApplication;
import com.juggle.chat.models.FriendRel;
import com.juggle.chat.models.User;
import com.juggle.chat.models.UserExtKeys;
import com.juggle.chat.utils.N3d;

@Service
public class FriendService {
    @Resource
    private FriendRelMapper friendMapper;
    @Resource
    private UserMapper userMapper;
    @Resource 
    private FriendApplicationMapper friendApplicationMapper;
    @Resource 
    private UserService userService;
    @Resource 
    private FriendCheckService friendCheckService;

    public UserInfos qryFriendsWithPage(int page, int size, String orderTag)throws JimException{
        String appkey = RequestContext.getAppkeyFromCtx();
        String currentUserId = RequestContext.getCurrentUserIdFromCtx();
        UserInfos ret = new UserInfos();
        int safeSize = size <= 0 ? 20 : size;
        int safePage = page <= 0 ? 1 : page;
        long offset = (long) (safePage - 1) * safeSize;
        List<FriendRel> items = this.friendMapper.queryFriendRelsWithPage(appkey, currentUserId, orderTag, offset, safeSize);
        if(!CollectionUtils.isEmpty(items)){
            HashMap<String,UserInfo> userMap = new HashMap<>();
            List<String> friendIds = new ArrayList<>();
            for (FriendRel item : items) {
                UserInfo userInfo = new UserInfo();
                userInfo.setUserId(item.getFriendId());
                userMap.put(item.getFriendId(), userInfo);
                ret.addUserInf(userInfo);
                friendIds.add(item.getFriendId());
            }
            List<User> users = this.userMapper.findByUserIds(appkey, friendIds);
            if(users!=null&&users.size()>0){
                for (User user : users) {
                    UserInfo u = userMap.get(user.getUserId());
                    if(u!=null){
                        u.setNickname(user.getNickname());
                        u.setAvatar(user.getUserPortrait());
                        u.setUserType(user.getUserType());
                    }
                }
            }
        }
        return ret;
    }

    public UserInfos searchFriends(SearchFriendsReq req){
        String appkey = RequestContext.getAppkeyFromCtx();
        String currentUserId = RequestContext.getCurrentUserIdFromCtx();
        long startId = 0L;
        if(req.getOffset()!=null&&!req.getOffset().isEmpty()){
            try {
                startId = N3d.decode(req.getOffset());
            } catch (Exception ignored) {
            }
        }
        long limit = req.getLimit()<=0?100L:req.getLimit();
        List<User> users = this.friendMapper.searchFriendsByName(appkey, currentUserId, req.getKey(), startId, limit);
        UserInfos resp = new UserInfos();
        if(users!=null){
            for (User user : users) {
                UserInfo info = new UserInfo();
                info.setUserId(user.getUserId());
                info.setNickname(user.getNickname());
                info.setAvatar(user.getUserPortrait());
                if(user.getUserType()!=null){
                    info.setUserType(user.getUserType());
                }
                resp.addUserInf(info);
                try {
                    resp.setOffset(N3d.encode(user.getId()));
                } catch (Exception ignored) {
                }
            }
        }
        return resp;
    }

    public void addFriends(FriendIds friendIds)throws JimException{
        if(friendIds==null||CollectionUtils.isEmpty(friendIds.getFriendIds())){
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        String appkey = RequestContext.getAppkeyFromCtx();
        String currentUserId = RequestContext.getCurrentUserIdFromCtx();
        List<String> targets = normalizeFriendIds(friendIds.getFriendIds(), currentUserId);
        if(CollectionUtils.isEmpty(targets)){
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        List<User> existing = this.userMapper.findByUserIds(appkey, targets);
        if(CollectionUtils.isEmpty(existing)){
            throw new JimException(JimErrorCode.ErrorCode_APP_USER_NOT_EXIST);
        }
        Set<String> existIds = existing.stream().map(User::getUserId).collect(Collectors.toSet());
        for (String target : targets) {
            if(!existIds.contains(target)){
                throw new JimException(JimErrorCode.ErrorCode_APP_USER_NOT_EXIST);
            }
        }
        List<FriendRel> rels = new ArrayList<>(targets.size() * 2);
        for (String friendId : targets) {
            rels.add(buildFriendRel(appkey, currentUserId, friendId));
            rels.add(buildFriendRel(appkey, friendId, currentUserId));
        }
        if(!rels.isEmpty()){
            this.friendMapper.batchUpsert(rels);
            //TODO sync to imserver
            //TODO send notify msg
        }
    }

    public void applyFriend(String friendId){
        if(!StringUtils.hasText(friendId)){
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        String appkey = RequestContext.getAppkeyFromCtx();
        String currentUserId = RequestContext.getCurrentUserIdFromCtx();
        if(currentUserId.equals(friendId)){
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        if(this.userMapper.findByUserId(appkey, friendId)==null){
            throw new JimException(JimErrorCode.ErrorCode_APP_USER_NOT_EXIST);
        }
        long now = System.currentTimeMillis();
        if(this.friendCheckService.checkFriend(friendId, currentUserId)){
            saveBidirectionalFriendRel(appkey, currentUserId, friendId);
            saveFriendApplication(appkey, currentUserId, friendId,
                    FriendApplication.FriendApplicationStatus_Agree, now);
            //TODO sync to imserver
            //TODO send notify msg
            return;
        }
        UserSettings settings = this.userService.getUserSettings(friendId);
        int verifyType = settings==null?UserExtKeys.FriendVerifyType_NoNeed:settings.getFriendVerifyType();
        if(verifyType == UserExtKeys.FriendVerifyType_Decline){
            throw new JimException(JimErrorCode.ErrorCode_APP_FRIEND_APPLY_DECLINE);
        }
        if(verifyType == UserExtKeys.FriendVerifyType_Need){
            saveFriendApplication(appkey, currentUserId, friendId,
                    FriendApplication.FriendApplicationStatus_Apply, now);
            //TODO send notify msg
            return;
        }
        saveBidirectionalFriendRel(appkey, currentUserId, friendId);
        saveFriendApplication(appkey, currentUserId, friendId,
                FriendApplication.FriendApplicationStatus_Agree, now);
        //TODO sync to imserver
        //TODO send notify msg
    }

    public void confirmFriend(String sponsorId, boolean isAgree)throws JimException{
        String appkey = RequestContext.getAppkeyFromCtx();
        String currentUserId = RequestContext.getCurrentUserIdFromCtx();
        if(isAgree){
            saveBidirectionalFriendRel(appkey, currentUserId, sponsorId);
            this.friendApplicationMapper.updateStatus(appkey, sponsorId, currentUserId,
                    FriendApplication.FriendApplicationStatus_Agree);
            //TODO sync to imserver
            //TODO send notify msg
        }else{
            this.friendApplicationMapper.updateStatus(appkey, sponsorId, currentUserId, FriendApplication.FriendApplicationStatus_Decline);
        }
    }

    public void delFriends(List<String> friendIds)throws JimException{
        String appkey = RequestContext.getAppkeyFromCtx();
        String currentUserId = RequestContext.getCurrentUserIdFromCtx();
        List<String> targets = normalizeFriendIds(friendIds, currentUserId);
        if(CollectionUtils.isEmpty(targets)){
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        int ok = this.friendMapper.batchDelete(appkey, currentUserId, targets);
        if(ok>0){
            //TODO sync to imserver
        }
    }

    public FriendApplications qryFriendApplications(long start, int count, int order)throws JimException{
        String appkey = RequestContext.getAppkeyFromCtx();
        String currentUserId = RequestContext.getCurrentUserIdFromCtx();
        FriendApplications ret = new FriendApplications();
        List<FriendApplication> apps = this.friendApplicationMapper.queryApplications(appkey, currentUserId, start, count, order>0);
        if(apps!=null&&apps.size()>0){
            for (FriendApplication app : apps) {
                FriendApplicationItem item = new FriendApplicationItem();
                item.setStatus(app.getStatus());
                item.setApplyTime(app.getApplyTime());
                if(currentUserId.equals(app.getSponsorId())){
                    item.setSponsor(true);
                    item.setTargetUser(this.userService.getUserInfo(app.getRecipientId()));
                }else{
                    item.setTargetUser(this.userService.getUserInfo(app.getSponsorId()));
                }
                ret.addFriendApplication(item);
            }
        }
        return ret;
    }

    public FriendApplications qryMyFriendApplications(long start,int count,int order)throws JimException{
        String appkey = RequestContext.getAppkeyFromCtx();
        String currentUserId = RequestContext.getCurrentUserIdFromCtx();
        FriendApplications ret = new FriendApplications();
        List<FriendApplication> apps = this.friendApplicationMapper.queryMyApplications(appkey, currentUserId, start, count, order>0);
        if(apps!=null&&apps.size()>0){
            for (FriendApplication app : apps) {
                FriendApplicationItem item = new FriendApplicationItem();
                item.setRecipient(this.userService.getUserInfo(app.getRecipientId()));
                item.setStatus(app.getStatus());
                item.setApplyTime(app.getApplyTime());
                item.setSponsor(true);
                ret.addFriendApplication(item);
            }
        }
        return ret;
    }

    public FriendApplications qryMyPendingFriendApplications(long start,int count,int order)throws JimException{
        String appkey = RequestContext.getAppkeyFromCtx();
        String currentUserId = RequestContext.getCurrentUserIdFromCtx();
        FriendApplications ret = new FriendApplications();
        List<FriendApplication> apps = this.friendApplicationMapper.queryPendingApplications(appkey, currentUserId, start, count, order>0);
        if(apps!=null&&apps.size()>0){
            for (FriendApplication app : apps) {
                FriendApplicationItem item = new FriendApplicationItem();
                item.setSponsorUser(this.userService.getUserInfo(app.getSponsorId()));
                item.setStatus(app.getStatus());
                item.setApplyTime(app.getApplyTime());
                ret.addFriendApplication(item);
            }
        }
        return ret;
    }

    private List<String> normalizeFriendIds(List<String> rawIds, String currentUserId)throws JimException{
        if(CollectionUtils.isEmpty(rawIds)){
            return List.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String friendId : rawIds) {
            if(!StringUtils.hasText(friendId) || friendId.equals(currentUserId)){
                throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
            }
            normalized.add(friendId);
        }
        return new ArrayList<>(normalized);
    }

    private FriendRel buildFriendRel(String appkey, String userId, String friendId){
        FriendRel rel = new FriendRel();
        rel.setAppkey(appkey);
        rel.setUserId(userId);
        rel.setFriendId(friendId);
        return rel;
    }

    private void saveBidirectionalFriendRel(String appkey, String userId, String friendId){
        List<FriendRel> rels = new ArrayList<>(2);
        rels.add(buildFriendRel(appkey, userId, friendId));
        rels.add(buildFriendRel(appkey, friendId, userId));
        this.friendMapper.batchUpsert(rels);
    }

    private void saveFriendApplication(String appkey, String sponsorId, String recipientId, int status, long applyTime){
        FriendApplication app = new FriendApplication();
        app.setAppkey(appkey);
        app.setSponsorId(sponsorId);
        app.setRecipientId(recipientId);
        app.setStatus(status);
        app.setApplyTime(applyTime);
        this.friendApplicationMapper.upsert(app);
    }
}
