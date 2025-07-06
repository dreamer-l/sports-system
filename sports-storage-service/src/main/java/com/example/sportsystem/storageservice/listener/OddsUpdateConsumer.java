package com.example.sportsystem.storageservice.listener;

import com.example.sportsystem.common.model.OddsUpdateMessage;
import com.example.sportsystem.storageservice.repository.MatchOddsRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 盘口数据消息消费者
 * 监听来自 RocketMQ 的盘口更新事件并持久化到数据库
 */
@Component
@RocketMQMessageListener(topic = "ODDS_UPDATE_TOPIC", consumerGroup = "odds-update-consumer-group")
@Slf4j
public class OddsUpdateConsumer implements RocketMQListener<OddsUpdateMessage> {

    private final MatchOddsRepository matchOddsRepository;

    public OddsUpdateConsumer(MatchOddsRepository matchOddsRepository) {
        this.matchOddsRepository = matchOddsRepository;
    }

    /**
     * 处理盘口更新消息
     * @param message 盘口消息对象
     */
    @Override
    public void onMessage(OddsUpdateMessage message) {
        try {
            // 示例：保存盘口信息到数据库
            matchOddsRepository.save(message.toEntity());
            log.info("[RocketMQ] 盘口消息已处理: {}", message);
        } catch (Exception e) {
            log.error("[RocketMQ] 盘口消息处理失败:", e);
        }
    }
}