package com.ziwei.doushu.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

/**
 * 宫位。对齐原项目 types.ts: Palace
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Palace {
    public int branch;          // 0-11 (地支索引)
    public int stem;            // 0-9 (天干索引)
    public String name;         // 宫名
    public List<Star> stars = new ArrayList<>();
    public int[] daXianAge;     // 大限年龄段 [start, end]
    public Boolean isCurrentDaXian;
    public Boolean isMingGong;
    public Boolean isShenGong;
    public List<SelfSihuaMark> selfSihua;
    public Integer oppositeBranch;      // 对宫地支索引 = (branch + 6) % 12
    public Boolean isEmpty;             // 是否空宫（无主星）
    public Integer borrowedFromBranch;  // 若空宫，借自地支索引
    public String borrowedFromName;     // 若空宫，借自宫名
    public List<String> borrowedStars;  // 若空宫，借到的对宫主星名列表

    public Palace() {
    }
}
