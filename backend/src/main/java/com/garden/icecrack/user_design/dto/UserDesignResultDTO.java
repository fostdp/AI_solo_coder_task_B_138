package com.garden.icecrack.user_design.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class UserDesignResultDTO {
    private UUID id;
    private String userSessionId;
    private String designName;
    private String crackPattern;
    private Double areaLength;
    private Double areaWidth;
    private Double slopeAngle;
    private Double basePermeability;
    private Object aestheticResult;
    private Object drainageResult;
}
