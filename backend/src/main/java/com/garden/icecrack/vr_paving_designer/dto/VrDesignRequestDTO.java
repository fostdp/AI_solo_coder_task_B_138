package com.garden.icecrack.vr_paving_designer.dto;

import lombok.Data;

@Data
public class VrDesignRequestDTO {
    private String userSessionId;
    private String designName;
    private String crackPattern;
    private Double areaLength;
    private Double areaWidth;
    private Double slopeAngle;
    private Double basePermeability;
    private Boolean runAesthetic = true;
    private Boolean runDrainage = true;
    private Double rainfallMm;
    private Double initialWaterDepthMm;
    private Double crackWidthMm;
    private Double stepFrequency;
}
