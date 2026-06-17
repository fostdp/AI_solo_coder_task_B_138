package com.garden.icecrack.pedestrian_simulator.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class PedestrianSimulationResultDTO {
    private Long id;
    private UUID pavementId;
    private Double initialCrackWidthMm;
    private Double stepFrequency;
    private Long totalSteps;
    private Double simulationHours;
    private Double finalCrackWidthMm;
    private String widthHistory;
    private String segmentPropagation;
    private Double damageIndex;
}
