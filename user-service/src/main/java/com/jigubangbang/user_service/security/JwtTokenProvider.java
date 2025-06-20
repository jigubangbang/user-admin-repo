package com.jigubangbang.user_service.security;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import com.jigubangbang.user_service.model.UserDto;

// JWT Token 생성 및 검증
@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);
    private final SecretKey key;
    private final long accessTokenValidityMs;
    private final long refreshTokenValidityMs;

    public JwtTokenProvider(
        @Value("${jwt.secret}") String secretString,
        @Value("${jwt.access-token-validity:7200000}") long accessTokenValidityMsConfig,
        @Value("${jwt.refresh-token-validity:604800000}") long refreshTokenValidityMsConfig
    ) {
        byte[] keyBytes = Decoders.BASE64.decode(secretString);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenValidityMs = accessTokenValidityMsConfig;
        this.refreshTokenValidityMs = refreshTokenValidityMsConfig;

        logger.info("JWT Initialized: HS512 / Access Exp: {}ms / Refresh Exp: {}ms", accessTokenValidityMs, refreshTokenValidityMs);
    }

    // Access Token 생성
    public String generateAccessToken(UserDto user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenValidityMs);

        Map<String, Object> claims = new HashMap<>();
        claims.put("name", user.getName());

        String role = user.getRole();
        if (role == null || role.isBlank()) {
            role = "ROLE_USER";
            logger.warn("User {} has null or empty role, defaulting to ROLE_USER", user.getId());
        } else if (!role.startsWith("ROLE_")) {
            role = "ROLE_" + role.toUpperCase();
        }
        claims.put("role", role);
        claims.put("type", "access");

        return Jwts.builder()
            .setSubject(user.getId())
            .addClaims(claims)
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(key, SignatureAlgorithm.HS512)
            .compact();
    }

    // Refresh Token 생성
    public String generateRefreshToken(String userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenValidityMs);

        return Jwts.builder()
            .setSubject(userId)
            .claim("type", "refresh")
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(key, SignatureAlgorithm.HS512)
            .compact();
    }

    // 토큰에서 userId(subject) 추출
    public String getUserIdFromToken(String token) {
        return parseClaims(token).getBody().getSubject();
    }

    // 토큰에서 role 추출
    public String getRoleFromToken(String token) {
        return (String) parseClaims(token).getBody().get("role");
    }

    // 토큰에서 type(access/refresh) 추출
    public String getTokenType(String token) {
        return (String) parseClaims(token).getBody().get("type");
    }

    // 토큰 유효성 검증
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            logger.warn("만료된 JWT: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.warn("지원하지 않는 JWT: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            logger.warn("형식이 잘못된 JWT: {}", e.getMessage());
        } catch (SecurityException | IllegalArgumentException e) {
            logger.warn("서명 오류 또는 잘못된 JWT: {}", e.getMessage());
        }
        return false;
    }

    // 토큰 만료 여부 체크
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = parseClaims(token).getBody().getExpiration();
            return expiration.before(new Date());
        } catch (JwtException e) {
            return true;
        }
    }

    // 내부용 - 토큰 파싱
    private Jws<Claims> parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
    }
    // 토큰에서 사용자 이름 추출
    public String getNameFromToken(String token) {
        return (String) parseClaims(token).getBody().get("name");
    }

    // 토큰에서 발급 시간 추출
    public Date getIssuedAtFromToken(String token) {
        return parseClaims(token).getBody().getIssuedAt();
    }

    // 토큰에서 만료 시간 추출
    public Date getExpirationFromToken(String token) {
        return parseClaims(token).getBody().getExpiration();
    }

}
