package com.example.sportsystem.pushservice.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * WebSocket 会话管理器
 * 统一管理连接池与订阅关系
 */
@Component
@Slf4j
public class WebSocketSessionManager {

    // 全局连接池
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    // 按赛事ID订阅的用户
    private final Map<String, Map<String, WebSocketSession>> matchSubscribers = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public WebSocketSessionManager() {
        // 每30秒执行一次心跳检测
        scheduler.scheduleAtFixedRate(this::checkHeartbeat, 0, 30, TimeUnit.SECONDS);
    }

    /**
     * 心跳检测任务
     */
    private void checkHeartbeat() {
        sessions.forEach((id, session) -> {
            try {
                if (session.isOpen()) {
                    // 发送心跳消息（可选）
                    session.sendMessage(new TextMessage("{\"type\":\"ping\"}"));
                } else {
                    removeSession(id);
                }
            } catch (IOException e) {
                log.warn("[WebSocket] 心跳检测失败: {}", id);
                removeSession(id);
            }
        });
    }

    /**
     * 添加新连接
     * @param session WebSocket会话
     * @param user 用户标识
     */
    public void addSession(WebSocketSession session, String user) {
        sessions.put(session.getId(), session);
        log.info("[WebSocket] 新增连接: {}", session.getId());
    }

    /**
     * 移除连接
     * @param sessionId 会话ID
     */
    public void removeSession(String sessionId) {
        WebSocketSession session = sessions.remove(sessionId);
        if (session != null && session.getAttributes().containsKey("topic")) {
            String topic = (String) session.getAttributes().get("topic");
            matchSubscribers.computeIfPresent(topic, (k, v) -> {
                v.remove(sessionId);
                return v;
            });
        }
        log.info("[WebSocket] 连接关闭: {}", sessionId);
    }

    /**
     * 订阅特定赛事
     * @param sessionId 会话ID
     * @param matchId 赛事ID
     * @param session WebSocket会话
     */
    public void subscribeMatch(String sessionId, String matchId, WebSocketSession session) {
        matchSubscribers.computeIfAbsent(matchId, k -> new ConcurrentHashMap<>()).put(sessionId, session);
        session.getAttributes().put("topic", matchId);
    }

    /**
     * 取消订阅
     * @param sessionId 会话ID
     * @param matchId 赛事ID
     */
    public void unsubscribeMatch(String sessionId, String matchId) {
        matchSubscribers.computeIfPresent(matchId, (k, v) -> {
            v.remove(sessionId);
            return v;
        });
    }

    /**
     * 向指定赛事的所有订阅者推送消息
     * @param matchId 赛事ID
     * @param message 消息内容
     */
    public void sendMessageToMatchSubscribers(String matchId, String message) {
        Map<String, WebSocketSession> subscribers = matchSubscribers.get(matchId);
        if (subscribers != null && !subscribers.isEmpty()) {
            subscribers.forEach((id, session) -> {
                try {
                    if (session.isOpen()) {
                        session.sendMessage(new TextMessage(message));
                    }
                } catch (IOException e) {
                    log.error("[WebSocket] 推送失败:", e);
                }
            });
        }
    }

    /**
     * 获取当前连接总数
     * @return 连接数
     */
    public int getTotalConnectionCount() {
        return sessions.size();
    }

    /**
     * 获取当前订阅赛事的用户总数
     * @return 用户数
     */
    public int getSubscribedMatchCount() {
        return matchSubscribers.values().stream()
                .mapToInt(Map::size)
                .sum();
    }

    /**
     * 获取每个赛事的订阅人数统计
     * @return 统计数据
     */
    public Map<String, Integer> getMatchSubscriptionStats() {
        Map<String, Integer> stats = new HashMap<>();
        matchSubscribers.forEach((matchId, subscribers) ->
                stats.put(matchId, subscribers.size()));
        return stats;
    }
}