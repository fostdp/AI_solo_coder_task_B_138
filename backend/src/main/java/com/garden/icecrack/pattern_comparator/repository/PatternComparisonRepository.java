package com.garden.icecrack.pattern_comparator.repository;

import com.garden.icecrack.pattern_comparator.entity.PatternComparison;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PatternComparisonRepository extends JpaRepository<PatternComparison, Long> {
    List<PatternComparison> findByOrderByCreatedAtDesc();
}
