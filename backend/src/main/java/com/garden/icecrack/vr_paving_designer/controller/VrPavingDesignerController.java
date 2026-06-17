package com.garden.icecrack.vr_paving_designer.controller;

import com.garden.icecrack.vr_paving_designer.dto.VrDesignRequestDTO;
import com.garden.icecrack.vr_paving_designer.dto.VrDesignResultDTO;
import com.garden.icecrack.vr_paving_designer.service.VrPavingDesignerService;
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
@RequestMapping("/api/vr-paving-designer")
@RequiredArgsConstructor
public class VrPavingDesignerController {

    private final VrPavingDesignerService service;

    @PostMapping("/submit")
    public ResponseEntity<VrDesignResultDTO> submit(@RequestBody VrDesignRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.processDesign(request));
    }

    @GetMapping("/session/{userSessionId}")
    public ResponseEntity<List<VrDesignResultDTO>> listBySession(@PathVariable String userSessionId) {
        return ResponseEntity.ok(service.getDesignsBySession(userSessionId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VrDesignResultDTO> get(@PathVariable UUID id) {
        VrDesignResultDTO result = service.getDesign(id);
        return result != null ? ResponseEntity.ok(result) : ResponseEntity.notFound().build();
    }
}
