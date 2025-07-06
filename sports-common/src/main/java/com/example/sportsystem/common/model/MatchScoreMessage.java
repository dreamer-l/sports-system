package com.example.sportsystem.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.example.sportsystem.common.entity.MatchScoreEntity;

// 添加缺失的导入语句
import java.time.LocalDateTime;

/**
 * 比分更新消息实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatchScoreMessage {
    private String matchId;
    private int homeScore;
    private int awayScore;
    private int minute;
    private String period;
    private String status;

    // 转换为数据库实体类（示例）
    public MatchScoreEntity toEntity() {
        MatchScoreEntity entity = new MatchScoreEntity();
        entity.setMatchId(matchId);
        entity.setHomeScore(homeScore);
        entity.setAwayScore(awayScore);
        entity.setMinute(minute);
        entity.setPeriod(period);
        entity.setStatus(status);
        entity.setUpdateTime(LocalDateTime.now());
        return entity;
    }
}