package com.garden.icecrack.pattern_comparator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.garden.icecrack.aesthetics_analyzer.dto.AestheticResultDTO;
import com.garden.icecrack.aesthetics_analyzer.service.AestheticQuantificationService;
import com.garden.icecrack.common.entity.Pavement;
import com.garden.icecrack.common.repository.PavementRepository;
import com.garden.icecrack.drainage_simulator.dto.SimulationResultDTO;
import com.garden.icecrack.drainage_simulator.service.DrainageSimulationService;
import com.garden.icecrack.pattern_comparator.dto.PatternComparisonRequestDTO;
import com.garden.icecrack.pattern_comparator.dto.PatternComparisonResultDTO;
import com.garden.icecrack.pattern_comparator.entity.PatternComparison;
import com.garden.icecrack.pattern_comparator.repository.PatternComparisonRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
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
class PatternComparatorServiceTest {

    @Mock
    private PatternComparisonRepository comparisonRepository;

    @Mock
    private PavementRepository pavementRepository;

    @Mock
    private AestheticQuantificationService aestheticService;

    @Mock
    private DrainageSimulationService drainageService;

    @Mock
    private ObjectMapper objectMapper;

    private PatternComparatorService service;

    private Pavement buildPavement(UUID id, String name, Pavement.PavementStyle style, Pavement.Era era) {
        Pavement p = new Pavement();
        p.setId(id);
        p.setName(name);
        p.setPavementStyle(style);
        p.setEra(era);
        p.setAreaLength(10.0);
        p.setAreaWidth(10.0);
        return p;
    }

    private AestheticResultDTO buildAestheticResult() {
        AestheticResultDTO dto = new AestheticResultDTO();
        dto.setFractalDimension(1.5);
        dto.setBoxCountingDim(1.4);
        dto.setInfoEntropy(2.0);
        dto.setVisualComplexity(0.75);
        dto.setCrackCount(20);
        dto.setAvgCrackLength(3.5);
        dto.setCrackDensity(0.1);
        dto.setPatternSymmetry(0.8);
        return dto;
    }

    private SimulationResultDTO buildSimulationResult() {
        SimulationResultDTO dto = new SimulationResultDTO();
        dto.setInitialWaterDepth(0.01);
        dto.setRecessionTimeSec(120.0);
        dto.setPeakWaterDepth(0.015);
        dto.setDrainageRate(0.005);
        dto.setInfiltrationRate(0.001);
        dto.setSurfaceRunoffRate(0.003);
        dto.setAlertTriggered(false);
        dto.setAlertMessage(null);
        return dto;
    }

    @Test
    void compareStyles_withTwoPavementIds_returnsResultsOfSize2() throws Exception {
        service = new PatternComparatorService(comparisonRepository, pavementRepository, aestheticService, drainageService, objectMapper);

        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        Pavement p1 = buildPavement(id1, "p1", Pavement.PavementStyle.ICE_CRACK, Pavement.Era.ANCIENT);
        Pavement p2 = buildPavement(id2, "p2", Pavement.PavementStyle.HERRINGBONE, Pavement.Era.MODERN);

        when(pavementRepository.findAllById(anyList())).thenReturn(List.of(p1, p2));
        when(aestheticService.analyzePavement(id1)).thenReturn(buildAestheticResult());
        when(aestheticService.analyzePavement(id2)).thenReturn(buildAestheticResult());
        when(drainageService.runSimulation(any())).thenReturn(buildSimulationResult());
        when(objectMapper.writeValueAsString(any())).thenReturn("[]");
        when(comparisonRepository.save(any())).thenAnswer(inv -> {
            PatternComparison e = inv.getArgument(0);
            e.setId(1L);
            return e;
        });

        PatternComparisonRequestDTO request = new PatternComparisonRequestDTO();
        request.setPavementIds(List.of(id1, id2));

        PatternComparisonResultDTO result = service.compareStyles(request);

        assertNotNull(result.getAestheticResults());
        assertNotNull(result.getDrainageResults());
        assertEquals(2, result.getAestheticResults().size());
        assertEquals(2, result.getDrainageResults().size());
        assertTrue(result.getSummary().contains("样式对比分析"));
    }

    @Test
    void compareStyles_withEraComparisonType_summaryContainsCrossEraText() throws Exception {
        service = new PatternComparatorService(comparisonRepository, pavementRepository, aestheticService, drainageService, objectMapper);

        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        Pavement p1 = buildPavement(id1, "ancient1", Pavement.PavementStyle.ICE_CRACK, Pavement.Era.ANCIENT);
        Pavement p2 = buildPavement(id2, "modern1", Pavement.PavementStyle.PERMEABLE_BRICK, Pavement.Era.MODERN);

        when(pavementRepository.findAllById(anyList())).thenReturn(List.of(p1, p2));
        when(aestheticService.analyzePavement(id1)).thenReturn(buildAestheticResult());
        when(aestheticService.analyzePavement(id2)).thenReturn(buildAestheticResult());
        when(drainageService.runSimulation(any())).thenReturn(buildSimulationResult());
        when(objectMapper.writeValueAsString(any())).thenReturn("[]");
        when(comparisonRepository.save(any())).thenAnswer(inv -> {
            PatternComparison e = inv.getArgument(0);
            e.setId(2L);
            return e;
        });

        PatternComparisonRequestDTO request = new PatternComparisonRequestDTO();
        request.setPavementIds(List.of(id1, id2));
        request.setComparisonType("ERA");

        PatternComparisonResultDTO result = service.compareStyles(request);

        assertTrue(result.getSummary().contains("跨时代对比"));
    }

    @Test
    void getHistory_returnsMappedDtos() throws Exception {
        service = new PatternComparatorService(comparisonRepository, pavementRepository, aestheticService, drainageService, objectMapper);

        PatternComparison entity = new PatternComparison();
        entity.setId(10L);
        entity.setComparisonType("STYLE");
        entity.setSummary("样式对比分析：ICE_CRACK：视觉复杂度 0.75");
        entity.setAestheticResults("[{\"pavementStyle\":\"ICE_CRACK\"}]");
        entity.setDrainageResults("[{\"pavementStyle\":\"ICE_CRACK\"}]");

        when(comparisonRepository.findByOrderByCreatedAtDesc()).thenReturn(List.of(entity));
        when(objectMapper.readValue(entity.getAestheticResults(), List.class)).thenReturn(List.of(java.util.Map.of("pavementStyle", "ICE_CRACK")));
        when(objectMapper.readValue(entity.getDrainageResults(), List.class)).thenReturn(List.of(java.util.Map.of("pavementStyle", "ICE_CRACK")));

        List<PatternComparisonResultDTO> history = service.getHistory();

        assertEquals(1, history.size());
        PatternComparisonResultDTO dto = history.get(0);
        assertEquals(10L, dto.getId());
        assertEquals("STYLE", dto.getComparisonType());
        assertEquals("样式对比分析：ICE_CRACK：视觉复杂度 0.75", dto.getSummary());
        assertNotNull(dto.getAestheticResults());
        assertNotNull(dto.getDrainageResults());
    }

    @Test
    void compareStyles_emptyPavementIds_usesRepositoryFindAll() throws Exception {
        service = new PatternComparatorService(comparisonRepository, pavementRepository, aestheticService, drainageService, objectMapper);

        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        Pavement p1 = buildPavement(id1, "p1", Pavement.PavementStyle.ICE_CRACK, Pavement.Era.ANCIENT);
        Pavement p2 = buildPavement(id2, "p2", Pavement.PavementStyle.HERRINGBONE, Pavement.Era.MODERN);

        when(pavementRepository.findAll()).thenReturn(List.of(p1, p2));
        when(pavementRepository.findAllById(anyList())).thenReturn(List.of(p1, p2));
        when(aestheticService.analyzePavement(id1)).thenReturn(buildAestheticResult());
        when(aestheticService.analyzePavement(id2)).thenReturn(buildAestheticResult());
        when(drainageService.runSimulation(any())).thenReturn(buildSimulationResult());
        when(objectMapper.writeValueAsString(any())).thenReturn("[]");
        when(comparisonRepository.save(any())).thenAnswer(inv -> {
            PatternComparison e = inv.getArgument(0);
            e.setId(3L);
            return e;
        });

        PatternComparisonRequestDTO request = new PatternComparisonRequestDTO();
        request.setPavementIds(null);

        PatternComparisonResultDTO result = service.compareStyles(request);

        assertNotNull(result);
        verify(pavementRepository).findAll();
    }

    @Test
    void compareStyles_aestheticServiceThrows_propagatesException() {
        service = new PatternComparatorService(comparisonRepository, pavementRepository, aestheticService, drainageService, objectMapper);

        UUID id1 = UUID.randomUUID();
        Pavement p1 = buildPavement(id1, "p1", Pavement.PavementStyle.ICE_CRACK, Pavement.Era.ANCIENT);

        when(pavementRepository.findAllById(anyList())).thenReturn(List.of(p1));
        when(aestheticService.analyzePavement(id1)).thenThrow(new RuntimeException("Aesthetic analysis failed"));

        PatternComparisonRequestDTO request = new PatternComparisonRequestDTO();
        request.setPavementIds(List.of(id1));

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> service.compareStyles(request));
        assertEquals("Aesthetic analysis failed", thrown.getMessage());
    }
}
