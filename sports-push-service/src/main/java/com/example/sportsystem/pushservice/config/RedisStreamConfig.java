package com.example.sportsystem.pushservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamReadRequest;
import org.springframework.data.redis.stream.StreamOffset;
import org.springframework.data.redis.stream.ReadOffset;
import org.springframework.data.redis.stream.Consumer;
import org.springframework.data.redis.stream.ObjectRecord;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Redis Stream 配置类
 * 配置消费者组并注册监听器
 */
@Configuration
public class RedisStreamConfig {

    @Value("${websocket.redis.stream.match-stream}")
    private String matchStreamKey;

    @Value("${websocket.redis.stream.odds-stream}")
    private String oddsStreamKey;

    /**
     * 创建并配置 Redis Stream 消费者容器
     * @param connectionFactory Redis 连接工厂
     * @param streamListener 自定义 Stream 监听器
     * @return StreamMessageListenerContainer 实例
     */
    @Bean
    public StreamMessageListenerContainer<String, ObjectRecord<String, String>> streamMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            StreamListener<String, ObjectRecord<String, String>> streamListener) {

        // 动态消费者组名（可基于实例ID生成）
        String consumerGroup = "sports-push-consumer-group";
        String consumerInstance = generateConsumerInstanceName();

        // 构建监听请求（支持多个 Stream）
        StreamReadRequest<String> matchReadRequest = StreamReadRequest.builder(StreamOffset.create(matchStreamKey, ReadOffset.lastConsumed()))
                .consumer(Consumer.from(consumerGroup, consumerInstance))
                .build();

        StreamReadRequest<String> oddsReadRequest = StreamReadRequest.builder(StreamOffset.create(oddsStreamKey, ReadOffset.lastConsumed()))
                .consumer(Consumer.from(consumerGroup, consumerInstance))
                .build();

        // 创建容器并注册监听器
        StreamMessageListenerContainer<String, ObjectRecord<String, String>> container =
                StreamMessageListenerContainer.create(connectionFactory, streamListener);

        container.register(matchReadRequest, streamListener);
        container.register(oddsReadRequest, streamListener);
        container.start();

        return container;
    }

    private String generateConsumerInstanceName() {
        try {
            String hostName = InetAddress.getLocalHost().getHostName();
            return "push-service-instance-" + hostName + "-" + System.currentTimeMillis() % 1000;
        } catch (UnknownHostException e) {
            return "push-service-instance-default-" + System.currentTimeMillis() % 1000;
        }
    }
}