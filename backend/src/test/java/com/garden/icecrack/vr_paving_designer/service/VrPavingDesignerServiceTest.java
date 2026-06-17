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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.*;

@DisplayName("VrPavingDesignerService — 虚拟体验设计测试")
@ExtendWith(MockitoExtension.class)
class VrPavingDesignerServiceTest {

    @Mock private VrPavementDesignRepository repository;
    @Mock private AestheticQuantificationService aestheticService;
    @Mock private DrainageSimulationService drainageService;

    private VrPavingDesignerService service;

    private static final String CROSS_PATTERN =
            "[[[1.0,5.0],[9.0,5.0]],[[5.0,1.0],[5.0,9.0]]]";

    @BeforeEach
    void setUp() {
        service = new VrPavingDesignerService(repository, aestheticService, drainageService);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private VrDesignRequestDTO baseRequest() {
        VrDesignRequestDTO r = new VrDesignRequestDTO();
        r.setUserSessionId("test-session-1");
        r.setDesignName("测试设计");
        r.setCrackPattern(CROSS_PATTERN);
        r.setAreaLength(10.0);
        r.setAreaWidth(10.0);
        r.setSlopeAngle(2.0);
        r.setBasePermeability(0.001);
        r.setRunAesthetic(true);
        r.setRunDrainage(true);
        r.setRainfallMm(50.0);
        r.setInitialWaterDepthMm(10.0);
        r.setCrackWidthMm(3.0);
        r.setStepFrequency(30.0);
        return r;
    }

    private AestheticResultDTO mockAesthetic(double dim, double entropy) {
        AestheticResultDTO a = new AestheticResultDTO();
        a.setFractalDimension(dim);
        a.setBoxCountingDim(dim);
        a.setInfoEntropy(entropy);
        a.setVisualComplexity(dim * 0.8);
        a.setCrackCount(2);
        a.setAvgCrackLength(8.0);
        a.setCrackDensity(0.128);
        a.setPatternSymmetry(0.8);
        a.setCrackSegments(CROSS_PATTERN);
        return a;
    }

    private SimulationResultDTO mockDrainage(double recSec, double infilt) {
        SimulationResultDTO s = new SimulationResultDTO();
        s.setInitialWaterDepth(0.01);
        s.setRecessionTimeSec(recSec);
        s.setPeakWaterDepth(0.015);
        s.setDrainageRate(0.005);
        s.setInfiltrationRate(infilt);
        s.setSurfaceRunoffRate(0.002);
        s.setTimeSeries("[]");
        s.setGridData("[]");
        s.setAlertTriggered(false);
        return s;
    }

    @Nested
    @DisplayName("正常场景")
    class NormalCases {

        @Test
        @DisplayName("简单十字形：美学+排水均成功运行")
        void simpleCross_runsBothAnalyses() {
            when(aestheticService.analyzeCustomSegments(CROSS_PATTERN, 10.0, 10.0))
                    .thenReturn(mockAesthetic(1.35, 2.8));
            when(drainageService.runCustomSimulation(any(SimulationRequestDTO.class),
                    eq(10.0), eq(10.0), eq(2.0), eq(0.001)))
                    .thenReturn(mockDrainage(1800.0, 0.00085));

            VrDesignResultDTO r = service.processDesign(baseRequest());

            assertNotNull(r);
            assertNotNull(r.getId());
            assertEquals("test-session-1", r.getUserSessionId());
            assertEquals("测试设计", r.getDesignName());

            Map<String, Object> aes = r.getAestheticResult();
            assertNotNull(aes, "美学结果不应为空");
            assertEquals(1.35, (double) aes.get("fractalDimension"), 0.001);
            assertEquals(2.8, (double) aes.get("infoEntropy"), 0.001);

            Map<String, Object> drain = r.getDrainageResult();
            assertNotNull(drain, "排水结果不应为空");
            assertEquals(1800.0, (double) drain.get("recessionTimeSec"), 0.001);
            assertEquals(0.00085, (double) drain.get("infiltrationRate"), 1e-6);
        }

        @Test
        @DisplayName("复杂手绘：50段随机线段仍可分析")
        void complexHandDrawing_handlesManySegments() {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < 50; i++) {
                double x1 = Math.random() * 10;
                double y1 = Math.random() * 10;
                double x2 = Math.random() * 10;
                double y2 = Math.random() * 10;
                if (i > 0) sb.append(",");
                sb.append("[[").append(String.format("%.2f", x1)).append(",")
                        .append(String.format("%.2f", y1)).append("],[")
                        .append(String.format("%.2f", x2)).append(",")
                        .append(String.format("%.2f", y2)).append("]]");
            }
            sb.append("]");
            String complexPattern = sb.toString();

            VrDesignRequestDTO req = baseRequest();
            req.setCrackPattern(complexPattern);
            when(aestheticService.analyzeCustomSegments(complexPattern, 10.0, 10.0))
                    .thenReturn(mockAesthetic(1.75, 4.5));
            when(drainageService.runCustomSimulation(any(SimulationRequestDTO.class),
                    anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                    .thenReturn(mockDrainage(2200.0, 0.0009));

            VrDesignResultDTO r = service.processDesign(req);

            assertNotNull(r);
            assertNotNull(r.getAestheticResult());
            assertNotNull(r.getDrainageResult());
            verify(aestheticService, times(1))
                    .analyzeCustomSegments(complexPattern, 10.0, 10.0);
        }

        @Test
        @DisplayName("非正方形场地(15x6)正确传入分析")
        void nonSquareDimensions_passedCorrectly() {
            VrDesignRequestDTO req = baseRequest();
            req.setAreaLength(15.0);
            req.setAreaWidth(6.0);
            when(aestheticService.analyzeCustomSegments(CROSS_PATTERN, 15.0, 6.0))
                    .thenReturn(mockAesthetic(1.2, 2.3));
            when(drainageService.runCustomSimulation(any(SimulationRequestDTO.class),
                    eq(15.0), eq(6.0), eq(2.0), eq(0.001)))
                    .thenReturn(mockDrainage(1500.0, 0.0008));

            VrDesignResultDTO r = service.processDesign(req);

            assertEquals(15.0, r.getAreaLength(), 0.001);
            assertEquals(6.0, r.getAreaWidth(), 0.001);
            verify(aestheticService).analyzeCustomSegments(CROSS_PATTERN, 15.0, 6.0);
        }

        @Test
        @DisplayName("仅美学分析")
        void onlyAesthetic_noDrainage() {
            VrDesignRequestDTO req = baseRequest();
            req.setRunAesthetic(true);
            req.setRunDrainage(false);
            when(aestheticService.analyzeCustomSegments(CROSS_PATTERN, 10.0, 10.0))
                    .thenReturn(mockAesthetic(1.3, 2.6));

            VrDesignResultDTO r = service.processDesign(req);

            assertNotNull(r.getAestheticResult());
            assertNull(r.getDrainageResult());
            verify(drainageService, never())
                    .runCustomSimulation(any(), anyDouble(), anyDouble(), anyDouble(), anyDouble());
        }

        @Test
        @DisplayName("仅排水分析")
        void onlyDrainage_noAesthetic() {
            VrDesignRequestDTO req = baseRequest();
            req.setRunAesthetic(false);
            req.setRunDrainage(true);
            when(drainageService.runCustomSimulation(any(SimulationRequestDTO.class),
                    anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                    .thenReturn(mockDrainage(1600.0, 0.0008));

            VrDesignResultDTO r = service.processDesign(req);

            assertNull(r.getAestheticResult());
            assertNotNull(r.getDrainageResult());
            verify(aestheticService, never()).analyzeCustomSegments(any(), anyDouble(), anyDouble());
        }
    }

    @Nested
    @DisplayName("边界场景")
    class BoundaryCases {

        @Test
        @DisplayName("空数组(无裂缝)仍可保存")
        void emptySegments_stillSaved() {
            VrDesignRequestDTO req = baseRequest();
            req.setCrackPattern("[]");
            when(aestheticService.analyzeCustomSegments("[]", 10.0, 10.0))
                    .thenReturn(mockAesthetic(0.0, 0.0));
            when(drainageService.runCustomSimulation(any(SimulationRequestDTO.class),
                    anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                    .thenReturn(mockDrainage(3500.0, 0.00001));

            VrDesignResultDTO r = service.processDesign(req);

            assertNotNull(r);
            assertEquals("[]", r.getCrackPattern());
        }

        @Test
        @DisplayName("单段设计")
        void singleSegment_handled() {
            String single = "[[[2.0,2.0],[8.0,8.0]]]";
            VrDesignRequestDTO req = baseRequest();
            req.setCrackPattern(single);
            when(aestheticService.analyzeCustomSegments(single, 10.0, 10.0))
                    .thenReturn(mockAesthetic(1.0, 1.0));
            when(drainageService.runCustomSimulation(any(SimulationRequestDTO.class),
                    anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                    .thenReturn(mockDrainage(3000.0, 0.0005));

            VrDesignResultDTO r = service.processDesign(req);

            assertNotNull(r);
            assertNotNull(r.getAestheticResult());
        }

        @Test
        @DisplayName("完全不分析(仅保存)")
        void noAnalysis_onlyPersists() {
            VrDesignRequestDTO req = baseRequest();
            req.setRunAesthetic(false);
            req.setRunDrainage(false);

            VrDesignResultDTO r = service.processDesign(req);

            assertNotNull(r);
            assertNull(r.getAestheticResult());
            assertNull(r.getDrainageResult());
            verify(repository, times(1)).save(any());
        }

        @Test
        @DisplayName("参数全为null使用默认值")
        void allNullParams_usesDefaults() {
            VrDesignRequestDTO req = new VrDesignRequestDTO();
            req.setUserSessionId("null-test");
            req.setCrackPattern(CROSS_PATTERN);
            req.setRunAesthetic(true);
            req.setRunDrainage(true);
            when(aestheticService.analyzeCustomSegments(CROSS_PATTERN, 10.0, 10.0))
                    .thenReturn(mockAesthetic(1.3, 2.5));
            when(drainageService.runCustomSimulation(any(SimulationRequestDTO.class),
                    eq(10.0), eq(10.0), eq(2.0), eq(0.001)))
                    .thenReturn(mockDrainage(2000.0, 0.0008));

            VrDesignResultDTO r = service.processDesign(req);

            assertEquals(10.0, r.getAreaLength(), 0.001);
            assertEquals(10.0, r.getAreaWidth(), 0.001);
            assertEquals(2.0, r.getSlopeAngle(), 0.001);
            assertEquals(0.001, r.getBasePermeability(), 0.0001);
        }

        @Test
        @DisplayName("极大场地100x100不抛异常")
        void veryLargeArea_noException() {
            VrDesignRequestDTO req = baseRequest();
            req.setAreaLength(100.0);
            req.setAreaWidth(100.0);
            when(aestheticService.analyzeCustomSegments(CROSS_PATTERN, 100.0, 100.0))
                    .thenReturn(mockAesthetic(1.2, 2.3));
            when(drainageService.runCustomSimulation(any(SimulationRequestDTO.class),
                    anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                    .thenReturn(mockDrainage(5000.0, 0.0007));

            VrDesignResultDTO r = assertDoesNotThrow(() -> service.processDesign(req));
            assertEquals(100.0, r.getAreaLength(), 0.001);
        }

        @Test
        @DisplayName("极端斜坡85°可处理")
        void extremeSlope_handled() {
            VrDesignRequestDTO req = baseRequest();
            req.setSlopeAngle(85.0);
            when(aestheticService.analyzeCustomSegments(CROSS_PATTERN, 10.0, 10.0))
                    .thenReturn(mockAesthetic(1.3, 2.5));
            when(drainageService.runCustomSimulation(any(SimulationRequestDTO.class),
                    eq(10.0), eq(10.0), eq(85.0), eq(0.001)))
                    .thenReturn(mockDrainage(100.0, 0.0005));

            VrDesignResultDTO r = service.processDesign(req);
            assertEquals(85.0, r.getSlopeAngle(), 0.001);
        }
    }

    @Nested
    @DisplayName("异常场景")
    class ExceptionCases {

        @Test
        @DisplayName("非法JSON美学降级，但排水仍运行")
        void illegalJsonPattern_aestheticDegrades() {
            VrDesignRequestDTO req = baseRequest();
            req.setCrackPattern("not a valid json!!!");
            req.setRunAesthetic(true);
            req.setRunDrainage(true);
            when(aestheticService.analyzeCustomSegments("not a valid json!!!", 10.0, 10.0))
                    .thenThrow(new RuntimeException("JSON解析失败"));
            when(drainageService.runCustomSimulation(any(SimulationRequestDTO.class),
                    anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                    .thenReturn(mockDrainage(2000.0, 0.0008));

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> service.processDesign(req));
            assertTrue(ex.getMessage().contains("JSON"),
                    "异常应含JSON信息: " + ex.getMessage());
        }

        @Test
        @DisplayName("crackPattern为null时跳过美学但运行排水")
        void nullPattern_skipsAestheticButRunsDrainage() {
            VrDesignRequestDTO req = baseRequest();
            req.setCrackPattern(null);
            req.setRunAesthetic(true);
            req.setRunDrainage(true);
            when(drainageService.runCustomSimulation(any(SimulationRequestDTO.class),
                    anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                    .thenReturn(mockDrainage(2500.0, 0.0007));

            VrDesignResultDTO r = service.processDesign(req);

            assertNull(r.getAestheticResult(), "pattern为null时美学应被跳过");
            assertNotNull(r.getDrainageResult());
            verify(aestheticService, never()).analyzeCustomSegments(any(), anyDouble(), anyDouble());
        }

        @Test
        @DisplayName("repository保存失败异常传播")
        void repositoryFails_propagatesException() {
            when(repository.save(any()))
                    .thenThrow(new RuntimeException("DB连接失败"));
            VrDesignRequestDTO req = baseRequest();
            req.setRunAesthetic(false);
            req.setRunDrainage(false);

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> service.processDesign(req));
            assertTrue(ex.getMessage().contains("DB"),
                    "异常应含DB信息: " + ex.getMessage());
        }

        @Test
        @DisplayName("按session查历史")
        void getDesignsBySession_returnsCorrectList() {
            UUID id1 = UUID.randomUUID();
            UUID id2 = UUID.randomUUID();
            VrPavementDesign e1 = new VrPavementDesign();
            e1.setId(id1);
            e1.setUserSessionId("session-a");
            e1.setDesignName("设计A");
            e1.setAreaLength(10.0);
            e1.setAreaWidth(10.0);
            VrPavementDesign e2 = new VrPavementDesign();
            e2.setId(id2);
            e2.setUserSessionId("session-a");
            e2.setDesignName("设计B");
            e2.setAreaLength(12.0);
            e2.setAreaWidth(8.0);
            when(repository.findByUserSessionIdOrderByCreatedAtDesc("session-a"))
                    .thenReturn(List.of(e1, e2));

            List<VrDesignResultDTO> list = service.getDesignsBySession("session-a");

            assertEquals(2, list.size());
            assertEquals(id1, list.get(0).getId());
            assertEquals("设计B", list.get(1).getDesignName());
            assertEquals(8.0, list.get(1).getAreaWidth(), 0.001);
        }

        @Test
        @DisplayName("单个ID查询找不到返回null")
        void getDesignById_notFound_returnsNull() {
            UUID missId = UUID.randomUUID();
            when(repository.findById(missId)).thenReturn(Optional.empty());

            VrDesignResultDTO r = service.getDesign(missId);

            assertNull(r);
        }
    }
}
