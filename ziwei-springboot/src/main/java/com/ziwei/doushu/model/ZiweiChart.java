package com.ziwei.doushu.model;

import java.util.List;

/**
 * 命盘。对齐原项目 types.ts: ZiweiChart
 */
public class ZiweiChart {
    public BirthInfo birthInfo;
    public LunarInfo lunarInfo;
    public int mingGongBranch;     // 命宫地支
    public int shenGongBranch;     // 身宫地支
    public int wuxingJu;           // 五行局 (2,3,4,5,6)
    public String wuxingJuName;    // e.g. '水二局'
    public int ziweiPos;           // 紫微星位置
    public List<Palace> palaces;   // 12宫，按地支0-11排序
    public List<DaXian> daXians;
    public int currentAge;
    public int currentDaXianIndex;

    public ZiweiChart() {
    }
}
