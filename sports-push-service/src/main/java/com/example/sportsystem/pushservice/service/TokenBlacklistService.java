package com.example.sportsystem.pushservice.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Token 黑名单服务
 */
@Service
public class TokenBlacklistService {

    private final StringRedisTemplate redisTemplate;

    public TokenBlacklistService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 将 Token 加入黑名单
     * @param token Token 值
     * @param expirationInSeconds 过期时间（秒）
     */
    public void addTokenToBlacklist(String token, long expirationInSeconds) {
        redisTemplate.opsForValue().set("blacklist:token:" + token, "1", expirationInSeconds, java.util.concurrent.TimeUnit.SECONDS);
    }

    /**
     * 检查 Token 是否在黑名单中
     * @param token Token 值
     * @return 是否存在
     */
    public boolean isTokenBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey("blacklist:token:" + token));
    }
}