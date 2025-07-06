package com.example.sportsystem.storageservice.config;

import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RocketMQ 消费者基础配置类
 * 提供通用的消息监听模板
 */
@Configuration
public class RocketMQConfig {

    /**
     * 示例：注册一个通用的消息打印监听器
     * @return RocketMQListener 实例
     */
    @Bean
    public RocketMQListener<String> exampleRocketMQListener() {
        return message -> {
            System.out.println("[RocketMQ] 收到消息: " + message);
        };
    }
}