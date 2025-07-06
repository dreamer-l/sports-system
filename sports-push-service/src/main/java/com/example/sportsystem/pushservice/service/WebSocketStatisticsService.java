package com.example.sportsystem.pushservice.service;

import com.example.sportsystem.pushservice.handler.WebSocketSessionManager;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * WebSocket 统计信息接口
 */
@RestController
@RequestMapping("/api/v1/stats/websocket")
@AllArgsConstructor
public class WebSocketStatisticsService {

    private final WebSocketSessionManager sessionManager;

    /**
     * 获取当前 WebSocket 统计信息
     * @return 统计数据
     */
    @GetMapping
    public Map<String, Object> getWebSocketStats() {
        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("total_connections", sessionManager.getTotalConnectionCount());
        stats.put("active_subscribers", sessionManager.getSubscribedMatchCount());
        stats.put("match_subscriptions", sessionManager.getMatchSubscriptionStats());
        return stats;
    }
}