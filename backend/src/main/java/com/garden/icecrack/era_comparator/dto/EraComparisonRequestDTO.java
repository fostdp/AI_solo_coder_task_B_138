package com.garden.icecrack.era_comparator.dto;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class EraComparisonRequestDTO {
    private List<UUID> pavementIds;
    private Double rainfallMm;
    private Double initialWaterDepthMm;
    private Double crackWidthMm;
    private Double stepFrequency;
}
