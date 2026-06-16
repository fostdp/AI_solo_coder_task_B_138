package com.garden.icecrack.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
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
@Table(name = "pavement")
public class Pavement {

    public enum PavementStyle {
        ICE_CRACK,
        HERRINGBONE,
        BASKETWEAVE,
        PERMEABLE_BRICK,
        CUSTOM
    }

    public enum Era {
        ANCIENT,
        MODERN
    }

    @Id
    private UUID id;

    private String name;

    private String location;

    private Double areaLength;

    private Double areaWidth;

    private Double slopeAngle;

    private Double basePermeability;

    @Column(columnDefinition = "jsonb")
    private String crackPattern;

    @Enumerated(jakarta.persistence.EnumType.STRING)
    private PavementStyle pavementStyle = PavementStyle.ICE_CRACK;

    @Enumerated(jakarta.persistence.EnumType.STRING)
    private Era era = Era.ANCIENT;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
