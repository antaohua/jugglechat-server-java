package com.juggle.chat.controllers;



import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.juggle.chat.apimodels.Applications;
import com.juggle.chat.apimodels.Result;
import com.juggle.chat.services.ApplicationService;

@RestController
@RequestMapping("/jim/applications")
public class ApplicationController {
    @Resource
    private ApplicationService applicationService;

    @GetMapping("/list")
    public Result qryApplications(@RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            @RequestParam(value = "order", required = false) Integer order) {
        Applications apps = this.applicationService.qryApplications(page, size, order);
        return Result.success(apps);
    }
}
