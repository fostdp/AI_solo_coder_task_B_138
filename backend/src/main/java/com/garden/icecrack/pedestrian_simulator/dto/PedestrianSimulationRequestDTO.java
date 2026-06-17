package com.garden.icecrack.pedestrian_simulator.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class PedestrianSimulationRequestDTO {
    private UUID pavementId;
    private Double initialCrackWidthMm;
    private Double stepFrequency;
    private Long totalSteps;
    private Double simulationHours;
    private String crackPattern;
}
