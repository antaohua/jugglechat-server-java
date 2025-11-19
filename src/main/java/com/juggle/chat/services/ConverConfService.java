package com.juggle.chat.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.juggle.chat.apimodels.SetConverConfsReq;
import com.juggle.chat.interceptors.RequestContext;
import com.juggle.chat.mappers.ConverConfMapper;
import com.juggle.chat.models.ConverConf;
import com.juggle.chat.utils.CommonUtil;

@Service
public class ConverConfService {
    private static final String CONF_KEY_MSG_LIFE_TIME = "msg_life_time";

    @Resource
    private ConverConfMapper converConfMapper;

    public Map<String,Object> getConverConfs(String targetId, String subChannel, int converType){
        String appkey = RequestContext.getAppkeyFromCtx();
        String userId = RequestContext.getCurrentUserIdFromCtx();
        String converId = CommonUtil.getConversationId(userId, targetId, converType);
        List<ConverConf> items = this.converConfMapper.qryConverConfs(appkey, converId, subChannel, converType);
        Map<String,Object> ret = new HashMap<>();
        if(items!=null){
            for (ConverConf item : items) {
                if(CONF_KEY_MSG_LIFE_TIME.equals(item.getItemKey())){
                    long life = 0L;
                    try {
                        life = Long.parseLong(item.getItemValue());
                    }catch (NumberFormatException ignored){
                    }
                    ret.put(CONF_KEY_MSG_LIFE_TIME, life);
                }
            }
        }
        return ret;
    }

    public void setConverConfs(SetConverConfsReq req){
        String appkey = RequestContext.getAppkeyFromCtx();
        String userId = RequestContext.getCurrentUserIdFromCtx();
        String converId = CommonUtil.getConversationId(userId, req.getTargetId(), req.getConverType());
        List<ConverConf> items = new ArrayList<>();
        if(req.getConfs()!=null){
            for (Map.Entry<String, Object> entry : req.getConfs().entrySet()) {
                String key = entry.getKey();
                if(CONF_KEY_MSG_LIFE_TIME.equals(key)){
                    ConverConf conf = new ConverConf();
                    conf.setAppkey(appkey);
                    conf.setConverId(converId);
                    conf.setConverType(req.getConverType());
                    conf.setSubChannel(req.getSubChannel());
                    conf.setItemKey(key);
                    conf.setItemValue(String.valueOf(entry.getValue()));
                    conf.setItemType(0);
                    items.add(conf);
                }
            }
        }
        if(!items.isEmpty()){
            this.converConfMapper.batchUpsert(items);
        }
    }
}
