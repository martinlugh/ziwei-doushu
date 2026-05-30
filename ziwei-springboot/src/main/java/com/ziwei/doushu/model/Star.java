package com.ziwei.doushu.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 星曜。对齐原项目 types.ts: Star
 *   name: string
 *   type: 'major' | 'minor' | 'lucky' | 'sha'
 *   siHua?: SiHua
 *   brightness?: 'bright' | 'normal' | 'dim'
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Star {
    public String name;
    public String type;
    public SiHua siHua;
    public String brightness;

    public Star() {
    }

    public Star(String name, String type, String brightness, SiHua siHua) {
        this.name = name;
        this.type = type;
        this.brightness = brightness;
        this.siHua = siHua;
    }
}
