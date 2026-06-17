package com.garden.icecrack.user_design.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.garden.icecrack.aesthetics_analyzer.config.AestheticProperties;
import com.garden.icecrack.aesthetics_analyzer.dto.AestheticResultDTO;
import com.garden.icecrack.aesthetics_analyzer.service.AestheticQuantificationService;
import com.garden.icecrack.drainage_simulator.config.DrainageProperties;
import com.garden.icecrack.drainage_simulator.dto.SimulationResultDTO;
import com.garden.icecrack.drainage_simulator.service.DrainageSimulationService;
import com.garden.icecrack.user_design.dto.UserDesignRequestDTO;
import com.garden.icecrack.user_design.dto.UserDesignResultDTO;
import com.garden.icecrack.user_design.entity.UserPavementDesign;
import com.garden.icecrack.user_design.repository.UserPavementDesignRepository;
import com.garden.icecrack.common.entity.Pavement;
import com.garden.icecrack.common.pattern.PavementPatternFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("用户虚拟设计体验 — 设计自由度测试")
@ExtendWith(MockitoExtension.class)
class UserDesignServiceTest {

    @Mock private UserPavementDesignRepository repository;
    @Mock private AestheticQuantificationService aestheticService;
    @Mock private DrainageSimulationService drainageService;
    @Mock private ObjectMapper objectMapper;
    @Mock private AestheticProperties aestheticProps;
    @Mock private DrainageProperties drainageProps;

    @InjectMocks private UserDesignService service;

    private final ObjectMapper realMapper = new ObjectMapper();

    @BeforeEach
    void setupProps() {
        when(aestheticProps.getFractalClipMin()).thenReturn(1.0);
        when(aestheticProps.getFractalClipMax()).thenReturn(2.0);
        when(aestheticProps.getFractalScaleFactor()).thenReturn(1.0);
        when(aestheticProps.getBrownCorrectionPower()).thenReturn(0.5);
        when(aestheticProps.getEntropyBins()).thenReturn(18);
        when(aestheticProps.getFractalWeight()).thenReturn(0.4);
        when(aestheticProps.getEntropyWeight()).thenReturn(0.3);
        when(aestheticProps.getDensityWeight()).thenReturn(0.3);
        when(aestheticProps.getDensityScale()).thenReturn(5.0);
        when(aestheticProps.getSymmetryDistWeight()).thenReturn(0.5);
        when(aestheticProps.getSymmetryHorizWeight()).thenReturn(0.25);
        when(aestheticProps.getSymmetryVertWeight()).thenReturn(0.25);
        when(aestheticProps.getSymmetryChiDivisor()).thenReturn(10.0);
        when(aestheticProps.getLengthWeightMultiplier()).thenReturn(5.0);
        when(aestheticProps.getEntropyBinDegrees()).thenReturn(10.0);
        when(aestheticProps.getDefaultSeed()).thenReturn(42L);
        when(aestheticProps.getDefaultTargetSegments()).thenReturn(30);
        when(aestheticProps.getDefaultIrregularity()).thenReturn(0.5);
        when(aestheticProps.getDefaultSeedPoints()).thenReturn(12);
        when(aestheticProps.getSeedPointsVariation()).thenReturn(6);
        when(aestheticProps.getIrregularityScale()).thenReturn(0.15);
        when(aestheticProps.getCrackBranchHighProb()).thenReturn(0.85);
        when(aestheticProps.getCrackBranchLowProb()).thenReturn(0.4);
        when(aestheticProps.getCrackSplitProb()).thenReturn(0.35);
        when(aestheticProps.getBoundaryBandRatio()).thenReturn(8.0);
        when(aestheticProps.getRawBoxSizes()).thenReturn(List.of(4, 8, 16, 32, 64));
        when(aestheticProps.getUnbiasedBoxSizes()).thenReturn(List.of(4, 8, 16, 32, 64, 128));
    }

    private AestheticResultDTO makeAes(double fractal, double entropy, double complexity,
                                       int count, double avgLen, double density, double symmetry) {
        AestheticResultDTO dto = new AestheticResultDTO();
        dto.setFractalDimension(fractal);
        dto.setBoxCountingDim(fractal);
        dto.setInfoEntropy(entropy);
        dto.setVisualComplexity(complexity);
        dto.setCrackCount(count);
        dto.setAvgCrackLength(avgLen);
        dto.setCrackDensity(density);
        dto.setPatternSymmetry(symmetry);
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
    @DisplayName("正常场景：设计自由度 — 各种图案输入都能被处理")
    class NormalDesignFreedom {

        @Test
        @DisplayName("[正常] 简单十字形设计被正确处理")
        void designFreedom_simpleCrossHandles() {
            String pattern = "[[[2.0,5.0],[8.0,5.0]],[[5.0,1.0],[5.0,9.0]]]";

            when(repository.save(any())).thenAnswer(inv -> {
                UserPavementDesign d = inv.getArgument(0);
                d.setId(UUID.randomUUID());
                return d;
            });
            when(aestheticService.analyzeCustomSegments(anyString(), anyDouble(), anyDouble()))
                    .thenReturn(makeAes(1.45, 1.0, 0.35, 2, 6.0, 0.12, 0.95));
            when(drainageService.runCustomSimulation(any(), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                    .thenReturn(makeDrain(1500, 0.012, 0.00015, 0.0012, 0.00008));

            UserDesignRequestDTO req = new UserDesignRequestDTO();
            req.setUserSessionId("user_001");
            req.setDesignName("十字形简化冰裂纹");
            req.setCrackPattern(pattern);
            req.setAreaLength(10.0);
            req.setAreaWidth(10.0);
            req.setSlopeAngle(2.0);
            req.setBasePermeability(0.001);
            req.setRunAesthetic(true);
            req.setRunDrainage(true);
            req.setRainfallMm(50.0);
            req.setInitialWaterDepthMm(10.0);
            req.setCrackWidthMm(3.0);
            req.setStepFrequency(30.0);

            UserDesignResultDTO res = service.processDesign(req);

            assertNotNull(res.getId());
            assertEquals("user_001", res.getUserSessionId());
            assertEquals("十字形简化冰裂纹", res.getDesignName());
            assertEquals(10.0, res.getAreaLength(), 1e-9);
            assertNotNull(res.getAestheticResult());
            assertNotNull(res.getDrainageResult());
            assertEquals(1.45, ((Number) res.getAestheticResult().get("fractalDimension")).doubleValue(), 1e-9);
            assertEquals(0.95, ((Number) res.getAestheticResult().get("patternSymmetry")).doubleValue(), 1e-9);
            assertEquals(1500.0, ((Number) res.getDrainageResult().get("recessionTimeSec")).doubleValue(), 1e-9);
        }

        @Test
        @DisplayName("[正常] 复杂随机手绘50段设计被正确处理")
        void designFreedom_complexHandDrawnSegments() {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < 50; i++) {
                double x1 = Math.random() * 10;
                double y1 = Math.random() * 10;
                double x2 = Math.random() * 10;
                double y2 = Math.random() * 10;
                sb.append(String.format("[[%.2f,%.2f],[%.2f,%.2f]]", x1, y1, x2, y2));
                if (i < 49) sb.append(",");
            }
            sb.append("]");
            String pattern = sb.toString();

            when(repository.save(any())).thenAnswer(inv -> {
                UserPavementDesign d = inv.getArgument(0);
                d.setId(UUID.randomUUID());
                return d;
            });
            when(aestheticService.analyzeCustomSegments(anyString(), anyDouble(), anyDouble()))
                    .thenReturn(makeAes(1.68, 3.95, 0.78, 50, 2.8, 0.14, 0.52));
            when(drainageService.runCustomSimulation(any(), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                    .thenReturn(makeDrain(800, 0.015, 0.0004, 0.0022, 0.00015));

            UserDesignRequestDTO req = new UserDesignRequestDTO();
            req.setUserSessionId("user_002");
            req.setDesignName("复杂手绘图");
            req.setCrackPattern(pattern);
            req.setAreaLength(10.0);
            req.setAreaWidth(8.0);
            req.setRunAesthetic(true);
            req.setRunDrainage(true);

            UserDesignResultDTO res = service.processDesign(req);
            assertNotNull(res.getAestheticResult());
            assertEquals(50, ((Number) res.getAestheticResult().get("crackCount")).intValue());
            assertTrue(((Number) res.getAestheticResult().get("visualComplexity")).doubleValue() > 0.5,
                    "复杂设计视觉复杂度应>0.5");
        }

        @Test
        @DisplayName("[正常] 只跑美学分析，不跑排水")
        void designFreedom_aestheticOnly_noDrainage() {
            String pattern = "[[[1,1],[9,9]],[[9,1],[1,9]]]";

            when(repository.save(any())).thenAnswer(inv -> {
                UserPavementDesign d = inv.getArgument(0);
                d.setId(UUID.randomUUID());
                return d;
            });
            when(aestheticService.analyzeCustomSegments(anyString(), anyDouble(), anyDouble()))
                    .thenReturn(makeAes(1.5, 1.0, 0.45, 2, 11.3, 0.23, 0.98));

            UserDesignRequestDTO req = new UserDesignRequestDTO();
            req.setCrackPattern(pattern);
            req.setRunAesthetic(true);
            req.setRunDrainage(false);

            UserDesignResultDTO res = service.processDesign(req);
            assertNotNull(res.getAestheticResult());
            assertNull(res.getDrainageResult());
        }

        @Test
        @DisplayName("[正常] 只跑排水，不跑美学")
        void designFreedom_drainageOnly_noAesthetic() {
            String pattern = "[[[1,1],[9,9]],[[9,1],[1,9]]]";

            when(repository.save(any())).thenAnswer(inv -> {
                UserPavementDesign d = inv.getArgument(0);
                d.setId(UUID.randomUUID());
                return d;
            });
            when(drainageService.runCustomSimulation(any(), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                    .thenReturn(makeDrain(700, 0.01, 0.0005, 0.003, 0.0001));

            UserDesignRequestDTO req = new UserDesignRequestDTO();
            req.setCrackPattern(pattern);
            req.setRunAesthetic(false);
            req.setRunDrainage(true);

            UserDesignResultDTO res = service.processDesign(req);
            assertNull(res.getAestheticResult());
            assertNotNull(res.getDrainageResult());
        }

        @Test
        @DisplayName("[正常] 自定义非正方形场地尺寸")
        void designFreedom_nonSquareArea() {
            String pattern = "[[[1,1],[19,1]],[[1,1],[1,11]]]";

            when(repository.save(any())).thenAnswer(inv -> {
                UserPavementDesign d = inv.getArgument(0);
                d.setId(UUID.randomUUID());
                return d;
            });
            when(aestheticService.analyzeCustomSegments(anyString(), eq(20.0), eq(12.0)))
                    .thenReturn(makeAes(1.3, 0.8, 0.3, 2, 9.0, 0.075, 0.85));
            when(drainageService.runCustomSimulation(any(), eq(20.0), eq(12.0), anyDouble(), anyDouble()))
                    .thenReturn(makeDrain(1200, 0.012, 0.0002, 0.0015, 0.0001));

            UserDesignRequestDTO req = new UserDesignRequestDTO();
            req.setCrackPattern(pattern);
            req.setAreaLength(20.0);
            req.setAreaWidth(12.0);
            req.setRunAesthetic(true);
            req.setRunDrainage(true);

            UserDesignResultDTO res = service.processDesign(req);
            assertEquals(20.0, res.getAreaLength(), 1e-9);
            assertEquals(12.0, res.getAreaWidth(), 1e-9);
        }
    }

    @Nested
    @DisplayName("边界场景")
    class BoundaryScenarios {

        @Test
        @DisplayName("[边界] 空设计（空数组）仍可运行排水分析")
        void boundary_emptySegments_stillProcessed() {
            String pattern = "[]";

            when(repository.save(any())).thenAnswer(inv -> {
                UserPavementDesign d = inv.getArgument(0);
                d.setId(UUID.randomUUID());
                return d;
            });
            when(aestheticService.analyzeCustomSegments(anyString(), anyDouble(), anyDouble()))
                    .thenReturn(makeAes(1.0, 0.0, 0.0, 0, 0.0, 0.0, 1.0));
            when(drainageService.runCustomSimulation(any(), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                    .thenReturn(makeDrain(3000, 0.03, 0.00005, 0.0005, 0.00002));

            UserDesignRequestDTO req = new UserDesignRequestDTO();
            req.setCrackPattern(pattern);
            req.setRunAesthetic(true);
            req.setRunDrainage(true);

            UserDesignResultDTO res = service.processDesign(req);
            assertEquals(0, ((Number) res.getAestheticResult().get("crackCount")).intValue());
            assertTrue(((Number) res.getDrainageResult().get("recessionTimeSec")).doubleValue() > 1000,
                    "无裂缝时退水应较慢");
        }

        @Test
        @DisplayName("[边界] 单段设计：单条裂缝")
        void boundary_singleSegment_handled() {
            String pattern = "[[[2,5],[8,5]]]";

            when(repository.save(any())).thenAnswer(inv -> {
                UserPavementDesign d = inv.getArgument(0);
                d.setId(UUID.randomUUID());
                return d;
            });
            when(aestheticService.analyzeCustomSegments(anyString(), anyDouble(), anyDouble()))
                    .thenReturn(makeAes(1.1, 0.0, 0.05, 1, 6.0, 0.06, 0.7));
            when(drainageService.runCustomSimulation(any(), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                    .thenReturn(makeDrain(2500, 0.025, 0.0001, 0.0008, 0.00005));

            UserDesignRequestDTO req = new UserDesignRequestDTO();
            req.setCrackPattern(pattern);
            req.setRunAesthetic(true);
            req.setRunDrainage(true);

            UserDesignResultDTO res = service.processDesign(req);
            assertEquals(1, ((Number) res.getAestheticResult().get("crackCount")).intValue());
        }

        @Test
        @DisplayName("[边界] 完全不分析（runAesthetic=runDrainage=false）仍保存设计")
        void boundary_noAnalysis_designStillSaved() {
            String pattern = "[[[3,3],[7,7]]]";

            when(repository.save(any())).thenAnswer(inv -> {
                UserPavementDesign d = inv.getArgument(0);
                d.setId(UUID.randomUUID());
                return d;
            });

            UserDesignRequestDTO req = new UserDesignRequestDTO();
            req.setUserSessionId("user_idle");
            req.setDesignName("仅保存不分析");
            req.setCrackPattern(pattern);
            req.setRunAesthetic(false);
            req.setRunDrainage(false);

            UserDesignResultDTO res = service.processDesign(req);
            assertNotNull(res.getId());
            assertNull(res.getAestheticResult());
            assertNull(res.getDrainageResult());
            verify(repository, times(1)).save(any());
        }

        @Test
        @DisplayName("[边界] 所有参数为 null，使用默认值")
        void boundary_allNull_defaultsApplied() {
            when(repository.save(any())).thenAnswer(inv -> {
                UserPavementDesign d = inv.getArgument(0);
                d.setId(UUID.randomUUID());
                return d;
            });

            UserDesignRequestDTO req = new UserDesignRequestDTO();
            UserDesignResultDTO res = service.processDesign(req);

            assertEquals("未命名设计", res.getDesignName());
            assertEquals(10.0, res.getAreaLength(), 1e-9);
            assertEquals(10.0, res.getAreaWidth(), 1e-9);
            assertEquals(2.0, res.getSlopeAngle(), 1e-9);
            assertEquals(0.001, res.getBasePermeability(), 1e-9);
            assertNotNull(res.getUserSessionId());
        }

        @Test
        @DisplayName("[边界] 极大场地尺寸仍可运行")
        void boundary_veryLargeArea_stillRuns() {
            String pattern = "[[[10,10],[90,90]],[[90,10],[10,90]]]";

            when(repository.save(any())).thenAnswer(inv -> {
                UserPavementDesign d = inv.getArgument(0);
                d.setId(UUID.randomUUID());
                return d;
            });
            when(aestheticService.analyzeCustomSegments(anyString(), anyDouble(), anyDouble()))
                    .thenReturn(makeAes(1.4, 1.0, 0.4, 2, 113.0, 0.0023, 0.95));
            when(drainageService.runCustomSimulation(any(), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                    .thenReturn(makeDrain(1000, 0.01, 0.0003, 0.002, 0.0001));

            UserDesignRequestDTO req = new UserDesignRequestDTO();
            req.setCrackPattern(pattern);
            req.setAreaLength(100.0);
            req.setAreaWidth(100.0);
            req.setRunAesthetic(true);
            req.setRunDrainage(true);

            UserDesignResultDTO res = service.processDesign(req);
            assertEquals(100.0, res.getAreaLength(), 1e-9);
        }
    }

    @Nested
    @DisplayName("异常场景")
    class ExceptionScenarios {

        @Test
        @DisplayName("[异常] 非法JSON图案：美学分析降级为默认冰裂纹")
        void exception_invalidJsonPattern_fallbackToIceCrack() {
            String pattern = "{not valid json array";

            when(repository.save(any())).thenAnswer(inv -> {
                UserPavementDesign d = inv.getArgument(0);
                d.setId(UUID.randomUUID());
                return d;
            });
            when(aestheticService.analyzeCustomSegments(anyString(), anyDouble(), anyDouble()))
                    .thenReturn(makeAes(1.65, 4.0, 0.8, 30, 1.8, 0.12, 0.7));
            when(drainageService.runCustomSimulation(any(), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                    .thenReturn(makeDrain(900, 0.014, 0.00025, 0.0015, 0.0001));

            UserDesignRequestDTO req = new UserDesignRequestDTO();
            req.setCrackPattern(pattern);
            req.setRunAesthetic(true);
            req.setRunDrainage(true);

            assertDoesNotThrow(() -> service.processDesign(req));
            UserDesignResultDTO res = service.processDesign(req);
            assertNotNull(res);
        }

        @Test
        @DisplayName("[异常] null 图案仍可运行（美学降级，排水正常）")
        void exception_nullPattern_stillSaved() {
            when(repository.save(any())).thenAnswer(inv -> {
                UserPavementDesign d = inv.getArgument(0);
                d.setId(UUID.randomUUID());
                return d;
            });
            when(drainageService.runCustomSimulation(any(), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                    .thenReturn(makeDrain(2000, 0.02, 0.0001, 0.0008, 0.00005));

            UserDesignRequestDTO req = new UserDesignRequestDTO();
            req.setRunAesthetic(true);
            req.setRunDrainage(true);

            UserDesignResultDTO res = service.processDesign(req);
            assertNotNull(res);
            assertNull(res.getAestheticResult(), "crackPattern=null时美学分析被跳过");
            assertNotNull(res.getDrainageResult(), "排水不依赖crackPattern");
        }

        @Test
        @DisplayName("[异常] repository save抛异常，异常被传播")
        void exception_repositoryFails_propagates() {
            when(repository.save(any())).thenThrow(new RuntimeException("数据库连接失败"));

            UserDesignRequestDTO req = new UserDesignRequestDTO();
            req.setRunAesthetic(false);
            req.setRunDrainage(false);

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> service.processDesign(req));
            assertEquals("数据库连接失败", ex.getMessage());
        }

        @Test
        @DisplayName("[异常] 斜坡角度异常大，排水服务仍被调用")
        void exception_extremeSlope_noThrow() {
            String pattern = "[[[1,1],[9,9]]]";

            when(repository.save(any())).thenAnswer(inv -> {
                UserPavementDesign d = inv.getArgument(0);
                d.setId(UUID.randomUUID());
                return d;
            });
            when(aestheticService.analyzeCustomSegments(anyString(), anyDouble(), anyDouble()))
                    .thenReturn(makeAes(1.3, 0.5, 0.2, 1, 11.3, 0.113, 0.5));
            when(drainageService.runCustomSimulation(any(), anyDouble(), anyDouble(), eq(85.0), anyDouble()))
                    .thenReturn(makeDrain(200, 0.005, 0.001, 0.003, 0.0008));

            UserDesignRequestDTO req = new UserDesignRequestDTO();
            req.setCrackPattern(pattern);
            req.setSlopeAngle(85.0);
            req.setRunAesthetic(true);
            req.setRunDrainage(true);

            UserDesignResultDTO res = service.processDesign(req);
            assertEquals(85.0, res.getSlopeAngle(), 1e-9);
        }
    }

    @Nested
    @DisplayName("历史记录接口")
    class HistoryScenarios {

        @Test
        @DisplayName("[正常] getDesignsBySession 返回相同 session 的所有设计")
        void history_getBySession_returnsMatching() {
            String sessionId = "user_session_abc";
            UserPavementDesign d1 = new UserPavementDesign();
            d1.setId(UUID.randomUUID());
            d1.setUserSessionId(sessionId);
            d1.setDesignName("设计一");
            d1.setAreaLength(10.0); d1.setAreaWidth(10.0);
            d1.setSlopeAngle(2.0); d1.setBasePermeability(0.001);

            UserPavementDesign d2 = new UserPavementDesign();
            d2.setId(UUID.randomUUID());
            d2.setUserSessionId(sessionId);
            d2.setDesignName("设计二");
            d2.setAreaLength(15.0); d2.setAreaWidth(8.0);
            d2.setSlopeAngle(3.0); d2.setBasePermeability(0.0015);

            when(repository.findByUserSessionIdOrderByCreatedAtDesc(sessionId))
                    .thenReturn(List.of(d1, d2));

            List<UserDesignResultDTO> list = service.getDesignsBySession(sessionId);
            assertEquals(2, list.size());
            assertEquals("设计一", list.get(0).getDesignName());
            assertEquals(15.0, list.get(1).getAreaLength(), 1e-9);
        }

        @Test
        @DisplayName("[正常] getDesign 找到正确设计")
        void history_getDesign_found() {
            UUID id = UUID.randomUUID();
            UserPavementDesign d = new UserPavementDesign();
            d.setId(id);
            d.setUserSessionId("x");
            d.setDesignName("查找设计");
            d.setAreaLength(12.0); d.setAreaWidth(12.0);
            d.setSlopeAngle(2.5); d.setBasePermeability(0.0012);

            when(repository.findById(id)).thenReturn(Optional.of(d));
            UserDesignResultDTO res = service.getDesign(id);
            assertNotNull(res);
            assertEquals(id, res.getId());
        }

        @Test
        @DisplayName("[边界] getDesign 找不到返回 null")
        void history_getDesign_notFound() {
            UUID missing = UUID.randomUUID();
            when(repository.findById(missing)).thenReturn(Optional.empty());
            assertNull(service.getDesign(missing));
        }
    }
}
