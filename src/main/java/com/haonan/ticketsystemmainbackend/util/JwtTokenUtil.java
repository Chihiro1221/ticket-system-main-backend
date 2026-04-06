package com.haonan.ticketsystemmainbackend.util;

import com.haonan.ticketsystemmainbackend.config.JwtProperties;
import com.haonan.ticketsystemmainbackend.domain.UserInfo;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/**
 * JWT 工具类
 */
@Component
public class JwtTokenUtil {

    private final JwtProperties jwtProperties;

    public JwtTokenUtil(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    public String generateToken(UserInfo userInfo) {
        Date now = new Date();
        Date expireTime = new Date(now.getTime() + jwtProperties.getExpireSeconds() * 1000);
        return Jwts.builder()
                .subject(userInfo.getUserId())
                .issuer(jwtProperties.getIssuer())
                .issuedAt(now)
                .expiration(expireTime)
                .claim("userId", userInfo.getUserId())
                .claim("username", userInfo.getUsername())
                .claim("role", userInfo.getRole())
                .signWith(getSignKey())
                .compact();
    }

    private SecretKey getSignKey() {
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
