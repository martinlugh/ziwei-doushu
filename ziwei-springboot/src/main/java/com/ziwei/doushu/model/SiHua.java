package com.ziwei.doushu.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 四化：禄 / 权 / 科 / 忌
 * 对齐原项目 types.ts:  export type SiHua = '禄' | '权' | '科' | '忌';
 */
public enum SiHua {
    LU("禄"), QUAN("权"), KE("科"), JI("忌");

    private final String cn;

    SiHua(String cn) {
        this.cn = cn;
    }

    @JsonValue
    public String cn() {
        return cn;
    }

    @JsonCreator
    public static SiHua fromCn(String s) {
        if (s == null) return null;
        for (SiHua v : values()) {
            if (v.cn.equals(s)) return v;
        }
        return null;
    }
}
