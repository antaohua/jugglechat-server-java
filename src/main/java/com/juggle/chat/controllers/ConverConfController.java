package com.juggle.chat.controllers;

import java.util.Map;

import jakarta.annotation.Resource;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.juggle.chat.apimodels.Result;
import com.juggle.chat.apimodels.SetConverConfsReq;
import com.juggle.chat.services.ConverConfService;

@RestController
@RequestMapping("/jim/converconfs")
public class ConverConfController {
    @Resource
    private ConverConfService converConfService;

    @GetMapping("/get")
    public Result getConverConfs(@RequestParam("target_id") String targetId,
            @RequestParam(value = "sub_channel", required = false) String subChannel,
            @RequestParam("conver_type") int converType) {
        Map<String,Object> confs = this.converConfService.getConverConfs(targetId, subChannel, converType);
        return Result.success(confs);
    }

    @PostMapping("/set")
    public Result setConverConfs(@RequestBody SetConverConfsReq req) {
        this.converConfService.setConverConfs(req);
        return Result.success(null);
    }
}
