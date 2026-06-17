package com.garden.icecrack.vr_paving_designer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_pavement_design")
public class VrPavementDesign {

    @Id
    private UUID id;

    private String userSessionId;

    private String designName;

    private LocalDateTime createdAt;

    @Column(columnDefinition = "jsonb")
    private String crackPattern;

    private Double areaLength;

    private Double areaWidth;

    private Double slopeAngle;

    private Double basePermeability;

    private Long aestheticResultId;

    private Long drainageSimulationId;
}
