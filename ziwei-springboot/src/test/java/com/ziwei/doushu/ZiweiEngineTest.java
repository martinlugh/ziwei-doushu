package com.ziwei.doushu;

import com.ziwei.doushu.engine.ZiweiEngine;
import com.ziwei.doushu.model.BirthInfo;
import com.ziwei.doushu.model.Palace;
import com.ziwei.doushu.model.Star;
import com.ziwei.doushu.model.ZiweiChart;
import com.ziwei.doushu.service.PatternsService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 排盘内核冒烟测试。
 *
 * 用例 2000-8-16 寅时(timeIndex=2) 女命，
 * 对齐 iztro@2.5.8 实际输出：木三局、命宫在午、紫微在午、命宫主星仅紫微。
 */
class ZiweiEngineTest {

    private final ZiweiEngine engine = new ZiweiEngine();
    private final PatternsService patterns = new PatternsService();

    @Test
    void iztroDocExample_2000_8_16_yinHour_female() {
        BirthInfo info = new BirthInfo(2000, 8, 16, 2, "female");
        ZiweiChart chart = engine.generateChart(info);

        assertNotNull(chart);
        assertEquals(12, chart.palaces.size());

        // 五行局：iztro@2.5.8 实际输出为 木三局
        assertEquals(3, chart.wuxingJu);
        assertEquals("木三局", chart.wuxingJuName);

        // 命宫地支：午(6)
        assertEquals(6, chart.mingGongBranch, "命宫应在午");

        // 紫微星所在地支：午(6)
        assertEquals(6, chart.ziweiPos, "紫微应在午宫");

        // 命宫主星：iztro 示例午宫仅 紫微
        Palace ming = chart.palaces.stream().filter(p -> p.branch == chart.mingGongBranch)
                .findFirst().orElseThrow();
        boolean hasZiwei = ming.stars.stream().anyMatch(s -> "紫微".equals(s.name) && "major".equals(s.type));
        assertTrue(hasZiwei, "命宫应有紫微");

        // 每宫地支应唯一覆盖 0~11
        boolean[] seen = new boolean[12];
        for (Palace p : chart.palaces) {
            assertTrue(p.branch >= 0 && p.branch < 12);
            assertTrue(!seen[p.branch], "地支重复: " + p.branch);
            seen[p.branch] = true;
        }

        // 四化应被正确标注：2000 年庚辰，庚干四化 = 太阳禄/武曲权/太阴科/天同忌
        boolean foundTaiyangLu = chart.palaces.stream().flatMap(p -> p.stars.stream())
                .anyMatch(s -> "太阳".equals(s.name) && s.siHua != null && "禄".equals(s.siHua.cn()));
        assertTrue(foundTaiyangLu, "庚年太阳应化禄");

        // 大限应有 12 段
        assertEquals(12, chart.daXians.size());

        // 格局识别不应抛异常，且返回非空列表对象
        assertNotNull(patterns.detectPatterns(chart));
    }

    @Test
    void fiveElementsAndPalacesConsistent_ren_male() {
        BirthInfo info = new BirthInfo(1990, 5, 20, 6, "male");
        ZiweiChart chart = engine.generateChart(info);
        assertNotNull(chart.wuxingJuName);
        assertTrue(chart.wuxingJu >= 2 && chart.wuxingJu <= 6);
        // 主星总数应为 14（紫微系6 + 天府系8）
        long majorCount = chart.palaces.stream().flatMap(p -> p.stars.stream())
                .filter(s -> "major".equals(s.type)).count();
        assertEquals(14, majorCount, "十四主星应全部安放");
        // 命宫摘要可用
        PatternsService.MingGongSummary summary = patterns.getMingGongSummary(chart);
        assertNotNull(summary);
    }
}
