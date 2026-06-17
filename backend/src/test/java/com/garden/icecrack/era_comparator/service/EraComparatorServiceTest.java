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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EraComparatorServiceTest {

    @Mock
    private EraComparisonRepository comparisonRepository;

    @Mock
    private PavementRepository pavementRepository;

    @Mock
    private AestheticQuantificationService aestheticService;

    @Mock
    private DrainageSimulationService drainageService;

    @Mock
    private ObjectMapper objectMapper;

    private Pavement buildPavement(UUID id, String name, Pavement.Era era) {
        Pavement p = new Pavement();
        p.setId(id);
        p.setName(name);
        p.setEra(era);
        p.setAreaLength(10.0);
        p.setAreaWidth(10.0);
        return p;
    }

    private AestheticResultDTO buildAestheticResult(double visualComplexity) {
        AestheticResultDTO dto = new AestheticResultDTO();
        dto.setFractalDimension(1.5);
        dto.setVisualComplexity(visualComplexity);
        dto.setPatternSymmetry(0.8);
        return dto;
    }

    private SimulationResultDTO buildSimulationResult(double recessionTimeSec, double infiltrationRate) {
        SimulationResultDTO dto = new SimulationResultDTO();
        dto.setRecessionTimeSec(recessionTimeSec);
        dto.setInfiltrationRate(infiltrationRate);
        dto.setPeakWaterDepth(0.015);
        dto.setDrainageRate(0.005);
        return dto;
    }

    @Test
    void compareEras_ancientAndModern_avgFieldsPopulatedAndSummaryContainsCrossEra() throws Exception {
        EraComparatorService service = new EraComparatorService(comparisonRepository, pavementRepository, aestheticService, drainageService, objectMapper);

        UUID ancientId = UUID.randomUUID();
        UUID modernId = UUID.randomUUID();
        Pavement ancient = buildPavement(ancientId, "ancient1", Pavement.Era.ANCIENT);
        Pavement modern = buildPavement(modernId, "modern1", Pavement.Era.MODERN);

        when(pavementRepository.findAllById(anyList())).thenReturn(List.of(ancient, modern));
        when(aestheticService.analyzePavement(ancientId)).thenReturn(buildAestheticResult(0.9));
        when(aestheticService.analyzePavement(modernId)).thenReturn(buildAestheticResult(0.5));
        when(drainageService.runSimulation(any())).thenAnswer(inv -> {
            SimulationRequestDTO req = inv.getArgument(0);
            if (req.getPavementId().equals(ancientId)) {
                return buildSimulationResult(200.0, 0.002);
            }
            return buildSimulationResult(80.0, 0.01);
        });
        when(objectMapper.writeValueAsString(any())).thenReturn("[]");
        when(comparisonRepository.save(any())).thenAnswer(inv -> {
            EraComparison e = inv.getArgument(0);
            e.setId(1L);
            return e;
        });

        EraComparisonRequestDTO request = new EraComparisonRequestDTO();
        request.setPavementIds(List.of(ancientId, modernId));

        EraComparisonResultDTO result = service.compareEras(request);

        assertNotNull(result.getAncientAvg());
        assertNotNull(result.getModernAvg());
        assertTrue(result.getAncientAvg().containsKey("avgRecession"));
        assertTrue(result.getAncientAvg().containsKey("avgComplexity"));
        assertTrue(result.getAncientAvg().containsKey("avgInfiltration"));
        assertTrue(result.getModernAvg().containsKey("avgRecession"));
        assertTrue(result.getModernAvg().containsKey("avgComplexity"));
        assertTrue(result.getModernAvg().containsKey("avgInfiltration"));
        assertTrue(result.getSummary().contains("跨时代对比"));

        double ancientInfiltration = ((Number) result.getAncientAvg().get("avgInfiltration")).doubleValue();
        double modernInfiltration = ((Number) result.getModernAvg().get("avgInfiltration")).doubleValue();
        assertTrue(modernInfiltration > ancientInfiltration);
    }

    @Test
    void compareEras_allAncient_modernAvgAllZero() throws Exception {
        EraComparatorService service = new EraComparatorService(comparisonRepository, pavementRepository, aestheticService, drainageService, objectMapper);

        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        Pavement p1 = buildPavement(id1, "ancient1", Pavement.Era.ANCIENT);
        Pavement p2 = buildPavement(id2, "ancient2", Pavement.Era.ANCIENT);

        when(pavementRepository.findAllById(anyList())).thenReturn(List.of(p1, p2));
        when(aestheticService.analyzePavement(id1)).thenReturn(buildAestheticResult(0.8));
        when(aestheticService.analyzePavement(id2)).thenReturn(buildAestheticResult(0.7));
        when(drainageService.runSimulation(any())).thenReturn(buildSimulationResult(150.0, 0.003));
        when(objectMapper.writeValueAsString(any())).thenReturn("[]");
        when(comparisonRepository.save(any())).thenAnswer(inv -> {
            EraComparison e = inv.getArgument(0);
            e.setId(2L);
            return e;
        });

        EraComparisonRequestDTO request = new EraComparisonRequestDTO();
        request.setPavementIds(List.of(id1, id2));

        EraComparisonResultDTO result = service.compareEras(request);

        assertEquals(0.0, ((Number) result.getModernAvg().get("avgRecession")).doubleValue());
        assertEquals(0.0, ((Number) result.getModernAvg().get("avgComplexity")).doubleValue());
        assertEquals(0.0, ((Number) result.getModernAvg().get("avgInfiltration")).doubleValue());
        assertTrue(result.getSummary().contains("跨时代对比"));
    }

    @Test
    void compareEras_emptyPavementIds_usesFindAllLimit4() throws Exception {
        EraComparatorService service = new EraComparatorService(comparisonRepository, pavementRepository, aestheticService, drainageService, objectMapper);

        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();
        UUID id4 = UUID.randomUUID();
        Pavement p1 = buildPavement(id1, "p1", Pavement.Era.ANCIENT);
        Pavement p2 = buildPavement(id2, "p2", Pavement.Era.MODERN);
        Pavement p3 = buildPavement(id3, "p3", Pavement.Era.ANCIENT);
        Pavement p4 = buildPavement(id4, "p4", Pavement.Era.MODERN);

        when(pavementRepository.findAll()).thenReturn(List.of(p1, p2, p3, p4));
        when(pavementRepository.findAllById(anyList())).thenReturn(List.of(p1, p2, p3, p4));
        when(aestheticService.analyzePavement(any(UUID.class))).thenReturn(buildAestheticResult(0.6));
        when(drainageService.runSimulation(any())).thenReturn(buildSimulationResult(100.0, 0.005));
        when(objectMapper.writeValueAsString(any())).thenReturn("[]");
        when(comparisonRepository.save(any())).thenAnswer(inv -> {
            EraComparison e = inv.getArgument(0);
            e.setId(3L);
            return e;
        });

        EraComparisonRequestDTO request = new EraComparisonRequestDTO();
        request.setPavementIds(null);

        EraComparisonResultDTO result = service.compareEras(request);

        assertNotNull(result);
        verify(pavementRepository).findAll();
    }

    @Test
    void compareEras_drainageServiceThrows_propagatesException() {
        EraComparatorService service = new EraComparatorService(comparisonRepository, pavementRepository, aestheticService, drainageService, objectMapper);

        UUID id1 = UUID.randomUUID();
        Pavement p1 = buildPavement(id1, "p1", Pavement.Era.ANCIENT);

        when(pavementRepository.findAllById(anyList())).thenReturn(List.of(p1));
        when(aestheticService.analyzePavement(id1)).thenReturn(buildAestheticResult(0.6));
        when(drainageService.runSimulation(any())).thenThrow(new RuntimeException("Drainage simulation failed"));

        EraComparisonRequestDTO request = new EraComparisonRequestDTO();
        request.setPavementIds(List.of(id1));

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> service.compareEras(request));
        assertEquals("Drainage simulation failed", thrown.getMessage());
    }

    @Test
    void getHistory_returnsCorrectMapping() throws Exception {
        EraComparatorService service = new EraComparatorService(comparisonRepository, pavementRepository, aestheticService, drainageService, objectMapper);

        EraComparison entity = new EraComparison();
        entity.setId(10L);
        entity.setSummary("跨时代对比分析：summary");
        entity.setAncientResults("{\"avgRecession\":200.0,\"avgComplexity\":0.9,\"avgInfiltration\":0.002}");
        entity.setModernResults("{\"avgRecession\":80.0,\"avgComplexity\":0.5,\"avgInfiltration\":0.01}");

        when(comparisonRepository.findByOrderByCreatedAtDesc()).thenReturn(List.of(entity));
        when(objectMapper.readValue(entity.getAncientResults(), java.util.Map.class))
                .thenReturn(java.util.Map.of("avgRecession", 200.0, "avgComplexity", 0.9, "avgInfiltration", 0.002));
        when(objectMapper.readValue(entity.getModernResults(), java.util.Map.class))
                .thenReturn(java.util.Map.of("avgRecession", 80.0, "avgComplexity", 0.5, "avgInfiltration", 0.01));

        List<EraComparisonResultDTO> history = service.getHistory();

        assertEquals(1, history.size());
        EraComparisonResultDTO dto = history.get(0);
        assertEquals(10L, dto.getId());
        assertEquals("跨时代对比分析：summary", dto.getSummary());
        assertNotNull(dto.getAncientAvg());
        assertNotNull(dto.getModernAvg());
    }
}
