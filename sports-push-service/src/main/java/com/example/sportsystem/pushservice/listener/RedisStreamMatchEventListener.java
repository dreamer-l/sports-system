package com.example.sportsystem.pushservice.listener;

import com.example.sportsystem.pushservice.handler.WebSocketHandshakeHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.sportsystem.common.model.MatchScoreMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.data.redis.stream.ObjectRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Redis Stream 监听器
 * 用于监听比分、事件、盘口等实时数据流
 */
@Component
@Slf4j
public class RedisStreamMatchEventListener implements StreamListener<String, ObjectRecord<String, String>> {

    private final WebSocketHandshakeHandler webSocketHandler;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;
    private static final int MAX_RETRY = 3;

    public RedisStreamMatchEventListener(WebSocketHandshakeHandler webSocketHandler,
                                         ObjectMapper objectMapper,
                                         StringRedisTemplate redisTemplate) {
        this.webSocketHandler = webSocketHandler;
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 当接收到 Redis Stream 消息时触发
     * @param record 消息记录
     */
    @Override
    public void onMessage(ObjectRecord<String, String> record) {
        String streamKey = record.getStream();
        String message = record.getValue();
        String id = record.getId().getValue();

        log.info("[Redis Stream] 收到消息: {} from {}", message, streamKey);

        try {
            if ("match_stream".equals(streamKey)) {
                MatchScoreMessage scoreMessage = objectMapper.readValue(message, MatchScoreMessage.class);
                retrySendMessage(scoreMessage.getMatchId(), message, 0);
            } else if ("odds_stream".equals(streamKey)) {
                OddsUpdateMessage oddsMessage = objectMapper.readValue(message, OddsUpdateMessage.class);
                retrySendMessage(oddsMessage.getMatchId(), message, 0);
            }

            // 手动提交偏移量
            redisTemplate.getConnectionFactory().getConnection()
                    .streamCommands()
                    .xAck(streamKey.getBytes(), "sports-push-consumer-group", id);

        } catch (Exception e) {
            log.error("[WebSocket] 推送失败:", e);
            sendToDeadLetterQueue("unknown", message);
        }
    }

    /**
     * 带重试的消息推送方法
     * @param matchId 赛事ID
     * @param message 消息内容
     * @param retryCount 当前重试次数
     */
    private void retrySendMessage(String matchId, String message, int retryCount) {
        try {
            webSocketHandler.sendMessageToTopic(matchId, message);
        } catch (Exception e) {
            if (retryCount < MAX_RETRY) {
                log.warn("[WebSocket] 推送失败，第 {} 次重试...", retryCount + 1);
                try {
                    Thread.sleep(1000 * (retryCount + 1)); // 指数退避
                } catch (InterruptedException ignored) {}
                retrySendMessage(matchId, message, retryCount + 1);
            } else {
                log.error("[WebSocket] 推送失败超过最大重试次数:", e);
                sendToDeadLetterQueue(matchId, message);
            }
        }
    }

    /**
     * 将消息发送到死信队列
     * @param matchId 赛事ID
     * @param message 消息内容
     */
    private void sendToDeadLetterQueue(String matchId, String message) {
        String dlqKey = "dlq_" + matchId;
        redisTemplate.opsForStream().add(dlqKey, Map.of("match_id", matchId, "message", message));
        log.info("[Redis Stream] 消息已写入死信队列: {}@{}", dlqKey, message);
    }
}