package com.garden.icecrack.pattern_comparator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.garden.icecrack.aesthetics_analyzer.dto.AestheticResultDTO;
import com.garden.icecrack.aesthetics_analyzer.service.AestheticQuantificationService;
import com.garden.icecrack.common.entity.Pavement;
import com.garden.icecrack.common.repository.PavementRepository;
import com.garden.icecrack.drainage_simulator.dto.SimulationRequestDTO;
import com.garden.icecrack.drainage_simulator.dto.SimulationResultDTO;
import com.garden.icecrack.drainage_simulator.service.DrainageSimulationService;
import com.garden.icecrack.pattern_comparator.dto.PatternComparisonRequestDTO;
import com.garden.icecrack.pattern_comparator.dto.PatternComparisonResultDTO;
import com.garden.icecrack.pattern_comparator.entity.PatternComparison;
import com.garden.icecrack.pattern_comparator.repository.PatternComparisonRepository;
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
public class PatternComparatorService {

    private final PatternComparisonRepository comparisonRepository;
    private final PavementRepository pavementRepository;
    private final AestheticQuantificationService aestheticService;
    private final DrainageSimulationService drainageService;
    private final ObjectMapper objectMapper;

    public PatternComparisonResultDTO compareStyles(PatternComparisonRequestDTO request) {
        List<UUID> pavementIds = request.getPavementIds();
        if (pavementIds == null || pavementIds.isEmpty()) {
            pavementIds = pavementRepository.findAll().stream()
                    .limit(4)
                    .map(Pavement::getId)
                    .toList();
        }
        List<Pavement> pavements = pavementRepository.findAllById(pavementIds);

        List<Map<String, Object>> aestheticResults = new ArrayList<>();
        List<Map<String, Object>> drainageResults = new ArrayList<>();

        for (Pavement p : pavements) {
            AestheticResultDTO a = aestheticService.analyzePavement(p.getId());
            Map<String, Object> aMap = new HashMap<>();
            aMap.put("pavementId", p.getId());
            aMap.put("pavementName", p.getName());
            aMap.put("pavementStyle", p.getPavementStyle() != null ? p.getPavementStyle().name() : "ICE_CRACK");
            aMap.put("era", p.getEra() != null ? p.getEra().name() : "ANCIENT");
            aMap.put("fractalDimension", a.getFractalDimension());
            aMap.put("boxCountingDim", a.getBoxCountingDim());
            aMap.put("infoEntropy", a.getInfoEntropy());
            aMap.put("visualComplexity", a.getVisualComplexity());
            aMap.put("crackCount", a.getCrackCount());
            aMap.put("avgCrackLength", a.getAvgCrackLength());
            aMap.put("crackDensity", a.getCrackDensity());
            aMap.put("patternSymmetry", a.getPatternSymmetry());
            aestheticResults.add(aMap);

            SimulationRequestDTO simReq = new SimulationRequestDTO();
            simReq.setPavementId(p.getId());
            simReq.setRainfallMm(request.getRainfallMm() != null ? request.getRainfallMm() : 50.0);
            simReq.setInitialWaterDepthMm(request.getInitialWaterDepthMm() != null ? request.getInitialWaterDepthMm() : 10.0);
            simReq.setCrackWidthMm(request.getCrackWidthMm() != null ? request.getCrackWidthMm() : 3.0);
            simReq.setStepFrequency(request.getStepFrequency() != null ? request.getStepFrequency() : 30.0);
            SimulationResultDTO s = drainageService.runSimulation(simReq);
            Map<String, Object> sMap = new HashMap<>();
            sMap.put("pavementId", p.getId());
            sMap.put("pavementName", p.getName());
            sMap.put("pavementStyle", p.getPavementStyle() != null ? p.getPavementStyle().name() : "ICE_CRACK");
            sMap.put("era", p.getEra() != null ? p.getEra().name() : "ANCIENT");
            sMap.put("initialWaterDepth", s.getInitialWaterDepth());
            sMap.put("recessionTimeSec", s.getRecessionTimeSec());
            sMap.put("peakWaterDepth", s.getPeakWaterDepth());
            sMap.put("drainageRate", s.getDrainageRate());
            sMap.put("infiltrationRate", s.getInfiltrationRate());
            sMap.put("surfaceRunoffRate", s.getSurfaceRunoffRate());
            sMap.put("alertTriggered", s.getAlertTriggered());
            sMap.put("alertMessage", s.getAlertMessage());
            drainageResults.add(sMap);
        }

        String summary = generateStyleSummary(aestheticResults, drainageResults, request.getComparisonType());

        PatternComparison entity = new PatternComparison();
        entity.setComparisonType(request.getComparisonType() != null ? request.getComparisonType() : "STYLE");
        entity.setCreatedAt(LocalDateTime.now());
        entity.setSummary(summary);
        try {
            entity.setPavementIds(objectMapper.writeValueAsString(pavementIds));
            entity.setAestheticResults(objectMapper.writeValueAsString(aestheticResults));
            entity.setDrainageResults(objectMapper.writeValueAsString(drainageResults));
        } catch (Exception e) {
            entity.setPavementIds("[]");
            entity.setAestheticResults("[]");
            entity.setDrainageResults("[]");
        }
        PatternComparison saved = comparisonRepository.save(entity);

        PatternComparisonResultDTO dto = new PatternComparisonResultDTO();
        dto.setId(saved.getId());
        dto.setComparisonType(saved.getComparisonType());
        dto.setSummary(saved.getSummary());
        dto.setAestheticResults(aestheticResults);
        dto.setDrainageResults(drainageResults);
        return dto;
    }

    private String generateStyleSummary(List<Map<String, Object>> aestheticResults,
                                         List<Map<String, Object>> drainageResults,
                                         String comparisonType) {
        StringBuilder sb = new StringBuilder();
        if ("ERA".equals(comparisonType)) {
            sb.append("跨时代对比分析：");
            double ancientRecession = average(drainageResults, "recessionTimeSec", "ANCIENT");
            double modernRecession = average(drainageResults, "recessionTimeSec", "MODERN");
            double ancientComplexity = average(aestheticResults, "visualComplexity", "ANCIENT");
            double modernComplexity = average(aestheticResults, "visualComplexity", "MODERN");
            sb.append(String.format("古代铺地平均退水时间 %.0fs，视觉复杂度 %.2f；", ancientRecession, ancientComplexity));
            sb.append(String.format("现代透水砖平均退水时间 %.0fs，视觉复杂度 %.2f。", modernRecession, modernComplexity));
            if (modernRecession < ancientRecession) {
                sb.append("现代透水砖在排水效率上优势显著，而古代铺地在美学艺术性上更具价值。");
            } else {
                sb.append("本次样本中古代铺地表现出更好的排水效率，可能与冰裂纹的裂缝渗透有关。");
            }
        } else {
            sb.append("样式对比分析：");
            Map<String, Double> recessionByStyle = groupAverage(drainageResults, "recessionTimeSec");
            Map<String, Double> complexityByStyle = groupAverage(aestheticResults, "visualComplexity");
            for (Map.Entry<String, Double> entry : complexityByStyle.entrySet()) {
                String style = entry.getKey();
                Double rec = recessionByStyle.get(style);
                sb.append(String.format("%s：视觉复杂度 %.2f，退水时间 %.0fs；",
                        style, entry.getValue(), rec != null ? rec : 0.0));
            }
        }
        return sb.toString();
    }

    private double average(List<Map<String, Object>> list, String key, String eraFilter) {
        double sum = 0.0;
        int count = 0;
        for (Map<String, Object> m : list) {
            if (eraFilter.equals(m.get("era")) && m.get(key) instanceof Number n) {
                sum += n.doubleValue();
                count++;
            }
        }
        return count > 0 ? sum / count : 0.0;
    }

    private Map<String, Double> groupAverage(List<Map<String, Object>> list, String key) {
        Map<String, Double> sum = new HashMap<>();
        Map<String, Integer> count = new HashMap<>();
        for (Map<String, Object> m : list) {
            String style = (String) m.get("pavementStyle");
            if (m.get(key) instanceof Number n) {
                sum.merge(style, n.doubleValue(), Double::sum);
                count.merge(style, 1, Integer::sum);
            }
        }
        Map<String, Double> result = new HashMap<>();
        for (String s : sum.keySet()) {
            int c = count.getOrDefault(s, 1);
            result.put(s, sum.get(s) / c);
        }
        return result;
    }

    public List<PatternComparisonResultDTO> getHistory() {
        return comparisonRepository.findByOrderByCreatedAtDesc().stream().map(entity -> {
            PatternComparisonResultDTO dto = new PatternComparisonResultDTO();
            dto.setId(entity.getId());
            dto.setComparisonType(entity.getComparisonType());
            dto.setSummary(entity.getSummary());
            try {
                if (entity.getAestheticResults() != null) {
                    dto.setAestheticResults(objectMapper.readValue(entity.getAestheticResults(), List.class));
                }
                if (entity.getDrainageResults() != null) {
                    dto.setDrainageResults(objectMapper.readValue(entity.getDrainageResults(), List.class));
                }
            } catch (Exception ignored) {
            }
            return dto;
        }).toList();
    }
}
