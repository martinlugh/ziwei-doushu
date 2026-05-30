package com.ziwei.doushu.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ziwei.doushu.model.classics.Book;
import com.ziwei.doushu.model.classics.Chapter;
import com.ziwei.doushu.model.classics.Paragraph;
import com.ziwei.doushu.model.classics.SearchHit;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 古籍原典查询库。严格对齐原项目 lib/classics/index.ts。
 *
 * 数据来自 resources/data/classics-*.json（由原 TS 数据模块 esbuild 提取，逐字一致），
 * 收录顺序与 ALL_BOOKS 一致：骨髓赋、紫微斗数全集、紫微斗数全书。
 */
@Service
public class ClassicsService {

    private final ObjectMapper om = new ObjectMapper();
    private List<Book> allBooks;
    private int totalParagraphs;

    @PostConstruct
    void load() {
        allBooks = new ArrayList<>();
        allBooks.add(read("data/classics-gusuifu.json"));
        allBooks.add(read("data/classics-quanji.json"));
        allBooks.add(read("data/classics-quanshu.json"));
        // 对齐 TOTAL_PARAGRAPHS
        int sum = 0;
        for (Book b : allBooks) {
            for (Chapter c : b.chapters) sum += c.paragraphs.size();
        }
        totalParagraphs = sum;
    }

    private Book read(String path) {
        try (InputStream is = new ClassPathResource(path).getInputStream()) {
            return om.readValue(is, Book.class);
        } catch (Exception e) {
            throw new IllegalStateException("加载古籍数据失败: " + path, e);
        }
    }

    /** 所有已收录古籍（对齐 ALL_BOOKS） */
    public List<Book> allBooks() {
        return allBooks;
    }

    /** 总段落数（对齐 TOTAL_PARAGRAPHS） */
    public int totalParagraphs() {
        return totalParagraphs;
    }

    /** 按 slug 取书（对齐 getBookBySlug） */
    public Book getBookBySlug(String slug) {
        return allBooks.stream().filter(b -> b.slug.equals(slug)).findFirst().orElse(null);
    }

    public record ChapterRef(Book book, Chapter chapter, int chapterIdx) {
    }

    /** 按章节序号取章节（对齐 getChapter） */
    public ChapterRef getChapter(String bookSlug, int chapterIdx) {
        Book book = getBookBySlug(bookSlug);
        if (book == null) return null;
        if (chapterIdx < 0 || chapterIdx >= book.chapters.size()) return null;
        return new ChapterRef(book, book.chapters.get(chapterIdx), chapterIdx);
    }

    public record ParagraphRef(Book book, Chapter chapter, int chapterIdx, Paragraph paragraph) {
    }

    /** 按段落 id 取段落（对齐 getParagraphById） */
    public ParagraphRef getParagraphById(String id) {
        for (Book book : allBooks) {
            for (int i = 0; i < book.chapters.size(); i++) {
                Chapter ch = book.chapters.get(i);
                for (Paragraph p : ch.paragraphs) {
                    if (p.id.equals(id)) {
                        return new ParagraphRef(book, ch, i, p);
                    }
                }
            }
        }
        return null;
    }

    /**
     * 全文搜索（对齐 searchClassics）。
     * 简单子字符串匹配（不分词，对中文 OK），上下文前后各 40 字，带 &lt;mark&gt; 高亮。
     */
    public List<SearchHit> searchClassics(String query, int limit) {
        String q = query == null ? "" : query.trim();
        List<SearchHit> hits = new ArrayList<>();
        if (q.length() < 1) return hits;

        for (Book book : allBooks) {
            for (Chapter chapter : book.chapters) {
                for (Paragraph p : chapter.paragraphs) {
                    int idx = p.text.indexOf(q);
                    if (idx < 0) continue;

                    int start = Math.max(0, idx - 40);
                    int end = Math.min(p.text.length(), idx + q.length() + 40);
                    String before = p.text.substring(start, idx);
                    String matched = p.text.substring(idx, idx + q.length());
                    String after = p.text.substring(idx + q.length(), end);

                    String snippet = (start > 0 ? "…" : "")
                            + escapeHtml(before)
                            + "<mark>" + escapeHtml(matched) + "</mark>"
                            + escapeHtml(after)
                            + (end < p.text.length() ? "…" : "");

                    hits.add(new SearchHit(book.slug, book.title, chapter.title, p.id, snippet, p.text));
                    if (hits.size() >= limit) return hits;
                }
            }
        }
        return hits;
    }

    public List<SearchHit> searchClassics(String query) {
        return searchClassics(query, 30);
    }

    /** 对齐 index.ts 的 escapeHtml */
    private static String escapeHtml(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#039;");
    }
}
