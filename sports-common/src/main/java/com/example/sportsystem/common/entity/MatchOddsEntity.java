package com.example.sportsystem.common.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 盘口数据实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatchOddsEntity {
    private String matchId;
    private String bookmaker;
    private String oddsType;
    private String handicap;
    private double homeOdds;
    private double awayOdds;
    private LocalDateTime timestamp;
}