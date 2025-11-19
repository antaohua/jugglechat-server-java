package com.juggle.chat.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

import com.juggle.chat.apimodels.Result;

@RestController
@RequestMapping("/jim")
public class SyncController {
    @GetMapping("/syncconfs")
    public Result syncConfs() {
        Map<String,Object> confs = new HashMap<>();
        confs.put("version", 1);
        return Result.success(confs);
    }
}
