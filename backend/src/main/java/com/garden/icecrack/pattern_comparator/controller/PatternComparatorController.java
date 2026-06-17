package com.garden.icecrack.pattern_comparator.controller;

import com.garden.icecrack.pattern_comparator.dto.PatternComparisonRequestDTO;
import com.garden.icecrack.pattern_comparator.dto.PatternComparisonResultDTO;
import com.garden.icecrack.pattern_comparator.service.PatternComparatorService;
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
@RequestMapping("/api/pattern-comparison")
@RequiredArgsConstructor
public class PatternComparatorController {

    private final PatternComparatorService service;

    @PostMapping("/compare")
    public ResponseEntity<PatternComparisonResultDTO> compareStyles(@RequestBody PatternComparisonRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.compareStyles(request));
    }

    @GetMapping("/history")
    public ResponseEntity<List<PatternComparisonResultDTO>> getHistory() {
        return ResponseEntity.ok(service.getHistory());
    }
}
