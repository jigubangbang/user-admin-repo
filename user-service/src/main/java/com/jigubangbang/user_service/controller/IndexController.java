package com.jigubangbang.user_service.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequestMapping
@RefreshScope
public class IndexController {
    @Value("${spring.application.name}")
    private String appName;

    @Value("${my.config.user.property}")
    private String userProp;

    @GetMapping("/user")
    public String index(Model model) {
        System.out.println("Application Name : " + appName);
        System.out.println("User Property : " + userProp);

        return "forward:/index.html";
    }
}

