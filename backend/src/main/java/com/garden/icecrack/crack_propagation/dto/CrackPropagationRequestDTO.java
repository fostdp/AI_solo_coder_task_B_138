package com.garden.icecrack.crack_propagation.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class CrackPropagationRequestDTO {
    private UUID pavementId;
    private Double initialCrackWidthMm;
    private Double stepFrequency;
    private Long totalSteps;
    private Double simulationHours;
    private String crackPattern;
}
