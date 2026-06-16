package com.garden.icecrack.common.controller;

import com.garden.icecrack.common.dto.StyleComparisonRequestDTO;
import com.garden.icecrack.common.dto.StyleComparisonResultDTO;
import com.garden.icecrack.common.service.StyleComparisonService;
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
@RequestMapping("/api/comparison")
@RequiredArgsConstructor
public class ComparisonController {

    private final StyleComparisonService service;

    @PostMapping("/styles")
    public ResponseEntity<StyleComparisonResultDTO> compareStyles(@RequestBody StyleComparisonRequestDTO request) {
        request.setComparisonType("STYLE");
        return ResponseEntity.status(HttpStatus.CREATED).body(service.comparePavements(request));
    }

    @PostMapping("/eras")
    public ResponseEntity<StyleComparisonResultDTO> compareEras(@RequestBody StyleComparisonRequestDTO request) {
        request.setComparisonType("ERA");
        return ResponseEntity.status(HttpStatus.CREATED).body(service.comparePavements(request));
    }

    @GetMapping("/history")
    public ResponseEntity<List<StyleComparisonResultDTO>> history() {
        return ResponseEntity.ok(service.getHistory());
    }
}
