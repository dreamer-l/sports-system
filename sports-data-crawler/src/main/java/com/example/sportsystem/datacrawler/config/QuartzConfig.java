package com.example.sportsystem.datacrawler.config;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

/**
 * Quartz 定时任务配置类
 */
@Configuration
public class QuartzConfig {

    /**
     * 配置 SchedulerFactoryBean
     * @return SchedulerFactoryBean 实例
     */
    @Bean
    public SchedulerFactoryBean schedulerFactoryBean() {
        return new SchedulerFactoryBean();
    }

    /**
     * 获取 Scheduler 实例
     * @param schedulerFactoryBean Scheduler 工厂 bean
     * @return Scheduler 实例
     * @throws SchedulerException 异常
     */
    @Bean
    public Scheduler scheduler(SchedulerFactoryBean schedulerFactoryBean) throws SchedulerException {
        return schedulerFactoryBean.getScheduler();
    }
}