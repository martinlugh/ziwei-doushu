package com.ziwei.doushu.service;

import com.ziwei.doushu.engine.Constants;
import com.ziwei.doushu.model.Palace;
import com.ziwei.doushu.model.SelfSihuaMark;
import com.ziwei.doushu.model.SiHua;
import com.ziwei.doushu.model.Star;
import com.ziwei.doushu.model.ZiweiChart;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 四化工具模块。严格对齐原项目 lib/ziwei/sihua.ts。
 *
 * 倪海厦《天纪》体系核心：
 *   本命四化 = 出生年天干四化（静态基础）
 *   大限四化 = 大限宫宫干（非本命年干）的四化（十年动态）
 *   流年四化 = 当年年干的四化（一年动态）
 *   自化     = 某宫的宫干四化，其中被化星恰在本宫
 *   来因宫   = 某颗化星的"动力来源宫"——即宫干引发该化的宫位
 */
@Service
public class SiHuaService {

    /** 天干索引 0-9 → { 禄, 权, 科, 忌 } 对应星名 */
    public Map<SiHua, String> getSiHuaByStem(int stemIndex) {
        Map<SiHua, String> r = new LinkedHashMap<>();
        if (stemIndex < 0 || stemIndex > 9) {
            r.put(SiHua.LU, "");
            r.put(SiHua.QUAN, "");
            r.put(SiHua.KE, "");
            r.put(SiHua.JI, "");
            return r;
        }
        String[] arr = Constants.SI_HUA_TABLE[stemIndex];
        r.put(SiHua.LU, arr[0]);
        r.put(SiHua.QUAN, arr[1]);
        r.put(SiHua.KE, arr[2]);
        r.put(SiHua.JI, arr[3]);
        return r;
    }

    /** 星名 → 四化类型（由某天干确定） */
    public Map<String, SiHua> buildStarSiHuaMap(int stemIndex) {
        Map<String, SiHua> r = new LinkedHashMap<>();
        if (stemIndex < 0 || stemIndex > 9) return r;
        String[] arr = Constants.SI_HUA_TABLE[stemIndex];
        r.put(arr[0], SiHua.LU);
        r.put(arr[1], SiHua.QUAN);
        r.put(arr[2], SiHua.KE);
        r.put(arr[3], SiHua.JI);
        return r;
    }

    /** 公历年份 → 年柱天干索引（0=甲, ... 9=癸） */
    public int getYearStemIndex(int year) {
        return (((year - 4) % 10) + 10) % 10;
    }

    /** 公历年份 → 年柱地支索引（0=子, ... 11=亥） */
    public int getYearBranchIndex(int year) {
        return (((year - 4) % 12) + 12) % 12;
    }

    public record StemTransforms(int stemIndex, String stemName, Map<SiHua, String> transforms) {
    }

    /** 大限宫干四化 */
    public StemTransforms getDaXianSiHua(ZiweiChart chart, int dxIndex) {
        if (dxIndex < 0 || dxIndex >= chart.daXians.size()) return null;
        var dx = chart.daXians.get(dxIndex);
        Palace dxPalace = chart.palaces.stream()
                .filter(p -> p.branch == dx.palaceBranch).findFirst().orElse(null);
        if (dxPalace == null) return null;
        int stemIndex = dxPalace.stem;
        return new StemTransforms(stemIndex, stem(stemIndex), getSiHuaByStem(stemIndex));
    }

    /** 流年四化 */
    public StemTransforms getLiuNianSiHua(int year) {
        int stemIndex = getYearStemIndex(year);
        return new StemTransforms(stemIndex, stem(stemIndex), getSiHuaByStem(stemIndex));
    }

    /**
     * 流月天干（五虎遁：甲己年起丙寅、乙庚年起戊寅、丙辛年起庚寅、丁壬年起壬寅、戊癸年起甲寅）
     * month: 农历月 1-12
     */
    public int getLiuYueStemIndex(int yearStem, int month) {
        // 五虎遁：正月（寅月）天干
        int yinStem;
        switch (yearStem) {
            case 0, 5 -> yinStem = 2; // 甲己 → 丙
            case 1, 6 -> yinStem = 4; // 乙庚 → 戊
            case 2, 7 -> yinStem = 6; // 丙辛 → 庚
            case 3, 8 -> yinStem = 8; // 丁壬 → 壬
            case 4, 9 -> yinStem = 0; // 戊癸 → 甲
            default -> yinStem = 0;
        }
        return (yinStem + ((month - 1) % 12) + 10) % 10;
    }

    public StemTransforms getLiuYueSiHua(int yearStem, int month) {
        int stemIndex = getLiuYueStemIndex(yearStem, month);
        return new StemTransforms(stemIndex, stem(stemIndex), getSiHuaByStem(stemIndex));
    }

    /**
     * 自化：该宫宫干引发的四化，被化星恰在本宫。
     */
    public List<SelfSihuaMark> detectSelfSihua(Palace palace) {
        Map<SiHua, String> transforms = getSiHuaByStem(palace.stem);
        List<SelfSihuaMark> found = new ArrayList<>();
        List<String> palaceStarNames = palace.stars.stream().map(s -> s.name).toList();
        for (SiHua sh : new SiHua[]{SiHua.LU, SiHua.QUAN, SiHua.KE, SiHua.JI}) {
            String starName = transforms.get(sh);
            if (starName != null && !starName.isEmpty() && palaceStarNames.contains(starName)) {
                found.add(new SelfSihuaMark(sh, starName));
            }
        }
        return found;
    }

    /**
     * 来因宫追溯：对某颗星某种化，追溯是哪个宫的宫干"飞"过来的。
     */
    public List<Palace> findIncomingPalaces(ZiweiChart chart, String starName, SiHua sihua) {
        List<Palace> result = new ArrayList<>();
        for (Palace p : chart.palaces) {
            Map<SiHua, String> transforms = getSiHuaByStem(p.stem);
            if (starName.equals(transforms.get(sihua))) {
                result.add(p);
            }
        }
        return result;
    }

    /** 批量计算盘面所有宫位的自化列表 */
    public Map<Integer, List<SelfSihuaMark>> buildAllSelfSihua(ZiweiChart chart) {
        Map<Integer, List<SelfSihuaMark>> result = new LinkedHashMap<>();
        for (Palace p : chart.palaces) {
            List<SelfSihuaMark> list = detectSelfSihua(p);
            if (!list.isEmpty()) result.put(p.branch, list);
        }
        return result;
    }

    /** 某星名 → 多层四化的合成视图（本命/大限/流年/流月） */
    public Map<String, SiHua> buildOverlayForStar(String starName,
                                                  Map<String, SiHua> nativeMap,
                                                  Map<String, SiHua> daXianMap,
                                                  Map<String, SiHua> liuNianMap,
                                                  Map<String, SiHua> liuYueMap) {
        Map<String, SiHua> r = new LinkedHashMap<>();
        if (nativeMap != null) r.put("native", nativeMap.get(starName));
        if (daXianMap != null) r.put("daXian", daXianMap.get(starName));
        if (liuNianMap != null) r.put("liuNian", liuNianMap.get(starName));
        if (liuYueMap != null) r.put("liuYue", liuYueMap.get(starName));
        return r;
    }

    private String stem(int idx) {
        return (idx >= 0 && idx < Constants.STEMS.length) ? Constants.STEMS[idx] : "";
    }
}
