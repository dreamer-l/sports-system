package com.example.sportsystem.common.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 比分数据实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatchScoreEntity {
    private String matchId;
    private int homeScore;
    private int awayScore;
    private int minute;
    private String period;
    private String status;
    private LocalDateTime updateTime;
}