package com.ziwei.doushu.model.classics;

import java.util.List;

/**
 * 古籍。对齐原项目 classics/types.ts: Book
 */
public class Book {
    public String title;     // 书名
    public String slug;      // URL slug
    public String dynasty;   // 朝代
    public String author;    // 作者
    public String intro;     // 简介
    public int wordCount;    // 总字数（粗略）
    public List<Chapter> chapters;

    public Book() {
    }
}
