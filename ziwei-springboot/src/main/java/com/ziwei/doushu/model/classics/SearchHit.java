package com.ziwei.doushu.model.classics;

/**
 * 古籍全文搜索命中。对齐原项目 classics/types.ts: SearchHit
 */
public class SearchHit {
    public String bookSlug;
    public String bookTitle;
    public String chapterTitle;
    public String paragraphId;
    public String snippet;   // 高亮片段（含 <mark> 标签）
    public String text;      // 原文

    public SearchHit() {
    }

    public SearchHit(String bookSlug, String bookTitle, String chapterTitle,
                     String paragraphId, String snippet, String text) {
        this.bookSlug = bookSlug;
        this.bookTitle = bookTitle;
        this.chapterTitle = chapterTitle;
        this.paragraphId = paragraphId;
        this.snippet = snippet;
        this.text = text;
    }
}
