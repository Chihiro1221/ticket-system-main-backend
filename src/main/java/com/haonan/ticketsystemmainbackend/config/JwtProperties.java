package com.haonan.ticketsystemmainbackend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置
 */
@Component
@ConfigurationProperties(prefix = "jwt")
@Data
public class JwtProperties {

    /**
     * JWT 密钥，建议长度不少于 32 字节
     */
    private String secret;

    /**
     * 过期时间，单位秒
     */
    private long expireSeconds;

    /**
     * 签发者
     */
    private String issuer;
}
