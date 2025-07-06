package com.example.sportsystem.pushservice.handler;

import com.example.sportsystem.pushservice.config.OriginHandshakeInterceptor;
import com.example.sportsystem.pushservice.handler.WebSocketSessionManager;
import com.example.sportsystem.pushservice.service.ClientIdentityService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

/**
 * WebSocket 消息处理器
 * 管理连接池，处理消息收发
 */
@Component
@Slf4j
public class WebSocketHandshakeHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WebSocketSessionManager sessionManager;
    private final ClientIdentityService clientIdentityService;

    public WebSocketHandshakeHandler(WebSocketSessionManager sessionManager, ClientIdentityService clientIdentityService) {
        this.sessionManager = sessionManager;
        this.clientIdentityService = clientIdentityService;

        // 初始化消息路由
        messageHandlers.put(MessageType.SUBSCRIBE, this::handleSubscribe);
        messageHandlers.put(MessageType.UNSUBSCRIBE, this::handleUnsubscribe);
        messageHandlers.put(MessageType.PING, this::handlePing);
        messageHandlers.put(MessageType.MATCH_SCORE, this::handleMatchScore);
        messageHandlers.put(MessageType.ODDS_UPDATE, this::handleOddsUpdate);
        messageHandlers.put(MessageType.EVENT_ALERT, this::handleEventAlert);
    }

    /**
     * 消息类型枚举
     */
    private enum MessageType {
        SUBSCRIBE("subscribe"),
        UNSUBSCRIBE("unsubscribe"),
        PING("ping"),
        MATCH_SCORE("match_score"),
        ODDS_UPDATE("odds_update"),
        EVENT_ALERT("event_alert"),
        UNKNOWN("unknown");

        private final String type;

        MessageType(String type) {
            this.type = type;
        }

        public static MessageType fromString(String type) {
            for (MessageType t : values()) {
                if (t.type.equalsIgnoreCase(type)) {
                    return t;
                }
            }
            return UNKNOWN;
        }
    }

    /**
     * 消息处理器接口
     */
    @FunctionalInterface
    private interface MessageHandler {
        void handle(WebSocketSession session, String data);
    }

    // 消息路由表
    private final Map<MessageType, MessageHandler> messageHandlers = new HashMap<>();

    /**
     * 处理订阅消息
     */
    private void handleSubscribe(WebSocketSession session, String data) {
        if (data != null && !data.isEmpty()) {
            sessionManager.subscribeMatch(session.getId(), data, session);
            sendResponse(session, "subscribed", "Subscribed to " + data);
        }
    }

    /**
     * 处理取消订阅消息
     */
    private void handleUnsubscribe(WebSocketSession session, String data) {
        String currentTopic = (String) session.getAttributes().get("topic");
        if (currentTopic != null) {
            sessionManager.unsubscribeMatch(session.getId(), currentTopic);
            session.getAttributes().remove("topic");
            sendResponse(session, "unsubscribed", "Unsubscribed");
        }
    }

    /**
     * 处理心跳消息
     */
    private void handlePing(WebSocketSession session, String data) {
        sendResponse(session, "pong", "Keepalive");
    }

    /**
     * 处理比分更新消息（示例）
     */
    private void handleMatchScore(WebSocketSession session, String data) {
        // 可用于客户端发送请求获取最新比分
        log.info("[WebSocket] 收到比分查询请求: {}", data);
    }

    /**
     * 处理盘口更新消息（示例）
     */
    private void handleOddsUpdate(WebSocketSession session, String data) {
        log.info("[WebSocket] 收到盘口更新请求: {}", data);
    }

    /**
     * 处理赛事事件提醒
     */
    private void handleEventAlert(WebSocketSession session, String data) {
        log.info("[WebSocket] 收到赛事事件提醒请求: {}", data);
    }

    /**
     * 发送结构化响应
     */
    private void sendResponse(WebSocketSession session, String type, String message) {
        try {
            session.sendMessage(new TextMessage(String.format("{\"type\":\"%s\",\"message\":\"%s\"}", type, message)));
        } catch (IOException e) {
            log.error("[WebSocket] 发送响应失败:", e);
        }
    }

    /**
     * 当客户端连接时触发
     * @param session WebSocket会话
     * @throws Exception 异常
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String user = (String) session.getAttributes().get(OriginHandshakeInterceptor.KEY_USER);
        if (user == null) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        // 获取客户端信息
        String remoteAddress = session.getRemoteAddress() != null ? session.getRemoteAddress().getHostString() : "unknown";
        String userAgent = session.getPrincipal() != null ? session.getPrincipal().getName() : "unknown";
        String token = "anonymous"; // 可从 URL 参数提取

        // 注册客户端身份
        ClientIdentityService.ClientInfo info = clientIdentityService.registerClient(session.getId(), remoteAddress, userAgent, token);
        sessionManager.addSession(session, info.getClientId());
        log.info("[WebSocket] 新连接建立: {} from {}", session.getId(), remoteAddress);
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.info("[WebSocket] 收到消息: {} from {}", payload, session.getId());

        // 更新最后活跃时间
        clientIdentityService.updateLastActiveTime(session.getId());

        try {
            // 尝试解析为 JSON
            JsonMessage jsonMessage = objectMapper.readValue(payload, JsonMessage.class);
            MessageType type = MessageType.fromString(jsonMessage.getType());
            messageHandlers.getOrDefault(type, (s, d) -> log.warn("未知消息类型: {}", type)).handle(session, jsonMessage.getData());

        } catch (Exception e) {
            // 非 JSON 消息，使用原始方式处理
            if (payload.startsWith("subscribe:")) {
                String topic = payload.substring("subscribe:".length()).trim();
                if (!topic.isEmpty()) {
                    clientIdentityService.setCurrentMatchId(session.getId(), topic);
                    session.getAttributes().put("topic", topic);
                    session.sendMessage(new TextMessage("{\"status\":\"success\",\"message\":\"Subscribed to " + topic + "\"}"));
                }
            } else if (payload.startsWith("unsubscribe")) {
                session.getAttributes().remove("topic");
                clientIdentityService.setCurrentMatchId(session.getId(), null);
                session.sendMessage(new TextMessage("{\"status\":\"success\",\"message\":\"Unsubscribed\"}"));
            } else {
                session.sendMessage(new TextMessage("{\"status\":\"error\",\"message\":\"Unknown command\"}"));
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessionManager.removeSession(session.getId());
        clientIdentityService.removeClient(session.getId());
    }

    /**
     * 主动推送消息给所有在线用户
     * @param message 消息体
     */
    public void broadcast(String message) {
        sessions.forEach((id, session) -> {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(message));
                }
            } catch (IOException e) {
                log.error("[WebSocket] 推送失败:", e);
            }
        });
    }

    /**
     * 向指定用户/主题推送消息
     * @param topic 主题或用户ID
     * @param message 消息内容
     */
    public void sendMessageToTopic(String topic, String message) {
        sessions.values().stream()
                .filter(session -> topic.equals(session.getAttributes().get("topic")))
                .forEach(session -> {
                    try {
                        if (session.isOpen()) {
                            session.sendMessage(new TextMessage(message));
                        }
                    } catch (IOException e) {
                        log.error("[WebSocket] 推送失败:", e);
                    }
                });
    }

    /**
     * JSON 消息结构定义
     */
    @Data
    private static class JsonMessage {
        private String type;
        private String data;
    }
}