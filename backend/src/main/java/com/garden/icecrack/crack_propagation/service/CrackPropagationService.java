package com.garden.icecrack.crack_propagation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.garden.icecrack.crack_propagation.dto.CrackPropagationRequestDTO;
import com.garden.icecrack.crack_propagation.dto.CrackPropagationResultDTO;
import com.garden.icecrack.crack_propagation.entity.CrackPropagation;
import com.garden.icecrack.crack_propagation.repository.CrackPropagationRepository;
import com.garden.icecrack.common.entity.Pavement;
import com.garden.icecrack.common.pattern.PavementPatternFactory;
import com.garden.icecrack.common.repository.PavementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CrackPropagationService {

    private final CrackPropagationRepository repository;
    private final PavementRepository pavementRepository;
    private final ObjectMapper objectMapper;

    public CrackPropagationResultDTO simulatePropagation(CrackPropagationRequestDTO request) {
        UUID pavementId = request.getPavementId();
        Pavement pavement = null;
        double areaLength = 10.0, areaWidth = 10.0;
        if (pavementId != null) {
            pavement = pavementRepository.findById(pavementId).orElse(null);
            if (pavement != null) {
                areaLength = pavement.getAreaLength();
                areaWidth = pavement.getAreaWidth();
            }
        }

        double initialCrackWidthMm = request.getInitialCrackWidthMm() != null
                ? request.getInitialCrackWidthMm() : 2.0;
        double stepFrequency = request.getStepFrequency() != null
                ? request.getStepFrequency() : 30.0;
        long totalSteps = request.getTotalSteps() != null
                ? request.getTotalSteps() : 10000L;
        double simulationHours = request.getSimulationHours() != null
                ? request.getSimulationHours() : 8760.0;

        Pavement.PavementStyle style = pavement != null && pavement.getPavementStyle() != null
                ? pavement.getPavementStyle()
                : Pavement.PavementStyle.ICE_CRACK;
        String patternJson = request.getCrackPattern() != null
                ? request.getCrackPattern()
                : (pavement != null ? pavement.getCrackPattern() : null);
        List<double[][]> segments = PavementPatternFactory.generateSegments(
                style, areaLength, areaWidth, patternJson);

        int segCount = segments.size();
        double[] widths = new double[segCount];
        double[] growthRates = new double[segCount];
        Random rng = new Random(pavementId != null ? pavementId.getMostSignificantBits() : 42L);
        for (int i = 0; i < segCount; i++) {
            widths[i] = initialCrackWidthMm * (0.8 + 0.4 * rng.nextDouble());
            double len = Math.hypot(
                    segments.get(i)[1][0] - segments.get(i)[0][0],
                    segments.get(i)[1][1] - segments.get(i)[0][1]);
            growthRates[i] = 0.000001 * (0.5 + len * 0.1) * (0.7 + rng.nextDouble() * 0.6);
        }

        long recordInterval = Math.max(1, totalSteps / 100);
        List<Map<String, Object>> widthHistory = new ArrayList<>();
        int timeSteps = 100;
        double dt = simulationHours / timeSteps;
        double stepsPerHour = stepFrequency * 60.0;

        for (int t = 0; t <= timeSteps; t++) {
            double hour = t * dt;
            double avgWidth = 0.0;
            double maxWidth = 0.0;
            for (int i = 0; i < segCount; i++) {
                long stepsAtTime = (long) (stepsPerHour * hour);
                double fatigueFactor = 1.0 + 0.0005 * Math.sqrt(Math.max(0, stepsAtTime));
                double growth = growthRates[i] * fatigueFactor * hour * 1000.0;
                widths[i] = Math.max(initialCrackWidthMm * 0.5, initialCrackWidthMm + growth);
                widths[i] += rng.nextGaussian() * 0.01;
                avgWidth += widths[i];
                if (widths[i] > maxWidth) maxWidth = widths[i];
            }
            avgWidth /= Math.max(1, segCount);

            if (t % Math.max(1, timeSteps / 20) == 0 || t == timeSteps) {
                Map<String, Object> point = new HashMap<>();
                point.put("hour", hour);
                point.put("avgWidthMm", avgWidth);
                point.put("maxWidthMm", maxWidth);
                widthHistory.add(point);
            }
        }

        double finalAvgWidth = 0.0;
        double finalMaxWidth = 0.0;
        for (double w : widths) {
            finalAvgWidth += w;
            if (w > finalMaxWidth) finalMaxWidth = w;
        }
        finalAvgWidth /= Math.max(1, segCount);

        List<Map<String, Object>> segmentData = new ArrayList<>();
        for (int i = 0; i < segCount; i++) {
            double len = Math.hypot(
                    segments.get(i)[1][0] - segments.get(i)[0][0],
                    segments.get(i)[1][1] - segments.get(i)[0][1]);
            Map<String, Object> seg = new HashMap<>();
            seg.put("x1", segments.get(i)[0][0]);
            seg.put("y1", segments.get(i)[0][1]);
            seg.put("x2", segments.get(i)[1][0]);
            seg.put("y2", segments.get(i)[1][1]);
            seg.put("initialWidth", initialCrackWidthMm * (0.8 + 0.4 * (i % 5) / 4.0));
            seg.put("finalWidth", widths[i]);
            seg.put("growth", widths[i] - initialCrackWidthMm);
            seg.put("length", len);
            segmentData.add(seg);
        }

        double widthRatio = finalAvgWidth / Math.max(0.0001, initialCrackWidthMm);
        double damageIndex = Math.min(1.0,
                0.4 * Math.max(0, (widthRatio - 1.0))
                        + 0.3 * Math.min(1.0, finalMaxWidth / 15.0)
                        + 0.3 * Math.min(1.0, totalSteps / 100000.0));

        CrackPropagation entity = new CrackPropagation();
        if (pavement != null) entity.setPavement(pavement);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setInitialCrackWidthMm(initialCrackWidthMm);
        entity.setStepFrequency(stepFrequency);
        entity.setTotalSteps(totalSteps);
        entity.setSimulationHours(simulationHours);
        entity.setFinalCrackWidthMm(finalAvgWidth);
        entity.setDamageIndex(damageIndex);
        try {
            entity.setWidthHistory(objectMapper.writeValueAsString(widthHistory));
            entity.setSegmentPropagation(objectMapper.writeValueAsString(segmentData));
        } catch (Exception e) {
            entity.setWidthHistory("[]");
            entity.setSegmentPropagation("[]");
        }
        CrackPropagation saved = repository.save(entity);

        CrackPropagationResultDTO dto = new CrackPropagationResultDTO();
        dto.setId(saved.getId());
        if (pavement != null) dto.setPavementId(pavement.getId());
        dto.setInitialCrackWidthMm(saved.getInitialCrackWidthMm());
        dto.setStepFrequency(saved.getStepFrequency());
        dto.setTotalSteps(saved.getTotalSteps());
        dto.setSimulationHours(saved.getSimulationHours());
        dto.setFinalCrackWidthMm(saved.getFinalCrackWidthMm());
        dto.setWidthHistory(saved.getWidthHistory());
        dto.setSegmentPropagation(saved.getSegmentPropagation());
        dto.setDamageIndex(saved.getDamageIndex());
        return dto;
    }

    public List<CrackPropagationResultDTO> getHistory(UUID pavementId) {
        return repository.findByPavementIdOrderByCreatedAtDesc(pavementId)
                .stream().map(this::toDTO).toList();
    }

    private CrackPropagationResultDTO toDTO(CrackPropagation e) {
        CrackPropagationResultDTO dto = new CrackPropagationResultDTO();
        dto.setId(e.getId());
        if (e.getPavement() != null) dto.setPavementId(e.getPavement().getId());
        dto.setInitialCrackWidthMm(e.getInitialCrackWidthMm());
        dto.setStepFrequency(e.getStepFrequency());
        dto.setTotalSteps(e.getTotalSteps());
        dto.setSimulationHours(e.getSimulationHours());
        dto.setFinalCrackWidthMm(e.getFinalCrackWidthMm());
        dto.setWidthHistory(e.getWidthHistory());
        dto.setSegmentPropagation(e.getSegmentPropagation());
        dto.setDamageIndex(e.getDamageIndex());
        return dto;
    }
}
