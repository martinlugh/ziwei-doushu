package com.ziwei.doushu.service;

import com.ziwei.doushu.model.Palace;
import com.ziwei.doushu.model.Pattern;
import com.ziwei.doushu.model.PatternCondition;
import com.ziwei.doushu.model.SiHua;
import com.ziwei.doushu.model.Star;
import com.ziwei.doushu.model.ZiweiChart;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 紫微斗数格局识别（v2 严格化版本）。严格对齐原项目 lib/ziwei/patterns.ts。
 *
 * 说明：原 TS 中若干 bonus 条件检测的是星「名」集合里是否含 "化禄"/"化权"/"化科"
 * 这类字符串，而盘面星名集合只含星曜名（四化是独立字段），故这些条件在原项目中
 * 恒不触发——这里原样移植以保证行为与参数完全一致。
 */
@Service
public class PatternsService {

    private static final List<String> SHA_NAMES = List.of("擎羊", "陀罗", "火星", "铃星", "地空", "地劫");
    private static final List<String> SHA_HARD = List.of("擎羊", "陀罗", "火星", "铃星");
    private static final List<String> SHA_KONG = List.of("地空", "地劫");
    private static final String[] BRANCH_NAMES = {"子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥"};

    // ────────────────── 辅助函数 ──────────────────
    private List<String> getMajorStarNames(Palace palace) {
        return palace.stars.stream().filter(s -> "major".equals(s.type)).map(s -> s.name).toList();
    }

    private Star findStar(Palace palace, String name) {
        return palace.stars.stream().filter(s -> name.equals(s.name)).findFirst().orElse(null);
    }

    private boolean hasStar(Palace palace, String name) {
        return palace.stars.stream().anyMatch(s -> name.equals(s.name));
    }

    private Palace findStarPalace(ZiweiChart chart, String name) {
        return chart.palaces.stream().filter(p -> p.stars.stream().anyMatch(s -> name.equals(s.name)))
                .findFirst().orElse(null);
    }

    private Palace getPalaceByBranch(ZiweiChart chart, int branch) {
        int b = ((branch % 12) + 12) % 12;
        return chart.palaces.stream().filter(p -> p.branch == b).findFirst().orElse(null);
    }

    private int shaCountInPalace(Palace palace, List<String> list) {
        return (int) palace.stars.stream().filter(s -> list.contains(s.name)).count();
    }

    private boolean hasShaInPalace(Palace palace, List<String> list) {
        return palace.stars.stream().anyMatch(s -> list.contains(s.name));
    }

    private List<Palace> getSanFangPalaces(ZiweiChart chart) {
        int m = chart.mingGongBranch;
        List<Integer> branches = List.of(m, (m + 4) % 12, (m + 8) % 12, (m + 6) % 12);
        return chart.palaces.stream().filter(p -> branches.contains(p.branch)).toList();
    }

    private boolean isInSanFang(ZiweiChart chart, int branch) {
        int m = chart.mingGongBranch;
        return List.of(m, (m + 4) % 12, (m + 8) % 12, (m + 6) % 12).contains(branch);
    }

    private Palace getDuiGong(ZiweiChart chart, int branch) {
        return getPalaceByBranch(chart, (branch + 6) % 12);
    }

    private Palace[] getJiaPalaces(ZiweiChart chart, int branch) {
        return new Palace[]{
                getPalaceByBranch(chart, (branch + 11) % 12),  // prev
                getPalaceByBranch(chart, (branch + 1) % 12),   // next
        };
    }

    private Set<String> sanFangAllStars(ZiweiChart chart) {
        Set<String> set = new LinkedHashSet<>();
        for (Palace p : getSanFangPalaces(chart)) {
            for (Star s : p.stars) set.add(s.name);
        }
        return set;
    }

    private int sanFangShaCount(ZiweiChart chart, List<String> list) {
        return getSanFangPalaces(chart).stream().mapToInt(p -> shaCountInPalace(p, list)).sum();
    }

    private boolean isBright(Palace palace, String starName) {
        Star s = findStar(palace, starName);
        return s != null && "bright".equals(s.brightness);
    }

    private boolean isDim(Palace palace, String starName) {
        Star s = findStar(palace, starName);
        return s != null && "dim".equals(s.brightness);
    }

    private SiHua getStarSiHua(Palace palace, String starName) {
        Star s = findStar(palace, starName);
        return s == null ? null : s.siHua;
    }

    private Pattern pat(String name, String level, String description, List<String> palaces,
                        PatternCondition conditions, String source) {
        Pattern p = new Pattern();
        p.name = name;
        p.level = level;
        p.description = description;
        p.palaces = palaces;
        p.conditions = conditions;
        p.source = source;
        return p;
    }

    // ────────────────── 正格识别器 ──────────────────

    /** 君臣庆会：紫微入命，左辅右弼同会（同宫或三方） */
    private void detectJunChenQingHui(ZiweiChart chart, Palace ming, List<Pattern> patterns) {
        if (!hasStar(ming, "紫微")) return;
        Set<String> sf = sanFangAllStars(chart);
        if (!sf.contains("左辅") || !sf.contains("右弼")) return;

        List<String> required = new ArrayList<>(List.of("紫微入命", "左辅右弼同会三方四正"));
        List<String> bonus = new ArrayList<>();
        List<String> breaking = new ArrayList<>();
        if (sf.contains("文昌") || sf.contains("文曲")) bonus.add("再会文昌或文曲");
        if (sf.contains("天魁") || sf.contains("天钺")) bonus.add("魁钺贵人加照");
        if (getStarSiHua(ming, "紫微") == SiHua.QUAN) bonus.add("紫微化权");
        if (sanFangShaCount(chart, SHA_KONG) >= 2) breaking.add("地空地劫双夹会照（紫微忌空劫）");

        patterns.add(pat("君臣庆会", breaking.isEmpty() ? "excellent" : "good",
                "紫微入命，左辅右弼同会，帝王得贤臣辅佐，主大富大贵、统御之命。一生贵人不绝，宜走政商高位、跨界领袖之途。",
                List.of("命宫"), new PatternCondition(required, bonus, breaking), "《紫微斗数全书·君臣庆会格》"));
    }

    /** 紫府同宫 */
    private void detectZiFu(ZiweiChart chart, Palace ming, List<Pattern> patterns) {
        Palace ziwei = findStarPalace(chart, "紫微");
        Palace tianfu = findStarPalace(chart, "天府");
        if (ziwei == null || tianfu == null || ziwei.branch != tianfu.branch) return;

        boolean inMing = ziwei.branch == chart.mingGongBranch;
        List<String> required = new ArrayList<>(inMing
                ? List.of("紫微天府同入命宫")
                : List.of("紫微天府同宫（不在命宫，会照减力）"));
        List<String> bonus = new ArrayList<>();
        List<String> breaking = new ArrayList<>();
        Set<String> sf = sanFangAllStars(chart);
        if (sf.contains("左辅") && sf.contains("右弼")) bonus.add("左辅右弼同会");
        if (sf.contains("文昌") || sf.contains("文曲")) bonus.add("再会昌曲");
        if (hasShaInPalace(ziwei, SHA_KONG)) breaking.add("紫府宫坐空劫（破紫府之贵气）");
        if (shaCountInPalace(ziwei, SHA_HARD) >= 2) breaking.add("紫府宫见双煞同坐");

        patterns.add(pat("紫府同宫", inMing && breaking.isEmpty() ? "excellent" : "good",
                inMing
                        ? "紫微天府同入命宫，帝相并临，尊贵之命。主品行端正、衣食无忧、有领导才能，宜担任要职。需要左右辅弼来配合方为完整大格。"
                        : "紫微天府同宫但未坐命，主一生有贵人贵气依托，但本身不一定大富贵，需看会照吉煞而定。",
                List.of(ziwei.name), new PatternCondition(required, bonus, breaking), "《紫微斗数全书·紫府同宫格》"));
    }

    /** 府相朝垣 */
    private void detectFuXiangChaoYuan(ZiweiChart chart, Palace ming, List<Pattern> patterns) {
        Palace tianfu = findStarPalace(chart, "天府");
        Palace tianxiang = findStarPalace(chart, "天相");
        if (tianfu == null || tianxiang == null) return;
        if (!isInSanFang(chart, tianfu.branch) || !isInSanFang(chart, tianxiang.branch)) return;
        if (tianfu.branch == chart.mingGongBranch && tianxiang.branch == chart.mingGongBranch) return;
        if (tianfu.branch == tianxiang.branch) return;

        List<String> required = new ArrayList<>(List.of("天府坐命三方", "天相坐命三方", "两星不同宫"));
        List<String> bonus = new ArrayList<>();
        List<String> breaking = new ArrayList<>();
        if (hasStar(ming, "禄存") || hasStar(ming, "化禄")) bonus.add("命宫见禄");
        if (sanFangAllStars(chart).contains("左辅")) bonus.add("再会左辅");
        if (hasShaInPalace(ming, SHA_HARD)) breaking.add("命宫坐煞星");
        if (sanFangShaCount(chart, SHA_HARD) >= 3) breaking.add("三方四正煞星过多");

        patterns.add(pat("府相朝垣", breaking.isEmpty() ? "excellent" : "good",
                "天府天相分守命宫三方四正，文武并济、权印双辉，主一生衣食丰足、地位崇高。古书云\"府相朝垣千钟食禄\"，常见于政界、企业管理者。",
                List.of(tianfu.name, tianxiang.name), new PatternCondition(required, bonus, breaking),
                "《紫微斗数全书·府相朝垣格》"));
    }

    /** 阳梁昌禄 */
    private void detectYangLiangChangLu(ZiweiChart chart, Palace ming, List<Pattern> patterns) {
        Set<String> sf = sanFangAllStars(chart);
        if (!sf.contains("太阳") || !sf.contains("天梁") || !sf.contains("文昌") || !sf.contains("禄存")) return;

        Palace sun = findStarPalace(chart, "太阳");
        Palace liang = findStarPalace(chart, "天梁");
        List<String> required = new ArrayList<>(List.of("太阳会命宫三方", "天梁会命宫三方", "文昌会命宫三方", "禄存会命宫三方"));
        List<String> bonus = new ArrayList<>();
        List<String> breaking = new ArrayList<>();
        if (isBright(sun, "太阳")) bonus.add("太阳庙旺");
        if (isBright(liang, "天梁")) bonus.add("天梁庙旺");
        if (sf.contains("化科")) bonus.add("再会化科");
        if (isDim(sun, "太阳")) breaking.add("太阳落陷（阳梁失辉）");
        if (sanFangShaCount(chart, SHA_HARD) >= 2) breaking.add("三方煞重");

        patterns.add(pat("阳梁昌禄", breaking.isEmpty() ? "excellent" : "good",
                "太阳、天梁、文昌、禄存四星齐会命宫三方，号称\"科举之星\"，主清贵显达、考运极佳，宜走学术、文教、研究、专业认证之路，一生功名易就。",
                List.of(sun.name, liang.name), new PatternCondition(required, bonus, breaking),
                "《紫微斗数全书·阳梁昌禄格》"));
    }

    /** 火贪格 / 铃贪格 */
    private void detectHuoTanLingTan(ZiweiChart chart, Palace ming, List<Pattern> patterns) {
        Palace tan = findStarPalace(chart, "贪狼");
        if (tan == null) return;
        Palace huo = findStarPalace(chart, "火星");
        Palace ling = findStarPalace(chart, "铃星");

        Object[][] pairs = {{"火星", huo}, {"铃星", ling}};
        for (Object[] pair : pairs) {
            String shaName = (String) pair[0];
            Palace shaPalace = (Palace) pair[1];
            if (shaPalace == null) continue;
            boolean sameOrTrine = tan.branch == shaPalace.branch
                    || (tan.branch + 4) % 12 == shaPalace.branch
                    || (tan.branch + 8) % 12 == shaPalace.branch
                    || (tan.branch + 6) % 12 == shaPalace.branch;
            if (!sameOrTrine) continue;
            if (!isInSanFang(chart, tan.branch)) continue;

            boolean same = tan.branch == shaPalace.branch;
            List<String> required = new ArrayList<>(List.of(
                    "贪狼" + (same ? "同宫" : "会照") + shaName, "贪狼会照命宫三方"));
            List<String> bonus = new ArrayList<>();
            List<String> breaking = new ArrayList<>();
            if (isBright(tan, "贪狼")) bonus.add("贪狼庙旺");
            if (getStarSiHua(tan, "贪狼") == SiHua.LU || getStarSiHua(tan, "贪狼") == SiHua.QUAN) bonus.add("贪狼化禄/化权");
            if (hasShaInPalace(tan, List.of("擎羊", "陀罗"))) breaking.add("贪狼宫又见羊陀（破横发之力）");
            if (hasShaInPalace(tan, SHA_KONG)) breaking.add("贪狼遇空劫（财来财去）");

            patterns.add(pat(shaName.equals("火星") ? "火贪格" : "铃贪格", breaking.isEmpty() ? "excellent" : "good",
                    "贪狼遇" + shaName + (same ? "同宫" : "三方会照") + "，主突发横财、突如其来的机遇。古书云“贪狼遇火铃，必发横财”，但来得快去得也快，宜见好就收。"
                            + (!breaking.isEmpty() ? "本盘破格条件已触发，发力打折。" : ""),
                    List.of(tan.name, shaPalace.name), new PatternCondition(required, bonus, breaking),
                    "《紫微斗数骨髓赋》"));
        }
    }

    /** 武贪格 */
    private void detectWuTan(ZiweiChart chart, Palace ming, List<Pattern> patterns) {
        Palace wu = findStarPalace(chart, "武曲");
        Palace tan = findStarPalace(chart, "贪狼");
        if (wu == null || tan == null) return;
        boolean sameOrOppose = wu.branch == tan.branch || (wu.branch + 6) % 12 == tan.branch;
        if (!sameOrOppose) return;
        if (!isInSanFang(chart, wu.branch) && !isInSanFang(chart, tan.branch)) return;

        List<String> required = new ArrayList<>(List.of(
                wu.branch == tan.branch ? "武曲贪狼同宫（丑/未）" : "武曲贪狼对宫拱照", "会照命宫三方"));
        List<String> bonus = new ArrayList<>();
        List<String> breaking = new ArrayList<>();
        if (sanFangAllStars(chart).contains("火星") || sanFangAllStars(chart).contains("铃星"))
            bonus.add("再遇火星/铃星（火贪/铃贪叠加）");
        if (getStarSiHua(wu, "武曲") == SiHua.LU) bonus.add("武曲化禄");
        if (hasShaInPalace(wu, List.of("擎羊", "陀罗"))) breaking.add("武贪宫见羊陀");
        if (hasShaInPalace(wu, SHA_KONG)) breaking.add("武贪宫遇空劫");

        patterns.add(pat("武贪格", breaking.isEmpty() ? "excellent" : "good",
                "武曲贪狼会命，财星与桃花欲望星交辉，古书云\"武贪不发少年人\"——三十岁后方能厚积薄发。主中年以后大富大贵，财源由人脉、应酬、欲望管理而来，适合金融、投机、销售、娱乐业。",
                List.of(wu.name, tan.name), new PatternCondition(required, bonus, breaking), "《紫微斗数骨髓赋》"));
    }

    /** 杀破狼 */
    private void detectShaPoLang(ZiweiChart chart, Palace ming, List<Pattern> patterns) {
        Set<String> sf = sanFangAllStars(chart);
        List<String> has = Arrays.asList("七杀", "破军", "贪狼").stream().filter(sf::contains).toList();
        if (has.size() < 3) return;

        List<String> required = new ArrayList<>(List.of("七杀、破军、贪狼三星齐入命宫三方四正"));
        List<String> bonus = new ArrayList<>();
        List<String> breaking = new ArrayList<>();
        if (sf.contains("化禄") || sf.contains("化权")) bonus.add("三方有化禄或化权（动得有力）");
        if (sf.contains("左辅") && sf.contains("右弼")) bonus.add("辅弼同会（变动中得贵人）");
        if (sanFangShaCount(chart, SHA_HARD) >= 3) breaking.add("煞星过重（动而无成）");
        if (hasShaInPalace(ming, SHA_KONG)) breaking.add("命坐空劫（动得辛苦）");

        List<String> palaces = getSanFangPalaces(chart).stream()
                .filter(p -> {
                    List<String> mn = getMajorStarNames(p);
                    return !mn.isEmpty() && has.contains(mn.get(0));
                }).map(p -> p.name).toList();

        patterns.add(pat("杀破狼", breaking.isEmpty() ? "good" : "caution",
                "七杀、破军、贪狼三星会命，开创闯荡之命格。一生变动多、不甘平凡，宜创业、军警、业务、销售。中年后才能稳定守成，年轻时易因冲动失利。",
                palaces, new PatternCondition(required, bonus, breaking), "《紫微斗数全书·杀破狼》"));
    }

    /** 机月同梁（四星齐） */
    private void detectJiYueTongLiang(ZiweiChart chart, Palace ming, List<Pattern> patterns) {
        Set<String> sf = sanFangAllStars(chart);
        List<String> has = Arrays.asList("天机", "太阴", "天同", "天梁").stream().filter(sf::contains).toList();
        if (has.size() < 4) return;

        List<String> required = new ArrayList<>(List.of("天机、太阴、天同、天梁四星齐入命宫三方四正"));
        List<String> bonus = new ArrayList<>();
        List<String> breaking = new ArrayList<>();
        if (sf.contains("文昌") || sf.contains("文曲")) bonus.add("再会昌曲");
        if (sf.contains("化科")) bonus.add("再会化科");
        if (sanFangShaCount(chart, SHA_HARD) >= 3) breaking.add("煞星过多（机月同梁忌煞）");
        if (hasShaInPalace(ming, SHA_HARD)) breaking.add("命宫坐煞");

        List<String> palaces = getSanFangPalaces(chart).stream()
                .filter(p -> has.stream().anyMatch(s -> getMajorStarNames(p).contains(s)))
                .map(p -> p.name).toList();

        patterns.add(pat("机月同梁", breaking.isEmpty() ? "excellent" : "good",
                "天机太阴天同天梁四星齐入命迁财官，文质彬彬、聪慧善谋。最适合公职、学术、文艺、医疗、服务等需稳定累积的行业，不宜大冒险大投机。",
                palaces, new PatternCondition(required, bonus, breaking), "《紫微斗数全书·机月同梁格》"));
    }

    /** 廉贞天相 */
    private void detectLianXiang(ZiweiChart chart, List<Pattern> patterns) {
        Palace lian = findStarPalace(chart, "廉贞");
        Palace xiang = findStarPalace(chart, "天相");
        if (lian == null || xiang == null || lian.branch != xiang.branch) return;

        boolean inMing = lian.branch == chart.mingGongBranch;
        List<String> required = new ArrayList<>(List.of("廉贞天相同宫"));
        List<String> bonus = new ArrayList<>();
        List<String> breaking = new ArrayList<>();
        if (hasStar(lian, "禄存") || getStarSiHua(lian, "廉贞") == SiHua.LU) bonus.add("见禄存或廉贞化禄");
        if (sanFangAllStars(chart).contains("左辅")) bonus.add("左辅会照");
        if (hasShaInPalace(lian, List.of("擎羊"))) breaking.add("廉相宫坐擎羊（廉杀羊倾向）");
        if (getStarSiHua(lian, "廉贞") == SiHua.JI) breaking.add("廉贞化忌");

        patterns.add(pat("廉贞天相格", breaking.isEmpty() ? (inMing ? "good" : "neutral") : "caution",
                "廉贞天相同宫，印绶格局，主秉公处事、清廉之名，宜任公职、行政管理、法务、企划。怕见擎羊化忌，则反主官非。",
                List.of(lian.name), new PatternCondition(required, bonus, breaking), "《紫微斗数全书》"));
    }

    /** 武曲七杀 */
    private void detectWuQiSha(ZiweiChart chart, List<Pattern> patterns) {
        Palace wu = findStarPalace(chart, "武曲");
        Palace qi = findStarPalace(chart, "七杀");
        if (wu == null || qi == null || wu.branch != qi.branch) return;

        boolean inMing = wu.branch == chart.mingGongBranch;
        List<String> required = new ArrayList<>(List.of("武曲七杀同宫"));
        List<String> bonus = new ArrayList<>();
        List<String> breaking = new ArrayList<>();
        if (getStarSiHua(wu, "武曲") == SiHua.QUAN) bonus.add("武曲化权");
        if (getStarSiHua(wu, "武曲") == SiHua.LU) bonus.add("武曲化禄");
        if (getStarSiHua(wu, "武曲") == SiHua.JI) breaking.add("武曲化忌（武曲化忌为财劫之兆）");
        if (hasShaInPalace(wu, List.of("擎羊", "陀罗", "火星", "铃星"))) breaking.add("武杀宫煞星过多");

        patterns.add(pat("武曲七杀", breaking.isEmpty() ? (inMing ? "excellent" : "good") : "caution",
                "武曲七杀同宫，将星配财星，主果决刚毅、理财能力强，适合金融、军警、创业。但忌见化忌煞星，否则凶险。一生奋斗、积财但操心。",
                List.of(wu.name), new PatternCondition(required, bonus, breaking), "《紫微斗数全书》"));
    }

    /** 天同天梁 */
    private void detectTongLiang(ZiweiChart chart, List<Pattern> patterns) {
        Palace tong = findStarPalace(chart, "天同");
        Palace liang = findStarPalace(chart, "天梁");
        if (tong == null || liang == null || tong.branch != liang.branch) return;

        List<String> required = new ArrayList<>(List.of("天同天梁同宫"));
        List<String> bonus = new ArrayList<>();
        List<String> breaking = new ArrayList<>();
        if (sanFangAllStars(chart).contains("文昌")) bonus.add("文昌会照");
        if (getStarSiHua(tong, "天同") == SiHua.LU) bonus.add("天同化禄");
        if (hasShaInPalace(tong, SHA_HARD)) breaking.add("煞星同坐");

        patterns.add(pat("天同天梁格", breaking.isEmpty() ? "good" : "neutral",
                "天同天梁同宫，福星与荫星共会，主宽厚和善、乐于助人，宜医疗、教育、宗教、社会公益。但偏温和保守，难成大富大贵之局。",
                List.of(tong.name), new PatternCondition(required, bonus, breaking), "《紫微斗数全书》"));
    }

    /** 日月同宫 */
    private void detectRiYueTongGong(ZiweiChart chart, List<Pattern> patterns) {
        Palace sun = findStarPalace(chart, "太阳");
        Palace moon = findStarPalace(chart, "太阴");
        if (sun == null || moon == null || sun.branch != moon.branch) return;
        if (sun.branch != 1 && sun.branch != 7) return;

        boolean inMing = sun.branch == chart.mingGongBranch;
        List<String> required = new ArrayList<>(List.of("太阳太阴同入" + BRANCH_NAMES[sun.branch] + "宫"));
        List<String> bonus = new ArrayList<>();
        List<String> breaking = new ArrayList<>();
        if (sun.branch == 7) bonus.add("未宫日月同辉（古书云未宫日月双美）");
        if (sanFangAllStars(chart).contains("文昌") && sanFangAllStars(chart).contains("文曲")) bonus.add("昌曲会照");
        if (hasShaInPalace(sun, SHA_HARD)) breaking.add("日月宫煞星同坐");

        patterns.add(pat("日月同宫", breaking.isEmpty() ? (inMing ? "excellent" : "good") : "good",
                "太阳太阴于" + BRANCH_NAMES[sun.branch] + "宫同宫，阴阳平衡，文武兼备。主异性缘佳、事业顺遂、名声远播。"
                        + (sun.branch == 7 ? "未宫日月双美尤佳。" : "丑宫日月同宫力量较平。"),
                List.of(sun.name), new PatternCondition(required, bonus, breaking), "《紫微斗数全书》"));
    }

    /** 日月夹命 */
    private void detectRiYueJiaMing(ZiweiChart chart, List<Pattern> patterns) {
        Palace[] jia = getJiaPalaces(chart, chart.mingGongBranch);
        Palace prev = jia[0], next = jia[1];
        if (prev == null || next == null) return;
        boolean prevHasSun = hasStar(prev, "太阳");
        boolean prevHasMoon = hasStar(prev, "太阴");
        boolean nextHasSun = hasStar(next, "太阳");
        boolean nextHasMoon = hasStar(next, "太阴");
        boolean ok = (prevHasSun && nextHasMoon) || (prevHasMoon && nextHasSun);
        if (!ok) return;

        Palace sunPalace = prevHasSun ? prev : next;
        Palace moonPalace = prevHasMoon ? prev : next;
        List<String> required = new ArrayList<>(List.of("太阳太阴分居命宫前后两宫"));
        List<String> bonus = new ArrayList<>();
        List<String> breaking = new ArrayList<>();
        if (isBright(sunPalace, "太阳")) bonus.add("太阳庙旺");
        if (isBright(moonPalace, "太阴")) bonus.add("太阴庙旺");
        if (isDim(sunPalace, "太阳") || isDim(moonPalace, "太阴")) breaking.add("日月落陷（夹命无光）");

        patterns.add(pat("日月夹命", breaking.isEmpty() ? "excellent" : "good",
                "太阳太阴分居命宫两侧夹照，光明磊落，一生贵人相助，事业蓬勃。男主官贵，女主旺夫兴家。日月须不落陷方为真夹。",
                List.of(sunPalace.name, moonPalace.name), new PatternCondition(required, bonus, breaking),
                "《紫微斗数全书·日月夹命》"));
    }

    /** 巨日同宫 */
    private void detectJuRiTongGong(ZiweiChart chart, List<Pattern> patterns) {
        Palace ju = findStarPalace(chart, "巨门");
        Palace sun = findStarPalace(chart, "太阳");
        if (ju == null || sun == null || ju.branch != sun.branch) return;
        if (ju.branch != 2 && ju.branch != 8) return;

        boolean inMing = ju.branch == chart.mingGongBranch;
        List<String> required = new ArrayList<>(List.of("巨门太阳同入" + BRANCH_NAMES[ju.branch] + "宫"));
        List<String> bonus = new ArrayList<>();
        List<String> breaking = new ArrayList<>();
        if (ju.branch == 2) bonus.add("寅宫太阳庙旺，巨门得日光化解是非");
        if (getStarSiHua(ju, "巨门") == SiHua.LU || getStarSiHua(ju, "巨门") == SiHua.QUAN) bonus.add("巨门化禄/化权（口才生财）");
        if (getStarSiHua(ju, "巨门") == SiHua.JI) breaking.add("巨门化忌（口舌官非）");
        if (ju.branch == 8) breaking.add("申宫太阳偏西，巨门暗曜更显");

        patterns.add(pat("巨日同宫", breaking.isEmpty() ? (inMing && ju.branch == 2 ? "excellent" : "good") : "caution",
                "巨门太阳同" + BRANCH_NAMES[ju.branch] + "宫，太阳化解巨门暗曜，主以口才、传媒、外语、专业立业。寅宫为佳，申宫力减。怕巨门化忌则官非。",
                List.of(ju.name), new PatternCondition(required, bonus, breaking), "《紫微斗数全书·巨日同宫》"));
    }

    /** 石中隐玉 */
    private void detectShiZhongYinYu(ZiweiChart chart, Palace ming, List<Pattern> patterns) {
        if (!hasStar(ming, "巨门")) return;
        if (ming.branch != 0 && ming.branch != 6) return;

        List<String> required = new ArrayList<>(List.of("巨门入命于" + BRANCH_NAMES[ming.branch] + "宫"));
        List<String> bonus = new ArrayList<>();
        List<String> breaking = new ArrayList<>();
        if (getStarSiHua(ming, "巨门") == SiHua.LU || getStarSiHua(ming, "巨门") == SiHua.QUAN) bonus.add("巨门化禄/化权");
        if (sanFangAllStars(chart).contains("文昌")) bonus.add("文昌会照（石中隐玉得明）");
        if (getStarSiHua(ming, "巨门") == SiHua.JI) breaking.add("巨门化忌（玉藏深泥）");
        if (hasShaInPalace(ming, SHA_HARD)) breaking.add("命坐煞星");

        patterns.add(pat("石中隐玉", breaking.isEmpty() ? "excellent" : "caution",
                "巨门坐命子午，外表平凡而内蕴才学。早年默默无闻、中年方显贵气，宜走专业、研究、口才、传媒。需有禄权或文昌相助方能\"凿石见玉\"。",
                List.of("命宫"), new PatternCondition(required, bonus, breaking), "《紫微斗数骨髓赋·石中隐玉》"));
    }

    /** 明珠出海 */
    private void detectMingZhuChuHai(ZiweiChart chart, Palace ming, List<Pattern> patterns) {
        if (ming.branch != 7) return;
        if (!getMajorStarNames(ming).isEmpty()) return;
        Palace dui = getDuiGong(chart, ming.branch);
        if (dui == null) return;
        if (!hasStar(dui, "太阳") || !hasStar(dui, "太阴")) return;

        List<String> required = new ArrayList<>(List.of("命宫在未为空宫", "对宫丑宫为太阳太阴同度"));
        List<String> bonus = new ArrayList<>();
        List<String> breaking = new ArrayList<>();
        if (sanFangAllStars(chart).contains("文昌") || sanFangAllStars(chart).contains("文曲")) bonus.add("再会昌曲");
        if (sanFangAllStars(chart).contains("左辅") || sanFangAllStars(chart).contains("右弼")) bonus.add("辅弼相助");
        if (sanFangShaCount(chart, SHA_HARD) >= 2) breaking.add("煞星会照（珠光黯淡）");

        patterns.add(pat("明珠出海", breaking.isEmpty() ? "excellent" : "good",
                "命未空宫，对宫丑宫日月同辉拱照，号\"明珠出海\"。主出生平凡、后天努力出头，宜远赴他乡、学术研究或大公司高位，主大富大贵。",
                List.of("命宫", dui.name), new PatternCondition(required, bonus, breaking), "《紫微斗数全集·明珠出海》"));
    }

    /** 紫微独坐入命 */
    private void detectZiWeiInMing(ZiweiChart chart, Palace ming, List<Pattern> patterns) {
        if (!hasStar(ming, "紫微") || hasStar(ming, "天府")) return;

        List<String> required = new ArrayList<>(List.of("紫微独坐命宫（无天府同坐）"));
        List<String> bonus = new ArrayList<>();
        List<String> breaking = new ArrayList<>();
        Set<String> sf = sanFangAllStars(chart);
        if (sf.contains("左辅") && sf.contains("右弼")) bonus.add("左辅右弼同会");
        if (sf.contains("文昌") && sf.contains("文曲")) bonus.add("文昌文曲同会");
        if (!sf.contains("左辅") && !sf.contains("右弼")) breaking.add("无辅弼（孤君无臣）");
        if (hasShaInPalace(ming, SHA_KONG)) breaking.add("紫微遇空劫（古书最忌）");

        patterns.add(pat("紫微入命", breaking.isEmpty() ? (!bonus.isEmpty() ? "excellent" : "good") : "caution",
                "紫微独坐命宫，帝王之星，自尊心强、有领导魅力。但紫微最忌\"在野孤君\"——若无左右辅弼相会，反成孤高自傲、易招毁谤。",
                List.of("命宫"), new PatternCondition(required, bonus, breaking), "《紫微斗数全书》"));
    }

    /** 辅弼夹命 */
    private void detectFuBiJiaMing(ZiweiChart chart, List<Pattern> patterns) {
        Palace[] jia = getJiaPalaces(chart, chart.mingGongBranch);
        Palace prev = jia[0], next = jia[1];
        if (prev == null || next == null) return;
        boolean prevHasZuo = hasStar(prev, "左辅");
        boolean prevHasYou = hasStar(prev, "右弼");
        boolean nextHasZuo = hasStar(next, "左辅");
        boolean nextHasYou = hasStar(next, "右弼");
        if (!((prevHasZuo && nextHasYou) || (prevHasYou && nextHasZuo))) return;

        List<String> required = new ArrayList<>(List.of("左辅右弼分居命宫前后两宫"));
        List<String> bonus = new ArrayList<>();
        List<String> breaking = new ArrayList<>();
        if (sanFangAllStars(chart).contains("天魁") || sanFangAllStars(chart).contains("天钺")) bonus.add("再会魁钺");

        patterns.add(pat("辅弼夹命", "excellent",
                "左辅右弼夹命，一生贵人不断、逢凶化吉。适合走仕途、大企业管理，有贵人提携之命。古书云\"左辅右弼，终身福厚\"。",
                List.of("命宫", prev.name, next.name), new PatternCondition(required, bonus, breaking),
                "《紫微斗数全书·辅弼夹命》"));
    }

    /** 昌曲夹命 */
    private void detectChangQuJiaMing(ZiweiChart chart, List<Pattern> patterns) {
        Palace[] jia = getJiaPalaces(chart, chart.mingGongBranch);
        Palace prev = jia[0], next = jia[1];
        if (prev == null || next == null) return;
        boolean prevHasChang = hasStar(prev, "文昌");
        boolean prevHasQu = hasStar(prev, "文曲");
        boolean nextHasChang = hasStar(next, "文昌");
        boolean nextHasQu = hasStar(next, "文曲");
        if (!((prevHasChang && nextHasQu) || (prevHasQu && nextHasChang))) return;

        patterns.add(pat("昌曲夹命", "excellent",
                "文昌文曲夹命宫，主聪明俊秀、文采斐然，宜走文教、学术、艺术、写作。古书云\"昌曲夹命主科甲\"，最利考运。",
                List.of("命宫", prev.name, next.name),
                new PatternCondition(new ArrayList<>(List.of("文昌文曲分居命宫前后两宫")), null, null),
                "《紫微斗数全书》"));
    }

    /** 魁钺夹命 */
    private void detectKuiYueJiaMing(ZiweiChart chart, List<Pattern> patterns) {
        Palace[] jia = getJiaPalaces(chart, chart.mingGongBranch);
        Palace prev = jia[0], next = jia[1];
        if (prev == null || next == null) return;
        boolean okA = hasStar(prev, "天魁") && hasStar(next, "天钺");
        boolean okB = hasStar(prev, "天钺") && hasStar(next, "天魁");
        if (!okA && !okB) return;

        patterns.add(pat("魁钺夹命", "good",
                "天魁天钺夹命，男称天乙、女称玉堂，一生贵人提携。考试、求职、关键时刻常有意外贵人相助。",
                List.of("命宫", prev.name, next.name),
                new PatternCondition(new ArrayList<>(List.of("天魁天钺分居命宫前后两宫")), null, null),
                "《紫微斗数全书》"));
    }

    /** 双禄朝垣 */
    private void detectShuangLuChaoYuan(ZiweiChart chart, Palace ming, List<Pattern> patterns) {
        List<Palace> sanFang = getSanFangPalaces(chart);
        boolean huaLuFound = false;
        boolean luCunFound = false;
        for (Palace p : sanFang) {
            if (p.stars.stream().anyMatch(s -> s.siHua == SiHua.LU)) huaLuFound = true;
            if (hasStar(p, "禄存")) luCunFound = true;
        }
        if (!huaLuFound || !luCunFound) return;

        List<String> breaking = hasShaInPalace(ming, SHA_KONG)
                ? new ArrayList<>(List.of("命坐空劫（双禄遇空，财来财去）")) : null;

        patterns.add(pat("双禄朝垣", "excellent",
                "化禄、禄存同会命宫三方四正，财源涌动、衣食丰足。古书云\"双禄朝垣，富比陶朱\"，主一生不愁财，多有正财横财兼得。",
                sanFang.stream().map(p -> p.name).toList(),
                new PatternCondition(new ArrayList<>(List.of("化禄会照三方四正", "禄存会照三方四正")), null, breaking),
                "《紫微斗数全书·双禄朝垣》"));
    }

    /** 三奇加会 */
    private void detectSanQiJiaHui(ZiweiChart chart, List<Pattern> patterns) {
        List<Palace> sf = getSanFangPalaces(chart);
        boolean lu = false, quan = false, ke = false;
        for (Palace p : sf) {
            for (Star s : p.stars) {
                if (s.siHua == SiHua.LU) lu = true;
                if (s.siHua == SiHua.QUAN) quan = true;
                if (s.siHua == SiHua.KE) ke = true;
            }
        }
        if (!(lu && quan && ke)) return;

        patterns.add(pat("三奇加会", "excellent",
                "化禄、化权、化科三吉化齐会命宫三方四正，号称\"三奇加会\"。主一生功名、财富、贵人三全，是紫微斗数最高吉格之一。",
                sf.stream().map(p -> p.name).toList(),
                new PatternCondition(new ArrayList<>(List.of("化禄、化权、化科三吉化齐会命宫三方四正")), null, null),
                "《紫微斗数全书·三奇加会》"));
    }

    /** 化禄入命 */
    private void detectHuaLuRuMing(ZiweiChart chart, Palace ming, List<Pattern> patterns) {
        Star huaLuStar = ming.stars.stream()
                .filter(s -> s.siHua == SiHua.LU && "major".equals(s.type)).findFirst().orElse(null);
        if (huaLuStar == null) return;

        String extra = "武曲".equals(huaLuStar.name) ? "武曲化禄属正财，宜实业、金融。"
                : "太阴".equals(huaLuStar.name) ? "太阴化禄属阴财、不动产。"
                : "贪狼".equals(huaLuStar.name) ? "贪狼化禄属人脉财、桃花财。" : "";

        patterns.add(pat(huaLuStar.name + "化禄入命", "good",
                huaLuStar.name + "化禄坐命，主生财顺利、人缘佳、机缘多。" + extra,
                List.of("命宫"),
                new PatternCondition(new ArrayList<>(List.of(huaLuStar.name + "化禄坐命宫")), null, null),
                "《紫微斗数全书》"));
    }

    // ────────────────── 恶格识别器 ──────────────────

    /** 化忌入命/迁 */
    private void detectHuaJiRuMingQian(ZiweiChart chart, List<Pattern> patterns) {
        int qianBranch = (chart.mingGongBranch + 6) % 12;
        for (Palace palace : chart.palaces) {
            if (palace.branch != chart.mingGongBranch && palace.branch != qianBranch) continue;
            Star jiStar = palace.stars.stream()
                    .filter(s -> s.siHua == SiHua.JI && "major".equals(s.type)).findFirst().orElse(null);
            if (jiStar == null) continue;

            boolean inMing = palace.branch == chart.mingGongBranch;
            patterns.add(pat(jiStar.name + "化忌入" + (inMing ? "命" : "迁"), "caution",
                    inMing
                            ? jiStar.name + "化忌坐命宫，需留意自身固执、心理障碍或健康隐患，凡事退一步思考。化忌不一定坏，代表此星能量需要特别关注。"
                            : jiStar.name + "化忌坐迁移宫，外出、远行、人际关系易有波折，宜守不宜动。",
                    List.of(palace.name),
                    new PatternCondition(new ArrayList<>(List.of(jiStar.name + "化忌坐" + (inMing ? "命" : "迁") + "宫")), null, null),
                    "《紫微斗数全书》"));
        }
    }

    /** 羊陀夹忌 */
    private void detectYangTuoJiaJi(ZiweiChart chart, List<Pattern> patterns) {
        for (Palace palace : chart.palaces) {
            Star jiStar = palace.stars.stream().filter(s -> s.siHua == SiHua.JI).findFirst().orElse(null);
            if (jiStar == null) continue;
            if (palace.branch != chart.mingGongBranch) continue;

            Palace[] jia = getJiaPalaces(chart, palace.branch);
            Palace prev = jia[0], next = jia[1];
            if (prev == null || next == null) continue;
            boolean aPrev = hasStar(prev, "擎羊") && hasStar(next, "陀罗");
            boolean aNext = hasStar(prev, "陀罗") && hasStar(next, "擎羊");
            if (!aPrev && !aNext) continue;

            patterns.add(pat("羊陀夹忌", "caution",
                    "化忌坐命，左右擎羊陀罗夹命，古书云\"羊陀夹忌为败局\"，主一生劳碌奔波、坎坷不顺、身心俱疲。需以德行修养与积极做事化解，凡事谨慎为上。",
                    List.of("命宫", prev.name, next.name),
                    new PatternCondition(new ArrayList<>(List.of("化忌坐命", "擎羊陀罗分居命宫前后两宫")), null, null),
                    "《紫微斗数骨髓赋·羊陀夹忌》"));
            return;
        }
    }

    /** 火铃夹命 */
    private void detectHuoLingJiaMing(ZiweiChart chart, List<Pattern> patterns) {
        Palace[] jia = getJiaPalaces(chart, chart.mingGongBranch);
        Palace prev = jia[0], next = jia[1];
        if (prev == null || next == null) return;
        boolean okA = hasStar(prev, "火星") && hasStar(next, "铃星");
        boolean okB = hasStar(prev, "铃星") && hasStar(next, "火星");
        if (!okA && !okB) return;

        patterns.add(pat("火铃夹命", "caution",
                "火星铃星分居命宫前后两宫夹命，主性急、易冲动、突发意外或纠纷。需培养耐性、避免冲动决策。",
                List.of("命宫", prev.name, next.name),
                new PatternCondition(new ArrayList<>(List.of("火星铃星分居命宫前后两宫")), null, null),
                "《紫微斗数全书》"));
    }

    /** 空劫夹命 */
    private void detectKongJieJiaMing(ZiweiChart chart, List<Pattern> patterns) {
        Palace[] jia = getJiaPalaces(chart, chart.mingGongBranch);
        Palace prev = jia[0], next = jia[1];
        if (prev == null || next == null) return;
        boolean okA = hasStar(prev, "地空") && hasStar(next, "地劫");
        boolean okB = hasStar(prev, "地劫") && hasStar(next, "地空");
        if (!okA && !okB) return;

        patterns.add(pat("空劫夹命", "caution",
                "地空地劫夹命，主财来财去、思想脱俗、易遁入宗教哲学。古书云\"空劫夹命，财不聚\"。宜技艺、宗教、研究等不重物质之业。",
                List.of("命宫", prev.name, next.name),
                new PatternCondition(new ArrayList<>(List.of("地空地劫分居命宫前后两宫")), null, null),
                "《紫微斗数全书》"));
    }

    /** 廉杀羊 */
    private void detectLianShaYang(ZiweiChart chart, List<Pattern> patterns) {
        Set<String> sf = sanFangAllStars(chart);
        if (!(sf.contains("廉贞") && sf.contains("七杀") && sf.contains("擎羊"))) return;

        patterns.add(pat("廉杀羊", "caution",
                "廉贞、七杀、擎羊三星会照命宫三方，古书警示之凶格。主血光、官非、意外。本命有此格不必惊慌，但流年大限再触发时需特别谨慎驾驶、避免冲突、注意手术风险。",
                List.of("命宫"),
                new PatternCondition(new ArrayList<>(List.of("廉贞、七杀、擎羊三星会照三方四正")), null, null),
                "《紫微斗数全书·廉杀羊》"));
    }

    /** 巨火羊 */
    private void detectJuHuoYang(ZiweiChart chart, List<Pattern> patterns) {
        Set<String> sf = sanFangAllStars(chart);
        if (!(sf.contains("巨门") && sf.contains("火星") && sf.contains("擎羊"))) return;

        patterns.add(pat("巨火羊", "caution",
                "巨门、火星、擎羊三星会照，古书云\"巨火羊，终身缢死\"——古时凶格。现代理解为：易因口舌、激烈冲突而招大祸。需修身养性、慎言慎行，避免极端情绪。",
                List.of("命宫"),
                new PatternCondition(new ArrayList<>(List.of("巨门、火星、擎羊三星会照三方四正")), null, null),
                "《紫微斗数骨髓赋·巨火羊》"));
    }

    /** 铃昌陀武 */
    private void detectLingChangTuoWu(ZiweiChart chart, List<Pattern> patterns) {
        Set<String> sf = sanFangAllStars(chart);
        if (!(sf.contains("铃星") && sf.contains("文昌") && sf.contains("陀罗") && sf.contains("武曲"))) return;

        patterns.add(pat("铃昌陀武", "caution",
                "铃星、文昌、陀罗、武曲四星齐会，古书云\"铃昌陀武，限至投河\"——古时大凶格。本命有此组合本身不必恐慌，但流年大限触发时需高度警觉重大决策、情绪起伏、水边活动。",
                List.of("命宫"),
                new PatternCondition(new ArrayList<>(List.of("铃星、文昌、陀罗、武曲四星会照三方四正")), null, null),
                "《紫微斗数骨髓赋·铃昌陀武》"));
    }

    /** 马头带箭 */
    private void detectMaTouDaiJian(ZiweiChart chart, Palace ming, List<Pattern> patterns) {
        if (ming.branch != 6) return;
        if (!hasStar(ming, "擎羊")) return;

        List<String> required = new ArrayList<>(List.of("擎羊于午宫坐命"));
        List<String> bonus = new ArrayList<>();
        if (sanFangAllStars(chart).contains("七杀") || sanFangAllStars(chart).contains("破军")) bonus.add("再会七杀或破军（武职大贵）");
        if (sanFangAllStars(chart).contains("天魁") || sanFangAllStars(chart).contains("天钺")) bonus.add("魁钺加照");

        patterns.add(pat("马头带箭", !bonus.isEmpty() ? "good" : "caution",
                "擎羊于午宫坐命，号\"马头带箭\"。古书云\"威镇边疆\"——主刚毅果决、有冲杀之力，宜军警武职、运动员、外科医师。但同时主危险与意外，需配合杀破狼或贵人方为大格，否则反主血光。",
                List.of("命宫"), new PatternCondition(required, bonus, null), "《紫微斗数骨髓赋·马头带箭》"));
    }

    // ────────────────── 基础格局 ──────────────────

    /** 禄存守身/守命 */
    private void detectLuCunShouShen(ZiweiChart chart, List<Pattern> patterns) {
        Palace luCunPalace = findStarPalace(chart, "禄存");
        if (luCunPalace == null) return;
        boolean inMing = luCunPalace.branch == chart.mingGongBranch;
        boolean inShen = luCunPalace.branch == chart.shenGongBranch;
        if (!inMing && !inShen) return;
        patterns.add(pat(inMing ? "禄存守命" : "禄存守身", "good",
                inMing
                        ? "禄存坐命，主一生衣食无忧、财禄稳定。性格保守，善积累，但羊陀夹禄须防小人。最宜配化禄、左辅右弼方为大格。"
                        : "禄存入身宫，主中年后财源稳定、得禄自享。倪师说「禄存入身，财气近身」——配偶或事业方向能带来稳定财禄。",
                List.of(inMing ? "命宫" : "身宫"),
                new PatternCondition(new ArrayList<>(List.of(inMing ? "禄存入命宫" : "禄存入身宫")), null, null),
                "《紫微斗数全书·禄存星》"));
    }

    /** 天马入命/在迁 */
    private void detectTianMaRuMing(ZiweiChart chart, List<Pattern> patterns) {
        Palace tianMaPalace = findStarPalace(chart, "天马");
        if (tianMaPalace == null) return;
        boolean inMing = tianMaPalace.branch == chart.mingGongBranch;
        boolean inQian = tianMaPalace.branch == ((chart.mingGongBranch + 6) % 12);
        if (!inMing && !inQian) return;
        patterns.add(pat(inMing ? "天马入命" : "天马在迁", "neutral",
                inMing
                        ? "天马坐命，主一生奔波、动中得财，宜走商旅、外勤、跨界发展。倪师说「天马入命，无禄不发」——若再会禄存或化禄即「禄马交驰」之富格。"
                        : "天马在迁移宫，主外出有利、远行得财，宜异乡发展。配化禄主异地生财，配煞星则旅途多波折。",
                List.of(tianMaPalace.name),
                new PatternCondition(new ArrayList<>(List.of(inMing ? "天马入命宫" : "天马入迁移宫")), null, null),
                "《紫微斗数全书·天马星》"));
    }

    /** 化禄入财 */
    private void detectHuaLuRuCai(ZiweiChart chart, List<Pattern> patterns) {
        Palace cai = chart.palaces.stream().filter(p -> "财帛".equals(p.name)).findFirst().orElse(null);
        if (cai == null) return;
        Star luStar = cai.stars.stream()
                .filter(s -> "major".equals(s.type) && s.siHua == SiHua.LU).findFirst().orElse(null);
        if (luStar == null) return;
        patterns.add(pat("化禄入财", "good",
                luStar.name + "化禄入财帛宫，主财源畅通、收入稳定。倪师讲化禄是「正财」象征——这个化禄星所代表的能力（" + luStar.name + "的核心特质）是你赚钱的主轴。配禄存或天马则财源更广。",
                List.of("财帛"),
                new PatternCondition(new ArrayList<>(List.of(luStar.name + "化禄入财帛宫")), null, null),
                "《紫微斗数全书·四化论》"));
    }

    /** 化权入官 */
    private void detectHuaQuanRuGuan(ZiweiChart chart, List<Pattern> patterns) {
        Palace guan = chart.palaces.stream().filter(p -> "官禄".equals(p.name)).findFirst().orElse(null);
        if (guan == null) return;
        Star quanStar = guan.stars.stream()
                .filter(s -> "major".equals(s.type) && s.siHua == SiHua.QUAN).findFirst().orElse(null);
        if (quanStar == null) return;
        patterns.add(pat("化权入官", "good",
                quanStar.name + "化权入官禄宫，主事业有掌控力、能担当独当一面的职位。化权代表权力与执行力——" + quanStar.name + "化权说明你在事业上能成为决策者或核心执行者，宜走管理或技术权威路线。",
                List.of("官禄"),
                new PatternCondition(new ArrayList<>(List.of(quanStar.name + "化权入官禄宫")), null, null),
                "《紫微斗数全书·四化论》"));
    }

    /** 化科入命/身 */
    private void detectHuaKeRuMingShen(ZiweiChart chart, List<Pattern> patterns) {
        Palace ming = chart.palaces.stream().filter(p -> p.branch == chart.mingGongBranch).findFirst().orElse(null);
        Palace shen = chart.palaces.stream().filter(p -> p.branch == chart.shenGongBranch).findFirst().orElse(null);
        List<Palace> target = new ArrayList<>();
        if (ming != null) target.add(ming);
        if (shen != null) target.add(shen);
        for (Palace p : target) {
            Star keStar = p.stars.stream()
                    .filter(s -> "major".equals(s.type) && s.siHua == SiHua.KE).findFirst().orElse(null);
            if (keStar == null) continue;
            boolean isMing = p.branch == chart.mingGongBranch;
            patterns.add(pat(isMing ? "化科入命" : "化科入身", "good",
                    keStar.name + "化科入" + (isMing ? "命" : "身") + "宫，主名声、文书、学术运。倪师讲化科是「贵人星」——" + keStar.name + "化科带来的是被人看重的特质，宜从事文书、教育、研究、咨询、文创等“以名取利”的方向。",
                    List.of(isMing ? "命宫" : "身宫"),
                    new PatternCondition(new ArrayList<>(List.of(keStar.name + "化科入" + (isMing ? "命" : "身") + "宫")), null, null),
                    "《紫微斗数全书·四化论》"));
            return;
        }
    }

    /** 机月同梁三星会（降级版） */
    private void detectJiYueTongLiangPartial(ZiweiChart chart, Palace ming, List<Pattern> patterns) {
        Set<String> sf = sanFangAllStars(chart);
        List<String> has = Arrays.asList("天机", "太阴", "天同", "天梁").stream().filter(sf::contains).toList();
        if (has.size() != 3) return;
        List<String> missing = Arrays.asList("天机", "太阴", "天同", "天梁").stream().filter(s -> !sf.contains(s)).toList();
        List<String> palaces = getSanFangPalaces(chart).stream()
                .filter(p -> has.stream().anyMatch(s -> getMajorStarNames(p).contains(s)))
                .map(p -> p.name).toList();
        patterns.add(pat("机月同梁三星会", "neutral",
                "三方四正会齐" + String.join("、", has) + "，差" + String.join("、", missing) + "未会。机月同梁不全格，文质带谋，但稳定度不如四星齐。仍宜公职、教研、医疗、服务等需要积累与稳定的行业，关键看缺位星与四化的配合。",
                palaces,
                new PatternCondition(new ArrayList<>(List.of("三方四正会" + String.join("、", has) + "（机月同梁缺" + String.join("、", missing) + "）")), null, null),
                "《紫微斗数全书·机月同梁格》（降级版）"));
    }

    /** 昌曲坐命/同会 */
    private void detectChangQuTongHui(ZiweiChart chart, List<Pattern> patterns) {
        Set<String> sf = sanFangAllStars(chart);
        if (!sf.contains("文昌") || !sf.contains("文曲")) return;
        Palace ming = chart.palaces.stream().filter(p -> p.branch == chart.mingGongBranch).findFirst().orElse(null);
        if (ming == null) return;
        boolean inMing = hasStar(ming, "文昌") && hasStar(ming, "文曲");
        patterns.add(pat(inMing ? "昌曲坐命" : "昌曲同会", "good",
                inMing
                        ? "文昌文曲同入命宫，主聪明俊秀、文采斐然，宜文学、教育、写作、咨询。最忌化忌——昌曲化忌主文书契约暗亏。"
                        : "文昌文曲同会三方四正，主才华横溢、口才文笔俱佳。宜走需要表达与文采的行业，化科加持则名声大显。",
                List.of("命宫"),
                new PatternCondition(new ArrayList<>(List.of("文昌、文曲同会命宫三方四正")), null, null),
                "《紫微斗数全书·文星论》"));
    }

    /** 辅弼同会 */
    private void detectFuBiTongHui(ZiweiChart chart, List<Pattern> patterns) {
        Set<String> sf = sanFangAllStars(chart);
        if (!sf.contains("左辅") || !sf.contains("右弼")) return;
        patterns.add(pat("辅弼同会", "good",
                "左辅右弼同会命宫三方四正，主一生贵人不绝、人缘极佳。最宜领导岗位与团队合作型工作。倪师说「辅弼夹命，平生贵人多」——你不是单打独斗的命，要善用人际网络。",
                List.of("命宫"),
                new PatternCondition(new ArrayList<>(List.of("左辅、右弼同会命宫三方四正")), null, null),
                "《紫微斗数全书·辅弼论》"));
    }

    /** 魁钺同会 */
    private void detectKuiYueTongHui(ZiweiChart chart, List<Pattern> patterns) {
        Set<String> sf = sanFangAllStars(chart);
        if (!sf.contains("天魁") || !sf.contains("天钺")) return;
        patterns.add(pat("魁钺同会", "good",
                "天魁天钺同会命宫三方四正，主\"天乙贵人\"加持，关键时刻总有贵人提携。倪师说「魁钺夹命，必为贵人」——遇到困难时身边会出现得力相助者，宜主动维护人脉。",
                List.of("命宫"),
                new PatternCondition(new ArrayList<>(List.of("天魁、天钺同会命宫三方四正")), null, null),
                "《紫微斗数全书·魁钺论》"));
    }

    /** 科权双会 */
    private void detectKeQuanShuangHui(ZiweiChart chart, List<Pattern> patterns) {
        List<Palace> sf = getSanFangPalaces(chart);
        boolean hasKe = false, hasQuan = false;
        for (Palace p : sf) {
            for (Star s : p.stars) {
                if ("major".equals(s.type) && s.siHua == SiHua.KE) hasKe = true;
                if ("major".equals(s.type) && s.siHua == SiHua.QUAN) hasQuan = true;
            }
        }
        if (!hasKe || !hasQuan) return;
        patterns.add(pat("科权双会", "good",
                "化科 + 化权 同会三方四正，主名权双美——既有学识/名声（科），又有掌控力（权），宜走\"专业权威\"路线（如医生、律师、教授、技术骨干），名利双收且根基扎实。",
                List.of("命宫"),
                new PatternCondition(new ArrayList<>(List.of("化科、化权同会命宫三方四正")), null, null),
                "《紫微斗数全书·四化会照》"));
    }

    // ────────────────── 主入口 ──────────────────
    public List<Pattern> detectPatterns(ZiweiChart chart) {
        List<Pattern> patterns = new ArrayList<>();
        Palace ming = chart.palaces.stream().filter(p -> p.branch == chart.mingGongBranch).findFirst().orElse(null);
        if (ming == null) return patterns;

        // 上格
        detectJunChenQingHui(chart, ming, patterns);
        detectZiFu(chart, ming, patterns);
        detectFuXiangChaoYuan(chart, ming, patterns);
        detectYangLiangChangLu(chart, ming, patterns);
        detectHuoTanLingTan(chart, ming, patterns);
        detectWuTan(chart, ming, patterns);
        detectShaPoLang(chart, ming, patterns);
        detectJiYueTongLiang(chart, ming, patterns);

        // 中格
        detectLianXiang(chart, patterns);
        detectWuQiSha(chart, patterns);
        detectTongLiang(chart, patterns);
        detectRiYueTongGong(chart, patterns);
        detectRiYueJiaMing(chart, patterns);
        detectJuRiTongGong(chart, patterns);
        detectShiZhongYinYu(chart, ming, patterns);
        detectMingZhuChuHai(chart, ming, patterns);
        detectZiWeiInMing(chart, ming, patterns);

        // 助力格
        detectFuBiJiaMing(chart, patterns);
        detectChangQuJiaMing(chart, patterns);
        detectKuiYueJiaMing(chart, patterns);
        detectShuangLuChaoYuan(chart, ming, patterns);
        detectSanQiJiaHui(chart, patterns);
        detectHuaLuRuMing(chart, ming, patterns);

        // 恶格
        detectHuaJiRuMingQian(chart, patterns);
        detectYangTuoJiaJi(chart, patterns);
        detectHuoLingJiaMing(chart, patterns);
        detectKongJieJiaMing(chart, patterns);
        detectLianShaYang(chart, patterns);
        detectJuHuoYang(chart, patterns);
        detectLingChangTuoWu(chart, patterns);
        detectMaTouDaiJian(chart, ming, patterns);

        // 基础格局
        detectLuCunShouShen(chart, patterns);
        detectTianMaRuMing(chart, patterns);
        detectHuaLuRuCai(chart, patterns);
        detectHuaQuanRuGuan(chart, patterns);
        detectHuaKeRuMingShen(chart, patterns);
        detectJiYueTongLiangPartial(chart, ming, patterns);
        detectChangQuTongHui(chart, patterns);
        detectFuBiTongHui(chart, patterns);
        detectKuiYueTongHui(chart, patterns);
        detectKeQuanShuangHui(chart, patterns);

        return patterns;
    }

    // ────────────────── 命宫摘要 ──────────────────
    public record MingGongSummary(List<String> stars, List<String> keywords, String nature) {
    }

    public MingGongSummary getMingGongSummary(ZiweiChart chart) {
        Palace mingPalace = chart.palaces.stream()
                .filter(p -> p.branch == chart.mingGongBranch).findFirst().orElse(null);
        if (mingPalace == null) return new MingGongSummary(List.of(), List.of(), "");

        List<String> starNames = mingPalace.stars.stream()
                .filter(s -> "major".equals(s.type)).map(s -> s.name).toList();

        java.util.Map<String, List<String>> keywordMap = new java.util.HashMap<>();
        keywordMap.put("紫微", List.of("尊贵", "独立", "领导"));
        keywordMap.put("天机", List.of("智慧", "机变", "善谋"));
        keywordMap.put("太阳", List.of("阳刚", "官贵", "慷慨"));
        keywordMap.put("武曲", List.of("财富", "刚毅", "果断"));
        keywordMap.put("天同", List.of("温和", "享福", "随缘"));
        keywordMap.put("廉贞", List.of("才艺", "桃花", "多变"));
        keywordMap.put("天府", List.of("财库", "稳重", "保守"));
        keywordMap.put("太阴", List.of("柔美", "财富", "细腻"));
        keywordMap.put("贪狼", List.of("欲望", "桃花", "多才"));
        keywordMap.put("巨门", List.of("善辩", "多思", "口才"));
        keywordMap.put("天相", List.of("辅佐", "行政", "稳健"));
        keywordMap.put("天梁", List.of("荫护", "医药", "长辈"));
        keywordMap.put("七杀", List.of("将星", "果决", "孤克"));
        keywordMap.put("破军", List.of("开创", "变动", "破旧"));

        java.util.Map<String, String> natureMap = new java.util.HashMap<>();
        natureMap.put("紫微", "帝王星");
        natureMap.put("天机", "智慧星");
        natureMap.put("太阳", "贵人星");
        natureMap.put("武曲", "财帛星");
        natureMap.put("天同", "福德星");
        natureMap.put("廉贞", "桃花星");
        natureMap.put("天府", "财库星");
        natureMap.put("太阴", "财富星");
        natureMap.put("贪狼", "桃花星");
        natureMap.put("巨门", "是非星");
        natureMap.put("天相", "印绶星");
        natureMap.put("天梁", "荫庇星");
        natureMap.put("七杀", "将帅星");
        natureMap.put("破军", "变动星");

        List<String> keywords = new ArrayList<>();
        for (String n : starNames) {
            keywords.addAll(keywordMap.getOrDefault(n, List.of()));
        }
        keywords = keywords.size() > 5 ? keywords.subList(0, 5) : keywords;
        String nature = !starNames.isEmpty() ? natureMap.getOrDefault(starNames.get(0), "") : "空宫";

        return new MingGongSummary(starNames, keywords, nature);
    }
}
