package com.example.sportsystem.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// 添加缺失的导入语句
import java.time.LocalDateTime;

/**
 * 比赛信息实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatchInfo {
    private String matchId;
    private String homeTeam;
    private String awayTeam;
    private LocalDateTime matchTime;
    private String sportType;
    private String league;
}