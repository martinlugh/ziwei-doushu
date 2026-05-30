# 紫微斗数排盘引擎 · Java / Spring Boot 移植版

本目录是对仓库根目录原 **Next.js / TypeScript** 项目（`lib/ziwei/*`）的 **Java + Spring Boot** 移植。
目标：**严格保持算法逻辑与参数与原项目一致**，并通过与原项目实际依赖的排盘库 `iztro@2.5.8`
逐字段对拍验证。

---

## 一、架构对照

| 原 TypeScript | Java / Spring Boot | 状态 |
|---|---|---|
| `lib/ziwei/types.ts` | `model/*.java` | ✅ |
| `lib/ziwei/constants.ts` | `engine/Constants.java` | ✅ 参数逐项对齐 |
| `lib/ziwei/sihua.ts` | `service/SiHuaService.java` | ✅ |
| `lib/ziwei/patterns.ts`（1100+ 行格局库） | `service/PatternsService.java` | ✅ 逐函数对齐 |
| `lib/ziwei/share.ts`（真太阳时、晚子时进位） | `engine/TrueSolarTime.java` | ✅ |
| `lib/ziwei/algorithm.ts` → `iztro` + `lunar-javascript` | `engine/ZiweiEngine.java` | ✅ **已完成** |

> `algorithm.ts` 本身是薄封装，真正的排盘内核（安命身宫、定五行局、安紫微/十四主星/辅星/煞星/杂耀、
> 亮度、四化标记、大限）由外部库 **`iztro`**（`lib/star/location.js`、`star.js`、`astro/palace.js`、
> `utils`）配合 **`lunar-lite`** 日历完成。本移植已把这部分内核完整重写为 `ZiweiEngine.java`：
> - 日历转换用 lunar-javascript 的官方 Java 孪生库 **`cn.6tail:lunar`**（同源 6tail，同算法）
> - 安星/定局/亮度/四化/大限算法与 iztro 一一对应（口诀、索引偏移、五虎遁、纳音局数完全一致）
> - 星曜类型/亮度/四化的最终字段映射与 `algorithm.ts` 的 `mapBrightness` / `mapStarType` 一致

## 二、对拍验证（与 iztro@2.5.8 逐字段一致）

`src/test/java/.../IztroParityTest.java` 读取 `src/test/resources/iztro-truth.jsonl`
（由 `astro.bySolar(date, timeIndex, gender, true, 'zh-CN')` 生成，与 `algorithm.ts` 调用方式完全相同），
对 5 个出生样本逐项校验：

- 五行局、命宫地支、紫微地支
- 每宫：宫干、十二宫名
- 每宫主星：星名 + 四化 + 亮度映射（bright/normal/dim），顺序一致
- 每宫全部星曜名集合（主星 + 辅星 + 杂耀，含红鸾/天喜/年解等）
- 每宫大限年龄段 [start, end]

**结果：`mvn test` → Tests run: 3, Failures: 0, Errors: 0（BUILD SUCCESS）**，全部字段与 iztro 一致。

## 三、运行

```bash
cd ziwei-springboot
mvn spring-boot:run                 # 启动 REST 服务（默认 8080）
# 或
mvn -DskipTests package && java -jar target/ziwei-doushu-1.0.0.jar
```

### REST 接口

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/chart` | body 为 `BirthInfo`（`hour` 为时辰地支索引 0~11）→ 完整命盘 |
| GET | `/api/chart` | query：`year,month,day,clockHour,clockMinute,longitude,gender,unknownTime,name,province,city`（真太阳时/晚子时进位）→ 命盘 + 格局 + 命宫摘要 |
| POST | `/api/patterns` | body 为 `BirthInfo` → 仅格局识别结果 |
| GET | `/api/classics` | 古籍列表 + 总段落数 |
| GET | `/api/classics/{slug}` | 取某部古籍（gusuifu/quanji/quanshu） |
| GET | `/api/classics/{slug}/{chapterIdx}` | 取某章 |
| GET | `/api/classics/search?q=&limit=` | 全文搜索（&lt;mark&gt; 高亮，对齐 searchClassics） |
| GET | `/api/famous?category=` | 名人样本（可按分类） |
| GET | `/api/cities` | 省市经纬度（真太阳时用） |
| GET | `/api/heming-knowledge` | 合盘断语 + 方法论 |
| GET | `/api/nihai/{tianji\|diji\|renji\|bio}` | 倪海厦三纪知识库 |
| GET | `/api/seo/stars` | 14 主星 SEO 知识（slug/简介/主题） |

示例：

```bash
curl -X POST localhost:8080/api/chart -H 'Content-Type: application/json' \
  -d '{"year":2000,"month":8,"day":16,"hour":2,"gender":"female"}'
# → 木三局、命宫午、紫微午（与 iztro 一致）

curl "localhost:8080/api/chart?year=2000&month=8&day=16&clockHour=4&gender=female"
# → 命盘 + 格局（府相朝垣/火贪格/铃贪格/紫微入命）+ 命宫摘要
```

## 四、纯内容数据（古籍/三纪/名人/城市/合盘/SEO）

已全部移植。大段纯文本以 **JSON 资源**（`resources/data/*.json`）打包，由原 TS 数据模块经 **esbuild 提取**
（逐字一致，非手抄），配套 model + 加载/查询 service：

| 原 TypeScript | Java |
|---|---|
| `classics/*`（骨髓赋/全集/全书 + 搜索） | `model/classics/*` + `service/ClassicsService.java`（含 `searchClassics`/`escapeHtml` 逐字对齐）|
| `cities.ts` | `data/cities.json` + `ContentDataService`（省市经纬度）|
| `famous.ts` | `data/famous.json` + `ContentDataService`（11 名人）|
| `heming-knowledge.ts` | `data/heming-knowledge.json`（夫妻宫断语 + 合盘方法论）|
| `nihai/{tianji,diji,renji,index}` | `data/nihai-*.json`（三纪 + 64 卦 + 倪师传记）|
| `seo/knowledge.ts`（不依赖 db-analysis 部分）| `data/SeoKnowledge.java` |

> `seo/knowledge.ts` 的 `getKnowledge`/`getAllKnowledgeRoutes` 依赖 `@/lib/ziwei/db-analysis`（README 所述
> "未开源的 51.8 万样本/分析层"），该模块在开源仓库中不存在 —— 原文件在开源版本里本就无法编译。
> 故仅移植其中纯静态映射（`ALL_STARS`/`STAR_TO_SLUG`/`ALL_TOPICS`/`STAR_BRIEF_SEO`），逐字对齐。

`ContentDataTest` 验证全部内容加载与查询（古籍 3 部 75 段 + 全文搜索高亮、名人 11、省级 31、
易经 64 卦、合盘断语、SEO 14 星）；REST 接口实测均 HTTP 200。

## 五、说明

- 原 `algorithm.ts` 排盘时 `timeIndex` 恒为 0~11（晚子时 23:00 已在 `share.ts` 中按次日早子时处理），
  故引擎不涉及 iztro 的晚子时（timeIndex=12）分支，行为与原项目一致。
- AI 解读 prompt 与后端 `/api/interpret`、`db-analysis` 分析层、51.8 万样本数据集等属平台运营层，
  README 已明确不在开源范围，故不在本移植内。
