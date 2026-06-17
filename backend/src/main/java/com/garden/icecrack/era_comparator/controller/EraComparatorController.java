package com.garden.icecrack.era_comparator.controller;

import com.garden.icecrack.era_comparator.dto.EraComparisonRequestDTO;
import com.garden.icecrack.era_comparator.dto.EraComparisonResultDTO;
import com.garden.icecrack.era_comparator.service.EraComparatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/era-comparison")
@RequiredArgsConstructor
public class EraComparatorController {

    private final EraComparatorService service;

    @PostMapping("/compare")
    public ResponseEntity<EraComparisonResultDTO> compareEras(@RequestBody EraComparisonRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.compareEras(request));
    }

    @GetMapping("/history")
    public ResponseEntity<List<EraComparisonResultDTO>> getHistory() {
        return ResponseEntity.ok(service.getHistory());
    }
}
