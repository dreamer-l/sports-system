package com.example.sportsystem.storageservice.repository;

import com.example.sportsystem.common.entity.MatchOddsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 盘口数据持久化接口
 */
public interface MatchOddsRepository extends JpaRepository<MatchOddsEntity, String> {
}