package com.garden.icecrack.user_design.controller;

import com.garden.icecrack.user_design.dto.UserDesignRequestDTO;
import com.garden.icecrack.user_design.dto.UserDesignResultDTO;
import com.garden.icecrack.user_design.service.UserDesignService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/user-design")
@RequiredArgsConstructor
public class UserDesignController {

    private final UserDesignService service;

    @PostMapping("/submit")
    public ResponseEntity<UserDesignResultDTO> submit(@RequestBody UserDesignRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.processDesign(request));
    }

    @GetMapping("/session/{userSessionId}")
    public ResponseEntity<List<UserDesignResultDTO>> listBySession(@PathVariable String userSessionId) {
        return ResponseEntity.ok(service.getDesignsBySession(userSessionId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDesignResultDTO> get(@PathVariable UUID id) {
        UserDesignResultDTO result = service.getDesign(id);
        return result != null ? ResponseEntity.ok(result) : ResponseEntity.notFound().build();
    }
}
