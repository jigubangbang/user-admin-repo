package com.jigubangbang.payment_service.controller;

import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping
@RefreshScope     // Config Server 값 변경 시 자동 갱신
public class IndexController {

    @GetMapping( "/payment" )
    public String index( Model model ) {
        return "forward:index.html";
    }
}
