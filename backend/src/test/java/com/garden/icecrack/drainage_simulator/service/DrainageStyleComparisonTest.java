package com.garden.icecrack.drainage_simulator.service;

import com.garden.icecrack.common.entity.Pavement;
import com.garden.icecrack.common.repository.PavementRepository;
import com.garden.icecrack.drainage_simulator.config.DrainageProperties;
import com.garden.icecrack.drainage_simulator.dto.SimulationRequestDTO;
import com.garden.icecrack.drainage_simulator.dto.SimulationResultDTO;
import com.garden.icecrack.drainage_simulator.repository.SimulationResultRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("DrainageSimulationService — 样式对比排水效率测试")
@ExtendWith(MockitoExtension.class)
class DrainageStyleComparisonTest {

    private static final double DELTA = 1e-4;

    @Mock private SimulationResultRepository resultRepo;
    @Mock private PavementRepository pavementRepo;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();
    @Spy private DrainageProperties props = new DrainageProperties();

    @InjectMocks private DrainageSimulationService service;

    private UUID iceId;
    private UUID brickId;
    private UUID herrId;

    @BeforeEach
    void setUp() {
        iceId = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
        brickId = UUID.fromString("1bcdefa2-3456-7890-bcde-678901234567");
        herrId = UUID.fromString("f6a7b8c9-d0e1-2345-fabc-456789012345");
    }

    private Pavement makePavement(UUID id, Pavement.PavementStyle style, double permeability) {
        Pavement p = new Pavement();
        p.setId(id);
        p.setName("测试-" + style.name());
        p.setPavementStyle(style);
        p.setAreaLength(10.0);
        p.setAreaWidth(8.0);
        p.setSlopeAngle(2.0);
        p.setBasePermeability(permeability);
        p.setCrackPattern("{\"seed\":100}");
        return p;
    }

    @Nested
    @DisplayName("样式对比：透水砖 vs 冰裂纹 排水效率")
    class PermeableVsIceCrack {

        @Test
        @DisplayName("[正常场景] 透水砖渗透速率显著高于冰裂纹（5x系数）")
        void styleComparison_permeableBrickHigherInfiltration() {
            Pavement ice = makePavement(iceId, Pavement.PavementStyle.ICE_CRACK, 0.001);
            Pavement brick = makePavement(brickId, Pavement.PavementStyle.PERMEABLE_BRICK, 0.001);
            when(pavementRepo.findById(iceId)).thenReturn(Optional.of(ice));
            when(pavementRepo.findById(brickId)).thenReturn(Optional.of(brick));
            when(resultRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SimulationRequestDTO req = new SimulationRequestDTO();
            req.setRainfallMm(50.0);
            req.setCrackWidthMm(3.0);
            req.setStepFrequency(30.0);
            req.setInitialWaterDepthMm(10.0);
            req.setGridResolution(10);
            req.setSimulationDurationSec(600.0);

            req.setPavementId(iceId);
            SimulationResultDTO iceRes = service.runSimulation(req);

            req.setPavementId(brickId);
            SimulationResultDTO brickRes = service.runSimulation(req);

            assertNotNull(iceRes.getInfiltrationRate());
            assertNotNull(brickRes.getInfiltrationRate());
            assertTrue(brickRes.getInfiltrationRate() > iceRes.getInfiltrationRate(),
                    "透水砖渗透率 > 冰裂纹渗透率");
            double ratio = brickRes.getInfiltrationRate() / Math.max(iceRes.getInfiltrationRate(), 1e-9);
            assertTrue(ratio >= 4.5, "渗透率提升约5倍 (实际=" + ratio + ")");
        }

        @Test
        @DisplayName("[正常场景] 相同降雨下，透水砖退水时间更短")
        void styleComparison_permeableBrickShorterRecession() {
            Pavement ice = makePavement(iceId, Pavement.PavementStyle.ICE_CRACK, 0.0008);
            Pavement brick = makePavement(brickId, Pavement.PavementStyle.PERMEABLE_BRICK, 0.008);
            when(pavementRepo.findById(iceId)).thenReturn(Optional.of(ice));
            when(pavementRepo.findById(brickId)).thenReturn(Optional.of(brick));
            when(resultRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SimulationRequestDTO req = new SimulationRequestDTO();
            req.setRainfallMm(40.0);
            req.setCrackWidthMm(3.0);
            req.setInitialWaterDepthMm(15.0);
            req.setGridResolution(10);
            req.setSimulationDurationSec(1800.0);

            req.setPavementId(iceId);
            SimulationResultDTO iceRes = service.runSimulation(req);

            req.setPavementId(brickId);
            SimulationResultDTO brickRes = service.runSimulation(req);

            assertTrue(brickRes.getRecessionTimeSec() <= iceRes.getRecessionTimeSec(),
                    "透水砖退水时间不大于冰裂纹");
        }

        @Test
        @DisplayName("[正常场景] 人字纹：规律性接缝，中等效率")
        void styleComparison_herringboneMediumEfficiency() {
            Pavement ice = makePavement(iceId, Pavement.PavementStyle.ICE_CRACK, 0.001);
            Pavement herr = makePavement(herrId, Pavement.PavementStyle.HERRINGBONE, 0.001);
            when(pavementRepo.findById(iceId)).thenReturn(Optional.of(ice));
            when(pavementRepo.findById(herrId)).thenReturn(Optional.of(herr));
            when(resultRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SimulationRequestDTO req = new SimulationRequestDTO();
            req.setRainfallMm(30.0);
            req.setCrackWidthMm(4.0);
            req.setGridResolution(10);
            req.setSimulationDurationSec(600.0);

            req.setPavementId(iceId);
            SimulationResultDTO iceRes = service.runSimulation(req);
            req.setPavementId(herrId);
            SimulationResultDTO herrRes = service.runSimulation(req);

            assertNotNull(herrRes.getDrainageRate());
            assertTrue(herrRes.getDrainageRate() > 0, "排水速率为正");
        }
    }

    @Nested
    @DisplayName("边界场景")
    class BoundaryScenarios {

        @Test
        @DisplayName("[边界场景] 降雨量为0时仍有初始水被排出")
        void boundary_zeroRainfall_stillDrains() {
            Pavement p = makePavement(iceId, Pavement.PavementStyle.ICE_CRACK, 0.001);
            when(pavementRepo.findById(iceId)).thenReturn(Optional.of(p));
            when(resultRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SimulationRequestDTO req = new SimulationRequestDTO();
            req.setRainfallMm(0.0);
            req.setInitialWaterDepthMm(20.0);
            req.setCrackWidthMm(2.0);
            req.setGridResolution(8);
            req.setSimulationDurationSec(300.0);
            req.setPavementId(iceId);

            SimulationResultDTO res = service.runSimulation(req);
            assertNotNull(res);
            assertTrue(res.getPeakWaterDepth() > 0.015, "峰值水深接近初始水深");
        }

        @Test
        @DisplayName("[边界场景] 裂缝宽度为0时，渗透速率等于基础值")
        void boundary_zeroCrackWidth_baseInfiltration() {
            Pavement p = makePavement(iceId, Pavement.PavementStyle.ICE_CRACK, 0.001);
            when(pavementRepo.findById(iceId)).thenReturn(Optional.of(p));
            when(resultRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SimulationRequestDTO req = new SimulationRequestDTO();
            req.setRainfallMm(10.0);
            req.setCrackWidthMm(0.0);
            req.setStepFrequency(0.0);
            req.setInitialWaterDepthMm(5.0);
            req.setGridResolution(8);
            req.setSimulationDurationSec(300.0);
            req.setPavementId(iceId);

            SimulationResultDTO res = service.runSimulation(req);
            double expected = 0.001 * (1 + 0) * (1 + 0);
            assertEquals(expected, res.getInfiltrationRate(), 0.0002,
                    "无裂缝无踩踏时渗透速率等于基础值");
        }

        @Test
        @DisplayName("[边界场景] 极大裂缝宽度：渗透显著增强")
        void boundary_hugeCrackWidth_enhancedInfiltration() {
            Pavement p = makePavement(iceId, Pavement.PavementStyle.ICE_CRACK, 0.001);
            when(pavementRepo.findById(iceId)).thenReturn(Optional.of(p));
            when(resultRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SimulationRequestDTO req = new SimulationRequestDTO();
            req.setRainfallMm(20.0);
            req.setCrackWidthMm(20.0);
            req.setInitialWaterDepthMm(10.0);
            req.setGridResolution(8);
            req.setSimulationDurationSec(300.0);
            req.setPavementId(iceId);

            SimulationResultDTO res = service.runSimulation(req);
            assertTrue(res.getInfiltrationRate() > 0.001 * 2, "大裂缝渗透至少2倍于基础");
        }
    }

    @Nested
    @DisplayName("异常场景")
    class ExceptionScenarios {

        @Test
        @DisplayName("[异常场景] 无效铺地ID抛出异常")
        void exception_pavementNotFound_throws() {
            UUID missingId = UUID.randomUUID();
            when(pavementRepo.findById(missingId)).thenReturn(Optional.empty());

            SimulationRequestDTO req = new SimulationRequestDTO();
            req.setPavementId(missingId);

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> service.runSimulation(req));
            assertTrue(ex.getMessage().contains("not found"));
            verify(resultRepo, never()).save(any());
        }

        @Test
        @DisplayName("[异常场景] 负降雨量视为0，不抛异常")
        void exception_negativeRainfall_noThrow() {
            Pavement p = makePavement(iceId, Pavement.PavementStyle.ICE_CRACK, 0.001);
            when(pavementRepo.findById(iceId)).thenReturn(Optional.of(p));
            when(resultRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SimulationRequestDTO req = new SimulationRequestDTO();
            req.setPavementId(iceId);
            req.setRainfallMm(-50.0);
            req.setInitialWaterDepthMm(5.0);
            req.setGridResolution(6);
            req.setSimulationDurationSec(120.0);

            assertDoesNotThrow(() -> service.runSimulation(req));
            SimulationResultDTO res = service.runSimulation(req);
            assertTrue(res.getPeakWaterDepth() >= 0, "水深非负");
        }

        @Test
        @DisplayName("[异常场景] 网格分辨率=0 被替换为默认值")
        void exception_zeroGridResolution_noThrow() {
            Pavement p = makePavement(iceId, Pavement.PavementStyle.ICE_CRACK, 0.001);
            when(pavementRepo.findById(iceId)).thenReturn(Optional.of(p));
            when(resultRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SimulationRequestDTO req = new SimulationRequestDTO();
            req.setPavementId(iceId);
            req.setRainfallMm(20.0);
            req.setGridResolution(0);

            assertDoesNotThrow(() -> service.runSimulation(req));
        }
    }

    @Test
    @DisplayName("[正常场景] 自定义仿真（runCustomSimulation）对透水砖渗透率高")
    void customSimulation_permeableBrickParameters() {
        SimulationRequestDTO req = new SimulationRequestDTO();
        req.setRainfallMm(30.0);
        req.setInitialWaterDepthMm(10.0);
        req.setCrackWidthMm(3.0);
        req.setGridResolution(10);
        req.setSimulationDurationSec(300.0);

        SimulationResultDTO ice = service.runCustomSimulation(req, 10, 10, 2.0, 0.001);
        SimulationResultDTO brick = service.runCustomSimulation(req, 10, 10, 2.0, 0.005);

        assertTrue(brick.getInfiltrationRate() > ice.getInfiltrationRate(),
                "透水砖(高基础渗透) > 冰裂纹(低基础渗透)");
    }
}
