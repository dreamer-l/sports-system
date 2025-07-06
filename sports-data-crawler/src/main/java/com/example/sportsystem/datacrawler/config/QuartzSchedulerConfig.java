package com.example.sportsystem.datacrawler.config;

import com.example.sportsystem.datacrawler.job.FutureMatchesJob;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 定时任务调度配置类
 */
@Configuration
public class QuartzSchedulerConfig {

    /**
     * 定义 JobDetail
     * @return JobDetail 实例
     */
    @Bean
    public JobDetail futureMatchesJobDetail() {
        return JobBuilder.newJob(FutureMatchesJob.class)
                .withIdentity("futureMatchesJob")
                .storeDurably()
                .build();
    }

    /**
     * 定义 Trigger（每5分钟执行一次）
     * @param futureMatchesJobDetail JobDetail
     * @return Trigger 实例
     */
    @Bean
    public Trigger futureMatchesJobTrigger(JobDetail futureMatchesJobDetail) {
        SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
                .withIntervalInSeconds(300) // 每5分钟执行一次
                .repeatForever();

        return TriggerBuilder.newTrigger()
                .forJob(futureMatchesJobDetail)
                .withIdentity("futureMatchesTrigger")
                .withSchedule(scheduleBuilder)
                .build();
    }
}