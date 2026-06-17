package com.garden.icecrack.era_comparator.repository;

import com.garden.icecrack.era_comparator.entity.EraComparison;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EraComparisonRepository extends JpaRepository<EraComparison, Long> {
    List<EraComparison> findByOrderByCreatedAtDesc();
}
