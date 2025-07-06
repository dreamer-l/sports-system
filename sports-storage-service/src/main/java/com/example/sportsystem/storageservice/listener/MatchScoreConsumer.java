package com.example.sportsystem.storageservice.listener;

import com.example.sportsystem.common.model.MatchScoreMessage;
import com.example.sportsystem.storageservice.repository.MatchScoreRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 比赛比分消息消费者
 * 监听来自 RocketMQ 的比分更新事件并持久化到数据库
 */
@Component
@RocketMQMessageListener(topic = "MATCH_SCORE_TOPIC", consumerGroup = "match-score-consumer-group")
@Slf4j
public class MatchScoreConsumer implements RocketMQListener<MatchScoreMessage> {

    private final MatchScoreRepository matchScoreRepository;

    public MatchScoreConsumer(MatchScoreRepository matchScoreRepository) {
        this.matchScoreRepository = matchScoreRepository;
    }

    /**
     * 处理比分更新消息
     * @param message 比分消息对象
     */
    @Override
    public void onMessage(MatchScoreMessage message) {
        try {
            // 示例：保存比分信息到数据库
            matchScoreRepository.save(message.toEntity());
            log.info("[RocketMQ] 比分消息已处理: {}", message);
        } catch (Exception e) {
            log.error("[RocketMQ] 比分消息处理失败:", e);
        }
    }
}