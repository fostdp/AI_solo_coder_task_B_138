package com.garden.icecrack.vr_paving_designer.service;

import com.garden.icecrack.aesthetics_analyzer.dto.AestheticResultDTO;
import com.garden.icecrack.aesthetics_analyzer.service.AestheticQuantificationService;
import com.garden.icecrack.drainage_simulator.dto.SimulationRequestDTO;
import com.garden.icecrack.drainage_simulator.dto.SimulationResultDTO;
import com.garden.icecrack.drainage_simulator.service.DrainageSimulationService;
import com.garden.icecrack.vr_paving_designer.dto.VrDesignRequestDTO;
import com.garden.icecrack.vr_paving_designer.dto.VrDesignResultDTO;
import com.garden.icecrack.vr_paving_designer.entity.VrPavementDesign;
import com.garden.icecrack.vr_paving_designer.repository.VrPavementDesignRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VrPavingDesignerService {

    private final VrPavementDesignRepository repository;
    private final AestheticQuantificationService aestheticService;
    private final DrainageSimulationService drainageService;

    public VrDesignResultDTO processDesign(VrDesignRequestDTO request) {
        VrPavementDesign design = new VrPavementDesign();
        design.setId(UUID.randomUUID());
        design.setUserSessionId(request.getUserSessionId() != null ? request.getUserSessionId() : UUID.randomUUID().toString());
        design.setDesignName(request.getDesignName() != null ? request.getDesignName() : "未命名设计");
        design.setCreatedAt(LocalDateTime.now());
        design.setCrackPattern(request.getCrackPattern());
        design.setAreaLength(request.getAreaLength() != null ? request.getAreaLength() : 10.0);
        design.setAreaWidth(request.getAreaWidth() != null ? request.getAreaWidth() : 10.0);
        design.setSlopeAngle(request.getSlopeAngle() != null ? request.getSlopeAngle() : 2.0);
        design.setBasePermeability(request.getBasePermeability() != null ? request.getBasePermeability() : 0.001);

        Map<String, Object> aestheticResult = null;
        Map<String, Object> drainageResult = null;

        if (Boolean.TRUE.equals(request.getRunAesthetic()) && request.getCrackPattern() != null) {
            AestheticResultDTO a = aestheticService.analyzeCustomSegments(
                    request.getCrackPattern(), design.getAreaLength(), design.getAreaWidth());
            aestheticResult = new HashMap<>();
            aestheticResult.put("fractalDimension", a.getFractalDimension());
            aestheticResult.put("boxCountingDim", a.getBoxCountingDim());
            aestheticResult.put("infoEntropy", a.getInfoEntropy());
            aestheticResult.put("visualComplexity", a.getVisualComplexity());
            aestheticResult.put("crackCount", a.getCrackCount());
            aestheticResult.put("avgCrackLength", a.getAvgCrackLength());
            aestheticResult.put("crackDensity", a.getCrackDensity());
            aestheticResult.put("patternSymmetry", a.getPatternSymmetry());
            aestheticResult.put("crackSegments", a.getCrackSegments());
        }

        if (Boolean.TRUE.equals(request.getRunDrainage())) {
            SimulationRequestDTO simReq = new SimulationRequestDTO();
            simReq.setRainfallMm(request.getRainfallMm() != null ? request.getRainfallMm() : 50.0);
            simReq.setInitialWaterDepthMm(request.getInitialWaterDepthMm() != null ? request.getInitialWaterDepthMm() : 10.0);
            simReq.setCrackWidthMm(request.getCrackWidthMm() != null ? request.getCrackWidthMm() : 3.0);
            simReq.setStepFrequency(request.getStepFrequency() != null ? request.getStepFrequency() : 30.0);
            SimulationResultDTO s = drainageService.runCustomSimulation(
                    simReq, design.getAreaLength(), design.getAreaWidth(),
                    design.getSlopeAngle(), design.getBasePermeability());
            drainageResult = new HashMap<>();
            drainageResult.put("initialWaterDepth", s.getInitialWaterDepth());
            drainageResult.put("recessionTimeSec", s.getRecessionTimeSec());
            drainageResult.put("peakWaterDepth", s.getPeakWaterDepth());
            drainageResult.put("drainageRate", s.getDrainageRate());
            drainageResult.put("infiltrationRate", s.getInfiltrationRate());
            drainageResult.put("surfaceRunoffRate", s.getSurfaceRunoffRate());
            drainageResult.put("timeSeries", s.getTimeSeries());
            drainageResult.put("gridData", s.getGridData());
            drainageResult.put("alertTriggered", s.getAlertTriggered());
            drainageResult.put("alertMessage", s.getAlertMessage());
        }

        VrPavementDesign saved = repository.save(design);

        VrDesignResultDTO dto = new VrDesignResultDTO();
        dto.setId(saved.getId());
        dto.setUserSessionId(saved.getUserSessionId());
        dto.setDesignName(saved.getDesignName());
        dto.setCrackPattern(saved.getCrackPattern());
        dto.setAreaLength(saved.getAreaLength());
        dto.setAreaWidth(saved.getAreaWidth());
        dto.setSlopeAngle(saved.getSlopeAngle());
        dto.setBasePermeability(saved.getBasePermeability());
        dto.setAestheticResult(aestheticResult);
        dto.setDrainageResult(drainageResult);
        return dto;
    }

    public List<VrDesignResultDTO> getDesignsBySession(String userSessionId) {
        return repository.findByUserSessionIdOrderByCreatedAtDesc(userSessionId)
                .stream().map(e -> {
                    VrDesignResultDTO dto = new VrDesignResultDTO();
                    dto.setId(e.getId());
                    dto.setUserSessionId(e.getUserSessionId());
                    dto.setDesignName(e.getDesignName());
                    dto.setCrackPattern(e.getCrackPattern());
                    dto.setAreaLength(e.getAreaLength());
                    dto.setAreaWidth(e.getAreaWidth());
                    dto.setSlopeAngle(e.getSlopeAngle());
                    dto.setBasePermeability(e.getBasePermeability());
                    return dto;
                }).toList();
    }

    public VrDesignResultDTO getDesign(UUID id) {
        return repository.findById(id).map(e -> {
            VrDesignResultDTO dto = new VrDesignResultDTO();
            dto.setId(e.getId());
            dto.setUserSessionId(e.getUserSessionId());
            dto.setDesignName(e.getDesignName());
            dto.setCrackPattern(e.getCrackPattern());
            dto.setAreaLength(e.getAreaLength());
            dto.setAreaWidth(e.getAreaWidth());
            dto.setSlopeAngle(e.getSlopeAngle());
            dto.setBasePermeability(e.getBasePermeability());
            return dto;
        }).orElse(null);
    }
}
