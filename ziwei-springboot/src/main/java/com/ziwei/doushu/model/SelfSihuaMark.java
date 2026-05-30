package com.ziwei.doushu.model;

/**
 * 宫干自化标记。对齐原项目 types.ts: SelfSihuaMark
 */
public class SelfSihuaMark {
    public SiHua siHua;     // 禄/权/科/忌
    public String starName; // 自化的星

    public SelfSihuaMark() {
    }

    public SelfSihuaMark(SiHua siHua, String starName) {
        this.siHua = siHua;
        this.starName = starName;
    }
}
