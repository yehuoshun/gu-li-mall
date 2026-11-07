# gu-li-mall · 谷粒商城现代化改造

> 尚硅谷《谷粒商城》实战项目的**现代化重构 + 学习靶场**：把 2021 年的 Boot 2.2 时代代码，搬到 **Spring Boot 3.5 / Spring Cloud 2025 / Spring Cloud Alibaba 2025** 这一代技术栈上。
> 原始仓库：`gitee.com/AdverseQ/gulimall_Advanced`（Gitee fork）→ 本仓库为现代化后的主线工程。

---

## 一、技术栈

| 组件 | 版本 | 说明 |
|---|---|---|
| JDK | **17** | SCA 2025 线要求 17+ |
| Spring Boot | **3.5.16** | |
| Spring Cloud | **2025.0.0** | |
| Spring Cloud Alibaba | **2025.0.0.0** | 带 `nacos-client 3.0.3` |
| Nacos | **3.x**（服务端） | 注册中心 + 配置中心；3.0 起控制台端口独立为 **8080**，主端口仍 8848 |
| MyBatis-Plus | **3.5.18** | `mybatis-plus-spring-boot3-starter` + `mybatis-plus-jsqlparser`（3.5.9 起 jsqlparser 从 extension 剥离，必须显式引） |
| MPJ（多表 join） | **icu.mhb `2.1.3-nameFix`** | 自编译 fork，见下文「多表查询」 |
| MySQL | 8.x | 六库：`gu_li_mall_{pms,ums,sms,wms,oms,admin}` |
| 构建 | Maven 3.9+ | 根 pom 统一管理版本（parent + 三个 BOM import） |

> 版本由根 `pom.xml` 的属性与 BOM 统一管控，子模块**不写版本号**。

## 二、模块清单（13 个）

| 模块 | 职责 | 端口 | 备注 |
|---|---|---|---|
| `gu-li-mall-gateway` | 网关路由 | 10000 | WebFlux；**严禁依赖 common** |
| `gu-li-mall-product` | 商品 | 10010 | 库 `gu_li_mall_pms` |
| `gu-li-mall-member` | 会员 | 10020 | `gu_li_mall_ums` |
| `gu-li-mall-coupon` | 优惠券 | 10030 | `gu_li_mall_sms` |
| `gu-li-mall-ware` | 库存 | 10040 | `gu_li_mall_wms` |
| `gu-li-mall-order` | 订单 | 10050 | `gu_li_mall_oms` |
| `gu-li-mall-search` | 检索（ES） | 10060 | 暂未接 ES |
| `gu-li-mall-cart` | 购物车 | 10070 | |
| `gu-li-mall-seckill` | 秒杀 | 10080 | `gu_li_mall_sms` |
| `gu-li-mall-auth-server` | 登录鉴权 | 10090 | |
| `gu-li-mall-third-part` | 第三方（OSS/短信） | 10100 | |
| `gu-li-mall-common` | 公共基座：`R<T>` / 错误码 / 全局异常 / 校验组 / Jackson / 日期工具 | — | 内含 `starter-web` + actuator + nacos-config，业务模块靠它获得 Web 能力 |
| `gu-li-mall-common-datasource` | 数据源基座：`BaseEntity` + 分页/乐观锁/防全表插件 + 自动填充 + MPJ | — | 仅连 MySQL 的业务模块依赖 |

**依赖关系（重要）**

```
业务模块(10) ──> gu-li-mall-common（Web/Actuator/Nacos-Config）
连库模块(6) ───> gu-li-mall-common-datasource（MyBatis-Plus + MPJ + MySQL 驱动）

gateway ──✗──> gu-li-mall-common     ← 禁止：Tomcat/WebMVC 与 WebFlux 冲突
gateway ─────> 自行引 actuator / loadbalancer / nacos-config
```

## 三、快速开始

### 1. 前置环境
- JDK 17、Maven 3.9+
- MySQL 8（本地 3306）
- Nacos **3.x**（本地 8848；控制台 8080）

### 2. 建库
先建六个库（`gu_li_mall_pms` / `gu_li_mall_ums` / `gu_li_mall_sms` / `gu_li_mall_wms` / `gu_li_mall_oms` / `gu_li_mall_admin`），再逐个执行 `doc/数据库/gu_li_mall_*.sql` 建表。

### 3. 准备 Nacos
1. 起 Nacos 单机：`bin/startup.cmd -m standalone`（Windows）
   - 3.x **必须**先在 `conf/application.properties` 配好三样，否则起不来（详见 `doc/交接提示词.md` 坑 18/19 与语雀《Nacos 3.3.0 接入踩坑》）：
     `nacos.core.auth.server.identity.key/value`、`nacos.plugin.auth.nacos.token.secret.key`（Base64、解码后 ≥32 字节）
2. 建一个**命名空间**（本项目用项目专用命名空间，`namespace` 实填**命名空间 ID** 而非名称）
3. 服务列表里应能看到各服务注册（记得先把控制台左上角**切到该命名空间**）

### 4. 配置
每个服务 `src/main/resources/application.yml` 结构：

```yaml
spring:
  config:
    import:                                  # 配置中心（混合模式：本地兜底 + Nacos 覆盖）
      - optional:nacos:gu-li-mall-common.yaml?group=DEFAULT_GROUP&refreshEnabled=true
      - optional:nacos:<服务名>.yaml?group=DEFAULT_GROUP&refreshEnabled=true
  application:
    name: <服务名>
  datasource: ...                            # 本地 MySQL（凭据按环境自行填写）
  cloud:
    nacos:
      server-addr: 127.0.0.1:8848
      discovery:
        namespace: <命名空间ID>
        group: DEFAULT_GROUP
      config:
        namespace: <命名空间ID>              # 与注册中心必须一致，否则配置拉不到
      username: ${NACOS_USERNAME:nacos}     # 占位符写法：不把真实密码提交进仓库
      password: ${NACOS_PASSWORD:nacos}
server:
  port: <见模块表>
```

> ⚠️ **优先级**：本地 `application.yml` **高于** Nacos 导入的配置。想让 Nacos 覆盖某项，本地就不能写同名键。

### 5. 构建与启动

```bash
mvn clean compile           # 全量编译（13 模块）
mvn -pl gu-li-mall-product -am spring-boot:run    # 起单个服务
```

### 6. 验证

1. 启动日志出现 `Tomcat started on port 10010 (http)` + `nacos registry, DEFAULT_GROUP gu-li-mall-product 10010 register finished`
2. Nacos 控制台（`http://127.0.0.1:8080`）服务列表可见
3. 网关穿透：`http://127.0.0.1:10000/gu-li-mall-product/actuator/health` → `{"status":"UP"}`

## 四、多表查询约定（MPJ）

- **首选 Lambda 版**：`JoinLambdaWrapper`（`selectAll()` / `leftJoin(...)` / `.select(...)` / `.end()` / `joinSelectList|joinSelectPage`），关联表实体**必须有 Mapper**
- **chain 链式版**（可选）：需 APT 生成模型类。本仓库用 **fork 版处理器 `2.1.3-nameFix`**：
  - `@JoinChainModel(fieldMethodNaming = FIELD_SUFFIX)` → 生成 `idField()`；不写该属性则沿用官方 `_id()`（向后兼容）
  - ⚠️ `2.1.3-nameFix` **不在 Maven Central**，本地仓库需先有 `icu/mhb/*/2.1.3-nameFix/*`（`mvn install` fork 仓库，或解压预编译产物）
  - ⚠️ 改完 pom 在 IDEA 里要 **Reload Maven Project**，否则 IDE 内不生成 Chain 类 → 满屏「找不到符号 XxxChain」
  - ⚠️ fork 仓库本身只能用 **JDK 8** 编译（其 lombok 1.18.10 限制）

## 五、编码约定

- **包名**：`com.shun.gu_li_mall_<模块>`
- **统一返回**：`R<T>`（对象化 + `@JsonAnyGetter` 平铺兼容旧前端）
- **异常**：走 `gu-li-mall-common` 的 `GlobalExceptionHandler` + `BizCodeEnum`
- **分页**：用 MP 原生 `Page` / `IPage`，不自造分页层
- **实体**：连库模块继承 `BaseEntity`（`id` + `createTime` / `updateTime` 自动填充）
- **配置**：本地 yml 只放「自举 + 兜底」，可变的业务配置后续迁 Nacos

## 六、文档地图

| 位置 | 内容 |
|---|---|
| `doc/agent.md` | AI 协作约束（角色边界、红线、文档规范） |
| `doc/交接提示词.md` | **实时状态页**：技术基线 / 当前进度 / 已知坑 / 下次开工待办 |
| `doc/2026-11-01-项目体检与改进学习方案.md` | 体检报告 + 三阶段改造方案（含状态回填横幅） |
| `doc/工程骨架铺设蓝图.md` | 模块规划、根 pom 策略、依赖策略 |
| `doc/MySQL多表操作方案-mybatis-plus-join.md` | MPJ 调研 + 实测结论 + chain/nameFix 决策 |
| `doc/数据库/` | 六库建表 SQL |
| 语雀 `yehuoshun/em82l9` | 项目总览（决策记录）/ 开发进度（Day1~Day3）/ 技术笔记 |

## 七、当前进度（2026-11-07）

- ✅ 13 模块骨架 + 根 pom（三大 BOM）+ 编译通过
- ✅ `common` 基座 / `common-datasource` 数据源基座
- ✅ MPJ 接入并实测（Lambda + chain nameFix）
- ✅ Nacos 3.x 接入：**注册成功**，配置中心**混合模式**已接通（配置暂留本地）
- ⏳ 业务代码尚未搬入（product 目前是空壳）→ 下一步：**product 模块搬入适配清单**
- ⏳ 中间件环境待定（Docker vs 服务器）；Redis / ES / RabbitMQ 未接

## 八、常见问题

| 现象 | 原因 / 处理 |
|---|---|
| `Port 8080 was already in use`（起应用时） | Nacos 3.x 控制台就占 8080；确认应用端口（gateway 10000 等），或把控制台改到别的端口 |
| 注册失败 `ErrCode:403 ... 401 User not found!` | Nacos 3.x 开启了鉴权，yml 里补 `spring.cloud.nacos.username/password` |
| 服务注册上了但看不到 | Nacos 控制台左上角**命名空间没切对** |
| Nacos 启动报 `Empty identity` | 3.x 鉴权开关默认开，`server.identity.key/value` 不能为空 |
| Nacos 启动报 `Required config missing: token.secret.key` | 3.x 密钥键名改为 `nacos.plugin.auth.nacos.token.secret.key`（新键优先） |
| 配置改了不生效 | ① 写了 `spring.config.import` 吗 ② namespace 对吗 ③ 本地是不是有同名键（本地优先） |
| `找不到符号 XxxChain` | IDEA 没 Reload Maven；或本地仓库缺 `2.1.3-nameFix` 产物 |
| gateway 起不来 / 行为异常 | 检查是否误把 `gu-li-mall-common` 引进了 gateway（Tomcat 与 WebFlux 冲突） |

---

> 更详细的踩坑记录与决策依据见 `doc/` 与语雀知识库；协作方式见 `doc/agent.md`。
