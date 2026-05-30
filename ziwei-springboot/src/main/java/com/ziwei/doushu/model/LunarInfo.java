package com.ziwei.doushu.model;

/**
 * 农历信息。对齐原项目 types.ts: LunarInfo
 */
public class LunarInfo {
    public int lunarYear;
    public int lunarMonth;   // 正数=正常月，原项目用 Math.abs(rawMonth)（恒为正）
    public int lunarDay;
    public int yearStem;     // 0-9 (甲乙丙丁戊己庚辛壬癸)
    public int yearBranch;   // 0-11 (子丑寅卯辰巳午未申酉戌亥)
    public boolean isLeapMonth;

    public LunarInfo() {
    }
}
