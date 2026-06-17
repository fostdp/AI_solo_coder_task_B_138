package com.garden.icecrack.era_comparator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.garden.icecrack.aesthetics_analyzer.dto.AestheticResultDTO;
import com.garden.icecrack.aesthetics_analyzer.service.AestheticQuantificationService;
import com.garden.icecrack.common.entity.Pavement;
import com.garden.icecrack.common.repository.PavementRepository;
import com.garden.icecrack.drainage_simulator.dto.SimulationRequestDTO;
import com.garden.icecrack.drainage_simulator.dto.SimulationResultDTO;
import com.garden.icecrack.drainage_simulator.service.DrainageSimulationService;
import com.garden.icecrack.era_comparator.dto.EraComparisonRequestDTO;
import com.garden.icecrack.era_comparator.dto.EraComparisonResultDTO;
import com.garden.icecrack.era_comparator.entity.EraComparison;
import com.garden.icecrack.era_comparator.repository.EraComparisonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EraComparatorService {

    private final EraComparisonRepository comparisonRepository;
    private final PavementRepository pavementRepository;
    private final AestheticQuantificationService aestheticService;
    private final DrainageSimulationService drainageService;
    private final ObjectMapper objectMapper;

    public EraComparisonResultDTO compareEras(EraComparisonRequestDTO request) {
        List<UUID> pavementIds = request.getPavementIds();
        if (pavementIds == null || pavementIds.isEmpty()) {
            pavementIds = pavementRepository.findAll().stream()
                    .limit(4)
                    .map(Pavement::getId)
                    .toList();
        }
        List<Pavement> pavements = pavementRepository.findAllById(pavementIds);

        List<Pavement> ancientGroup = new ArrayList<>();
        List<Pavement> modernGroup = new ArrayList<>();
        for (Pavement p : pavements) {
            if (p.getEra() == Pavement.Era.MODERN) {
                modernGroup.add(p);
            } else {
                ancientGroup.add(p);
            }
        }

        List<Map<String, Object>> allResults = new ArrayList<>();
        double ancientSumRecession = 0.0, ancientSumComplexity = 0.0, ancientSumInfiltration = 0.0;
        int ancientCount = 0;
        double modernSumRecession = 0.0, modernSumComplexity = 0.0, modernSumInfiltration = 0.0;
        int modernCount = 0;

        for (Pavement p : pavements) {
            AestheticResultDTO a = aestheticService.analyzePavement(p.getId());

            SimulationRequestDTO simReq = new SimulationRequestDTO();
            simReq.setPavementId(p.getId());
            simReq.setRainfallMm(request.getRainfallMm() != null ? request.getRainfallMm() : 50.0);
            simReq.setInitialWaterDepthMm(request.getInitialWaterDepthMm() != null ? request.getInitialWaterDepthMm() : 10.0);
            simReq.setCrackWidthMm(request.getCrackWidthMm() != null ? request.getCrackWidthMm() : 3.0);
            simReq.setStepFrequency(request.getStepFrequency() != null ? request.getStepFrequency() : 30.0);
            SimulationResultDTO s = drainageService.runSimulation(simReq);

            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("pavementId", p.getId());
            resultMap.put("pavementName", p.getName());
            resultMap.put("era", p.getEra() != null ? p.getEra().name() : "ANCIENT");
            resultMap.put("fractalDimension", a.getFractalDimension());
            resultMap.put("visualComplexity", a.getVisualComplexity());
            resultMap.put("patternSymmetry", a.getPatternSymmetry());
            resultMap.put("recessionTimeSec", s.getRecessionTimeSec());
            resultMap.put("infiltrationRate", s.getInfiltrationRate());
            resultMap.put("drainageRate", s.getDrainageRate());
            resultMap.put("peakWaterDepth", s.getPeakWaterDepth());
            allResults.add(resultMap);

            boolean isModern = p.getEra() == Pavement.Era.MODERN;
            if (isModern) {
                modernSumRecession += s.getRecessionTimeSec() != null ? s.getRecessionTimeSec() : 0.0;
                modernSumComplexity += a.getVisualComplexity() != null ? a.getVisualComplexity() : 0.0;
                modernSumInfiltration += s.getInfiltrationRate() != null ? s.getInfiltrationRate() : 0.0;
                modernCount++;
            } else {
                ancientSumRecession += s.getRecessionTimeSec() != null ? s.getRecessionTimeSec() : 0.0;
                ancientSumComplexity += a.getVisualComplexity() != null ? a.getVisualComplexity() : 0.0;
                ancientSumInfiltration += s.getInfiltrationRate() != null ? s.getInfiltrationRate() : 0.0;
                ancientCount++;
            }
        }

        Map<String, Object> ancientAvg = new HashMap<>();
        ancientAvg.put("avgRecession", ancientCount > 0 ? ancientSumRecession / ancientCount : 0.0);
        ancientAvg.put("avgComplexity", ancientCount > 0 ? ancientSumComplexity / ancientCount : 0.0);
        ancientAvg.put("avgInfiltration", ancientCount > 0 ? ancientSumInfiltration / ancientCount : 0.0);

        Map<String, Object> modernAvg = new HashMap<>();
        modernAvg.put("avgRecession", modernCount > 0 ? modernSumRecession / modernCount : 0.0);
        modernAvg.put("avgComplexity", modernCount > 0 ? modernSumComplexity / modernCount : 0.0);
        modernAvg.put("avgInfiltration", modernCount > 0 ? modernSumInfiltration / modernCount : 0.0);

        String summary = generateSummary(ancientAvg, modernAvg);

        EraComparison entity = new EraComparison();
        entity.setComparisonType("ERA");
        entity.setCreatedAt(LocalDateTime.now());
        entity.setSummary(summary);
        try {
            entity.setPavementIds(objectMapper.writeValueAsString(pavementIds));
            entity.setAncientResults(objectMapper.writeValueAsString(ancientAvg));
            entity.setModernResults(objectMapper.writeValueAsString(modernAvg));
        } catch (Exception e) {
            entity.setPavementIds("[]");
            entity.setAncientResults("{}");
            entity.setModernResults("{}");
        }
        EraComparison saved = comparisonRepository.save(entity);

        EraComparisonResultDTO dto = new EraComparisonResultDTO();
        dto.setId(saved.getId());
        dto.setSummary(saved.getSummary());
        dto.setAncientAvg(ancientAvg);
        dto.setModernAvg(modernAvg);
        dto.setAllResults(allResults);
        return dto;
    }

    private String generateSummary(Map<String, Object> ancientAvg, Map<String, Object> modernAvg) {
        StringBuilder sb = new StringBuilder();
        sb.append("跨时代对比分析：");
        double ancientRecession = ((Number) ancientAvg.getOrDefault("avgRecession", 0.0)).doubleValue();
        double modernRecession = ((Number) modernAvg.getOrDefault("avgRecession", 0.0)).doubleValue();
        double ancientComplexity = ((Number) ancientAvg.getOrDefault("avgComplexity", 0.0)).doubleValue();
        double modernComplexity = ((Number) modernAvg.getOrDefault("avgComplexity", 0.0)).doubleValue();
        double ancientInfiltration = ((Number) ancientAvg.getOrDefault("avgInfiltration", 0.0)).doubleValue();
        double modernInfiltration = ((Number) modernAvg.getOrDefault("avgInfiltration", 0.0)).doubleValue();
        sb.append(String.format("古代铺地平均退水时间 %.0fs，视觉复杂度 %.2f，渗透率 %.4f；",
                ancientRecession, ancientComplexity, ancientInfiltration));
        sb.append(String.format("现代透水砖平均退水时间 %.0fs，视觉复杂度 %.2f，渗透率 %.4f。",
                modernRecession, modernComplexity, modernInfiltration));
        if (modernInfiltration > ancientInfiltration) {
            sb.append("现代透水砖在排水效率上优势显著，渗透率远超古代铺地；而古代铺地在美学艺术性上更具价值，视觉复杂度更高。");
        } else {
            sb.append("本次样本中古代铺地表现出更好的排水效率，可能与冰裂纹的裂缝渗透有关；现代铺地视觉复杂度较低，偏重功能性。");
        }
        return sb.toString();
    }

    public List<EraComparisonResultDTO> getHistory() {
        return comparisonRepository.findByOrderByCreatedAtDesc().stream().map(entity -> {
            EraComparisonResultDTO dto = new EraComparisonResultDTO();
            dto.setId(entity.getId());
            dto.setSummary(entity.getSummary());
            try {
                if (entity.getAncientResults() != null) {
                    dto.setAncientAvg(objectMapper.readValue(entity.getAncientResults(), Map.class));
                }
                if (entity.getModernResults() != null) {
                    dto.setModernAvg(objectMapper.readValue(entity.getModernResults(), Map.class));
                }
            } catch (Exception ignored) {
            }
            return dto;
        }).toList();
    }
}
