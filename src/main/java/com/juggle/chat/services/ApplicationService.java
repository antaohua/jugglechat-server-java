package com.juggle.chat.services;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.juggle.chat.apimodels.Application;
import com.juggle.chat.apimodels.Applications;
import com.juggle.chat.interceptors.RequestContext;
import com.juggle.chat.mappers.ApplicationMapper;

@Service
public class ApplicationService {
    @Resource
    private ApplicationMapper applicationMapper;

    public Applications qryApplications(Integer page, Integer size, Integer order){
        int pageNo = page==null||page<=0?1:page;
        int pageSize = size==null||size<=0?20:size;
        List<com.juggle.chat.models.Application> records = this.applicationMapper
                .qryApplicationsByPage(RequestContext.getAppkeyFromCtx(), pageNo, pageSize);
        List<Application> items = new ArrayList<>();
        if(records!=null){
            for (com.juggle.chat.models.Application record : records) {
                Application item = new Application();
                item.setAppId(record.getAppId());
                item.setAppName(record.getAppName());
                item.setAppIcon(record.getAppIcon());
                item.setAppDesc(record.getAppDesc());
                item.setAppUrl(record.getAppUrl());
                if(record.getAppOrder()!=null){
                    item.setAppOrder(record.getAppOrder());
                }
                if(record.getCreatedTime()!=null){
                    item.setCreatedTime(record.getCreatedTime());
                }
                if(record.getUpdatedTime()!=null){
                    item.setUpdatedTime(record.getUpdatedTime());
                }
                items.add(item);
            }
        }
        Applications resp = new Applications();
        resp.setItems(items);
        resp.setPage(pageNo);
        resp.setSize(pageSize);
        return resp;
    }
}
