package com.garden.icecrack.user_design.repository;

import com.garden.icecrack.user_design.entity.UserPavementDesign;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserPavementDesignRepository extends JpaRepository<UserPavementDesign, UUID> {
    List<UserPavementDesign> findByUserSessionIdOrderByCreatedAtDesc(String userSessionId);
}
