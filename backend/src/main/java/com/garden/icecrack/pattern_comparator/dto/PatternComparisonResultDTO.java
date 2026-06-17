package com.garden.icecrack.pattern_comparator.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class PatternComparisonResultDTO {
    private Long id;
    private String comparisonType;
    private String summary;
    private List<Map<String, Object>> aestheticResults;
    private List<Map<String, Object>> drainageResults;
}
