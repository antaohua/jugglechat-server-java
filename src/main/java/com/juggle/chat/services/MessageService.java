package com.juggle.chat.services;

import java.util.List;

import jakarta.annotation.Resource;

import org.springframework.stereotype.Service;

import com.juggle.chat.apimodels.DelHisMsgsReq;
import com.juggle.chat.apimodels.RecallMsgReq;
import com.juggle.chat.apimodels.SimpleMsg;
import com.juggle.chat.exceptions.JimErrorCode;
import com.juggle.chat.exceptions.JimException;
import com.juggle.chat.interceptors.RequestContext;
import com.juggle.chat.mappers.GroupAdminMapper;
import com.juggle.chat.mappers.GroupMapper;
import com.juggle.chat.models.Group;

@Service
public class MessageService {
    @Resource
    private GroupMapper groupMapper;
    @Resource
    private GroupAdminMapper groupAdminMapper;

    public void recallMessage(RecallMsgReq req)throws JimException{
        validateRecallReq(req);
        ensureGroupPrivilege(req.getTargetId());
        // The IM SDK is unavailable here, so only permission checks are enforced.
    }

    public void deleteMessages(DelHisMsgsReq req)throws JimException{
        validateDeleteReq(req);
        ensureGroupPrivilege(req.getTargetId());
        // The IM SDK is unavailable here, so only permission checks are enforced.
    }

    private void validateRecallReq(RecallMsgReq req){
        if(req==null||req.getTargetId()==null||req.getTargetId().isEmpty()
                ||req.getMsgId()==null||req.getMsgId().isEmpty()||req.getChannelType()!=2){
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
    }

    private void validateDeleteReq(DelHisMsgsReq req){
        if(req==null||req.getTargetId()==null||req.getTargetId().isEmpty()||req.getChannelType()!=2){
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        List<SimpleMsg> msgs = req.getMsgs();
        if(msgs==null||msgs.isEmpty()){
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        for (SimpleMsg msg : msgs) {
            if(msg.getMsgId()==null||msg.getMsgId().isEmpty()){
                throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
            }
        }
    }

    private void ensureGroupPrivilege(String groupId){
        String appkey = RequestContext.getAppkeyFromCtx();
        String operator = RequestContext.getCurrentUserIdFromCtx();
        Group group = this.groupMapper.findById(appkey, groupId);
        if(group==null){
            throw new JimException(JimErrorCode.ErrorCode_APP_GROUP_DEFAULT);
        }
        if(operator==null||operator.isEmpty()){
            throw new JimException(JimErrorCode.ErrorCode_APP_NOT_LOGIN);
        }
        if(!operator.equals(group.getCreatorId())){
            Boolean isAdmin = this.groupAdminMapper.checkAdmin(appkey, groupId, operator);
            if(isAdmin==null || !isAdmin){
                throw new JimException(JimErrorCode.ErrorCode_APP_GROUP_NORIGHT);
            }
        }
    }
}
