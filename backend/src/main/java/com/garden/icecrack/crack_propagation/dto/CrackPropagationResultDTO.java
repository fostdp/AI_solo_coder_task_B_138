package com.garden.icecrack.crack_propagation.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class CrackPropagationResultDTO {
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
