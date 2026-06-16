package com.garden.icecrack.common.repository;

import com.garden.icecrack.common.entity.StyleComparison;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StyleComparisonRepository extends JpaRepository<StyleComparison, Long> {
    List<StyleComparison> findByOrderByCreatedAtDesc();
}
