package com.ziwei.doushu.model.classics;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 古籍段落。对齐原项目 classics/types.ts: Paragraph
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Paragraph {
    public String id;       // 段落唯一 id（锚点跳转）
    public int idx;         // 段落序号（章节内）
    public String text;     // 段落原文（古文）
    public String translation; // 现代翻译（可选）
    public String niNote;      // 倪师注解（可选）

    public Paragraph() {
    }
}
