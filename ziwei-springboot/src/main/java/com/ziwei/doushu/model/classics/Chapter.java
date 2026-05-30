package com.ziwei.doushu.model.classics;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * 古籍章节。对齐原项目 classics/types.ts: Chapter
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Chapter {
    public String title;       // 章节标题（如"卷一"、"总论篇"）
    public String subtitle;    // 章节副标题/简介（可选）
    public List<Paragraph> paragraphs;

    public Chapter() {
    }
}
