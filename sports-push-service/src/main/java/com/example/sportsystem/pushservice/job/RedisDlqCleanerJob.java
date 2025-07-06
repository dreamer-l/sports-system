package com.example.sportsystem.pushservice.job;

import com.example.sportsystem.pushservice.handler.WebSocketHandshakeHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Redis 死信队列（DLQ）清理任务
 */
@Component
@Slf4j
public class RedisDlqCleanerJob {

    private final StringRedisTemplate redisTemplate;
    private final WebSocketHandshakeHandler webSocketHandler;

    public RedisDlqCleanerJob(StringRedisTemplate redisTemplate, WebSocketHandshakeHandler webSocketHandler) {
        this.redisTemplate = redisTemplate;
        this.webSocketHandler = webSocketHandler;
    }

    /**
     * 每隔5分钟扫描一次 DLQ 并尝试重新消费
     */
    @Scheduled(fixedRate = 300000) // 5分钟
    public void cleanDeadLetterQueue() {
        Set<String> dlqKeys = redisTemplate.getConnectionFactory().getConnection()
                .keys("dlq_*".getBytes());

        if (dlqKeys == null || dlqKeys.isEmpty()) {
            return;
        }

        for (String key : dlqKeys) {
            try {
                Set<ZSetOperations.TypedTuple<String>> messages = redisTemplate.opsForZSet().rangeWithScores(key, 0, -1);

                if (messages != null && !messages.isEmpty()) {
                    for (ZSetOperations.TypedTuple<String> tuple : messages) {
                        String message = tuple.getValue();
                        String matchId = extractMatchIdFromMessage(message);

                        // 尝试重新推送
                        webSocketHandler.sendMessageToTopic(matchId, message);

                        // 推送成功后移除该消息
                        redisTemplate.opsForZSet().remove(key, message);
                        log.info("[Redis DLQ] 消息已重新推送并从队列中移除: {}@{}", key, message);
                    }
                }

            } catch (Exception e) {
                log.error("[Redis DLQ] 清理失败:", e);
            }
        }
    }

    /**
     * 从消息中提取 match_id（示例逻辑）
     * @param message 消息内容
     * @return 赛事ID
     */
    private String extractMatchIdFromMessage(String message) {
        // 示例解析逻辑（实际应使用 JSON 解析）
        if (message.contains("match_id")) {
            return "1001"; // 实际应从消息中提取真实 ID
        }
        return "default";
    }
}