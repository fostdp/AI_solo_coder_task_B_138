package com.garden.icecrack.vr_paving_designer.dto;

import lombok.Data;

import java.util.Map;
import java.util.UUID;

@Data
public class VrDesignResultDTO {
    private UUID id;
    private String userSessionId;
    private String designName;
    private String crackPattern;
    private Double areaLength;
    private Double areaWidth;
    private Double slopeAngle;
    private Double basePermeability;
    private Map<String, Object> aestheticResult;
    private Map<String, Object> drainageResult;
}
