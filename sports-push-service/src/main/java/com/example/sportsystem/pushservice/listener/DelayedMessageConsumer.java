package com.example.sportsystem.pushservice.listener;

import com.example.sportsystem.pushservice.handler.WebSocketHandshakeHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 延迟消息消费者
 * 用于在未来某个时间点推送消息
 */
@Component
@Slf4j
public class DelayedMessageConsumer {

    private final StringRedisTemplate redisTemplate;
    private final WebSocketHandshakeHandler webSocketHandler;

    public DelayedMessageConsumer(StringRedisTemplate redisTemplate, WebSocketHandshakeHandler webSocketHandler) {
        this.redisTemplate = redisTemplate;
        this.webSocketHandler = webSocketHandler;
    }

    /**
     * 每隔1分钟扫描一次延迟消息队列
     */
    @Scheduled(fixedRate = 60000)
    public void processDelayedMessages() {
        long now = System.currentTimeMillis();

        // 获取已到期的消息
        Set<String> messages = redisTemplate.opsForZSet().rangeByScore("delayed_messages", 0, now);

        if (messages != null && !messages.isEmpty()) {
            for (String message : messages) {
                try {
                    // 解析消息中的 match_id（示例）
                    String matchId = extractMatchIdFromMessage(message);

                    // 推送消息
                    webSocketHandler.sendMessageToTopic(matchId, message);

                    // 移除已处理的消息
                    redisTemplate.opsForZSet().remove("delayed_messages", message);
                    log.info("[Redis] 延迟消息已处理: {}", message);

                } catch (Exception e) {
                    log.error("[Redis] 延迟消息处理失败:", e);
                }
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