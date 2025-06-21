package com.jigubangbang.payment_service.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jigubangbang.payment_service.model.User;
import com.jigubangbang.payment_service.user_service.UserServiceClient;
import reactor.core.publisher.Mono;


@RestController
@RequestMapping( "/payment" )
public class PaymentController {
    @Autowired
    private UserServiceClient userServiceClient;
    @GetMapping
    public Mono<Resource> payment() {
        return Mono.just( new ClassPathResource( "static/index.html" ) );
    }
    
    @GetMapping("/{userId}")
    public User getOrderUserInfo(@PathVariable String userId) {
        return userServiceClient.getUser( userId );
    }
    
}
