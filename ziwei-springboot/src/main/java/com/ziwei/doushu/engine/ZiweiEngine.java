package com.ziwei.doushu.engine;

import com.nlf.calendar.Lunar;
import com.nlf.calendar.Solar;
import com.ziwei.doushu.model.BirthInfo;
import com.ziwei.doushu.model.DaXian;
import com.ziwei.doushu.model.LunarInfo;
import com.ziwei.doushu.model.Palace;
import com.ziwei.doushu.model.SiHua;
import com.ziwei.doushu.model.Star;
import com.ziwei.doushu.model.ZiweiChart;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 紫微斗数排盘内核。
 *
 * 严格移植原项目 lib/ziwei/algorithm.ts 所依赖的 iztro 安星算法
 * （lib/star/location.js + lib/star/star.js + lib/astro/palace.js + lib/astro/astro.js + lib/utils/index.js）
 * 与 lib/ziwei/algorithm.ts 的组装/后处理逻辑，参数逐项对齐：
 *
 *  - 日历公历↔农历转换使用 lunar-javascript 的官方 Java 孪生库 cn.6tail:lunar（同源 6tail）
 *  - 年干/年支以"正月初一"为界（iztro 默认 yearDivide='normal' ≡ lunar.getYearGan()/getYearZhi()）
 *  - 安命身宫、定五行局、安紫微天府十四主星、安十四辅星、安杂耀、起大限，均与 iztro 一致
 *  - 星曜类型/亮度/四化的最终映射与 algorithm.ts 的 mapBrightness / mapStarType 一致
 *
 * 说明：原项目排盘时 timeIndex 恒为 0~11（晚子时 23:00 在 share.ts 中已按次日早子时处理），
 * 故此处不涉及 iztro 的 timeIndex===12（晚子时）分支。
 */
@Service
public class ZiweiEngine {

    // ─── 寅宫为 0 的宫位索引体系；地支名 → 子(0) 基准索引 ───────────────
    private static int ziOf(String name) {
        return switch (name) {
            case "zi" -> 0;
            case "chou" -> 1;
            case "yin" -> 2;
            case "mao" -> 3;
            case "chen" -> 4;
            case "si" -> 5;
            case "wu", "woo" -> 6;
            case "wei" -> 7;
            case "shen" -> 8;
            case "you" -> 9;
            case "xu" -> 10;
            case "hai" -> 11;
            default -> 0;
        };
    }

    /** 锁定索引到 0~max-1（对齐 utils.fixIndex；含 1/index===-Infinity → 0 的负零处理） */
    private static int fixIdx(int index, int max) {
        int i = index % max;
        if (i < 0) i += max;
        return i;
    }

    private static int fixIdx(int index) {
        return fixIdx(index, 12);
    }

    /** 地支相对寅宫的索引（对齐 utils.fixEarthlyBranchIndex） */
    private static int fixEB(String name) {
        return fixIdx(ziOf(name) - ziOf("yin"));
    }

    // ─── 五虎遁：年干 → 寅宫天干索引 ───────────────────────────────
    private static final int[] TIGER_START = {2, 4, 6, 8, 0, 2, 4, 6, 8, 0};

    // ─── 安紫微/天府诸星：从紫微逆排、从天府顺排（空串表示空宫不安） ──
    private static final String[] ZIWEI_GROUP = {"紫微", "天机", "", "太阳", "武曲", "天同", "", "", "廉贞"};
    private static final String[] TIANFU_GROUP = {"天府", "太阴", "贪狼", "巨门", "天相", "天梁", "七杀", "", "", "", "破军"};

    /**
     * 十四主星亮度表（已按 algorithm.ts mapBrightness 折算为 bright/normal/dim）。
     * 数据来自 iztro lib/data/stars.js 的 STARS_INFO.brightness（庙旺→bright、陷不→dim、其余→normal），
     * 数组下标为宫位位置（0=寅, 1=卯, …, 11=丑）。
     */
    private static final Map<String, String[]> MAJOR_BRIGHTNESS = Map.ofEntries(
            Map.entry("紫微", new String[]{"bright", "bright", "normal", "bright", "bright", "bright", "bright", "bright", "normal", "bright", "normal", "bright"}),
            Map.entry("天机", new String[]{"normal", "bright", "normal", "normal", "bright", "dim", "normal", "bright", "normal", "normal", "bright", "dim"}),
            Map.entry("太阳", new String[]{"bright", "bright", "bright", "bright", "bright", "normal", "normal", "dim", "dim", "dim", "dim", "dim"}),
            Map.entry("武曲", new String[]{"normal", "normal", "bright", "normal", "bright", "bright", "normal", "normal", "bright", "normal", "bright", "bright"}),
            Map.entry("天同", new String[]{"normal", "normal", "normal", "bright", "dim", "dim", "bright", "normal", "normal", "bright", "bright", "dim"}),
            Map.entry("廉贞", new String[]{"bright", "normal", "normal", "dim", "normal", "normal", "bright", "normal", "normal", "dim", "normal", "normal"}),
            Map.entry("天府", new String[]{"bright", "normal", "bright", "normal", "bright", "bright", "normal", "bright", "bright", "normal", "bright", "bright"}),
            Map.entry("太阴", new String[]{"bright", "dim", "dim", "dim", "dim", "dim", "normal", "dim", "bright", "bright", "bright", "bright"}),
            Map.entry("贪狼", new String[]{"normal", "normal", "bright", "dim", "bright", "bright", "normal", "normal", "bright", "dim", "bright", "bright"}),
            Map.entry("巨门", new String[]{"bright", "bright", "dim", "bright", "bright", "dim", "bright", "bright", "dim", "bright", "bright", "dim"}),
            Map.entry("天相", new String[]{"bright", "dim", "normal", "normal", "bright", "normal", "bright", "dim", "normal", "normal", "bright", "bright"}),
            Map.entry("天梁", new String[]{"bright", "bright", "bright", "dim", "bright", "bright", "dim", "normal", "bright", "dim", "bright", "bright"}),
            Map.entry("七杀", new String[]{"bright", "bright", "bright", "normal", "bright", "bright", "bright", "bright", "bright", "normal", "bright", "bright"}),
            Map.entry("破军", new String[]{"normal", "dim", "bright", "normal", "bright", "bright", "normal", "dim", "bright", "normal", "bright", "bright"})
    );

    /** 星名 → 四化（按年干，对齐 utils.getMutagen + Constants.SI_HUA_TABLE） */
    private static SiHua mutagenOf(String star, int ganIdx) {
        if (ganIdx < 0 || ganIdx > 9) return null;
        String[] arr = Constants.SI_HUA_TABLE[ganIdx];
        if (star.equals(arr[0])) return SiHua.LU;
        if (star.equals(arr[1])) return SiHua.QUAN;
        if (star.equals(arr[2])) return SiHua.KE;
        if (star.equals(arr[3])) return SiHua.JI;
        return null;
    }

    // ─── 命宫/身宫 ─────────────────────────────────────────────────
    private record SoulBody(int soulIndex, int bodyIndex, int soulStemIdx, int soulBranchZi) {
    }

    private SoulBody getSoulAndBody(int yearGanIdx, int monthIndex, int timeIndex) {
        int soulIndex = fixIdx(monthIndex - timeIndex);
        int bodyIndex = fixIdx(monthIndex + timeIndex);
        int startHS = TIGER_START[yearGanIdx];
        int soulStemIdx = fixIdx(startHS + soulIndex, 10);
        int soulBranchZi = fixIdx(soulIndex + ziOf("yin")); // 命宫地支（子基准）
        return new SoulBody(soulIndex, bodyIndex, soulStemIdx, soulBranchZi);
    }

    // ─── 定五行局（以命宫干支）→ 局数 2/3/4/5/6 ───────────────────
    private int fiveElementsValue(int soulStemIdx, int soulBranchZi) {
        int hsNumber = soulStemIdx / 2 + 1;
        int ebNumber = fixIdx(soulBranchZi, 6) / 2 + 1;
        int index = hsNumber + ebNumber;
        while (index > 5) index -= 5;
        // table = [wood3rd(3), metal4th(4), water2nd(2), fire6th(6), earth5th(5)]
        int[] values = {3, 4, 2, 6, 5};
        return values[index - 1];
    }

    // ─── 起紫微/天府 ───────────────────────────────────────────────
    private int[] getStartIndex(int lunarDay, int fiveElementsValue) {
        int remainder;
        int quotient;
        int offset = -1;
        int day = lunarDay; // timeIndex 恒 < 12，无需 +1 / 跨月修正
        do {
            offset++;
            int divisor = day + offset;
            quotient = divisor / fiveElementsValue;
            remainder = divisor % fiveElementsValue;
        } while (remainder != 0);
        quotient %= 12;
        int ziweiIndex = quotient - 1;
        if (offset % 2 == 0) {
            ziweiIndex += offset;
        } else {
            ziweiIndex -= offset;
        }
        ziweiIndex = fixIdx(ziweiIndex);
        int tianfuIndex = fixIdx(12 - ziweiIndex);
        return new int[]{ziweiIndex, tianfuIndex};
    }

    // ─── 临时星结构（按宫位位置 0=寅 收集） ───────────────────────
    private static final class S {
        final String name;
        final String type;
        final String brightness;
        final SiHua siHua;

        S(String name, String type, String brightness, SiHua siHua) {
            this.name = name;
            this.type = type;
            this.brightness = brightness;
            this.siHua = siHua;
        }
    }

    private static void push(List<List<S>> arr, int idx, S s) {
        arr.get(fixIdx(idx)).add(s);
    }

    private static List<List<S>> initStars() {
        List<List<S>> a = new ArrayList<>(12);
        for (int i = 0; i < 12; i++) a.add(new ArrayList<>());
        return a;
    }

    // ─── 安主星 ────────────────────────────────────────────────────
    private List<List<S>> getMajorStar(int ziweiIndex, int tianfuIndex, int yearGanIdx) {
        List<List<S>> stars = initStars();
        for (int i = 0; i < ZIWEI_GROUP.length; i++) {
            String s = ZIWEI_GROUP[i];
            if (s.isEmpty()) continue;
            int idx = fixIdx(ziweiIndex - i);
            push(stars, idx, new S(s, "major", MAJOR_BRIGHTNESS.get(s)[idx], mutagenOf(s, yearGanIdx)));
        }
        for (int i = 0; i < TIANFU_GROUP.length; i++) {
            String s = TIANFU_GROUP[i];
            if (s.isEmpty()) continue;
            int idx = fixIdx(tianfuIndex + i);
            push(stars, idx, new S(s, "major", MAJOR_BRIGHTNESS.get(s)[idx], mutagenOf(s, yearGanIdx)));
        }
        return stars;
    }

    // 禄存/擎羊/陀罗/天马
    private int[] getLuYangTuoMaIndex(int ganIdx, int branchZi) {
        int luIndex = switch (ganIdx) {
            case 0 -> fixEB("yin");
            case 1 -> fixEB("mao");
            case 2, 4 -> fixEB("si");
            case 3, 5 -> fixEB("woo");
            case 6 -> fixEB("shen");
            case 7 -> fixEB("you");
            case 8 -> fixEB("hai");
            case 9 -> fixEB("zi");
            default -> -1;
        };
        int maIndex = switch (branchZi) {
            case 2, 6, 10 -> fixEB("shen"); // 寅午戌
            case 8, 0, 4 -> fixEB("yin");   // 申子辰
            case 5, 9, 1 -> fixEB("hai");   // 巳酉丑
            case 11, 3, 7 -> fixEB("si");   // 亥卯未
            default -> 0;
        };
        return new int[]{luIndex, maIndex, fixIdx(luIndex + 1), fixIdx(luIndex - 1)};
    }

    // 天魁/天钺（按年干）
    private int[] getKuiYueIndex(int ganIdx) {
        return switch (ganIdx) {
            case 0, 4, 6 -> new int[]{fixEB("chou"), fixEB("wei")};
            case 1, 5 -> new int[]{fixEB("zi"), fixEB("shen")};
            case 7 -> new int[]{fixEB("woo"), fixEB("yin")};
            case 2, 3 -> new int[]{fixEB("hai"), fixEB("you")};
            case 8, 9 -> new int[]{fixEB("mao"), fixEB("si")};
            default -> new int[]{-1, -1};
        };
    }

    // 左辅/右弼（按农历月）
    private int[] getZuoYouIndex(int lunarMonth) {
        return new int[]{fixIdx(fixEB("chen") + (lunarMonth - 1)), fixIdx(fixEB("xu") - (lunarMonth - 1))};
    }

    // 文昌/文曲（按时支）
    private int[] getChangQuIndex(int timeIndex) {
        return new int[]{fixIdx(fixEB("xu") - fixIdx(timeIndex)), fixIdx(fixEB("chen") + fixIdx(timeIndex))};
    }

    // 地空/地劫（按时支）
    private int[] getKongJieIndex(int timeIndex) {
        int t = fixIdx(timeIndex);
        int hai = fixEB("hai");
        return new int[]{fixIdx(hai - t), fixIdx(hai + t)};
    }

    // 火星/铃星（按年支 + 时支）
    private int[] getHuoLingIndex(int branchZi, int timeIndex) {
        int t = fixIdx(timeIndex);
        int huo, ling;
        switch (branchZi) {
            case 2, 6, 10 -> { huo = fixEB("chou") + t; ling = fixEB("mao") + t; }   // 寅午戌
            case 8, 0, 4 -> { huo = fixEB("yin") + t; ling = fixEB("xu") + t; }       // 申子辰
            case 5, 9, 1 -> { huo = fixEB("mao") + t; ling = fixEB("xu") + t; }       // 巳酉丑
            case 11, 7, 3 -> { huo = fixEB("you") + t; ling = fixEB("xu") + t; }      // 亥未卯
            default -> { huo = -1; ling = -1; }
        }
        return new int[]{fixIdx(huo), fixIdx(ling)};
    }

    // 红鸾/天喜（按年支）
    private int[] getLuanXiIndex(int branchZi) {
        int hong = fixIdx(fixEB("mao") - branchZi);
        return new int[]{hong, fixIdx(hong + 6)};
    }

    // ─── 安十四辅星 ────────────────────────────────────────────────
    private List<List<S>> getMinorStar(int yearGanIdx, int yearBranchZi, int monthIndex, int timeIndex) {
        List<List<S>> stars = initStars();
        int[] zuoyou = getZuoYouIndex(monthIndex + 1);
        int[] changqu = getChangQuIndex(timeIndex);
        int[] kuiyue = getKuiYueIndex(yearGanIdx);
        int[] huoling = getHuoLingIndex(yearBranchZi, timeIndex);
        int[] kongjie = getKongJieIndex(timeIndex);
        int[] lytm = getLuYangTuoMaIndex(yearGanIdx, yearBranchZi);
        int luIndex = lytm[0], maIndex = lytm[1], yangIndex = lytm[2], tuoIndex = lytm[3];

        // 左辅右弼文昌文曲：lucky，带四化
        push(stars, zuoyou[0], new S("左辅", "lucky", null, mutagenOf("左辅", yearGanIdx)));
        push(stars, zuoyou[1], new S("右弼", "lucky", null, mutagenOf("右弼", yearGanIdx)));
        push(stars, changqu[0], new S("文昌", "lucky", null, mutagenOf("文昌", yearGanIdx)));
        push(stars, changqu[1], new S("文曲", "lucky", null, mutagenOf("文曲", yearGanIdx)));
        // 天魁天钺禄存天马：lucky，无四化
        push(stars, kuiyue[0], new S("天魁", "lucky", null, null));
        push(stars, kuiyue[1], new S("天钺", "lucky", null, null));
        push(stars, luIndex, new S("禄存", "lucky", null, null));
        push(stars, maIndex, new S("天马", "lucky", null, null));
        // 地空地劫火星铃星擎羊陀罗：sha
        push(stars, kongjie[0], new S("地空", "sha", null, null));
        push(stars, kongjie[1], new S("地劫", "sha", null, null));
        push(stars, huoling[0], new S("火星", "sha", null, null));
        push(stars, huoling[1], new S("铃星", "sha", null, null));
        push(stars, yangIndex, new S("擎羊", "sha", null, null));
        push(stars, tuoIndex, new S("陀罗", "sha", null, null));
        return stars;
    }

    // ─── 安杂耀（全部映射为 minor，对齐 algorithm.ts） ───────────────
    private List<List<S>> getAdjectiveStar(int yearGanIdx, int yearBranchZi, int soulIndex, int bodyIndex,
                                           int monthIndex, int lunarDay, int timeIndex) {
        List<List<S>> stars = initStars();
        int b = yearBranchZi;
        int g = yearGanIdx;

        // 年系
        int[] hgxc = getHuagaiXianchi(b);
        int[] gugua = getGuGua(b);
        int tiancai = fixIdx(soulIndex + b);
        int tianshou = fixIdx(bodyIndex + b);
        int tianchu = fixIdx(fixEB(new String[]{"si", "woo", "zi", "si", "woo", "shen", "yin", "woo", "you", "hai"}[g]));
        int posui = fixIdx(fixEB(new String[]{"si", "chou", "you"}[b % 3]));
        int feilian = fixIdx(fixEB(new String[]{"shen", "you", "xu", "si", "woo", "wei", "yin", "mao", "chen", "hai", "zi", "chou"}[b]));
        int longchi = fixIdx(fixEB("chen") + b);
        int fengge = fixIdx(fixEB("xu") - b);
        int tianku = fixIdx(fixEB("woo") - b);
        int tianxu = fixIdx(fixEB("woo") + b);
        int tianguan = fixIdx(fixEB(new String[]{"wei", "chen", "si", "yin", "mao", "you", "hai", "you", "xu", "woo"}[g]));
        int tianfu = fixIdx(fixEB(new String[]{"you", "shen", "zi", "hai", "mao", "yin", "woo", "si", "woo", "si"}[g]));
        int tiande = fixIdx(fixEB("you") + b);
        int yuede = fixIdx(fixEB("si") + b);
        int tiankong = fixIdx(fixEB(branchZiName(b)) + 1);
        int jielu = fixIdx(fixEB(new String[]{"shen", "woo", "chen", "yin", "zi"}[g % 5]));
        int kongwang = fixIdx(fixEB(new String[]{"you", "wei", "si", "mao", "chou"}[g % 5]));
        int xunkong = fixIdx(fixEB(branchZiName(b)) + (9 - g) + 1);
        int yinyang = b % 2;
        if (yinyang != xunkong % 2) xunkong = fixIdx(xunkong + 1);
        int guchen = gugua[0], guasu = gugua[1];

        // 天使天伤（通用派别，不互换）
        int tianshang = fixIdx(5 + soulIndex);  // friendsPalace=5
        int tianshi = fixIdx(7 + soulIndex);    // healthPalace=7

        // 月系
        int monthIdx = monthIndex;
        int jieshen = fixIdx(fixEB(new String[]{"shen", "xu", "zi", "yin", "chen", "woo"}[monthIdx / 2]));
        int tianyao = fixIdx(fixEB("chou") + monthIdx);
        int tianxing = fixIdx(fixEB("you") + monthIdx);
        int yinsha = fixIdx(fixEB(new String[]{"yin", "zi", "xu", "shen", "woo", "chen"}[monthIdx % 6]));
        int tianyue = fixIdx(fixEB(new String[]{"xu", "si", "chen", "yin", "wei", "mao", "hai", "wei", "yin", "woo", "xu", "yin"}[monthIdx]));
        int tianwu = fixIdx(fixEB(new String[]{"si", "shen", "yin", "hai"}[monthIdx % 4]));

        // 日系
        int[] zy = getZuoYouIndex(monthIndex + 1);
        int[] cq = getChangQuIndex(timeIndex);
        int dayIndex = lunarDay - 1; // timeIndex < 12
        int santai = fixIdx((zy[0] + dayIndex) % 12);
        int bazuo = fixIdx((zy[1] - dayIndex) % 12);
        int enguang = fixIdx(((cq[0] + dayIndex) % 12) - 1);
        int tiangui = fixIdx(((cq[1] + dayIndex) % 12) - 1);

        // 时系
        int taifu = fixIdx(fixEB("woo") + fixIdx(timeIndex));
        int fenggao = fixIdx(fixEB("yin") + fixIdx(timeIndex));

        // 红鸾天喜（年支）
        int[] lx = getLuanXiIndex(b);
        // 年解（按年支）：解神从戌起子，逆数至太岁；对齐 location.js getNianjieIndex
        int nianjie = fixIdx(fixEB(new String[]{
                "xu", "you", "shen", "wei", "woo", "si", "chen", "mao", "yin", "chou", "zi", "hai"}[b]));

        push(stars, nianjie, adj("年解"));
        push(stars, lx[0], adj("红鸾"));
        push(stars, lx[1], adj("天喜"));
        push(stars, tianyao, adj("天姚"));
        push(stars, hgxc[1], adj("咸池"));
        push(stars, jieshen, adj("解神"));
        push(stars, santai, adj("三台"));
        push(stars, bazuo, adj("八座"));
        push(stars, enguang, adj("恩光"));
        push(stars, tiangui, adj("天贵"));
        push(stars, longchi, adj("龙池"));
        push(stars, fengge, adj("凤阁"));
        push(stars, tiancai, adj("天才"));
        push(stars, tianshou, adj("天寿"));
        push(stars, taifu, adj("台辅"));
        push(stars, fenggao, adj("封诰"));
        push(stars, tianwu, adj("天巫"));
        push(stars, hgxc[0], adj("华盖"));
        push(stars, tianguan, adj("天官"));
        push(stars, tianfu, adj("天福"));
        push(stars, tianchu, adj("天厨"));
        push(stars, tianyue, adj("天月"));
        push(stars, tiande, adj("天德"));
        push(stars, yuede, adj("月德"));
        push(stars, tiankong, adj("天空"));
        push(stars, xunkong, adj("旬空"));
        push(stars, jielu, adj("截路"));
        push(stars, kongwang, adj("空亡"));
        push(stars, guchen, adj("孤辰"));
        push(stars, guasu, adj("寡宿"));
        push(stars, feilian, adj("蜚廉"));
        push(stars, posui, adj("破碎"));
        push(stars, tianxing, adj("天刑"));
        push(stars, yinsha, adj("阴煞"));
        push(stars, tianku, adj("天哭"));
        push(stars, tianxu, adj("天虚"));
        push(stars, tianshi, adj("天使"));
        push(stars, tianshang, adj("天伤"));
        return stars;
    }

    private static S adj(String name) {
        return new S(name, "minor", null, null);
    }

    private static String branchZiName(int b) {
        return new String[]{"zi", "chou", "yin", "mao", "chen", "si", "wu", "wei", "shen", "you", "xu", "hai"}[b];
    }

    private int[] getHuagaiXianchi(int b) {
        return switch (b) {
            case 2, 6, 10 -> new int[]{fixEB("xu"), fixEB("mao")};
            case 8, 0, 4 -> new int[]{fixEB("chen"), fixEB("you")};
            case 5, 9, 1 -> new int[]{fixEB("chou"), fixEB("woo")};
            case 11, 7, 3 -> new int[]{fixEB("wei"), fixEB("zi")};
            default -> new int[]{-1, -1};
        };
    }

    private int[] getGuGua(int b) {
        return switch (b) {
            case 2, 3, 4 -> new int[]{fixEB("si"), fixEB("chou")};
            case 5, 6, 7 -> new int[]{fixEB("shen"), fixEB("chen")};
            case 8, 9, 10 -> new int[]{fixEB("hai"), fixEB("wei")};
            case 11, 0, 1 -> new int[]{fixEB("yin"), fixEB("xu")};
            default -> new int[]{-1, -1};
        };
    }

    // ─── 月份索引（对齐 utils.fixLunarMonthIndex） ────────────────
    private int fixLunarMonthIndex(int lunarMonth, int lunarDay, boolean isLeap, boolean fixLeap, int timeIndex) {
        boolean needToAdd = isLeap && fixLeap && lunarDay > 15 && timeIndex != 12;
        return fixIdx(lunarMonth + 1 - ziOf("yin") + (needToAdd ? 1 : 0));
    }

    // ─── 起大限：返回每个宫位位置(0=寅)的年龄段 [start,end]；无则 null ──
    private int[][] getDecadals(int soulIndex, int yearBranchZi, int fiveElementsValue, String gender) {
        int[][] decadals = new int[12][];
        boolean forward; // 阳男阴女顺行
        boolean branchYang = (yearBranchZi % 2 == 0);
        boolean male = "male".equals(gender);
        forward = (male == branchYang);
        for (int i = 0; i < 12; i++) {
            int idx = forward ? fixIdx(soulIndex + i) : fixIdx(soulIndex - i);
            int start = fiveElementsValue + 10 * i;
            decadals[idx] = new int[]{start, start + 9};
        }
        return decadals;
    }

    // ═══════════════════════ 主入口 ═══════════════════════════════
    public ZiweiChart generateChart(BirthInfo birthInfo) {
        int year = birthInfo.year, month = birthInfo.month, day = birthInfo.day;
        int timeIndex = birthInfo.hour; // 0~11
        String gender = birthInfo.gender;

        Lunar lunar = Solar.fromYmd(year, month, day).getLunar();
        int yearGanIdx = indexOf(Constants.STEMS, lunar.getYearGan());
        int yearBranchZi = indexOf(Constants.BRANCHES, lunar.getYearZhi());
        if (yearGanIdx < 0) yearGanIdx = 0;
        if (yearBranchZi < 0) yearBranchZi = 0;
        int rawMonth = lunar.getMonth();
        int lunarMonth = Math.abs(rawMonth);
        boolean isLeap = rawMonth < 0;
        int lunarDay = lunar.getDay();

        int monthIndex = fixLunarMonthIndex(lunarMonth, lunarDay, isLeap, true, timeIndex);
        SoulBody sb = getSoulAndBody(yearGanIdx, monthIndex, timeIndex);
        int feValue = fiveElementsValue(sb.soulStemIdx(), sb.soulBranchZi());
        String wuxingJuName = Constants.JU_NAMES.get(feValue);

        int[] zt = getStartIndex(lunarDay, feValue);
        int ziweiIndex = zt[0], tianfuIndex = zt[1];

        List<List<S>> majors = getMajorStar(ziweiIndex, tianfuIndex, yearGanIdx);
        List<List<S>> minors = getMinorStar(yearGanIdx, yearBranchZi, monthIndex, timeIndex);
        List<List<S>> adjs = getAdjectiveStar(yearGanIdx, yearBranchZi, sb.soulIndex(), sb.bodyIndex(),
                monthIndex, lunarDay, timeIndex);

        int[][] decadals = getDecadals(sb.soulIndex(), yearBranchZi, feValue, gender);

        // ── 宫名：从寅宫(位置0)开始，命宫位于 soulIndex ──
        String[] palaceNames = palaceNames(sb.soulIndex());
        int startHS = TIGER_START[yearGanIdx];

        // ── 组装十二宫（按宫位位置 0=寅 → branch=fixIdx(2+i)）──
        List<Palace> palaces = new ArrayList<>(12);
        for (int i = 0; i < 12; i++) {
            Palace p = new Palace();
            p.branch = fixIdx(2 + i);                 // 子基准地支索引
            p.stem = fixIdx(startHS + i, 10);
            p.name = palaceNames[i];
            p.stars = new ArrayList<>();
            for (S s : majors.get(i)) p.stars.add(toStar(s));
            for (S s : minors.get(i)) p.stars.add(toStar(s));
            for (S s : adjs.get(i)) p.stars.add(toStar(s));
            if (decadals[i] != null) p.daXianAge = new int[]{decadals[i][0], decadals[i][1]};
            p.isMingGong = "命宫".equals(p.name);
            p.isShenGong = (sb.bodyIndex() == i);
            p.isCurrentDaXian = false;
            palaces.add(p);
        }

        int currentYear = LocalDate.now().getYear();
        int currentAge = currentYear - year;

        for (Palace p : palaces) {
            if (p.daXianAge != null && currentAge >= p.daXianAge[0] && currentAge <= p.daXianAge[1]) {
                p.isCurrentDaXian = true;
            }
        }

        // ── 借对宫结构化字段（对齐 algorithm.ts P0）──
        for (Palace p : palaces) {
            p.oppositeBranch = (p.branch + 6) % 12;
            long mainCount = p.stars.stream().filter(s -> "major".equals(s.type)).count();
            p.isEmpty = mainCount == 0;
            if (p.isEmpty) {
                final int ob = p.oppositeBranch;
                Palace opp = palaces.stream().filter(q -> q.branch == ob).findFirst().orElse(null);
                if (opp != null) {
                    p.borrowedFromBranch = opp.branch;
                    p.borrowedFromName = opp.name;
                    p.borrowedStars = opp.stars.stream().filter(s -> "major".equals(s.type)).map(s -> s.name).toList();
                }
            }
        }

        int mingGongBranch = fixIdx(sb.soulIndex() + 2);
        int shenGongBranch = fixIdx(sb.bodyIndex() + 2);

        Palace ziweiPalace = palaces.stream()
                .filter(p -> p.stars.stream().anyMatch(s -> "紫微".equals(s.name) && "major".equals(s.type)))
                .findFirst().orElse(null);
        int ziweiPos = ziweiPalace != null ? ziweiPalace.branch : 0;

        List<DaXian> daXians = palaces.stream()
                .filter(p -> p.daXianAge != null)
                .sorted((a, c) -> Integer.compare(a.daXianAge[0], c.daXianAge[0]))
                .map(p -> new DaXian(p.daXianAge[0], p.daXianAge[1], p.branch, p.name))
                .toList();
        daXians = new ArrayList<>(daXians);

        int currentDaXianIndex = -1;
        for (int i = 0; i < daXians.size(); i++) {
            DaXian dx = daXians.get(i);
            if (currentAge >= dx.startAge && currentAge <= dx.endAge) {
                currentDaXianIndex = i;
                break;
            }
        }

        // ── 农历信息（对齐 getLunarInfo）──
        LunarInfo lunarInfo = new LunarInfo();
        lunarInfo.lunarYear = lunar.getYear();
        lunarInfo.lunarMonth = lunarMonth;
        lunarInfo.lunarDay = lunarDay;
        lunarInfo.yearStem = yearGanIdx;
        lunarInfo.yearBranch = yearBranchZi;
        lunarInfo.isLeapMonth = isLeap;

        ZiweiChart chart = new ZiweiChart();
        chart.birthInfo = birthInfo;
        chart.lunarInfo = lunarInfo;
        chart.mingGongBranch = mingGongBranch;
        chart.shenGongBranch = shenGongBranch;
        chart.wuxingJu = feValue;
        chart.wuxingJuName = wuxingJuName;
        chart.ziweiPos = ziweiPos;
        chart.palaces = palaces;
        chart.daXians = daXians;
        chart.currentAge = currentAge;
        chart.currentDaXianIndex = currentDaXianIndex;
        return chart;
    }

    /** 农历信息（兼容保留，对齐 algorithm.ts getLunarInfo） */
    public LunarInfo getLunarInfo(int year, int month, int day) {
        Lunar lunar = Solar.fromYmd(year, month, day).getLunar();
        int yearStem = indexOf(Constants.STEMS, lunar.getYearGan());
        int yearBranch = indexOf(Constants.BRANCHES, lunar.getYearZhi());
        int rawMonth = lunar.getMonth();
        LunarInfo info = new LunarInfo();
        info.lunarYear = lunar.getYear();
        info.lunarMonth = Math.abs(rawMonth);
        info.lunarDay = lunar.getDay();
        info.yearStem = yearStem >= 0 ? yearStem : 0;
        info.yearBranch = yearBranch >= 0 ? yearBranch : 0;
        info.isLeapMonth = rawMonth < 0;
        return info;
    }

    private Star toStar(S s) {
        return new Star(s.name, s.type, s.brightness, s.siHua);
    }

    private static int indexOf(String[] arr, String v) {
        for (int i = 0; i < arr.length; i++) if (arr[i].equals(v)) return i;
        return -1;
    }

    // 从寅宫(位置0)开始的十二宫名；命宫位于 soulIndex
    private static final String[] PALACES_ORDER = {
            "命宫", "父母", "福德", "田宅", "官禄", "仆役",
            "迁移", "疾厄", "财帛", "子女", "夫妻", "兄弟"
    };

    private static String[] palaceNames(int soulIndex) {
        String[] names = new String[12];
        for (int i = 0; i < 12; i++) {
            int idx = fixIdx(i - soulIndex);
            names[i] = PALACES_ORDER[idx];
        }
        return names;
    }
}
