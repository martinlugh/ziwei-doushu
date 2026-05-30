package com.ziwei.doushu.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 出生信息。对齐原项目 types.ts: BirthInfo
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BirthInfo {
    public int year;       // 公历年
    public int month;      // 公历月 (1-12)
    public int day;        // 公历日
    public int hour;       // 时辰地支索引 (0=子, 1=丑, ... 11=亥)
    public String gender;  // 'male' | 'female'
    public String name;
    public String province;
    public String city;
    public Double longitude;

    public BirthInfo() {
    }

    public BirthInfo(int year, int month, int day, int hour, String gender) {
        this.year = year;
        this.month = month;
        this.day = day;
        this.hour = hour;
        this.gender = gender;
    }
}
