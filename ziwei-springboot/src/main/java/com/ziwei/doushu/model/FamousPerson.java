package com.ziwei.doushu.model;

/**
 * 名人命盘数据。对齐原项目 famous.ts: FamousPerson
 *   category: '商业' | '文艺' | '历史' | '体育' | '科技'
 */
public class FamousPerson {
    public String id;
    public String name;
    public String category;
    public String description;   // 一句话身份介绍
    public int year;
    public int month;
    public int day;
    public int hour;             // 时辰地支索引 0-11
    public String gender;        // 'male' | 'female'
    public String notable;       // 命盘亮点提示

    public FamousPerson() {
    }
}
