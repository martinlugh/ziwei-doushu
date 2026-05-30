package com.ziwei.doushu.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.ziwei.doushu.model.FamousPerson;
import com.ziwei.doushu.model.ProvinceInfo;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;

/**
 * 纯内容数据加载与查询。
 *
 * 数据来自 resources/data/*.json（由原 TS 数据模块 esbuild 提取，逐字一致）：
 *  - cities.ts            → cities.json（省市经纬度，真太阳时校正用）
 *  - famous.ts            → famous.json（名人命盘样本）
 *  - heming-knowledge.ts  → heming-knowledge.json（合盘断语 + 方法论）
 *  - nihai/tianji|diji|renji|index → nihai-*.json（倪海厦三纪知识库）
 *
 * 大段嵌套结构（合盘、三纪）以 JsonNode 原样供给，避免为内容数据堆叠 POJO，
 * 同时保证与原项目字段、文案逐字一致。
 */
@Service
public class ContentDataService {

    private final ObjectMapper om = new ObjectMapper();

    private List<ProvinceInfo> provinces;
    private List<FamousPerson> famousPersons;
    private JsonNode hemingKnowledge;
    private JsonNode nihaiTianji;
    private JsonNode nihaiDiji;
    private JsonNode nihaiRenji;
    private JsonNode nihaiBio;

    @PostConstruct
    void load() {
        provinces = readList("data/cities.json", new TypeReference<>() {
        });
        JsonNode famousRoot = readTree("data/famous.json");
        famousPersons = om.convertValue(famousRoot.get("FAMOUS_PERSONS"),
                new TypeReference<List<FamousPerson>>() {
                });
        hemingKnowledge = readTree("data/heming-knowledge.json");
        nihaiTianji = readTree("data/nihai-tianji.json");
        nihaiDiji = readTree("data/nihai-diji.json");
        nihaiRenji = readTree("data/nihai-renji.json");
        nihaiBio = readTree("data/nihai-bio.json");
    }

    private <T> T readList(String path, TypeReference<T> ref) {
        try (InputStream is = new ClassPathResource(path).getInputStream()) {
            return om.readValue(is, ref);
        } catch (Exception e) {
            throw new IllegalStateException("加载数据失败: " + path, e);
        }
    }

    private JsonNode readTree(String path) {
        try (InputStream is = new ClassPathResource(path).getInputStream()) {
            return om.readTree(is);
        } catch (Exception e) {
            throw new IllegalStateException("加载数据失败: " + path, e);
        }
    }

    // ─── 城市 ───────────────────────────────────────────────────
    /** 全部省市（对齐 cities.ts PROVINCES） */
    public List<ProvinceInfo> provinces() {
        return provinces;
    }

    /** 按省名取下属城市（对齐 BirthForm 中 PROVINCES.find(...).cities 的用法） */
    public ProvinceInfo provinceByName(String name) {
        return provinces.stream().filter(p -> p.name.equals(name)).findFirst().orElse(null);
    }

    // ─── 名人 ───────────────────────────────────────────────────
    /** 全部名人（对齐 famous.ts FAMOUS_PERSONS） */
    public List<FamousPerson> famousPersons() {
        return famousPersons;
    }

    /** 按分类取名人（对齐 famous.ts getFamousByCategory） */
    public List<FamousPerson> famousByCategory(String category) {
        return famousPersons.stream().filter(p -> p.category.equals(category)).toList();
    }

    /** 名人分类（对齐 famous.ts FAMOUS_CATEGORIES） */
    public List<String> famousCategories() {
        return List.of("商业", "文艺", "科技", "体育");
    }

    public FamousPerson famousById(String id) {
        return famousPersons.stream().filter(p -> p.id.equals(id)).findFirst().orElse(null);
    }

    // ─── 合盘知识库 ──────────────────────────────────────────────
    /** 合盘断语 + 方法论全量（对齐 heming-knowledge.ts 全部命名导出） */
    public JsonNode hemingKnowledge() {
        return hemingKnowledge;
    }

    // ─── 倪海厦三纪 ──────────────────────────────────────────────
    public JsonNode nihaiTianji() {
        return nihaiTianji;
    }

    public JsonNode nihaiDiji() {
        return nihaiDiji;
    }

    public JsonNode nihaiRenji() {
        return nihaiRenji;
    }

    public JsonNode nihaiBio() {
        return nihaiBio;
    }
}
