package com.garden.icecrack.era_comparator.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class EraComparisonResultDTO {
    private Long id;
    private String summary;
    private Map<String, Object> ancientAvg;
    private Map<String, Object> modernAvg;
    private List<Map<String, Object>> allResults;
}
