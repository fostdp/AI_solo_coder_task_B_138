package com.garden.icecrack.common.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.garden.icecrack.aesthetics_analyzer.dto.AestheticResultDTO;
import com.garden.icecrack.aesthetics_analyzer.service.AestheticQuantificationService;
import com.garden.icecrack.common.dto.StyleComparisonRequestDTO;
import com.garden.icecrack.common.dto.StyleComparisonResultDTO;
import com.garden.icecrack.common.entity.Pavement;
import com.garden.icecrack.common.entity.StyleComparison;
import com.garden.icecrack.common.repository.PavementRepository;
import com.garden.icecrack.common.repository.StyleComparisonRepository;
import com.garden.icecrack.drainage_simulator.dto.SimulationResultDTO;
import com.garden.icecrack.drainage_simulator.service.DrainageSimulationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("StyleComparisonService — 跨时代对比渗透系数验证")
@ExtendWith(MockitoExtension.class)
class EraComparisonServiceTest {

    @Mock private PavementRepository pavementRepository;
    @Mock private StyleComparisonRepository comparisonRepository;
    @Mock private AestheticQuantificationService aestheticService;
    @Mock private DrainageSimulationService drainageService;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks private StyleComparisonService service;

    private UUID ancientIceId;
    private UUID ancientHerId;
    private UUID modernBrick1Id;
    private UUID modernBrick2Id;
    private Pavement ancientIce;
    private Pavement ancientHer;
    private Pavement modernBrick1;
    private Pavement modernBrick2;

    @BeforeEach
    void setUp() throws Exception {
        ancientIceId = UUID.fromString("a1a1a1a1-b2b2-c3c3-d4d4-e5e5e5e5e5e5");
        ancientHerId = UUID.fromString("b2b2b2b2-c3c3-d4d4-e5e5-f6f6f6f6f6f6");
        modernBrick1Id = UUID.fromString("c3c3c3c3-d4d4-e5e5-f6f6-a1a1a1a1a1a1");
        modernBrick2Id = UUID.fromString("d4d4d4d4-e5e5-f6f6-a1a1-b2b2b2b2b2b2");

        ancientIce = build(ancientIceId, "留园冰裂纹",
                Pavement.PavementStyle.ICE_CRACK, Pavement.Era.ANCIENT, 0.001);
        ancientHer = build(ancientHerId, "艺圃人字纹",
                Pavement.PavementStyle.HERRINGBONE, Pavement.Era.ANCIENT, 0.0012);
        modernBrick1 = build(modernBrick1Id, "博物馆透水砖A",
                Pavement.PavementStyle.PERMEABLE_BRICK, Pavement.Era.MODERN, 0.005);
        modernBrick2 = build(modernBrick2Id, "社区透水砖B",
                Pavement.PavementStyle.PERMEABLE_BRICK, Pavement.Era.MODERN, 0.006);

        when(objectMapper.writeValueAsString(any())).thenReturn("[]");
    }

    private Pavement build(UUID id, String name, Pavement.PavementStyle style,
                           Pavement.Era era, double permeability) {
        Pavement p = new Pavement();
        p.setId(id); p.setName(name);
        p.setPavementStyle(style); p.setEra(era);
        p.setAreaLength(10.0); p.setAreaWidth(8.0);
        p.setBasePermeability(permeability);
        return p;
    }

    private AestheticResultDTO makeAes(double dim, double entropy, double complexity,
                                       int count, double avgLen, double density, double symmetry) {
        AestheticResultDTO dto = new AestheticResultDTO();
        dto.setFractalDimension(dim); dto.setBoxCountingDim(dim);
        dto.setInfoEntropy(entropy); dto.setVisualComplexity(complexity);
        dto.setCrackCount(count); dto.setAvgCrackLength(avgLen);
        dto.setCrackDensity(density); dto.setPatternSymmetry(symmetry);
        return dto;
    }

    private SimulationResultDTO makeDrain(double recession, double peak, double drain,
                                          double infil, double runoff) {
        SimulationResultDTO dto = new SimulationResultDTO();
        dto.setInitialWaterDepth(0.01);
        dto.setRecessionTimeSec(recession);
        dto.setPeakWaterDepth(peak);
        dto.setDrainageRate(drain);
        dto.setInfiltrationRate(infil);
        dto.setSurfaceRunoffRate(runoff);
        return dto;
    }

    @Nested
    @DisplayName("正常场景：跨时代渗透系数验证")
    class NormalScenarios {

        @Test
        @DisplayName("[正常] 现代组平均渗透系数 >= 5x 古代组")
        void eraComparison_modernPermeabilityFiveTimesAncient() {
            List<Pavement> all = List.of(ancientIce, ancientHer, modernBrick1, modernBrick2);
            when(pavementRepository.findAllById(any())).thenReturn(all);

            when(aestheticService.analyzePavement(ancientIceId))
                    .thenReturn(makeAes(1.7, 4.2, 0.85, 40, 1.8, 0.12, 0.72));
            when(aestheticService.analyzePavement(ancientHerId))
                    .thenReturn(makeAes(1.55, 3.8, 0.70, 200, 0.5, 0.30, 0.90));
            when(aestheticService.analyzePavement(modernBrick1Id))
                    .thenReturn(makeAes(1.35, 3.0, 0.45, 500, 0.3, 0.45, 0.95));
            when(aestheticService.analyzePavement(modernBrick2Id))
                    .thenReturn(makeAes(1.40, 3.2, 0.50, 450, 0.32, 0.42, 0.94));

            when(drainageService.runSimulation(any()))
                    .thenReturn(makeDrain(1500.0, 0.014, 0.00015, 0.0015, 0.00010))
                    .thenReturn(makeDrain(1200.0, 0.015, 0.00018, 0.0018, 0.00012))
                    .thenReturn(makeDrain(250.0,  0.008, 0.00080, 0.0085, 0.00003))
                    .thenReturn(makeDrain(220.0,  0.007, 0.00090, 0.0095, 0.00002));

            when(comparisonRepository.save(any())).thenAnswer(inv -> {
                StyleComparison e = inv.getArgument(0);
                e.setId(UUID.randomUUID());
                return e;
            });

            StyleComparisonRequestDTO req = new StyleComparisonRequestDTO();
            req.setPavementIds(all.stream().map(Pavement::getId).toList());
            req.setComparisonType("ERA");
            req.setRainfallMm(50.0);
            req.setCrackWidthMm(3.0);

            StyleComparisonResultDTO res = service.comparePavements(req);

            assertNotNull(res);
            assertEquals("ERA", res.getComparisonType());
            assertNotNull(res.getDrainageResults());
            assertEquals(4, res.getDrainageResults().size());

            double ancientInfil = 0, modernInfil = 0;
            int ancientCount = 0, modernCount = 0;
            for (var m : res.getDrainageResults()) {
                String era = (String) m.get("era");
                double v = ((Number) m.get("infiltrationRate")).doubleValue();
                if ("ANCIENT".equals(era)) { ancientInfil += v; ancientCount++; }
                else { modernInfil += v; modernCount++; }
            }
            double ancientAvg = ancientInfil / ancientCount;
            double modernAvg = modernInfil / modernCount;
            double ratio = modernAvg / ancientAvg;

            assertTrue(ratio >= 5.0,
                    String.format("现代渗透系数均值应为古代5倍以上: 现代=%.6f, 古代=%.6f, 倍数=%.2f",
                            modernAvg, ancientAvg, ratio));
        }

        @Test
        @DisplayName("[正常] 现代组退水时间显著小于古代组")
        void eraComparison_modernRecessionShorter() {
            List<Pavement> all = List.of(ancientIce, modernBrick1);
            when(pavementRepository.findAllById(any())).thenReturn(all);

            when(aestheticService.analyzePavement(ancientIceId))
                    .thenReturn(makeAes(1.65, 4.0, 0.8, 30, 1.5, 0.1, 0.7));
            when(aestheticService.analyzePavement(modernBrick1Id))
                    .thenReturn(makeAes(1.35, 3.0, 0.45, 400, 0.3, 0.4, 0.95));

            when(drainageService.runSimulation(any()))
                    .thenReturn(makeDrain(1400.0, 0.015, 0.0002, 0.0015, 0.0001))
                    .thenReturn(makeDrain(260.0, 0.008, 0.0009, 0.0085, 0.00002));

            when(comparisonRepository.save(any())).thenAnswer(inv -> {
                StyleComparison e = inv.getArgument(0);
                e.setId(UUID.randomUUID());
                return e;
            });

            StyleComparisonRequestDTO req = new StyleComparisonRequestDTO();
            req.setPavementIds(List.of(ancientIceId, modernBrick1Id));
            req.setComparisonType("ERA");
            StyleComparisonResultDTO res = service.comparePavements(req);

            assertTrue(res.getSummary().contains("跨时代对比分析"));
            double ancientRec = ((Number) res.getDrainageResults().get(0).get("recessionTimeSec")).doubleValue();
            double modernRec = ((Number) res.getDrainageResults().get(1).get("recessionTimeSec")).doubleValue();
            assertTrue(modernRec < ancientRec * 0.8,
                    String.format("现代退水时间应远小于古代: 古代=%.0f, 现代=%.0f", ancientRec, modernRec));
        }

        @Test
        @DisplayName("[正常] STYLE 对比模式：所有铺地渗透系数均被列出")
        void styleComparison_listAllPermeabilities() {
            List<Pavement> all = List.of(ancientIce, ancientHer, modernBrick1);
            when(pavementRepository.findAllById(any())).thenReturn(all);

            for (var p : all) {
                when(aestheticService.analyzePavement(p.getId()))
                        .thenReturn(makeAes(1.5, 3.5, 0.65, 50, 1.0, 0.2, 0.8));
                when(drainageService.runSimulation(any())).thenReturn(
                        makeDrain(800.0, 0.012, 0.0003, p.getBasePermeability() * 1.5, 0.00005));
            }
            when(comparisonRepository.save(any())).thenAnswer(inv -> {
                StyleComparison e = inv.getArgument(0);
                e.setId(UUID.randomUUID());
                return e;
            });

            StyleComparisonRequestDTO req = new StyleComparisonRequestDTO();
            req.setPavementIds(List.of(ancientIceId, ancientHerId, modernBrick1Id));
            req.setComparisonType("STYLE");

            StyleComparisonResultDTO res = service.comparePavements(req);

            assertEquals(3, res.getDrainageResults().size());
            assertTrue(res.getSummary().contains("样式对比分析"));
            for (var m : res.getDrainageResults()) {
                assertTrue(((Number) m.get("infiltrationRate")).doubleValue() > 0,
                        "每个铺地渗透系数都大于0");
            }
        }
    }

    @Nested
    @DisplayName("边界场景")
    class BoundaryScenarios {

        @Test
        @DisplayName("[边界] 空 pavementIds 自动取前4个")
        void boundary_emptyIds_selectsFirstFour() {
            List<Pavement> topFour = List.of(ancientIce, ancientHer, modernBrick1, modernBrick2);
            when(pavementRepository.findAll()).thenReturn(topFour);
            when(pavementRepository.findAllById(any())).thenReturn(topFour);
            for (var p : topFour) {
                when(aestheticService.analyzePavement(p.getId()))
                        .thenReturn(makeAes(1.5, 3.5, 0.6, 50, 1.0, 0.2, 0.8));
                when(drainageService.runSimulation(any())).thenReturn(
                        makeDrain(500, 0.012, 0.0003, 0.002, 0.00005));
            }
            when(comparisonRepository.save(any())).thenAnswer(inv -> {
                StyleComparison e = inv.getArgument(0);
                e.setId(UUID.randomUUID());
                return e;
            });

            StyleComparisonRequestDTO req = new StyleComparisonRequestDTO();
            StyleComparisonResultDTO res = service.comparePavements(req);

            verify(pavementRepository, times(1)).findAll();
            assertEquals(4, res.getAestheticResults().size());
        }

        @Test
        @DisplayName("[边界] 只有一组 ERA 样本（全古代）时对比仍成功")
        void boundary_onlyAncientEra_stillReturns() {
            when(pavementRepository.findAllById(any())).thenReturn(List.of(ancientIce, ancientHer));
            when(aestheticService.analyzePavement(ancientIceId))
                    .thenReturn(makeAes(1.7, 4.2, 0.85, 40, 1.8, 0.12, 0.7));
            when(aestheticService.analyzePavement(ancientHerId))
                    .thenReturn(makeAes(1.55, 3.8, 0.7, 200, 0.5, 0.3, 0.9));
            when(drainageService.runSimulation(any()))
                    .thenReturn(makeDrain(1200, 0.015, 0.0002, 0.0015, 0.0001))
                    .thenReturn(makeDrain(1300, 0.016, 0.00018, 0.0014, 0.00011));
            when(comparisonRepository.save(any())).thenAnswer(inv -> {
                StyleComparison e = inv.getArgument(0);
                e.setId(UUID.randomUUID());
                return e;
            });

            StyleComparisonRequestDTO req = new StyleComparisonRequestDTO();
            req.setPavementIds(List.of(ancientIceId, ancientHerId));
            req.setComparisonType("ERA");

            StyleComparisonResultDTO res = service.comparePavements(req);
            assertNotNull(res);
            assertEquals(2, res.getDrainageResults().size());
            assertTrue(res.getSummary().startsWith("跨时代对比分析"));
        }
    }

    @Nested
    @DisplayName("异常场景")
    class ExceptionScenarios {

        @Test
        @DisplayName("[异常] aestheticService 抛异常时，异常被传播")
        void exception_aestheticServiceFails_propagatesException() {
            when(pavementRepository.findAllById(any())).thenReturn(List.of(ancientIce));
            when(aestheticService.analyzePavement(ancientIceId))
                    .thenThrow(new RuntimeException("美学数据库连接失败"));

            StyleComparisonRequestDTO req = new StyleComparisonRequestDTO();
            req.setPavementIds(List.of(ancientIceId));

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> service.comparePavements(req));
            assertEquals("美学数据库连接失败", ex.getMessage());
        }

        @Test
        @DisplayName("[异常] 空铺地集合时，仍优雅返回（美学+排水为空列表）")
        void exception_emptyPavementList_returnsResult() {
            when(pavementRepository.findAllById(any())).thenReturn(new ArrayList<>());
            when(comparisonRepository.save(any())).thenAnswer(inv -> {
                StyleComparison e = inv.getArgument(0);
                e.setId(UUID.randomUUID());
                return e;
            });

            StyleComparisonRequestDTO req = new StyleComparisonRequestDTO();
            req.setPavementIds(List.of(UUID.randomUUID(), UUID.randomUUID()));
            StyleComparisonResultDTO res = service.comparePavements(req);

            assertNotNull(res);
            assertTrue(res.getAestheticResults().isEmpty());
            assertTrue(res.getDrainageResults().isEmpty());
        }

        @Test
        @DisplayName("[异常] ObjectMapper 抛异常时，降级使用空字符串")
        void exception_jsonMapperFails_savesEmpty() throws Exception {
            when(pavementRepository.findAllById(any())).thenReturn(List.of(ancientIce));
            when(aestheticService.analyzePavement(ancientIceId))
                    .thenReturn(makeAes(1.5, 3.5, 0.6, 50, 1.0, 0.2, 0.8));
            when(drainageService.runSimulation(any()))
                    .thenReturn(makeDrain(800, 0.012, 0.0003, 0.0015, 0.0001));
            when(objectMapper.writeValueAsString(any())).thenThrow(new RuntimeException("JSON错误"));

            ArgumentCaptor<StyleComparison> captor = ArgumentCaptor.forClass(StyleComparison.class);
            when(comparisonRepository.save(captor.capture())).thenAnswer(inv -> {
                StyleComparison e = inv.getArgument(0);
                e.setId(UUID.randomUUID());
                return e;
            });

            StyleComparisonRequestDTO req = new StyleComparisonRequestDTO();
            req.setPavementIds(List.of(ancientIceId));
            service.comparePavements(req);

            StyleComparison saved = captor.getValue();
            assertEquals("[]", saved.getPavementIds(), "降级成空数组字符串");
        }
    }

    @Test
    @DisplayName("[正常] getHistory 正确映射 DTO")
    void getHistory_mapsCorrectly() throws Exception {
        StyleComparison e = new StyleComparison();
        e.setId(UUID.randomUUID());
        e.setComparisonType("ERA");
        e.setSummary("测试摘要");
        e.setAestheticResults("[]");
        e.setDrainageResults("[]");
        when(objectMapper.readValue("[]", List.class)).thenReturn(new ArrayList<>());
        when(comparisonRepository.findByOrderByCreatedAtDesc()).thenReturn(List.of(e));

        List<StyleComparisonResultDTO> list = service.getHistory();
        assertEquals(1, list.size());
        assertEquals("ERA", list.get(0).getComparisonType());
    }
}
