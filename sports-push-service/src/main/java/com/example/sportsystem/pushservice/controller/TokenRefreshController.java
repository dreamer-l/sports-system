package com.example.sportsystem.pushservice.controller;

import com.example.sportsystem.pushservice.config.OriginHandshakeInterceptor;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Token 刷新接口
 */
@RestController
@RequestMapping("/api/v1/token")
@AllArgsConstructor
public class TokenRefreshController {

    private final OriginHandshakeInterceptor handshakeInterceptor;

    /**
     * 刷新 Token 接口
     * @return 新的 Token
     */
    @GetMapping("/refresh")
    public Map<String, String> refreshToken() {
        long timestamp = System.currentTimeMillis();
        String token = handshakeInterceptor.generateHmacSignature(timestamp);

        Map<String, String> response = new HashMap<>();
        response.put("token", timestamp + "_" + token);
        response.put("expires_in", "300"); // 5分钟有效期
        return response;
    }
}