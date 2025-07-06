package com.example.sportsystem.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.example.sportsystem.common.entity.MatchOddsEntity;

// 添加缺失的导入语句
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * 盘口更新消息实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OddsUpdateMessage {
    private String matchId;
    private String bookmaker;
    private String oddsType;
    private String handicap;
    private double homeOdds;
    private double awayOdds;
    private long timestamp;

    // 转换为数据库实体类（示例）
    public MatchOddsEntity toEntity() {
        MatchOddsEntity entity = new MatchOddsEntity();
        entity.setMatchId(matchId);
        entity.setBookmaker(bookmaker);
        entity.setOddsType(oddsType);
        entity.setHandicap(handicap);
        entity.setHomeOdds(homeOdds);
        entity.setAwayOdds(awayOdds);
        entity.setTimestamp(LocalDateTime.ofEpochSecond(timestamp / 1000, 0, ZoneOffset.UTC));
        return entity;
    }
}