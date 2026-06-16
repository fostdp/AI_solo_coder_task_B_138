package com.garden.icecrack.crack_propagation.controller;

import com.garden.icecrack.crack_propagation.dto.CrackPropagationRequestDTO;
import com.garden.icecrack.crack_propagation.dto.CrackPropagationResultDTO;
import com.garden.icecrack.crack_propagation.service.CrackPropagationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/crack-propagation")
@RequiredArgsConstructor
public class CrackPropagationController {

    private final CrackPropagationService service;

    @PostMapping("/simulate")
    public ResponseEntity<CrackPropagationResultDTO> simulate(@RequestBody CrackPropagationRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.simulatePropagation(request));
    }

    @GetMapping("/{pavementId}/history")
    public ResponseEntity<List<CrackPropagationResultDTO>> history(@PathVariable UUID pavementId) {
        return ResponseEntity.ok(service.getHistory(pavementId));
    }
}
