package com.garden.icecrack.pattern_comparator.dto;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class PatternComparisonRequestDTO {
    private String comparisonType;
    private List<UUID> pavementIds;
    private Double rainfallMm;
    private Double initialWaterDepthMm;
    private Double crackWidthMm;
    private Double stepFrequency;
}
