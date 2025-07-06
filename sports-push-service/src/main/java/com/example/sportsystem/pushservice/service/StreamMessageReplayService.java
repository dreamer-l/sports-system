package com.example.sportsystem.pushservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Redis Stream 消息重放服务
 * 提供接口用于回放历史比分、盘口等数据
 */
@RestController
@RequestMapping("/api/v1/stream/replay")
@Slf4j
public class StreamMessageReplayService {

    private final StringRedisTemplate redisTemplate;

    public StreamMessageReplayService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 回放指定赛事的比分历史记录
     * @param matchId 赛事ID
     * @param count 返回条数（可选）
     * @return 历史消息列表
     */
    @GetMapping("/match/{matchId}")
    public List<String> replayMatchScoreHistory(@PathVariable String matchId, @RequestParam(required = false) Integer count) {
        return replayStreamMessages("match_stream", matchId, count != null ? count : 10);
    }

    /**
     * 回放指定赛事的盘口历史记录
     * @param matchId 赛事ID
     * @param count 返回条数（可选）
     * @return 历史消息列表
     */
    @GetMapping("/odds/{matchId}")
    public List<String> replayOddsUpdateHistory(@PathVariable String matchId, @RequestParam(required = false) Integer count) {
        return replayStreamMessages("odds_stream", matchId, count != null ? count : 10);
    }

    /**
     * 回放 Redis Stream 中的消息
     * @param streamKey Stream 名称
     * @param matchId 赛事ID
     * @param count 获取条数
     * @return 消息列表
     */
    private List<String> replayStreamMessages(String streamKey, String matchId, int count) {
        try {
            // 构建查询条件（示例：查找包含 match_id 的消息）
            StreamOffset<String> offset = StreamOffset.from(streamKey).equalOrGreater().id(matchId);

            // 查询历史消息
            List<ObjectRecord<String, String>> records = redisTemplate.opsForStream().read(offset, count);

            if (records != null && !records.isEmpty()) {
                List<String> messages = new ArrayList<>();
                for (ObjectRecord<String, String> record : records) {
                    messages.add(record.getValue());
                }
                log.info("[Redis Stream] 成功回放 {} 条消息 from {}@{}", messages.size(), streamKey, matchId);
                return messages;
            }

            return List.of();

        } catch (Exception e) {
            log.error("[Redis Stream] 消息回放失败:", e);
            return List.of();
        }
    }
}