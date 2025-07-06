package com.example.sportsystem.datacrawler.job;

import com.example.sportsystem.common.model.MatchInfo;
import com.example.sportsystem.common.model.MatchScoreMessage;
import com.example.sportsystem.common.model.OddsUpdateMessage;
import com.example.sportsystem.datacrawler.service.DataCrawlerService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 定时任务类，用于获取未来比赛信息
 */
@Component
@Slf4j
public class FutureMatchesJob implements Job {

    @Autowired
    private DataCrawlerService dataCrawlerService;

    /**
     * 执行定时任务逻辑
     * @param context Job执行上下文
     * @throws JobExecutionException 异常
     */
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            List<MatchInfo> matches = dataCrawlerService.getFutureMatches();
            log.info("[定时任务] 采集到 {} 场未来赛事", matches.size());
        } catch (Exception e) {
            log.error("[定时任务] 采集未来赛事失败:", e);
        }
    }
}