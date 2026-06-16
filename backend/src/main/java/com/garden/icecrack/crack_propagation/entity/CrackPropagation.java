package com.garden.icecrack.crack_propagation.entity;

import com.garden.icecrack.common.entity.Pavement;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "crack_propagation")
public class CrackPropagation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Pavement pavement;

    private LocalDateTime createdAt;

    private Double initialCrackWidthMm;

    private Double stepFrequency;

    private Long totalSteps;

    private Double simulationHours;

    private Double finalCrackWidthMm;

    @Column(columnDefinition = "jsonb")
    private String widthHistory;

    @Column(columnDefinition = "jsonb")
    private String segmentPropagation;

    private Double damageIndex;
}
