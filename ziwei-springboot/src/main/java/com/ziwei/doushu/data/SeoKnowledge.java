package com.ziwei.doushu.data;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SEO 知识页静态数据。移植自原项目 lib/seo/knowledge.ts 中**不依赖** db-analysis 的部分。
 *
 * 注：原 knowledge.ts 的 getKnowledge / getAllKnowledgeRoutes / TOPIC_TO_FIELD / parseStarContent
 * 依赖 `@/lib/ziwei/db-analysis`（STAR_DB / TOPIC_LABEL / TOPIC_PALACE_NAME），
 * 而该模块属于 README 所述"未开源的分析层（51.8 万样本/db-analysis）"，在开源仓库中不存在，
 * 故原 knowledge.ts 在开源版本中本就无法编译运行。这里仅移植其中的纯静态映射数据，
 * 逐字对齐：ALL_STARS / STAR_TO_SLUG / SLUG_TO_STAR / ALL_TOPICS / STAR_BRIEF_SEO。
 */
public final class SeoKnowledge {

    private SeoKnowledge() {
    }

    /** 14 主星（对齐 ALL_STARS） */
    public static final List<String> ALL_STARS = List.of(
            "紫微", "天机", "太阳", "武曲", "天同", "廉贞", "天府",
            "太阴", "贪狼", "巨门", "天相", "天梁", "七杀", "破军"
    );

    /** 主星名 → 拼音 slug（对齐 STAR_TO_SLUG） */
    public static final Map<String, String> STAR_TO_SLUG = mapOf(
            "紫微", "ziwei",
            "天机", "tianji",
            "太阳", "taiyang",
            "武曲", "wuqu",
            "天同", "tiantong",
            "廉贞", "lianzhen",
            "天府", "tianfu",
            "太阴", "taiyin",
            "贪狼", "tanlang",
            "巨门", "jumen",
            "天相", "tianxiang",
            "天梁", "tianliang",
            "七杀", "qisha",
            "破军", "pojun"
    );

    /** slug → 主星名（对齐 SLUG_TO_STAR） */
    public static final Map<String, String> SLUG_TO_STAR = invert(STAR_TO_SLUG);

    /** 13 个 topic（对齐 ALL_TOPICS） */
    public static final List<String> ALL_TOPICS = List.of(
            "overview", "personality", "love", "career", "wealth", "health",
            "family", "children", "move", "friends", "home", "spirit", "parents"
    );

    /** 主星属性简介（对齐 STAR_BRIEF_SEO） */
    public static final Map<String, String> STAR_BRIEF_SEO = mapOf(
            "紫微", "紫微为帝星，主尊贵，化气为尊。落命主有领导气场、宜大平台高位。",
            "天机", "天机为智慧星，主善变机灵，化气为善。落命主聪明机变、宜辅佐策划。",
            "太阳", "太阳为男贵星，主名誉公务，化气为贵。落命主光明磊落、宜公职名声。",
            "武曲", "武曲为财星，主刚毅果决，化气为财。落命主理财能力强、宜实业金融。",
            "天同", "天同为福星，主温和享乐，化气为福。落命主性情温和、有福气。",
            "廉贞", "廉贞为次桃花星，文武兼备，化气为囚。落命主多才多艺、感情丰富。",
            "天府", "天府为南帝守财星，主稳重保守，化气为令。落命主品行端正、善守财库。",
            "太阴", "太阴为月亮富贵星，主田宅富贵，化气为富。落命主感情细腻、女命最吉。",
            "贪狼", "贪狼为桃花欲望星，多才多社交，化气为桃花。落命主多才艺、社交广。",
            "巨门", "巨门为是非口才星，主辩论传媒，化气为暗。落命主口才好、宜律师教师。",
            "天相", "天相为印星辅佐，主忠厚老实，化气为印。落命主品行端正、宜行政法务。",
            "天梁", "天梁为老人星荫星，善逢凶化吉，化气为荫。落命主慈悲善良、宜法律医学。",
            "七杀", "七杀为将星，主孤独果决冒险，化气为肃杀。落命主刚毅果决、宜军警创业。",
            "破军", "破军为破坏创新星，主六亲缘薄，化气为耗。落命主开创变动、宜技术专长。"
    );

    private static Map<String, String> mapOf(String... kv) {
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put(kv[i], kv[i + 1]);
        return m;
    }

    private static Map<String, String> invert(Map<String, String> src) {
        Map<String, String> m = new LinkedHashMap<>();
        src.forEach((k, v) -> m.put(v, k));
        return m;
    }
}
