package com.garden.icecrack.pedestrian_simulator.repository;

import com.garden.icecrack.pedestrian_simulator.entity.PedestrianSimulation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PedestrianSimulationRepository extends JpaRepository<PedestrianSimulation, Long> {
    List<PedestrianSimulation> findByPavementIdOrderByCreatedAtDesc(UUID pavementId);
}
