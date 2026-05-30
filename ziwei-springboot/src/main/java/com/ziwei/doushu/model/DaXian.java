package com.ziwei.doushu.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 大限。对齐原项目 types.ts: DaXian
 *
 * 倪师《天纪》正统：四化永远固定，大限只看宫位移动。
 * 原 algorithm.ts 已不再生成 stemIndex/stemName/siHua（飞星派字段下线），
 * 这里保留字段定义以兼容类型，但排盘主流程不填充。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DaXian {
    public int startAge;
    public int endAge;
    public int palaceBranch;
    public String palaceName;
    public Integer stemIndex;
    public String stemName;
    public DaXianSiHua siHua;

    public DaXian() {
    }

    public DaXian(int startAge, int endAge, int palaceBranch, String palaceName) {
        this.startAge = startAge;
        this.endAge = endAge;
        this.palaceBranch = palaceBranch;
        this.palaceName = palaceName;
    }
}
