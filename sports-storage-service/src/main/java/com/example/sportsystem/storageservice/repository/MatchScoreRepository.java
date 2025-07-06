package com.example.sportsystem.storageservice.repository;

import com.example.sportsystem.common.entity.MatchScoreEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 比分数据持久化接口
 */
public interface MatchScoreRepository extends JpaRepository<MatchScoreEntity, String> {
}