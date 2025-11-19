package com.juggle.chat.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.alibaba.fastjson.JSON;

public class CommonUtil {
    public static final String toJson(Object obj){
        String jsonStr = JSON.toJSONString(obj);
        return jsonStr;
    }

    public static final String generateUuid(){
        return java.util.UUID.randomUUID().toString().replace("-", "");
    }

    public static final String generageShortUuid(){
        return java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    public static final int randomInt(){
        Random ran = new Random();
        return ran.nextInt(100000);
    }

    public static final String int2String(int n){
        return String.valueOf(n);
    }

    public static final int string2Int(String a){
        return Integer.parseInt(a);
    }

    public static String getConversationId(String fromId, String targetId, int converType){
        if(converType == 1){
            List<String> members = new ArrayList<>();
            members.add(fromId);
            members.add(targetId);
            Collections.sort(members);
            return String.join(":", members);
        }
        return targetId;
    }

    public static String sha1(String value){
        if(value==null){
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] bs = digest.digest(value.getBytes());
            return Base64.getEncoder().encodeToString(bs);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 algorithm not found", e);
        }
    }

    public static String maskPhone(String phone){
        if(phone==null||phone.length()!=11){
            return phone;
        }
        StringBuilder sb = new StringBuilder(phone);
        for(int i=3;i<7 && i<sb.length();i++){
            sb.setCharAt(i, '*');
        }
        return sb.toString();
    }

    public static String maskEmail(String email){
        if(email==null||!email.contains("@")){
            return email;
        }
        String[] parts = email.split("@",2);
        if(parts.length!=2){
            return email;
        }
        String username = parts[0];
        String domain = parts[1];
        if(username.length()==0){
            return "@"+domain;
        }
        String maskedUsername;
        if(username.length()==1){
            maskedUsername = "*";
        }else if(username.length()==2){
            maskedUsername = username;
        }else if(username.length()==3){
            maskedUsername = username.substring(0,2)+"*";
        }else{
            maskedUsername = username.substring(0,2)+"***"+username.substring(username.length()-1);
        }
        return maskedUsername+"@"+domain;
    }
}
