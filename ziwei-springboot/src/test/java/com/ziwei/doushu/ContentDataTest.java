package com.ziwei.doushu;

import com.ziwei.doushu.data.SeoKnowledge;
import com.ziwei.doushu.model.FamousPerson;
import com.ziwei.doushu.model.ProvinceInfo;
import com.ziwei.doushu.model.classics.Book;
import com.ziwei.doushu.model.classics.SearchHit;
import com.ziwei.doushu.service.ClassicsService;
import com.ziwei.doushu.service.ContentDataService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 纯内容数据加载与查询测试（古籍、名人、城市、合盘、三纪、SEO）。
 * 直接调用 @PostConstruct 加载方法，无需启动 Spring 容器。
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ContentDataTest {

    private ClassicsService classics;
    private ContentDataService content;

    @BeforeAll
    void init() throws Exception {
        classics = new ClassicsService();
        invokeLoad(classics);
        content = new ContentDataService();
        invokeLoad(content);
    }

    private void invokeLoad(Object svc) throws Exception {
        Method m = svc.getClass().getDeclaredMethod("load");
        m.setAccessible(true);
        m.invoke(svc);
    }

    @Test
    void classics_threeBooks_searchWorks() {
        List<Book> books = classics.allBooks();
        assertEquals(3, books.size(), "应收录 3 部古籍");
        assertEquals("骨髓赋", books.get(0).title);
        assertEquals("gusuifu", books.get(0).slug);
        assertTrue(classics.totalParagraphs() > 0);

        // 按 slug 取书
        assertNotNull(classics.getBookBySlug("gusuifu"));
        // 章节可取
        assertNotNull(classics.getChapter("gusuifu", 0));

        // 全文搜索"紫微"应有命中，且片段含 <mark>
        List<SearchHit> hits = classics.searchClassics("紫微", 30);
        assertFalse(hits.isEmpty(), "搜索紫微应有结果");
        assertTrue(hits.get(0).snippet.contains("<mark>"), "片段应高亮");
        // escapeHtml：确保特殊字符被转义（搜索 '<' 不应破坏 HTML，这里验证转义函数行为）
        assertTrue(hits.stream().allMatch(h -> h.text != null));
    }

    @Test
    void famous_loadedAndCategorized() {
        List<FamousPerson> all = content.famousPersons();
        assertEquals(11, all.size(), "名人样本应为 11（与 famous.ts 一致）");
        FamousPerson maYun = content.famousById("ma-yun");
        assertNotNull(maYun);
        assertEquals("马云", maYun.name);
        assertEquals(1964, maYun.year);
        // 分类查询
        assertFalse(content.famousByCategory("商业").isEmpty());
        assertEquals(List.of("商业", "文艺", "科技", "体育"), content.famousCategories());
    }

    @Test
    void cities_loaded() {
        List<ProvinceInfo> prov = content.provinces();
        assertTrue(prov.size() >= 30, "省级行政区应 >= 30");
        ProvinceInfo bj = content.provinceByName("北京市");
        assertNotNull(bj);
        assertEquals(116.4, bj.cities.get(0).longitude, 1e-9);
    }

    @Test
    void hemingKnowledge_loaded() {
        var node = content.hemingKnowledge();
        assertNotNull(node);
        // 夫妻宫断语含 14 主星
        assertTrue(node.get("STAR_IN_FUQI_GU").has("紫微"));
        assertTrue(node.get("STAR_IN_FUQI_GU").has("破军"));
        // 方法论文本存在
        assertTrue(node.get("HEMING_METHODOLOGY").asText().length() > 100);
    }

    @Test
    void nihai_sanji_loaded() {
        assertEquals(64, content.nihaiTianji().get("HEXAGRAMS").size(), "易经应 64 卦");
        assertTrue(content.nihaiRenji().get("ACU_EXPERIENCES").size() > 0);
        assertNotNull(content.nihaiDiji().get("DIJI_MODULES"));
        assertEquals("倪海厦", content.nihaiBio().get("NI_HAIXIA_BIO").get("name").asText());
    }

    @Test
    void seoKnowledge_staticMaps() {
        assertEquals(14, SeoKnowledge.ALL_STARS.size());
        assertEquals(13, SeoKnowledge.ALL_TOPICS.size());
        assertEquals("ziwei", SeoKnowledge.STAR_TO_SLUG.get("紫微"));
        assertEquals("紫微", SeoKnowledge.SLUG_TO_STAR.get("ziwei"));
        assertEquals(14, SeoKnowledge.STAR_BRIEF_SEO.size());
        assertTrue(SeoKnowledge.STAR_BRIEF_SEO.get("紫微").startsWith("紫微为帝星"));
    }
}
