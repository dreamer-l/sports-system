package com.example.sportsystem.pushservice.filter;

import com.example.sportsystem.common.model.MatchScoreMessage;
import com.example.sportsystem.common.model.OddsUpdateMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * Redis Stream 消息过滤器
 * 支持按赛事类型、联赛等级等维度过滤消息
 */
@Component
public class StreamMessageFilter {

    /**
     * 消息匹配规则
     */
    @Data
    @AllArgsConstructor
    public static class FilterRule {
        private Set<String> sportTypes; // 赛事类型（足球、篮球等）
        private Set<String> leagueLevels; // 联赛等级（英超、西甲等）
        private Set<String> matchIds; // 赛事ID白名单
    }

    /**
     * 判断比分消息是否匹配规则
     * @param message 比分消息
     * @param rule 过滤规则
     * @return 是否匹配
     */
    public boolean matches(MatchScoreMessage message, FilterRule rule) {
        return (rule.getSportTypes() == null || rule.getSportTypes().isEmpty() || rule.getSportTypes().contains(message.getSportType())) &&
               (rule.getLeagueLevels() == null || rule.getLeagueLevels().isEmpty() || rule.getLeagueLevels().contains(message.getLeagueLevel())) &&
               (rule.getMatchIds() == null || rule.getMatchIds().isEmpty() || rule.getMatchIds().contains(message.getMatchId()));
    }

    /**
     * 判断盘口消息是否匹配规则
     * @param message 盘口消息
     * @param rule 过滤规则
     * @return 是否匹配
     */
    public boolean matches(OddsUpdateMessage message, FilterRule rule) {
        return (rule.getSportTypes() == null || rule.getSportTypes().isEmpty() || rule.getSportTypes().contains(message.getSportType())) &&
               (rule.getLeagueLevels() == null || rule.getLeagueLevels().isEmpty() || rule.getLeagueLevels().contains(message.getLeagueLevel())) &&
               (rule.getMatchIds() == null || rule.getMatchIds().isEmpty() || rule.getMatchIds().contains(message.getMatchId()));
    }

    /**
     * 构建默认规则（允许所有）
     * @return 默认规则
     */
    public static FilterRule defaultRule() {
        return new FilterRule(new HashSet<>(), new HashSet<>(), new HashSet<>());
    }
}