package com.garden.icecrack.pedestrian_simulator.controller;

import com.garden.icecrack.pedestrian_simulator.dto.PedestrianSimulationRequestDTO;
import com.garden.icecrack.pedestrian_simulator.dto.PedestrianSimulationResultDTO;
import com.garden.icecrack.pedestrian_simulator.service.PedestrianSimulatorService;
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
@RequestMapping("/api/pedestrian-simulator")
@RequiredArgsConstructor
public class PedestrianSimulatorController {

    private final PedestrianSimulatorService service;

    @PostMapping("/simulate")
    public ResponseEntity<PedestrianSimulationResultDTO> simulate(@RequestBody PedestrianSimulationRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.simulatePedestrianImpact(request));
    }

    @GetMapping("/{pavementId}/history")
    public ResponseEntity<List<PedestrianSimulationResultDTO>> history(@PathVariable UUID pavementId) {
        return ResponseEntity.ok(service.getHistory(pavementId));
    }
}
