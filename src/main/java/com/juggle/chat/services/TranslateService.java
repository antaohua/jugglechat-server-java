package com.juggle.chat.services;

import org.springframework.stereotype.Service;

import com.juggle.chat.apimodels.TransItem;
import com.juggle.chat.apimodels.TransReq;
import com.juggle.chat.exceptions.JimErrorCode;
import com.juggle.chat.exceptions.JimException;

@Service
public class TranslateService {
    public TransReq translate(TransReq req)throws JimException{
        if(req==null||req.getTargetLang()==null||req.getTargetLang().isEmpty()
                ||req.getItems()==null||req.getItems().isEmpty()){
            throw new JimException(JimErrorCode.ErrorCode_APP_REQ_BODY_ILLEGAL);
        }
        for (TransItem item : req.getItems()) {
            if(item.getContent()==null){
                item.setContent("");
            }
        }
        return req;
    }
}
