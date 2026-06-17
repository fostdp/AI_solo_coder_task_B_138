package com.garden.icecrack.common.pattern;

import com.garden.icecrack.common.entity.Pavement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PavementPatternFactory — 多样式图案生成测试")
class PavementPatternFactoryTest {

    private static final double DELTA = 1e-6;

    @Nested
    @DisplayName("冰裂纹（ICE_CRACK）生成测试")
    class IceCrackTest {

        @Test
        @DisplayName("[正常场景] 默认配置生成有效段数")
        void generateIceCrack_normal_segmentsReturned() {
            List<double[][]> segs = PavementPatternFactory.generateSegments(
                    Pavement.PavementStyle.ICE_CRACK, 10.0, 8.0,
                    "{\"seed\":42,\"segments\":30,\"irregularity\":0.5}");
            assertNotNull(segs);
            assertTrue(segs.size() >= 5, "至少生成5条裂缝");
            for (double[][] seg : segs) {
                assertEquals(2, seg.length, "每条段有2个端点");
                assertEquals(2, seg[0].length, "每个端点有XY坐标");
                assertEquals(2, seg[1].length);
            }
        }

        @Test
        @DisplayName("[边界场景] 极小场地仍生成有效线段")
        void generateIceCrack_boundary_tinyArea() {
            List<double[][]> segs = PavementPatternFactory.generateSegments(
                    Pavement.PavementStyle.ICE_CRACK, 0.5, 0.5,
                    "{\"seed\":1,\"segments\":5}");
            assertNotNull(segs);
            for (double[][] seg : segs) {
                assertTrue(seg[0][0] >= 0 && seg[0][0] <= 0.5, "X坐标在场地内");
                assertTrue(seg[1][0] >= 0 && seg[1][0] <= 0.5);
            }
        }

        @Test
        @DisplayName("[边界场景] 大场地生成更多段数")
        void generateIceCrack_boundary_largeArea() {
            List<double[][]> small = PavementPatternFactory.generateSegments(
                    Pavement.PavementStyle.ICE_CRACK, 5.0, 5.0, "{\"seed\":1,\"segments\":20}");
            List<double[][]> large = PavementPatternFactory.generateSegments(
                    Pavement.PavementStyle.ICE_CRACK, 20.0, 20.0, "{\"seed\":1,\"segments\":80}");
            assertTrue(large.size() >= small.size(), "大场地段数不小于小场地");
        }

        @Test
        @DisplayName("[异常场景] null 配置降级为默认配置")
        void generateIceCrack_exception_nullPattern_noThrow() {
            assertDoesNotThrow(() -> {
                List<double[][]> segs = PavementPatternFactory.generateSegments(
                        Pavement.PavementStyle.ICE_CRACK, 10.0, 10.0, null);
                assertNotNull(segs);
                assertFalse(segs.isEmpty());
            });
        }

        @Test
        @DisplayName("[异常场景] 非法JSON降级为默认配置")
        void generateIceCrack_exception_invalidJson_noThrow() {
            assertDoesNotThrow(() -> {
                List<double[][]> segs = PavementPatternFactory.generateSegments(
                        Pavement.PavementStyle.ICE_CRACK, 10.0, 10.0, "{not json");
                assertNotNull(segs);
            });
        }

        @Test
        @DisplayName("[正常场景] 同种子可重复生成相同段数")
        void generateIceCrack_normal_deterministicBySeed() {
            List<double[][]> a = PavementPatternFactory.generateSegments(
                    Pavement.PavementStyle.ICE_CRACK, 10.0, 10.0, "{\"seed\":777,\"segments\":20}");
            List<double[][]> b = PavementPatternFactory.generateSegments(
                    Pavement.PavementStyle.ICE_CRACK, 10.0, 10.0, "{\"seed\":777,\"segments\":20}");
            assertEquals(a.size(), b.size(), "同种子段数相同");
        }
    }

    @Nested
    @DisplayName("人字纹（HERRINGBONE）生成测试")
    class HerringboneTest {

        @Test
        @DisplayName("[正常场景] 生成密集人字型线段")
        void herringbone_normal_denseReturned() {
            List<double[][]> segs = PavementPatternFactory.generateSegments(
                    Pavement.PavementStyle.HERRINGBONE, 10.0, 10.0,
                    "{\"seed\":2048,\"spacing\":0.4,\"angle\":45}");
            assertNotNull(segs);
            assertTrue(segs.size() >= 100, "人字纹段数多");
        }

        @Test
        @DisplayName("[边界场景] spacing=0.8 段数小于 spacing=0.2")
        void herringbone_boundary_spacingAffectsDensity() {
            List<double[][]> sparse = PavementPatternFactory.generateSegments(
                    Pavement.PavementStyle.HERRINGBONE, 10.0, 10.0, "{\"spacing\":0.8}");
            List<double[][]> dense = PavementPatternFactory.generateSegments(
                    Pavement.PavementStyle.HERRINGBONE, 10.0, 10.0, "{\"spacing\":0.2}");
            assertTrue(dense.size() > sparse.size(), "小间距段数更多");
        }

        @Test
        @DisplayName("[正常场景] 不规则度增加不影响段数，增加波动")
        void herringbone_normal_irregularityWorks() {
            List<double[][]> base = PavementPatternFactory.generateSegments(
                    Pavement.PavementStyle.HERRINGBONE, 10.0, 10.0,
                    "{\"seed\":1,\"irregularity\":0.0}");
            List<double[][]> irr = PavementPatternFactory.generateSegments(
                    Pavement.PavementStyle.HERRINGBONE, 10.0, 10.0,
                    "{\"seed\":1,\"irregularity\":0.5}");
            assertEquals(base.size(), irr.size(), "不规则度不影响段数");
        }
    }

    @Nested
    @DisplayName("席纹（BASKETWEAVE）生成测试")
    class BasketweaveTest {

        @Test
        @DisplayName("[正常场景] 席纹具有固定重复模式")
        void basketweave_normal_repeatableTiles() {
            List<double[][]> segs = PavementPatternFactory.generateSegments(
                    Pavement.PavementStyle.BASKETWEAVE, 5.0, 5.0,
                    "{\"tileWidth\":0.5,\"tileHeight\":0.25}");
            assertNotNull(segs);
            assertTrue(segs.size() >= 50, "席纹段数充足");
        }

        @Test
        @DisplayName("[边界场景] 不规则度 0 仍能生成完整图案")
        void basketweave_boundary_zeroIrregularity() {
            assertDoesNotThrow(() -> {
                List<double[][]> segs = PavementPatternFactory.generateSegments(
                        Pavement.PavementStyle.BASKETWEAVE, 4.0, 4.0,
                        "{\"irregularity\":0.0}");
                assertFalse(segs.isEmpty());
            });
        }
    }

    @Nested
    @DisplayName("现代透水砖（PERMEABLE_BRICK）生成测试")
    class PermeableBrickTest {

        @Test
        @DisplayName("[正常场景] 砖缝网格 + 微裂缝 都生成")
        void brick_normal_gridAndMicroCracks() {
            List<double[][]> segs = PavementPatternFactory.generateSegments(
                    Pavement.PavementStyle.PERMEABLE_BRICK, 5.0, 5.0,
                    "{\"brickSize\":0.2,\"gapWidth\":0.015}");
            assertNotNull(segs);
            long gridCount = segs.size();
            assertTrue(gridCount >= 500, "透水砖段数很多，含微裂缝");
        }

        @Test
        @DisplayName("[边界场景] 场地极小仍生成少量微裂缝")
        void brick_boundary_minimalArea() {
            List<double[][]> segs = PavementPatternFactory.generateSegments(
                    Pavement.PavementStyle.PERMEABLE_BRICK, 0.3, 0.3,
                    "{\"brickSize\":0.1}");
            assertNotNull(segs);
        }
    }

    @Nested
    @DisplayName("自定义图案（CUSTOM）解析测试")
    class CustomTest {

        @Test
        @DisplayName("[正常场景] 合法坐标段数组被正确解析")
        void custom_normal_parsedCorrectly() {
            String json = "[[[1.0,2.0],[3.0,4.0]],[[5.0,6.0],[7.0,8.0]]]";
            List<double[][]> segs = PavementPatternFactory.generateSegments(
                    Pavement.PavementStyle.CUSTOM, 10.0, 10.0, json);
            assertEquals(2, segs.size());
            assertEquals(1.0, segs.get(0)[0][0], DELTA);
            assertEquals(2.0, segs.get(0)[0][1], DELTA);
            assertEquals(3.0, segs.get(0)[1][0], DELTA);
            assertEquals(7.0, segs.get(1)[1][0], DELTA);
        }

        @Test
        @DisplayName("[边界场景] 空数组返回空段列")
        void custom_boundary_emptyArray_emptyResult() {
            String json = "[]";
            List<double[][]> segs = PavementPatternFactory.generateSegments(
                    Pavement.PavementStyle.CUSTOM, 10.0, 10.0, json);
            assertTrue(segs.isEmpty());
        }

        @Test
        @DisplayName("[异常场景] 非法JSON不抛异常，返回空")
        void custom_exception_invalidJson_noThrow() {
            assertDoesNotThrow(() -> {
                List<double[][]> segs = PavementPatternFactory.generateSegments(
                        Pavement.PavementStyle.CUSTOM, 10.0, 10.0, "不是JSON{{{"));
                assertNotNull(segs);
            });
        }

        @Test
        @DisplayName("[异常场景] null pattern 返回空段列")
        void custom_exception_null_noThrow() {
            List<double[][]> segs = PavementPatternFactory.generateSegments(
                    Pavement.PavementStyle.CUSTOM, 10.0, 10.0, null);
            assertNotNull(segs);
        }
    }

    @Test
    @DisplayName("[正常场景] 相同参数不同样式产生不同段数")
    void style_differences_verifyDistinctOutputs() {
        String cfg = "{\"seed\":99}";
        int ice = PavementPatternFactory.generateSegments(
                Pavement.PavementStyle.ICE_CRACK, 8.0, 6.0, cfg).size();
        int her = PavementPatternFactory.generateSegments(
                Pavement.PavementStyle.HERRINGBONE, 8.0, 6.0, cfg).size();
        int bas = PavementPatternFactory.generateSegments(
                Pavement.PavementStyle.BASKETWEAVE, 8.0, 6.0, cfg).size();
        int bri = PavementPatternFactory.generateSegments(
                Pavement.PavementStyle.PERMEABLE_BRICK, 8.0, 6.0, cfg).size();
        assertTrue(her > ice, "人字纹段数>冰裂纹");
        assertTrue(bri > bas, "透水砖段数>席纹");
    }
}
