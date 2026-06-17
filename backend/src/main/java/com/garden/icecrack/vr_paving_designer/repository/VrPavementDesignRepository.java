package com.garden.icecrack.vr_paving_designer.repository;

import com.garden.icecrack.vr_paving_designer.entity.VrPavementDesign;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VrPavementDesignRepository extends JpaRepository<VrPavementDesign, UUID> {
    List<VrPavementDesign> findByUserSessionIdOrderByCreatedAtDesc(String userSessionId);
}
