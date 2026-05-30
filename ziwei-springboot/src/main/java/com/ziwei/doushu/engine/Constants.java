package com.ziwei.doushu.engine;

import java.util.List;
import java.util.Map;

/**
 * 常量表。严格对齐原项目 lib/ziwei/constants.ts 的全部参数。
 */
public final class Constants {

    private Constants() {
    }

    /** 天干 Heavenly Stems */
    public static final String[] STEMS = {"甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸"};

    /** 地支 Earthly Branches */
    public static final String[] BRANCHES = {"子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥"};

    /** 十二宫名，从命宫顺时针 */
    public static final String[] PALACE_NAMES_ORDER = {
            "命宫", "兄弟宫", "夫妻宫", "子女宫", "财帛宫", "疾厄宫",
            "迁移宫", "交友宫", "官禄宫", "田宅宫", "福德宫", "父母宫"
    };

    /** 纳音五行（30组干支对的五行） */
    public static final String[] NAYIN_ELEMENTS = {
            "金", "火", "木", "土", "金", "火", "水", "土", "金", "木",
            "水", "土", "火", "木", "水", "金", "火", "木", "土", "金",
            "火", "水", "土", "金", "木", "水", "土", "火", "木", "水"
    };

    /** 五行 → 局数 */
    public static final Map<String, Integer> ELEMENT_TO_JU = Map.of(
            "水", 2, "木", 3, "金", 4, "土", 5, "火", 6
    );

    /** 局数名称 */
    public static final Map<Integer, String> JU_NAMES = Map.of(
            2, "水二局", 3, "木三局", 4, "金四局", 5, "土五局", 6, "火六局"
    );

    /** 四化表（年干 → [化禄, 化权, 化科, 化忌]） */
    public static final String[][] SI_HUA_TABLE = {
            {"廉贞", "破军", "武曲", "太阳"},   // 甲 0
            {"天机", "天梁", "紫微", "太阴"},   // 乙 1
            {"天同", "天机", "文昌", "廉贞"},   // 丙 2
            {"太阴", "天同", "天机", "巨门"},   // 丁 3
            {"贪狼", "太阴", "右弼", "天机"},   // 戊 4
            {"武曲", "贪狼", "天梁", "文曲"},   // 己 5
            {"太阳", "武曲", "太阴", "天同"},   // 庚 6
            {"巨门", "太阳", "文曲", "文昌"},   // 辛 7
            {"天梁", "紫微", "左辅", "武曲"},   // 壬 8
            {"破军", "巨门", "太阴", "贪狼"},   // 癸 9
    };

    /** 天魁天钺表（年干 → [天魁branch, 天钺branch]） */
    public static final int[][] TIANKUI_TABLE = {
            {1, 7},   // 甲: 魁丑 钺未
            {0, 8},   // 乙: 魁子 钺申
            {11, 9},  // 丙: 魁亥 钺酉
            {11, 9},  // 丁: 魁亥 钺酉
            {1, 7},   // 戊: 魁丑 钺未
            {0, 8},   // 己: 魁子 钺申
            {1, 7},   // 庚: 魁丑 钺未
            {6, 2},   // 辛: 魁午 钺寅
            {3, 5},   // 壬: 魁卯 钺巳
            {3, 5},   // 癸: 魁卯 钺巳
    };

    /** 禄存表（年干 → 禄存branch） */
    public static final int[] LUCUN_TABLE = {
            2,  // 甲: 寅
            3,  // 乙: 卯
            5,  // 丙: 巳
            6,  // 丁: 午
            5,  // 戊: 巳
            6,  // 己: 午
            8,  // 庚: 申
            9,  // 辛: 酉
            11, // 壬: 亥
            0,  // 癸: 子
    };

    /** 天马表（年支三合 → 天马branch）。寅午戌→申, 申子辰→寅, 巳酉丑→亥, 亥卯未→巳 */
    public static final Map<Integer, Integer> TIANMA_TABLE = Map.ofEntries(
            Map.entry(2, 8), Map.entry(6, 8), Map.entry(10, 8),
            Map.entry(8, 2), Map.entry(0, 2), Map.entry(4, 2),
            Map.entry(5, 11), Map.entry(9, 11), Map.entry(1, 11),
            Map.entry(11, 5), Map.entry(3, 5), Map.entry(7, 5)
    );

    /**
     * 主星亮度表 [星名][branch] → 'bright' | 'normal' | 'dim'
     * 严格对齐 constants.ts 的 STAR_BRIGHTNESS（仅这 6 颗有显式表）。
     * 注：原项目排盘主流程实际使用 iztro 给出的亮度（庙旺→bright、陷不→dim、其余→normal），
     * 该表为 constants.ts 中保留的备用映射，原样移植以保持参数一致。
     */
    public static final Map<String, Map<Integer, String>> STAR_BRIGHTNESS = Map.of(
            "紫微", Map.ofEntries(
                    Map.entry(2, "bright"), Map.entry(5, "bright"), Map.entry(8, "bright"), Map.entry(11, "bright"),
                    Map.entry(1, "normal"), Map.entry(4, "normal"), Map.entry(7, "bright"), Map.entry(10, "normal"),
                    Map.entry(0, "normal"), Map.entry(3, "dim"), Map.entry(6, "dim"), Map.entry(9, "normal")),
            "天机", Map.ofEntries(
                    Map.entry(5, "bright"), Map.entry(11, "bright"), Map.entry(3, "bright"), Map.entry(9, "bright"),
                    Map.entry(1, "normal"), Map.entry(7, "normal"), Map.entry(2, "dim"), Map.entry(8, "dim"),
                    Map.entry(0, "normal"), Map.entry(4, "normal"), Map.entry(6, "normal"), Map.entry(10, "normal")),
            "太阳", Map.ofEntries(
                    Map.entry(3, "bright"), Map.entry(4, "bright"), Map.entry(5, "bright"), Map.entry(6, "bright"),
                    Map.entry(7, "normal"), Map.entry(8, "normal"), Map.entry(9, "normal"), Map.entry(10, "dim"),
                    Map.entry(11, "dim"), Map.entry(0, "dim"), Map.entry(1, "dim"), Map.entry(2, "normal")),
            "武曲", Map.ofEntries(
                    Map.entry(2, "bright"), Map.entry(5, "bright"), Map.entry(8, "bright"), Map.entry(11, "bright"),
                    Map.entry(0, "normal"), Map.entry(3, "normal"), Map.entry(6, "normal"), Map.entry(9, "normal"),
                    Map.entry(1, "dim"), Map.entry(4, "dim"), Map.entry(7, "dim"), Map.entry(10, "dim")),
            "天同", Map.ofEntries(
                    Map.entry(0, "bright"), Map.entry(3, "bright"), Map.entry(6, "bright"), Map.entry(9, "bright"),
                    Map.entry(2, "normal"), Map.entry(5, "normal"), Map.entry(8, "normal"), Map.entry(11, "normal"),
                    Map.entry(1, "dim"), Map.entry(4, "dim"), Map.entry(7, "dim"), Map.entry(10, "dim")),
            "廉贞", Map.ofEntries(
                    Map.entry(2, "bright"), Map.entry(5, "bright"), Map.entry(8, "bright"), Map.entry(11, "bright"),
                    Map.entry(0, "normal"), Map.entry(3, "normal"), Map.entry(6, "normal"), Map.entry(9, "normal"),
                    Map.entry(1, "dim"), Map.entry(4, "dim"), Map.entry(7, "dim"), Map.entry(10, "dim"))
    );

    /** 时辰：地支索引 / 名称 / 时间范围 */
    public static final List<int[]> SHICHEN_BRANCH = List.of(); // 占位，详见 ShiChen 工具
}
