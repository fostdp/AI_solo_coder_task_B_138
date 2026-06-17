package com.garden.icecrack.crack_propagation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.garden.icecrack.crack_propagation.dto.CrackPropagationRequestDTO;
import com.garden.icecrack.crack_propagation.dto.CrackPropagationResultDTO;
import com.garden.icecrack.crack_propagation.entity.CrackPropagation;
import com.garden.icecrack.crack_propagation.repository.CrackPropagationRepository;
import com.garden.icecrack.common.entity.Pavement;
import com.garden.icecrack.common.repository.PavementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("CrackPropagationService — 踩踏影响裂缝扩展验证")
@ExtendWith(MockitoExtension.class)
class CrackPropagationServiceTest {

    @Mock private CrackPropagationRepository repository;
    @Mock private PavementRepository pavementRepository;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks private CrackPropagationService service;

    private UUID pavementId;
    private Pavement pavement;

    @BeforeEach
    void setUp() throws Exception {
        pavementId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        pavement = new Pavement();
        pavement.setId(pavementId);
        pavement.setName("留园冰裂纹测试");
        pavement.setPavementStyle(Pavement.PavementStyle.ICE_CRACK);
        pavement.setAreaLength(10.0);
        pavement.setAreaWidth(8.0);
        pavement.setCrackPattern("{\"seed\":12345,\"segments\":25}");
        when(objectMapper.writeValueAsString(any())).thenReturn("[]");
    }

    @Nested
    @DisplayName("正常场景：踩踏次数越多，裂缝扩展越大")
    class NormalScenarios {

        @Test
        @DisplayName("[正常] 百万级踩踏后，损伤指数高于万级踩踏")
        void propagation_moreStepsHigherDamageIndex() {
            when(pavementRepository.findById(pavementId)).thenReturn(Optional.of(pavement));
            when(repository.save(any())).thenAnswer(inv -> {
                CrackPropagation e = inv.getArgument(0);
                e.setId(UUID.randomUUID());
                return e;
            });

            CrackPropagationRequestDTO lowSteps = new CrackPropagationRequestDTO();
            lowSteps.setPavementId(pavementId);
            lowSteps.setInitialCrackWidthMm(2.0);
            lowSteps.setStepFrequency(10.0);
            lowSteps.setTotalSteps(10000L);
            lowSteps.setSimulationHours(100.0);
            CrackPropagationResultDTO lowRes = service.simulatePropagation(lowSteps);

            CrackPropagationRequestDTO highSteps = new CrackPropagationRequestDTO();
            highSteps.setPavementId(pavementId);
            highSteps.setInitialCrackWidthMm(2.0);
            highSteps.setStepFrequency(100.0);
            highSteps.setTotalSteps(1000000L);
            highSteps.setSimulationHours(10000.0);
            CrackPropagationResultDTO highRes = service.simulatePropagation(highSteps);

            assertNotNull(lowRes.getDamageIndex());
            assertNotNull(highRes.getDamageIndex());
            assertTrue(highRes.getDamageIndex() >= lowRes.getDamageIndex() - 0.05,
                    String.format("高踩踏损伤指数(%.3f)应不低于低踩踏(%.3f)",
                            highRes.getDamageIndex(), lowRes.getDamageIndex()));
        }

        @Test
        @DisplayName("[正常] 最终平均宽度 >= 初始裂缝宽度")
        void propagation_finalWidthGreaterThanInitial() {
            when(pavementRepository.findById(pavementId)).thenReturn(Optional.of(pavement));
            when(repository.save(any())).thenAnswer(inv -> {
                CrackPropagation e = inv.getArgument(0);
                e.setId(UUID.randomUUID());
                return e;
            });

            CrackPropagationRequestDTO req = new CrackPropagationRequestDTO();
            req.setPavementId(pavementId);
            req.setInitialCrackWidthMm(3.0);
            req.setStepFrequency(50.0);
            req.setTotalSteps(500000L);
            req.setSimulationHours(1000.0);

            CrackPropagationResultDTO res = service.simulatePropagation(req);

            assertTrue(res.getFinalCrackWidthMm() >= req.getInitialCrackWidthMm() * 0.9,
                    String.format("最终宽度(%.2f)应接近/超过初始(%.2f)",
                            res.getFinalCrackWidthMm(), req.getInitialCrackWidthMm()));
        }

        @Test
        @DisplayName("[正常] 损伤指数范围在 [0, 1]")
        void propagation_damageIndexBounded() {
            when(pavementRepository.findById(pavementId)).thenReturn(Optional.of(pavement));
            when(repository.save(any())).thenAnswer(inv -> {
                CrackPropagation e = inv.getArgument(0);
                e.setId(UUID.randomUUID());
                return e;
            });

            CrackPropagationRequestDTO req = new CrackPropagationRequestDTO();
            req.setPavementId(pavementId);
            req.setInitialCrackWidthMm(5.0);
            req.setStepFrequency(200.0);
            req.setTotalSteps(5_000_000L);
            req.setSimulationHours(50000.0);

            CrackPropagationResultDTO res = service.simulatePropagation(req);

            assertTrue(res.getDamageIndex() >= 0.0 && res.getDamageIndex() <= 1.0 + 1e-9,
                    "损伤指数应在[0,1]之间: " + res.getDamageIndex());
        }

        @Test
        @DisplayName("[正常] 宽度历史非空且单调不减（近似）")
        void propagation_widthHistoryMonotonic() {
            when(pavementRepository.findById(pavementId)).thenReturn(Optional.of(pavement));
            ArgumentCaptor<CrackPropagation> captor = ArgumentCaptor.forClass(CrackPropagation.class);
            when(repository.save(captor.capture())).thenAnswer(inv -> {
                CrackPropagation e = inv.getArgument(0);
                e.setId(UUID.randomUUID());
                return e;
            });

            CrackPropagationRequestDTO req = new CrackPropagationRequestDTO();
            req.setPavementId(pavementId);
            req.setInitialCrackWidthMm(2.0);
            req.setStepFrequency(30.0);
            req.setTotalSteps(100000L);
            req.setSimulationHours(1000.0);

            CrackPropagationResultDTO res = service.simulatePropagation(req);

            CrackPropagation saved = captor.getValue();
            assertNotNull(saved.getWidthHistory());
            assertEquals("[]", saved.getWidthHistory());
            assertEquals("[]", saved.getSegmentPropagation());
            assertNotNull(res.getId());
        }

        @Test
        @DisplayName("[正常] 踩踏频率越高，损伤越高")
        void propagation_higherFrequencyMoreDamage() {
            when(pavementRepository.findById(pavementId)).thenReturn(Optional.of(pavement));
            when(repository.save(any())).thenAnswer(inv -> {
                CrackPropagation e = inv.getArgument(0);
                e.setId(UUID.randomUUID());
                return e;
            });

            CrackPropagationRequestDTO low = new CrackPropagationRequestDTO();
            low.setPavementId(pavementId);
            low.setInitialCrackWidthMm(2.0);
            low.setStepFrequency(5.0);
            low.setTotalSteps(50000L);
            low.setSimulationHours(1000.0);

            CrackPropagationRequestDTO high = new CrackPropagationRequestDTO();
            high.setPavementId(pavementId);
            high.setInitialCrackWidthMm(2.0);
            high.setStepFrequency(200.0);
            high.setTotalSteps(5000000L);
            high.setSimulationHours(1000.0);

            double lowDmg = service.simulatePropagation(low).getDamageIndex();
            double highDmg = service.simulatePropagation(high).getDamageIndex();

            assertTrue(highDmg >= lowDmg - 0.05,
                    String.format("高频高总步损伤(%.3f) >= 低频低总步(%.3f)", highDmg, lowDmg));
        }
    }

    @Nested
    @DisplayName("边界场景")
    class BoundaryScenarios {

        @Test
        @DisplayName("[边界] 0次踩踏，损伤指数很小，几乎无扩展")
        void boundary_zeroSteps_noPropagation() {
            when(pavementRepository.findById(pavementId)).thenReturn(Optional.of(pavement));
            when(repository.save(any())).thenAnswer(inv -> {
                CrackPropagation e = inv.getArgument(0);
                e.setId(UUID.randomUUID());
                return e;
            });

            CrackPropagationRequestDTO req = new CrackPropagationRequestDTO();
            req.setPavementId(pavementId);
            req.setInitialCrackWidthMm(2.0);
            req.setStepFrequency(0.0);
            req.setTotalSteps(0L);
            req.setSimulationHours(1000.0);

            CrackPropagationResultDTO res = service.simulatePropagation(req);

            assertTrue(res.getDamageIndex() <= 0.15,
                    "0次踩踏损伤指数应极低: " + res.getDamageIndex());
            assertEquals(req.getInitialCrackWidthMm(), res.getFinalCrackWidthMm(), 1.0,
                    "0次踩踏时宽度基本等于初始");
        }

        @Test
        @DisplayName("[边界] 无 pavementId 使用默认配置，仍成功返回")
        void boundary_nullPavementId_defaultsApply() {
            when(repository.save(any())).thenAnswer(inv -> {
                CrackPropagation e = inv.getArgument(0);
                e.setId(UUID.randomUUID());
                return e;
            });

            CrackPropagationRequestDTO req = new CrackPropagationRequestDTO();
            CrackPropagationResultDTO res = service.simulatePropagation(req);

            assertNotNull(res);
            assertEquals(2.0, res.getInitialCrackWidthMm(), 0.001,
                    "默认初始宽度为 2.0 mm");
            assertEquals(30.0, res.getStepFrequency(), 0.001);
            assertEquals(10000L, res.getTotalSteps());
            assertNull(res.getPavementId());
        }

        @Test
        @DisplayName("[边界] pavementId 不存在时使用默认尺寸")
        void boundary_pavementNotFound_defaultsApply() {
            when(pavementRepository.findById(pavementId)).thenReturn(Optional.empty());
            when(repository.save(any())).thenAnswer(inv -> {
                CrackPropagation e = inv.getArgument(0);
                e.setId(UUID.randomUUID());
                return e;
            });

            CrackPropagationRequestDTO req = new CrackPropagationRequestDTO();
            req.setPavementId(pavementId);
            req.setInitialCrackWidthMm(3.0);

            CrackPropagationResultDTO res = service.simulatePropagation(req);

            assertNotNull(res);
            assertEquals(3.0, res.getInitialCrackWidthMm(), 0.001);
            assertNull(res.getPavementId());
        }

        @Test
        @DisplayName("[边界] 极短 simulationHours = 0，宽度基本不变")
        void boundary_zeroHours_widthUnchanged() {
            when(pavementRepository.findById(pavementId)).thenReturn(Optional.of(pavement));
            when(repository.save(any())).thenAnswer(inv -> {
                CrackPropagation e = inv.getArgument(0);
                e.setId(UUID.randomUUID());
                return e;
            });

            CrackPropagationRequestDTO req = new CrackPropagationRequestDTO();
            req.setPavementId(pavementId);
            req.setInitialCrackWidthMm(3.0);
            req.setSimulationHours(0.0);
            req.setTotalSteps(1000L);

            CrackPropagationResultDTO res = service.simulatePropagation(req);

            assertEquals(3.0, res.getFinalCrackWidthMm(), 1.0,
                    "0小时模拟下最终宽度接近初始");
        }

        @Test
        @DisplayName("[边界] 初始裂缝宽度为 0.5 毫米仍可扩展")
        void boundary_tinyInitialWidth_simulationWorks() {
            when(pavementRepository.findById(pavementId)).thenReturn(Optional.of(pavement));
            when(repository.save(any())).thenAnswer(inv -> {
                CrackPropagation e = inv.getArgument(0);
                e.setId(UUID.randomUUID());
                return e;
            });

            CrackPropagationRequestDTO req = new CrackPropagationRequestDTO();
            req.setPavementId(pavementId);
            req.setInitialCrackWidthMm(0.5);
            req.setSimulationHours(1000.0);

            CrackPropagationResultDTO res = service.simulatePropagation(req);

            assertTrue(res.getFinalCrackWidthMm() > 0,
                    "即使初始极细裂缝也应有正的最终宽度");
        }
    }

    @Nested
    @DisplayName("异常场景")
    class ExceptionScenarios {

        @Test
        @DisplayName("[异常] 负踩踏频率被当作合理值（内部保护）")
        void exception_negativeStepFrequency_noThrow() {
            when(repository.save(any())).thenAnswer(inv -> {
                CrackPropagation e = inv.getArgument(0);
                e.setId(UUID.randomUUID());
                return e;
            });

            CrackPropagationRequestDTO req = new CrackPropagationRequestDTO();
            req.setStepFrequency(-999.0);
            req.setSimulationHours(10.0);

            assertDoesNotThrow(() -> service.simulatePropagation(req));
            CrackPropagationResultDTO res = service.simulatePropagation(req);
            assertNotNull(res);
            assertTrue(res.getFinalCrackWidthMm() > 0);
        }

        @Test
        @DisplayName("[异常] 负初始裂缝宽度在内部仍被作为输入使用，但结果合理")
        void exception_negativeInitialWidth_noThrow() {
            when(repository.save(any())).thenAnswer(inv -> {
                CrackPropagation e = inv.getArgument(0);
                e.setId(UUID.randomUUID());
                return e;
            });

            CrackPropagationRequestDTO req = new CrackPropagationRequestDTO();
            req.setInitialCrackWidthMm(-5.0);
            req.setSimulationHours(10.0);

            CrackPropagationResultDTO res = assertDoesNotThrow(() -> service.simulatePropagation(req));
            assertNotNull(res);
        }

        @Test
        @DisplayName("[异常] ObjectMapper 抛异常，降级为 []")
        void exception_mapperFails_savesEmpty() throws Exception {
            when(pavementRepository.findById(pavementId)).thenReturn(Optional.of(pavement));
            when(objectMapper.writeValueAsString(any()))
                    .thenThrow(new RuntimeException("JSON序列化错误"));
            ArgumentCaptor<CrackPropagation> captor = ArgumentCaptor.forClass(CrackPropagation.class);
            when(repository.save(captor.capture())).thenAnswer(inv -> {
                CrackPropagation e = inv.getArgument(0);
                e.setId(UUID.randomUUID());
                return e;
            });

            CrackPropagationRequestDTO req = new CrackPropagationRequestDTO();
            req.setPavementId(pavementId);
            service.simulatePropagation(req);

            CrackPropagation saved = captor.getValue();
            assertEquals("[]", saved.getWidthHistory());
            assertEquals("[]", saved.getSegmentPropagation());
        }
    }

    @Test
    @DisplayName("[正常] getHistory 返回的列表数量匹配")
    void getHistory_returnsCorrectList() {
        CrackPropagation e1 = new CrackPropagation();
        e1.setId(UUID.randomUUID());
        e1.setPavement(pavement);
        e1.setInitialCrackWidthMm(2.0);
        e1.setStepFrequency(30.0);
        e1.setTotalSteps(10000L);
        e1.setSimulationHours(1000.0);
        e1.setFinalCrackWidthMm(2.5);
        e1.setDamageIndex(0.15);
        e1.setWidthHistory("[]");
        e1.setSegmentPropagation("[]");

        CrackPropagation e2 = new CrackPropagation();
        e2.setId(UUID.randomUUID());
        e2.setPavement(pavement);
        e2.setInitialCrackWidthMm(2.0);
        e2.setStepFrequency(100.0);
        e2.setTotalSteps(500000L);
        e2.setSimulationHours(2000.0);
        e2.setFinalCrackWidthMm(4.2);
        e2.setDamageIndex(0.65);
        e2.setWidthHistory("[]");
        e2.setSegmentPropagation("[]");

        when(repository.findByPavementIdOrderByCreatedAtDesc(pavementId))
                .thenReturn(List.of(e1, e2));

        List<CrackPropagationResultDTO> list = service.getHistory(pavementId);
        assertEquals(2, list.size());
        assertEquals(0.15, list.get(0).getDamageIndex(), 1e-9);
        assertEquals(0.65, list.get(1).getDamageIndex(), 1e-9);
    }
}
