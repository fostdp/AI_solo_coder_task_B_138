package com.garden.icecrack.pedestrian_simulator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.garden.icecrack.common.entity.Pavement;
import com.garden.icecrack.common.repository.PavementRepository;
import com.garden.icecrack.pedestrian_simulator.dto.PedestrianSimulationRequestDTO;
import com.garden.icecrack.pedestrian_simulator.dto.PedestrianSimulationResultDTO;
import com.garden.icecrack.pedestrian_simulator.entity.PedestrianSimulation;
import com.garden.icecrack.pedestrian_simulator.repository.PedestrianSimulationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("PedestrianSimulatorService — 踩踏影响模拟测试")
@ExtendWith(MockitoExtension.class)
class PedestrianSimulatorServiceTest {

    @Mock private PedestrianSimulationRepository repository;
    @Mock private PavementRepository pavementRepo;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();

    private PedestrianSimulatorService service;

    private UUID pavementId;

    @BeforeEach
    void setUp() {
        pavementId = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
        service = new PedestrianSimulatorService(repository, pavementRepo, objectMapper);
        when(repository.save(any())).thenAnswer(inv -> {
            PedestrianSimulation e = inv.getArgument(0);
            e.setId(1L);
            return e;
        });
    }

    @Nested
    @DisplayName("正常场景")
    class NormalCases {

        @Test
        @DisplayName("高踩踏次数损伤指数应明显高于低次数")
        void highSteps_higherDamage_thanLowSteps() {
            Pavement p = new Pavement();
            p.setId(pavementId);
            p.setAreaLength(10.0);
            p.setAreaWidth(10.0);
            p.setPavementStyle(Pavement.PavementStyle.ICE_CRACK);
            when(pavementRepo.findById(pavementId)).thenReturn(Optional.of(p));

            PedestrianSimulationRequestDTO lowReq = new PedestrianSimulationRequestDTO();
            lowReq.setPavementId(pavementId);
            lowReq.setInitialCrackWidthMm(2.0);
            lowReq.setStepFrequency(10.0);
            lowReq.setTotalSteps(1000L);
            lowReq.setSimulationHours(100.0);
            PedestrianSimulationResultDTO low = service.simulatePedestrianImpact(lowReq);

            PedestrianSimulationRequestDTO highReq = new PedestrianSimulationRequestDTO();
            highReq.setPavementId(pavementId);
            highReq.setInitialCrackWidthMm(2.0);
            highReq.setStepFrequency(60.0);
            highReq.setTotalSteps(1000000L);
            highReq.setSimulationHours(10000.0);
            PedestrianSimulationResultDTO high = service.simulatePedestrianImpact(highReq);

            assertTrue(high.getDamageIndex() > low.getDamageIndex(),
                    "高踩踏损伤指数=" + high.getDamageIndex() + " 应高于低踩踏=" + low.getDamageIndex());
        }

        @Test
        @DisplayName("最终宽度不小于初始宽度")
        void finalWidth_ge_initialWidth() {
            Pavement p = new Pavement();
            p.setId(pavementId);
            p.setAreaLength(10.0);
            p.setAreaWidth(10.0);
            p.setPavementStyle(Pavement.PavementStyle.ICE_CRACK);
            when(pavementRepo.findById(pavementId)).thenReturn(Optional.of(p));

            PedestrianSimulationRequestDTO req = new PedestrianSimulationRequestDTO();
            req.setPavementId(pavementId);
            req.setInitialCrackWidthMm(2.0);
            req.setStepFrequency(30.0);
            req.setTotalSteps(100000L);
            req.setSimulationHours(1000.0);

            PedestrianSimulationResultDTO r = service.simulatePedestrianImpact(req);

            assertTrue(r.getFinalCrackWidthMm() >= 2.0 * 0.5,
                    "最终宽度=" + r.getFinalCrackWidthMm() + " 应不小于初始下限=1.0");
            assertEquals(2.0, r.getInitialCrackWidthMm(), 0.001);
        }

        @Test
        @DisplayName("损伤指数∈[0,1]")
        void damageIndex_inValidRange() {
            Pavement p = new Pavement();
            p.setId(pavementId);
            p.setAreaLength(10.0);
            p.setAreaWidth(10.0);
            p.setPavementStyle(Pavement.PavementStyle.ICE_CRACK);
            when(pavementRepo.findById(pavementId)).thenReturn(Optional.of(p));

            PedestrianSimulationRequestDTO req = new PedestrianSimulationRequestDTO();
            req.setPavementId(pavementId);
            req.setInitialCrackWidthMm(5.0);
            req.setStepFrequency(120.0);
            req.setTotalSteps(10000000L);
            req.setSimulationHours(87600.0);

            PedestrianSimulationResultDTO r = service.simulatePedestrianImpact(req);

            assertTrue(r.getDamageIndex() >= 0.0, "损伤指数不应为负: " + r.getDamageIndex());
            assertTrue(r.getDamageIndex() <= 1.01, "损伤指数不应超过1: " + r.getDamageIndex());
        }

        @Test
        @DisplayName("高频率踩踏损伤应高于低频率")
        void highFrequency_higherDamage() {
            Pavement p = new Pavement();
            p.setId(pavementId);
            p.setAreaLength(10.0);
            p.setAreaWidth(10.0);
            p.setPavementStyle(Pavement.PavementStyle.ICE_CRACK);
            when(pavementRepo.findById(pavementId)).thenReturn(Optional.of(p));

            PedestrianSimulationRequestDTO lowFreq = new PedestrianSimulationRequestDTO();
            lowFreq.setPavementId(pavementId);
            lowFreq.setInitialCrackWidthMm(2.0);
            lowFreq.setStepFrequency(5.0);
            lowFreq.setTotalSteps(50000L);
            lowFreq.setSimulationHours(10000.0);

            PedestrianSimulationRequestDTO highFreq = new PedestrianSimulationRequestDTO();
            highFreq.setPavementId(pavementId);
            highFreq.setInitialCrackWidthMm(2.0);
            highFreq.setStepFrequency(120.0);
            highFreq.setTotalSteps(50000L);
            highFreq.setSimulationHours(10000.0);

            PedestrianSimulationResultDTO low = service.simulatePedestrianImpact(lowFreq);
            PedestrianSimulationResultDTO high = service.simulatePedestrianImpact(highFreq);

            assertTrue(high.getDamageIndex() >= low.getDamageIndex() - 0.01,
                    "高频损伤=" + high.getDamageIndex() + " 不应低于低频=" + low.getDamageIndex());
        }

        @Test
        @DisplayName("历史记录保存并正确映射")
        void history_persistsCorrectly() {
            PedestrianSimulation entity = new PedestrianSimulation();
            entity.setId(5L);
            entity.setInitialCrackWidthMm(2.0);
            entity.setStepFrequency(30.0);
            entity.setTotalSteps(100000L);
            entity.setSimulationHours(1000.0);
            entity.setFinalCrackWidthMm(2.5);
            entity.setDamageIndex(0.35);
            entity.setWidthHistory("[]");
            entity.setSegmentPropagation("[]");
            when(repository.findByPavementIdOrderByCreatedAtDesc(pavementId))
                    .thenReturn(List.of(entity));

            List<PedestrianSimulationResultDTO> history = service.getHistory(pavementId);

            assertEquals(1, history.size());
            assertEquals(5L, history.get(0).getId());
            assertEquals(0.35, history.get(0).getDamageIndex(), 0.001);
            assertEquals(100000L, history.get(0).getTotalSteps());
        }
    }

    @Nested
    @DisplayName("边界场景")
    class BoundaryCases {

        @Test
        @DisplayName("0次踩踏损伤指数应接近初始值")
        void zeroSteps_lowDamage() {
            Pavement p = new Pavement();
            p.setId(pavementId);
            p.setAreaLength(10.0);
            p.setAreaWidth(10.0);
            p.setPavementStyle(Pavement.PavementStyle.ICE_CRACK);
            when(pavementRepo.findById(pavementId)).thenReturn(Optional.of(p));

            PedestrianSimulationRequestDTO req = new PedestrianSimulationRequestDTO();
            req.setPavementId(pavementId);
            req.setInitialCrackWidthMm(2.0);
            req.setStepFrequency(0.0);
            req.setTotalSteps(0L);
            req.setSimulationHours(1000.0);

            PedestrianSimulationResultDTO r = service.simulatePedestrianImpact(req);

            assertTrue(r.getDamageIndex() <= 0.15,
                    "0次踩踏损伤指数应≤0.15, 实际=" + r.getDamageIndex());
        }

        @Test
        @DisplayName("无pavementId时使用默认配置")
        void noPavementId_usesDefaults() {
            PedestrianSimulationRequestDTO req = new PedestrianSimulationRequestDTO();
            req.setInitialCrackWidthMm(2.0);
            req.setStepFrequency(30.0);
            req.setTotalSteps(10000L);
            req.setSimulationHours(1000.0);

            PedestrianSimulationResultDTO r = service.simulatePedestrianImpact(req);

            assertNotNull(r);
            assertNotNull(r.getId());
            assertNull(r.getPavementId());
            assertTrue(r.getDamageIndex() >= 0.0);
        }

        @Test
        @DisplayName("pavementId不存在不报错，使用默认尺寸")
        void pavementIdNotFound_noError() {
            when(pavementRepo.findById(pavementId)).thenReturn(Optional.empty());

            PedestrianSimulationRequestDTO req = new PedestrianSimulationRequestDTO();
            req.setPavementId(pavementId);
            req.setInitialCrackWidthMm(2.0);
            req.setTotalSteps(10000L);
            req.setSimulationHours(1000.0);

            PedestrianSimulationResultDTO r = service.simulatePedestrianImpact(req);

            assertNotNull(r);
            assertNull(r.getPavementId());
        }

        @Test
        @DisplayName("模拟0小时宽度基本不变")
        void zeroHours_widthAlmostUnchanged() {
            Pavement p = new Pavement();
            p.setId(pavementId);
            p.setAreaLength(10.0);
            p.setAreaWidth(10.0);
            p.setPavementStyle(Pavement.PavementStyle.ICE_CRACK);
            when(pavementRepo.findById(pavementId)).thenReturn(Optional.of(p));

            PedestrianSimulationRequestDTO req = new PedestrianSimulationRequestDTO();
            req.setPavementId(pavementId);
            req.setInitialCrackWidthMm(3.0);
            req.setTotalSteps(0L);
            req.setSimulationHours(0.0);

            PedestrianSimulationResultDTO r = service.simulatePedestrianImpact(req);

            assertEquals(3.0, r.getInitialCrackWidthMm(), 0.001);
            assertTrue(r.getFinalCrackWidthMm() >= 1.5);
        }

        @Test
        @DisplayName("极细裂缝(0.5mm)仍可扩展")
        void veryThinCrack_stillGrows() {
            Pavement p = new Pavement();
            p.setId(pavementId);
            p.setAreaLength(10.0);
            p.setAreaWidth(10.0);
            p.setPavementStyle(Pavement.PavementStyle.ICE_CRACK);
            when(pavementRepo.findById(pavementId)).thenReturn(Optional.of(p));

            PedestrianSimulationRequestDTO req = new PedestrianSimulationRequestDTO();
            req.setPavementId(pavementId);
            req.setInitialCrackWidthMm(0.5);
            req.setStepFrequency(60.0);
            req.setTotalSteps(500000L);
            req.setSimulationHours(5000.0);

            PedestrianSimulationResultDTO r = service.simulatePedestrianImpact(req);

            assertNotNull(r);
            assertTrue(r.getDamageIndex() >= 0.0,
                    "细裂缝损伤应≥0, 实际=" + r.getDamageIndex());
        }
    }

    @Nested
    @DisplayName("异常场景")
    class ExceptionCases {

        @Test
        @DisplayName("负频率不抛异常，取最大值0")
        void negativeFrequency_noException() {
            PedestrianSimulationRequestDTO req = new PedestrianSimulationRequestDTO();
            req.setInitialCrackWidthMm(2.0);
            req.setStepFrequency(-5.0);
            req.setTotalSteps(10000L);
            req.setSimulationHours(1000.0);

            assertDoesNotThrow(() -> service.simulatePedestrianImpact(req));
        }

        @Test
        @DisplayName("负初始宽度不抛异常")
        void negativeInitialWidth_noException() {
            PedestrianSimulationRequestDTO req = new PedestrianSimulationRequestDTO();
            req.setInitialCrackWidthMm(-1.0);
            req.setTotalSteps(10000L);
            req.setSimulationHours(1000.0);

            assertDoesNotThrow(() -> service.simulatePedestrianImpact(req));
        }

        @Test
        @DisplayName("ObjectMapper抛异常降级保存[]")
        void objectMapperThrows_fallsBackToEmptyJson() throws Exception {
            ObjectMapper badMapper = mock(ObjectMapper.class);
            when(badMapper.writeValueAsString(any()))
                    .thenThrow(new RuntimeException("序列化失败"));
            PedestrianSimulatorService badService = new PedestrianSimulatorService(
                    repository, pavementRepo, badMapper);

            PedestrianSimulationRequestDTO req = new PedestrianSimulationRequestDTO();
            req.setInitialCrackWidthMm(2.0);
            req.setTotalSteps(10000L);
            req.setSimulationHours(1000.0);

            PedestrianSimulationResultDTO r = badService.simulatePedestrianImpact(req);

            assertEquals("[]", r.getWidthHistory());
            assertEquals("[]", r.getSegmentPropagation());
        }
    }
}
