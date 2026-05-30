package com.ziwei.doushu.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.ziwei.doushu.data.SeoKnowledge;
import com.ziwei.doushu.model.FamousPerson;
import com.ziwei.doushu.model.ProvinceInfo;
import com.ziwei.doushu.model.classics.Book;
import com.ziwei.doushu.model.classics.SearchHit;
import com.ziwei.doushu.service.ClassicsService;
import com.ziwei.doushu.service.ContentDataService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 内容数据 REST 接口：古籍、名人、城市、合盘知识、倪海厦三纪、SEO 知识。
 */
@RestController
public class ContentController {

    private final ClassicsService classics;
    private final ContentDataService content;

    public ContentController(ClassicsService classics, ContentDataService content) {
        this.classics = classics;
        this.content = content;
    }

    // ─── 古籍 ───────────────────────────────────────────────────
    @GetMapping("/api/classics")
    public Map<String, Object> classicsList() {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("books", classics.allBooks());
        r.put("totalParagraphs", classics.totalParagraphs());
        return r;
    }

    @GetMapping("/api/classics/{slug}")
    public ResponseEntity<Book> classicsBook(@PathVariable String slug) {
        Book b = classics.getBookBySlug(slug);
        return b == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(b);
    }

    @GetMapping("/api/classics/{slug}/{chapterIdx}")
    public ResponseEntity<ClassicsService.ChapterRef> classicsChapter(
            @PathVariable String slug, @PathVariable int chapterIdx) {
        ClassicsService.ChapterRef ref = classics.getChapter(slug, chapterIdx);
        return ref == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(ref);
    }

    @GetMapping("/api/classics/search")
    public List<SearchHit> classicsSearch(@RequestParam String q,
                                          @RequestParam(defaultValue = "30") int limit) {
        return classics.searchClassics(q, limit);
    }

    // ─── 名人 ───────────────────────────────────────────────────
    @GetMapping("/api/famous")
    public List<FamousPerson> famous(@RequestParam(required = false) String category) {
        return category == null ? content.famousPersons() : content.famousByCategory(category);
    }

    @GetMapping("/api/famous/categories")
    public List<String> famousCategories() {
        return content.famousCategories();
    }

    // ─── 城市 ───────────────────────────────────────────────────
    @GetMapping("/api/cities")
    public List<ProvinceInfo> cities() {
        return content.provinces();
    }

    // ─── 合盘知识库 ──────────────────────────────────────────────
    @GetMapping("/api/heming-knowledge")
    public JsonNode hemingKnowledge() {
        return content.hemingKnowledge();
    }

    // ─── 倪海厦三纪 ──────────────────────────────────────────────
    @GetMapping("/api/nihai/{category}")
    public ResponseEntity<JsonNode> nihai(@PathVariable String category) {
        JsonNode node = switch (category) {
            case "tianji" -> content.nihaiTianji();
            case "diji" -> content.nihaiDiji();
            case "renji" -> content.nihaiRenji();
            case "bio" -> content.nihaiBio();
            default -> null;
        };
        return node == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(node);
    }

    // ─── SEO 知识 ────────────────────────────────────────────────
    @GetMapping("/api/seo/stars")
    public Map<String, Object> seoStars() {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("allStars", SeoKnowledge.ALL_STARS);
        r.put("starToSlug", SeoKnowledge.STAR_TO_SLUG);
        r.put("allTopics", SeoKnowledge.ALL_TOPICS);
        r.put("starBrief", SeoKnowledge.STAR_BRIEF_SEO);
        return r;
    }
}
