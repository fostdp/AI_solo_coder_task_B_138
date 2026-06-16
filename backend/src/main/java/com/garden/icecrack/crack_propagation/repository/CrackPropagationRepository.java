package com.garden.icecrack.crack_propagation.repository;

import com.garden.icecrack.crack_propagation.entity.CrackPropagation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CrackPropagationRepository extends JpaRepository<CrackPropagation, Long> {
    List<CrackPropagation> findByPavementIdOrderByCreatedAtDesc(UUID pavementId);
}
