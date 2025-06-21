package com.jigubangbang.payment_service.user_service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.jigubangbang.payment_service.model.User;

@FeignClient( name="user-service", configuration = UserServiceClientConfig.class )
public interface UserServiceClient {
    @GetMapping( "/user/{userId}" )
    User getUser( @PathVariable("userId") String userId );   
}
