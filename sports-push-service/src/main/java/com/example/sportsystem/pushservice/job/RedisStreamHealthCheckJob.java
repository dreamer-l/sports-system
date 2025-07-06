package com.example.sportsystem.pushservice.job;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.StreamInfo;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Redis Stream 消费者组健康检查任务
 */
@Component
@Slf4j
public class RedisStreamHealthCheckJob {

    private final StringRedisTemplate redisTemplate;

    public RedisStreamHealthCheckJob(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 每隔10分钟检查一次消费者组健康状态
     */
    @Scheduled(fixedRate = 600000) // 10分钟
    public void checkStreamConsumerGroups() {
        List<String> streams = List.of("match_stream", "odds_stream");

        for (String streamKey : streams) {
            try {
                StreamInfo.XInfoConsumers consumersInfo = redisTemplate.getConnectionFactory().getConnection()
                        .streamCommands()
                        .xInfoConsumers(streamKey.getBytes(), "sports-push-consumer-group".getBytes());

                if (consumersInfo != null && !consumersInfo.isEmpty()) {
                    for (Consumer consumer : consumersInfo.getConsumers()) {
                        log.info("[Redis Stream] 消费者状态: {}@{} - 最后活跃时间: {}ms前, 未确认消息数: {}",
                                consumer.getName(), streamKey,
                                System.currentTimeMillis() - consumer.getIdleTime(),
                                consumer.getPendingMessages());

                        // 示例逻辑：如果消费者长时间不活跃，可以触发重新分配
                        if (consumer.getIdleTime() > 5 * 60 * 1000) { // 超过5分钟无响应
                            log.warn("[Redis Stream] 检测到不活跃消费者: {}, 正在尝试重新分配...", consumer.getName());
                            // TODO: 可在此处实现自动重新分配或通知机制
                        }
                    }
                }

            } catch (Exception e) {
                log.error("[Redis Stream] 消费者组健康检查失败:", e);
            }
        }
    }
}