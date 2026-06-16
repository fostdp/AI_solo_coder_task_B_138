package com.garden.icecrack.user_design.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class UserDesignRequestDTO {
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
