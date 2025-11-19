package com.juggle.chat.services;

import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.Resource;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.juggle.chat.apimodels.BindEmailReq;
import com.juggle.chat.apimodels.BindPhoneReq;
import com.juggle.chat.apimodels.BlockUsers;
import com.juggle.chat.apimodels.BlockUsersReq;
import com.juggle.chat.apimodels.SearchReq;
import com.juggle.chat.apimodels.SetUserAccountReq;
import com.juggle.chat.apimodels.UpdUserPassReq;
import com.juggle.chat.apimodels.UserInfo;
import com.juggle.chat.apimodels.UserInfos;
import com.juggle.chat.apimodels.UserIds;
import com.juggle.chat.apimodels.UserSettings;
import com.juggle.chat.exceptions.JimErrorCode;
import com.juggle.chat.exceptions.JimException;
import com.juggle.chat.interceptors.RequestContext;
import com.juggle.chat.mappers.BlockUserMapper;
import com.juggle.chat.mappers.UserExtMapper;
import com.juggle.chat.mappers.UserMapper;
import com.juggle.chat.models.BlockUser;
import com.juggle.chat.models.User;
import com.juggle.chat.models.UserExt;
import com.juggle.chat.models.UserExtKeys;
import com.juggle.chat.utils.CommonUtil;
import com.juggle.chat.utils.N3d;
import com.juggle.im.JuggleIm;

@Service
public class UserService {
    @Resource
    private UserMapper userMapper;
    @Resource
    private UserExtMapper userExtMapper;
    @Resource
    private BlockUserMapper blockUserMapper;
    @Resource
    private FriendCheckService friendCheckService;
    @Resource
    private VerificationCodeService verificationCodeService;

    public UserInfo qryUserInfo(String userId){
        UserInfo user = this.getUserInfo(userId);
        boolean isSelf = userId.equals(RequestContext.getCurrentUserIdFromCtx());
        if(isSelf){
            //add usersettings
            user.setSettings(this.getUserSettings(userId));
        }else{
            //check friend
            user.setFriend(friendCheckService.checkFriend(RequestContext.getCurrentUserIdFromCtx(), userId));
        }
        maskUserContacts(user, isSelf);
        return user;
    }

    public UserSettings getUserSettings(String userId){
        UserSettings settings = new UserSettings();
        List<UserExt> exts = userExtMapper.qryExtFields(RequestContext.getAppkeyFromCtx(), userId);
        if(exts!=null&&exts.size()>0){
            for (UserExt ext : exts) {
                if(ext.getItemKey().equals(UserExtKeys.UserExtKey_Language)){
                    settings.setLanguage(ext.getItemValue());
                }else if(ext.getItemKey().equals(UserExtKeys.UserExtKey_Undisturb)){
                    settings.setUndisturb(ext.getItemValue());
                }else if(ext.getItemKey().equals(UserExtKeys.UserExtKey_FriendVerifyType)){
                    settings.setFriendVerifyType(CommonUtil.string2Int(ext.getItemValue()));
                }else if(ext.getItemKey().equals(UserExtKeys.UserExtKey_GrpVerifyType)){
                    settings.setGrpVerifyType(CommonUtil.string2Int(ext.getItemValue()));
                }
            }
        }
        return settings;
    }

    public void updateUser(UserInfo user)throws JimException{
        String appkey = RequestContext.getAppkeyFromCtx();
        int ok = userMapper.update(appkey,user.getUserId(),user.getNickname(),user.getAvatar());
        if(ok>0){
            JuggleIm sdk = ImSdkService.getJimSdk(appkey);
            try {
                sdk.user.register(new com.juggle.im.models.user.UserInfo(user.getUserId(), user.getNickname(), user.getAvatar()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void updatePass(UpdUserPassReq req)throws JimException{
        if(req==null||req.getUserId()==null||req.getUserId().isEmpty()){
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        String appkey = RequestContext.getAppkeyFromCtx();
        String requesterId = RequestContext.getCurrentUserIdFromCtx();
        if(!requesterId.equals(req.getUserId())){
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        User user = this.userMapper.findByUserId(appkey, req.getUserId());
        if(user==null){
            throw new JimException(JimErrorCode.ErrorCode_APP_USER_NOT_EXIST);
        }
        String oldPass = CommonUtil.sha1(req.getPassword());
        if(user.getLoginPass()!=null&&!user.getLoginPass().isEmpty()&&!user.getLoginPass().equals(oldPass)){
            throw new JimException(JimErrorCode.ErrorCode_APP_LOGIN_ERR_PASS);
        }
        this.userMapper.updatePass(appkey, req.getUserId(), CommonUtil.sha1(req.getNewPassword()));
    }

    public void setLoginAccount(SetUserAccountReq req)throws JimException{
        if(req==null||req.getAccount()==null||req.getAccount().isEmpty()){
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        if(!req.getAccount().matches("[A-Za-z0-9]{6,20}")){
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        String appkey = RequestContext.getAppkeyFromCtx();
        User exist = this.userMapper.findByAccount(appkey, req.getAccount());
        if(exist!=null){
            throw new JimException(JimErrorCode.ErrorCode_APP_USER_EXISTED);
        }
        this.userMapper.updateAccount(appkey, RequestContext.getCurrentUserIdFromCtx(), req.getAccount());
    }

    public void bindEmailSend(BindEmailReq req)throws JimException{
        if(req==null||req.getEmail()==null||req.getEmail().isEmpty()){
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        User exist = this.userMapper.findByEmail(RequestContext.getAppkeyFromCtx(), req.getEmail());
        if(exist!=null){
            throw new JimException(JimErrorCode.ErrorCode_APP_EMAIL_EXIST);
        }
        this.verificationCodeService.issueEmailCode(req.getEmail());
    }

    public void bindEmail(BindEmailReq req)throws JimException{
        if(req==null||req.getEmail()==null||req.getEmail().isEmpty()){
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        if(!this.verificationCodeService.verifyEmailCode(req.getEmail(), req.getCode())){
            throw new JimException(JimErrorCode.ErrorCode_APP_SMS_CODE_EXPIRED);
        }
        this.userMapper.updateEmail(RequestContext.getAppkeyFromCtx(),
                RequestContext.getCurrentUserIdFromCtx(), req.getEmail());
    }

    public void bindPhoneSend(BindPhoneReq req)throws JimException{
        if(req==null||req.getPhone()==null||req.getPhone().isEmpty()){
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        User exist = this.userMapper.findByPhone(RequestContext.getAppkeyFromCtx(), req.getPhone());
        if(exist!=null){
            throw new JimException(JimErrorCode.ErrorCode_APP_PHONE_EXISTED);
        }
        this.verificationCodeService.issuePhoneCode(req.getPhone());
    }

    public void bindPhone(BindPhoneReq req)throws JimException{
        if(req==null||req.getPhone()==null||req.getPhone().isEmpty()){
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        if(!this.verificationCodeService.verifyPhoneCode(req.getPhone(), req.getCode())){
            throw new JimException(JimErrorCode.ErrorCode_APP_SMS_CODE_EXPIRED);
        }
        this.userMapper.updatePhone(RequestContext.getAppkeyFromCtx(),
                RequestContext.getCurrentUserIdFromCtx(), req.getPhone());
    }

    public UserInfo getUserInfo(String userId){
        User u = userMapper.findByUserId(RequestContext.getAppkeyFromCtx(), userId);
        UserInfo info = buildUserInfo(u);
        if(info==null){
            info = new UserInfo();
            info.setUserId(userId);
        }
        return info;
    }

    public void updateUserSettings(UserSettings settings){
        String appkey = RequestContext.getAppkeyFromCtx();
        String currentUserId = RequestContext.getCurrentUserIdFromCtx();
        List<UserExt> exts = new ArrayList<>();
        if(settings.getLanguage()!=null&&!settings.getLanguage().isEmpty()){
            exts.add(new UserExt(appkey, currentUserId, UserExtKeys.UserExtKey_Language, settings.getLanguage(), UserExtKeys.AttItemType_Setting));
        }
        if(settings.getUndisturb()!=null&&!settings.getUndisturb().isEmpty()){
            exts.add(new UserExt(appkey, currentUserId, UserExtKeys.UserExtKey_Undisturb, settings.getUndisturb(), UserExtKeys.AttItemType_Setting));
        }
        exts.add(new UserExt(appkey, currentUserId, UserExtKeys.UserExtKey_FriendVerifyType, CommonUtil.int2String(settings.getFriendVerifyType()), UserExtKeys.AttItemType_Setting));
        exts.add(new UserExt(appkey, currentUserId, UserExtKeys.UserExtKey_GrpVerifyType, CommonUtil.int2String(settings.getGrpVerifyType()), UserExtKeys.AttItemType_Setting));
        if(exts.size()>0){
            this.userExtMapper.batchUpsert(exts);
            //TODO sync to im
        }
    }

    public UserInfos searchByPhone(String phone)throws JimException{
        UserInfos users = new UserInfos();
        User u = this.userMapper.findByPhone(RequestContext.getAppkeyFromCtx(), phone);
        if(u!=null){
            UserInfo userInfo = buildUserInfo(u);
            userInfo.setFriend(this.friendCheckService.checkFriend(RequestContext.getCurrentUserIdFromCtx(), u.getUserId()));
            maskUserContacts(userInfo, RequestContext.getCurrentUserIdFromCtx().equals(userInfo.getUserId()));
            users.addUserInf(userInfo);
        }
        return users;
    }

    public UserInfos searchByKeyword(SearchReq req){
        UserInfos users = new UserInfos();
        if(req==null||req.getKeyword()==null||req.getKeyword().isEmpty()){
            return users;
        }
        String appkey = RequestContext.getAppkeyFromCtx();
        String requester = RequestContext.getCurrentUserIdFromCtx();
        long startId = decodeOffset(req.getOffset());
        long limit = req.getLimit() > 0 ? req.getLimit() : 100L;
        List<User> items = this.userMapper.searchByKeyword(appkey, requester, req.getKeyword(), startId, limit);
        if(items!=null && !items.isEmpty()){
            List<String> userIds = new ArrayList<>();
            for (User item : items) {
                UserInfo userInfo = buildUserInfo(item);
                users.addUserInf(userInfo);
                userIds.add(item.getUserId());
                maskUserContacts(userInfo, requester.equals(userInfo.getUserId()));
                if(item.getId()!=null){
                    try {
                        users.setOffset(N3d.encode(item.getId()));
                    } catch (Exception ignored) {
                    }
                }
            }
            if(!userIds.isEmpty()){
                java.util.Map<String,Boolean> friendMap = this.friendCheckService.checkFriends(requester, userIds);
                if(users.getItems()!=null){
                    for (UserInfo info : users.getItems()) {
                        Boolean isFriend = friendMap.get(info.getUserId());
                        if(isFriend!=null){
                            info.setFriend(isFriend);
                        }
                    }
                }
            }
        }
        return users;
    }

    public void blockUsers(BlockUsersReq req){
        if(req==null||CollectionUtils.isEmpty(req.getBlockUserIds())){
            return;
        }
        String appkey = RequestContext.getAppkeyFromCtx();
        String currentUserId = RequestContext.getCurrentUserIdFromCtx();
        for (String blockUserId : req.getBlockUserIds()) {
            if(blockUserId==null||blockUserId.isEmpty()||blockUserId.equals(currentUserId)){
                continue;
            }
            BlockUser blockUser = new BlockUser();
            blockUser.setAppkey(appkey);
            blockUser.setUserId(currentUserId);
            blockUser.setBlockUserId(blockUserId);
            try {
                this.blockUserMapper.create(blockUser);
            }catch (DuplicateKeyException ex){
                // ignore duplicate records
            }
        }
    }

    public void unBlockUsers(BlockUsersReq req){
        if(req==null||CollectionUtils.isEmpty(req.getBlockUserIds())){
            return;
        }
        this.blockUserMapper.batchDelBlockUsers(RequestContext.getAppkeyFromCtx(),
                RequestContext.getCurrentUserIdFromCtx(), req.getBlockUserIds());
    }

    public BlockUsers qryBlockUsers(String offset, Integer count){
        String appkey = RequestContext.getAppkeyFromCtx();
        String currentUserId = RequestContext.getCurrentUserIdFromCtx();
        long limit = 20L;
        if(count!=null&&count>0){
            limit = count.longValue();
        }
        long startId = decodeOffset(offset);
        BlockUsers ret = new BlockUsers();
        List<BlockUser> records = this.blockUserMapper.qryBlockUsers(appkey, currentUserId, limit, startId);
        if(records!=null){
            for (BlockUser record : records) {
                UserInfo user = buildBlockUserInfo(record);
                ret.addUser(user);
                String encodedOffset = encodeOffset(record.getId());
                if(encodedOffset!=null){
                    ret.setOffset(encodedOffset);
                }
            }
        }
        return ret;
    }

    private UserInfo buildUserInfo(User user){
        if(user==null){
            return null;
        }
        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(user.getUserId());
        userInfo.setNickname(user.getNickname());
        userInfo.setAvatar(user.getUserPortrait());
        if(user.getUserType()!=null){
            userInfo.setUserType(user.getUserType());
        }
        userInfo.setPhone(user.getPhone());
        userInfo.setEmail(user.getEmail());
        userInfo.setAccount(user.getLoginAccount());
        userInfo.setPinyin(user.getPinyin());
        return userInfo;
    }

    private void maskUserContacts(UserInfo user, boolean isSelf){
        if(user==null || isSelf){
            return;
        }
        if(user.getPhone()!=null){
            user.setPhone(CommonUtil.maskPhone(user.getPhone()));
        }
        if(user.getEmail()!=null){
            user.setEmail(CommonUtil.maskEmail(user.getEmail()));
        }
    }

    private long decodeOffset(String offset){
        if(offset==null||offset.isEmpty()){
            return 0L;
        }
        try {
            return N3d.decode(offset);
        }catch (Exception ex){
            try {
                return Long.parseLong(offset);
            }catch (NumberFormatException ignored){
                return 0L;
            }
        }
    }

    private String encodeOffset(Long id){
        if(id==null||id<=0){
            return null;
        }
        try {
            return N3d.encode(id);
        }catch (Exception ex){
            return String.valueOf(id);
        }
    }

    private UserInfo buildBlockUserInfo(BlockUser blockUser){
        UserInfo user = new UserInfo();
        user.setUserId(blockUser.getBlockUserId());
        user.setNickname(blockUser.getNickname());
        user.setAvatar(blockUser.getUserPortrait());
        user.setPinyin(blockUser.getPinyin());
        if(blockUser.getUserType()!=null){
            user.setUserType(blockUser.getUserType());
        }
        user.setBlock(true);
        return user;
    }

}
