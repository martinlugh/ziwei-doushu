# 紫微斗数排盘引擎 · Java / Spring Boot 移植版

本目录是对仓库根目录原 **Next.js / TypeScript** 项目（`lib/ziwei/*`）的 **Java + Spring Boot** 移植。
目标：**严格保持算法逻辑与参数与原项目一致**。

---

## 一、架构对照

| 原 TypeScript | Java / Spring Boot | 状态 |
|---|---|---|
| `lib/ziwei/types.ts` | `model/*.java`（BirthInfo / LunarInfo / Star / Palace / DaXian / ZiweiChart / SiHua …） | ✅ 已移植 |
| `lib/ziwei/constants.ts` | `engine/Constants.java`（天干地支、四化表 `SI_HUA_TABLE`、禄存、天马、天魁钺、亮度表、纳音、局数…） | ✅ 已移植，参数逐项对齐 |
| `lib/ziwei/sihua.ts` | `service/SiHuaService.java`（年干/大限宫干/流年/流月四化、五虎遁、自化检测、来因宫追溯） | ✅ 已移植 |
| `lib/ziwei/patterns.ts`（1100+ 行格局库） | `service/PatternsService.java`（40+ 个格局识别器 + 命宫摘要） | ✅ 已移植，逐函数对齐 |
| `lib/ziwei/share.ts`（真太阳时、晚子时进位） | `engine/TrueSolarTime.java` | ✅ 已移植 |
| `lib/ziwei/algorithm.ts` → `iztro` + `lunar-javascript` | `engine/ZiweiEngine.java`（排盘内核） | 🚧 **进行中** |

> 关键说明：原 `algorithm.ts` 本身只是一层薄封装，真正的**排盘内核**（安命身宫、定五行局、
> 安紫微 / 十四主星 / 辅星 / 煞星 / 杂耀、亮度、四化标记、大限）由外部 npm 库
> **`iztro`**（`lib/star/location.js` 约 35KB）配合 **`lunar-lite`** 日历完成，并不在原仓库内。
> 因此 Java 端需要把 iztro 的内核与日历转换一并移植，才能做到“排盘结果”级别的严格一致。
> 该内核移植（`ZiweiEngine`）尚在进行中，已抽取 iztro 的关键参数表备用：
> - 十天干四化表（与 `constants.ts` 的 `SI_HUA_TABLE` 完全一致，已在 `Constants.java` 落地）
> - 十四主星 12 宫亮度表（iztro `lib/data/stars.js`，庙/旺/得/利/平/不/陷 → bright/normal/dim）

## 二、已落地的算法参数（与原项目逐项核对）

- **四化表** `SI_HUA_TABLE`（甲→廉破武阳 …… 癸→破巨阴贪）：与 `constants.ts` 完全一致，
  且与 iztro `lib/data/heavenlyStems.js` 的 `mutagen` 一致。
- **禄存 / 天马 / 天魁天钺 / 主星亮度 / 纳音 / 五行局** 等表：与 `constants.ts` 逐项对齐。
- **真太阳时校正** `(经度-120)×4` 分钟、子时分早晚（23:00–23:59 按次日）：与 `share.ts` 一致。
- **五虎遁起月**、自化、来因宫：与 `sihua.ts` 一致。
- **格局库**：40+ 个识别器（君臣庆会、紫府同宫、阳梁昌禄、火/铃贪、杀破狼、机月同梁、
  羊陀夹忌、廉杀羊、铃昌陀武、马头带箭 …… 及基础格局），三方四正 = 命+财+官+迁，
  夹宫 = 命宫前后两宫，level/description/source 文案与 `patterns.ts` 逐字一致。

## 三、运行

```bash
cd ziwei-springboot
mvn spring-boot:run         # 启动 REST 服务（默认 8080）
# 或
mvn -q -DskipTests package && java -jar target/ziwei-doushu-1.0.0.jar
```

## 四、待完成（TODO）

1. `engine/ZiweiEngine.java`：移植 iztro `lib/star/location.js` 的安星算法 +
   `lunar-lite` 的公历↔农历转换（建议接入 Maven 的 `cn.6tail:lunar-java`，
   `pom.xml` 中已预留依赖，待引擎接入后启用并填写中央仓库实际版本号）。
2. `controller/ZiweiController.java`：暴露 `/api/chart`、`/api/patterns`、`/api/heming` 等接口。
3. 与原项目对拍测试：对同一批生日（含 `lib/ziwei/famous.ts` 的名人样本）分别用原 TS 工程与本
   Java 工程生成命盘 JSON 并 diff，确保排盘结果逐字段一致。
4. 知识/古籍数据（`lib/nihai/*`、`lib/classics/*`、`lib/seo/*`、`heming-knowledge.ts`、
   `cities.ts`、`famous.ts`）作为资源/数据类移植（纯内容，无算法）。

> 当前提交聚焦于“仓库内自有算法逻辑与参数”的严格移植；排盘内核与端到端测试为后续步骤。
