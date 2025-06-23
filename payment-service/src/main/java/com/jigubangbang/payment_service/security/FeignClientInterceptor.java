package com.jigubangbang.payment_service.security;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;

@Component
public class FeignClientInterceptor implements RequestInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(FeignClientInterceptor.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String TOKEN_PREFIX = "Bearer ";
    @Override
    public void apply(RequestTemplate template) {
        // 현재 HTTP 요청의 속성을 가져옴
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            // 현재 요청의 Authorization 헤더 값을 가져옴
            String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);
            // Authorization 헤더가 존재하고 "Bearer "로 시작하는지 확인
            if (authorizationHeader != null && authorizationHeader.startsWith(TOKEN_PREFIX)) {
                // Feign 요청 템플릿의 헤더에 Authorization 헤더를 그대로 추가
                template.header(AUTHORIZATION_HEADER, authorizationHeader);
                logger.debug("Feign Client Interceptor: Added Authorization header to Feign request. Target: {}", 
                 template.feignTarget().name());
                } else {
                    logger.warn("Feign Client Interceptor: Authorization header is missing or not a Bearer token in theoriginal request. Target: {}", template.feignTarget().name());
                    // 토큰이 없는 경우, Feign 요청은 인증 헤더 없이 보내지게 됨
                    // user-service에서 해당 요청을 어떻게 처리할지에 따라 동작이 달라집니다.
                    // (예: 공개 API는 허용, 보호된 API는 401/403 반환)
                }
            } else {
                logger.warn("Feign Client Interceptor: Could not obtain RequestAttributes (e.g., called from a non-request thread). Authorization header will not be propagated. Target: {}", 
                template.feignTarget().name());
            }
        }
    }