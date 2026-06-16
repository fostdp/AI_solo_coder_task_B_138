package com.garden.icecrack.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "style_comparison")
public class StyleComparison {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String comparisonType;

    private LocalDateTime createdAt;

    @Column(columnDefinition = "jsonb")
    private String pavementIds;

    @Column(columnDefinition = "jsonb")
    private String aestheticResults;

    @Column(columnDefinition = "jsonb")
    private String drainageResults;

    @Column(columnDefinition = "text")
    private String summary;
}
