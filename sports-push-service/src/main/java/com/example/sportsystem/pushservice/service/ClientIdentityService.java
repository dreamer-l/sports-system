package com.example.sportsystem.pushservice.service;

import com.example.sportsystem.pushservice.handler.WebSocketSessionManager;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

/**
 * WebSocket 客户端身份识别与行为分析服务
 */
@Component
@AllArgsConstructor
public class ClientIdentityService {

    private final WebSocketSessionManager sessionManager;

    // 客户端连接信息存储
    private final Map<String, ClientInfo> clientRegistry = new ConcurrentHashMap<>();

    /**
     * 客户端信息模型
     */
    @Data
    public static class ClientInfo {
        private String clientId; // 唯一标识
        private String ipAddress; // IP 地址
        private String userAgent; // User-Agent
        private String token; // Token
        private LocalDateTime connectTime; // 连接时间
        private LocalDateTime lastActiveTime; // 最后活跃时间
        private String currentMatchId; // 当前订阅赛事ID
        private int messageCount; // 推送消息数量
    }

    /**
     * 注册新客户端连接
     * @param sessionId 会话ID
     * @param remoteAddress 客户端IP
     * @param userAgent User-Agent
     * @param token Token
     * @return 客户端信息
     */
    public ClientInfo registerClient(String sessionId, String remoteAddress, String userAgent, String token) {
        ClientInfo info = new ClientInfo();
        info.setClientId(UUID.randomUUID().toString());
        info.setIpAddress(remoteAddress);
        info.setUserAgent(userAgent);
        info.setToken(token);
        info.setConnectTime(LocalDateTime.now());
        info.setLastActiveTime(LocalDateTime.now());
        clientRegistry.put(sessionId, info);
        return info;
    }

    /**
     * 更新客户端最后活跃时间
     * @param sessionId 会话ID
     */
    public void updateLastActiveTime(String sessionId) {
        ClientInfo info = clientRegistry.get(sessionId);
        if (info != null) {
            info.setLastActiveTime(LocalDateTime.now());
        }
    }

    /**
     * 记录客户端订阅的赛事
     * @param sessionId 会话ID
     * @param matchId 赛事ID
     */
    public void setCurrentMatchId(String sessionId, String matchId) {
        ClientInfo info = clientRegistry.get(sessionId);
        if (info != null) {
            info.setCurrentMatchId(matchId);
        }
    }

    /**
     * 增加推送消息计数
     * @param sessionId 会话ID
     */
    public void incrementMessageCount(String sessionId) {
        ClientInfo info = clientRegistry.get(sessionId);
        if (info != null) {
            info.setMessageCount(info.getMessageCount() + 1);
        }
    }

    /**
     * 获取客户端信息
     * @param sessionId 会话ID
     * @return 客户端信息
     */
    public ClientInfo getClientInfo(String sessionId) {
        return clientRegistry.get(sessionId);
    }

    /**
     * 移除客户端信息
     * @param sessionId 会话ID
     */
    public void removeClient(String sessionId) {
        clientRegistry.remove(sessionId);
    }
}