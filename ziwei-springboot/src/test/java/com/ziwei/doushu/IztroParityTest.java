package com.ziwei.doushu;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ziwei.doushu.engine.Constants;
import com.ziwei.doushu.engine.ZiweiEngine;
import com.ziwei.doushu.model.BirthInfo;
import com.ziwei.doushu.model.Palace;
import com.ziwei.doushu.model.Star;
import com.ziwei.doushu.model.ZiweiChart;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 与 iztro@2.5.8（原项目 algorithm.ts 实际依赖的排盘库）逐字段对拍。
 *
 * 参照数据 src/test/resources/iztro-truth.jsonl 由 `astro.bySolar(date, timeIndex, gender, true, 'zh-CN')`
 * 生成（与 algorithm.ts 完全相同的调用方式）。对每个用例校验：
 *   - 五行局、命宫地支、紫微地支
 *   - 每宫：宫干、十二宫名、主星名+四化+亮度映射、全部星曜名集合、大限年龄段
 */
class IztroParityTest {

    private final ZiweiEngine engine = new ZiweiEngine();
    private final ObjectMapper om = new ObjectMapper();

    /** iztro 亮度（庙旺得利平不陷）→ algorithm.ts mapBrightness（bright/normal/dim） */
    private static String mapBrightness(String b) {
        if (b == null || b.isEmpty()) return "normal";
        if (b.equals("庙") || b.equals("旺")) return "bright";
        if (b.equals("陷") || b.equals("不")) return "dim";
        return "normal";
    }

    private static int feValueOf(String name) {
        return Constants.ELEMENT_TO_JU.getOrDefault(name.substring(0, 1), -1);
    }

    @Test
    void parityWithIztro() throws Exception {
        List<String> lines = new ArrayList<>();
        try (InputStream is = getClass().getResourceAsStream("/iztro-truth.jsonl");
             BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String l;
            while ((l = br.readLine()) != null) if (!l.isBlank()) lines.add(l);
        }
        assertTrue(lines.size() >= 5, "应有至少 5 个对拍用例");

        for (String line : lines) {
            JsonNode root = om.readTree(line);
            String label = root.get("case").asText();
            // case 格式："YYYY-M-D t=T 男|女"
            String[] parts = label.split("[ =-]+");
            int y = Integer.parseInt(parts[0]);
            int m = Integer.parseInt(parts[1]);
            int d = Integer.parseInt(parts[2]);
            int t = Integer.parseInt(parts[4]);
            String g = parts[5].equals("男") ? "male" : "female";

            ZiweiChart chart = engine.generateChart(new BirthInfo(y, m, d, t, g));

            // 五行局
            assertEquals(feValueOf(root.get("fiveElementsClass").asText()), chart.wuxingJu,
                    label + " 五行局");
            // 命宫 / 紫微地支
            assertEquals(root.get("mingGongBranchIdx").asInt(), chart.mingGongBranch, label + " 命宫地支");
            assertEquals(root.get("ziweiBranchIdx").asInt(), chart.ziweiPos, label + " 紫微地支");

            // 按地支建索引
            Map<Integer, Palace> byBranch = new HashMap<>();
            for (Palace p : chart.palaces) byBranch.put(p.branch, p);

            for (JsonNode tp : root.get("palaces")) {
                int branch = tp.get("branch").asInt();
                Palace mp = byBranch.get(branch);
                assertTrue(mp != null, label + " 缺少地支 " + branch);

                // 宫名（iztro 用 命宫/兄弟/夫妻… 简称；引擎一致使用简称除命宫外）
                assertEquals(tp.get("name").asText(), mp.name, label + " 宫名@" + branch);
                // 宫干
                assertEquals(tp.get("stem").asText(), Constants.STEMS[mp.stem], label + " 宫干@" + branch);

                // 主星：名+四化+亮度映射（顺序一致）
                List<String> expMajor = new ArrayList<>();
                for (JsonNode s : tp.get("major")) expMajor.add(s.asText());
                List<String> actMajor = new ArrayList<>();
                for (Star s : mp.stars) {
                    if (!"major".equals(s.type)) continue;
                    String br = mapBrightnessToCn(s.brightness);
                    String hua = s.siHua != null ? "化" + s.siHua.cn() : "";
                    actMajor.add(s.name + br + hua);
                }
                assertEquals(normalizeMajor(expMajor), normalizeMajor(actMajor),
                        label + " 主星@" + branch);

                // 全部星曜名集合（major+minor+adj）应一致
                TreeSet<String> exp = new TreeSet<>();
                for (JsonNode s : tp.get("major")) exp.add(stripBrightnessHua(s.asText()));
                for (JsonNode s : tp.get("minor")) exp.add(s.asText());
                for (JsonNode s : tp.get("adj")) exp.add(s.asText());
                // iztro '年解' 属流耀/helper，algorithm.ts 仍并入；引擎用 '年解' 名
                TreeSet<String> act = new TreeSet<>();
                for (Star s : mp.stars) act.add(s.name);
                assertEquals(exp, act, label + " 星曜集合@" + branch);

                // 大限年龄段
                JsonNode dec = tp.get("decadal");
                if (dec != null && dec.isArray() && dec.size() == 2) {
                    assertTrue(mp.daXianAge != null, label + " 缺大限@" + branch);
                    assertEquals(dec.get(0).asInt(), mp.daXianAge[0], label + " 大限起@" + branch);
                    assertEquals(dec.get(1).asInt(), mp.daXianAge[1], label + " 大限止@" + branch);
                }
            }
        }
    }

    // 引擎主星亮度(bright/normal/dim) → 用于和 iztro 中文亮度映射后的结果比较：
    // 直接转中文不可逆，故比较时把双方都折算到 bright/normal/dim 维度。
    private static String mapBrightnessToCn(String engineBrightness) {
        // 引擎已是 bright/normal/dim；为对比，附加一个不可见占位，
        // 真正比较在 normalizeMajor 中统一折算。
        return engineBrightness == null ? "" : "@" + engineBrightness;
    }

    /** 把期望(中文亮度)与实际(引擎亮度)统一折算成 名|bright/normal/dim|化X 后比较 */
    private static List<String> normalizeMajor(List<String> items) {
        List<String> out = new ArrayList<>();
        for (String it : items) {
            // 形态1（期望）："武曲得化权"；形态2（实际）："武曲@normal化权"
            String name;
            String bright;
            String hua = "";
            int huaIdx = it.indexOf("化");
            String head = huaIdx >= 0 ? it.substring(0, huaIdx) : it;
            if (huaIdx >= 0) hua = it.substring(huaIdx); // 含"化X"
            if (head.contains("@")) {
                int at = head.indexOf('@');
                name = head.substring(0, at);
                bright = head.substring(at + 1);
            } else {
                // 期望：末尾可能带 1 个中文亮度字
                String b = head.length() >= 2 ? head.substring(head.length() - 1) : "";
                if (b.matches("[庙旺得利平不陷]")) {
                    name = head.substring(0, head.length() - 1);
                    bright = mapBrightness(b);
                } else {
                    name = head;
                    bright = "normal";
                }
            }
            out.add(name + "|" + bright + "|" + hua);
        }
        return out;
    }

    private static String stripBrightnessHua(String s) {
        int huaIdx = s.indexOf("化");
        String head = huaIdx >= 0 ? s.substring(0, huaIdx) : s;
        if (head.length() >= 2) {
            String b = head.substring(head.length() - 1);
            if (b.matches("[庙旺得利平不陷]")) return head.substring(0, head.length() - 1);
        }
        return head;
    }
}
