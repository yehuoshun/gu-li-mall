# MySQL 多表操作方案：mybatis-plus-join（MPJ）

> 决策参考文档 · 2026-11-01 · 面向谷粒商城现代化改造（路线 B：Java 17 + Spring Boot 3.x + MP 3.5.x）
> 官网：https://mpj.aicats.cc/ · 代码：Gitee `mhb0409/mybatis-plus-join` / GitHub `bobo667/mybatis-plus-join`

---

## 〇、结论摘要（先看这里）

1. **MPJ 是什么**：MyBatis-Plus 的多表关联查询扩展插件——解决「多表 join + Lambda 类型安全」痛点，十分钟上手，仅额外依赖 lombok。
2. **兼容性**：MP 层面 **3.5.3+ 完全兼容**（正好覆盖路线 B 的 MP 3.5.x）✅；但 **Boot 3 存在自动配置风险，需试点验证** ⚠️（详见第二节）。
3. **落地建议**：路线 B 升级完成、MP 3.5.x 到位后，**先在 product 模块试点**，验证通过再铺开；验证不过就走备选方案（见第七节）。
4. **强提醒**：同名项目有两个作者！本方案用的是 **`icu.mhb`**（官网 mpj.aicats.cc）；另一个 `com.github.yulichang` 版不是同一个东西，依赖坐标别写混。

---

## 〇.1、实测更新（2026-11-07，沙箱内真跑过）

> 本文档初稿（2026-11-01）的两条关键结论已被实测推翻/修正，**以本节为准**。
> 验证工程：`tmp/mpj-verify`（Spring Boot 3.5.16 + MP 3.5.18 + MPJ 2.1.3 + H2，3 个用例全绿）。

| 初稿说法 | 实测结论 |
|---|---|
| Central 最新只有 2.1.0，2.1.3 未同步 | ❌ **2.1.3 已在 Central**（`icu.mhb:mybatis-plus-join-boot-starter:2.1.3`，含 core / processor） |
| Boot 3 下自动配置「大概率不生效」 | ✅ **生效**。starter 2.1.3 的 jar 里同时有 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` → `MybatisPlusJoinAutoConfig`；实测启动日志：`joinDefaultSqlInjectorOnMiss -> JoinDefaultSqlInjector` 已注册，join 查询正常返回 |
| MP 兼容上限（Gitee 表只写到 3.5.10+） | ✅ **MP 3.5.18 也能跑**（比其编译基线 3.5.11 高 7 个小版本）：join 单查 / 分页 join 均正常 |
| 需要引 `mybatis-plus-boot-starter`（Boot2）的风险 | ✅ 无忧：MPJ 对 `mybatis-plus-boot-starter` / `mybatis-plus-extension` 都是 **provided**，不传递；其自身 pom 导入的 Boot 2.5.3 BOM 只作用于自己，消费者侧的 `spring-boot-starter-parent` dependencyManagement 优先，仍是 Boot 3.5.16 |

**实测 API 形态**（与第三节的示意不同，以这个为准）：

```java
public interface UserMapper extends JoinBaseMapper<User> { }        // BaseMapper → JoinBaseMapper

JoinLambdaWrapper<User> wrapper = new JoinLambdaWrapper<>(User.class);
wrapper.selectAll()                                                 // 主表全字段（无 selectAll(Class) 这种重载）
       .leftJoin(Dept.class, Dept::getId, User::getDeptId)          // 返回 JoinWrapper<Dept, User>
       .select(Dept::getDeptName)                                   // 关联表字段在这里选
       .end()                                                       // 回主 wrapper 继续加条件
       .eq(User::getId, 1L);

List<UserVo> list = userMapper.joinSelectList(wrapper, UserVo.class);
// 分页：userMapper.joinSelectPage(new Page<>(1, 10), wrapper, UserVo.class)
```

生成 SQL：`SELECT t_user.name,t_user.dept_id,t_user.id,t_dept.dept_name FROM t_user t_user LEFT JOIN t_dept t_dept ON t_dept.id = t_user.dept_id WHERE (t_user.id = ?)`
→ 扁平 VO 字段名与列别名一致即自动映射（`t_dept.dept_name` → `deptName`）。

**新踩到的坑（初稿没写）**：

1. **join 目标实体必须有 Mapper**，否则 `leftJoin(Brand.class, ...)` 直接 NPE（`TableInfo` 为 null）——只给主表建 Mapper 不够。
2. **分页 count 默认去掉 join**（`optimizeJoin=true`），实测 count 语句为 `SELECT COUNT(*) AS total FROM t_user t_user`；一对多分页会算错总数，需 `setOptimizeJoin(false)` + `distinct()`。
3. 分页 SQL 实测为直连 `... LIMIT ?`（非嵌套子查询），简单 join 场景性能可接受。

**落地结论**：MPJ 已入 `gu-li-mall-common-datasource`（所有连库模块自动获赠），无需 core + 手写自动配置（初稿的 B2 方案），也不用退到 B1/B3。

---

## 一、MPJ 是什么

| 项       | 内容                                                                                                                          |
|----------|-------------------------------------------------------------------------------------------------------------------------------|
| 定位     | MyBatis-Plus 多表关联查询插件（扩展 MP 的构造器，不改动原构造器）                                                             |
| 核心理念 | 会用 MP 就会用 MPJ：Wrapper 链式写法直接升级为跨表 join                                                                       |
| 特性     | 一对一 / 一对多 / 多对多关联查询、子查询、动态条件、自定义字段映射与结果转换、Lambda 与字符串双查询方式、兼容逻辑删除与多租户 |
| 依赖面   | 仅 lombok；作为 MP 扩展不侵入业务代码（Mapper 继承即可）                                                                      |
| 版本线   | 2.x 基于 APT 生成链式代码（类型安全）+ 经典 Lambda 双模式                                                                     |
| 门槛     | 官方宣称「十分钟掌握全部功能」                                                                                                |

**为什么对谷粒商城有价值**：项目里多表操作极多（见第四节），原课程方案是「多次单表查询 + 内存组装」或「手写 XML join」，代码量大、易错、难维护。MPJ 让 join 查询保持 Wrapper 链式风格，直接写在 Service 层。

---

## 二、版本兼容性（关键结论）

### 2.1 MP 版本对应（官方 README 版本表）

| MyBatis-Plus       | 可用 MPJ 版本                                                     |
|--------------------|-------------------------------------------------------------------|
| 3.2.0              | 1.2.0                                                             |
| 3.3.1 – 3.4.2      | 1.0.2、1.3.4.1                                                    |
| 3.4.3.4 – 3.5.2    | 1.0.3 … 1.3.3（1.x 中期）                                         |
| **3.5.3 – 3.5.10** | **1.3.3.1、1.3.4~1.3.8、2.0.4、2.0.7~2.0.9、2.1.0、2.1.1、2.1.3** |
| 3.5.10+            | 2.1.2、2.1.3                                                      |

> 官网「安装」页的对应表标注为 `3.5.3 - *`（* 表示起始版本之后都可用），比 Gitee README 更激进，两处都指向同一结论：**MP 3.5.x 从 3.5.3 起直接上 MPJ 2.1.x 即可**。

### 2.2 与路线 B 的匹配度

| 维度               | 结论                                                                                                                                                                                                                                                                                                                                                                         |
|--------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Java               | ✅ JDK 8+（我们 17，无压力）                                                                                                                                                                                                                                                                                                                                                 |
| MyBatis-Plus 3.5.x | ✅ 2.1.x 专为 3.5.3+ 设计（boot3 starter 的 MP 3.5.3.1 编译基线）                                                                                                                                                                                                                                                                                                            |
| **Spring Boot 3**  | ⚠️ **风险点**：Maven Central 实际最新版 **2.1.0**（2025-05 发布）的 boot-starter 父工程是 **spring-boot-dependencies 2.5.3**（Boot 2 体系），依赖的是普通 `mybatis-plus-boot-starter`；**官方未提供独立 boot3 starter**，社区也无成规模案例。Boot 3 的自动配置机制变了（`spring.factories` → `AutoConfiguration.imports`），**直接引 boot2 版 starter 大概率自动配置不生效** |

### 2.3 版本号迷雾（写进这里留档）

- 官网首页写 `2.1.3`，安装页与 Maven Central 实际最新是 **2.1.0**；Gitee README 的版本表里 2.1.3 存在但 Central 未同步。
- **统一口径：以 Maven Central 可拉取的 2.1.0 为准**（2.1.3 若私有仓才有，引入成本不值）。

### 2.4 Boot 3 下的应对（按优先序）

1. **试点先行**：product 模块引 `mybatis-plus-join-boot-starter:2.1.0`，跑一次 join 查询验证自动配置是否生效（症状：`JoinWrappers.lambda()` 抛 `MybatisPlusException: 使用 JoinWrappers.lambda(clazz)...` 即为未生效）。
2. 若失效：改引 **core**（`mybatis-plus-join-core`）+ 手写自动配置类（需对照 2.1.0 源码确认要注册的 Bean 集合，属于进阶操作）。
3. 若嫌麻烦：直接评估**备选方案**（第七节）。

---

## 三、接入四步（官方标准姿势）

> 以下为官方快速开始的骨架，落地细节由开发者按模块调整。

| 步骤      | 动作                                         | 说明                                                     |
|-----------|----------------------------------------------|----------------------------------------------------------|
| 1         | Mapper 接口继承 `JoinBaseMapper<T>`          | 替代原 `BaseMapper<T>`                                   |
| 2（可选） | Service 接口继承 `JoinIService<T>`           | 替代原 `IService<T>`                                     |
| 3（可选） | 实现类继承 `JoinServiceImpl<Mapper, Entity>` | 替代原 `ServiceImpl<M, T>`                               |
| 4         | ~~注入 `JoinDefaultSqlInjector`~~            | **2.0.2 起框架自动注入，不再需要**（老教程里这步已废弃） |

依赖坐标（Maven）：

```xml
<dependency>
  <groupId>icu.mhb</groupId>
  <artifactId>mybatis-plus-join-boot-starter</artifactId>
  <version>2.1.0</version>
</dependency>
<!-- APT 链式模式（可选，用于类型安全链式调用） -->
<dependency>
  <groupId>icu.mhb</groupId>
  <artifactId>mybatis-plus-join-processor</artifactId>
  <version>2.1.0</version>
</dependency>
```

---

### 三.1 chain 链式模式（APT 生成，2026-11-07 引入）

- 依赖：`icu.mhb:mybatis-plus-join-processor`（版本随 starter，2.1.3），根 pom `<dependencyManagement>` 管版本。
- ⚠️ **APT 的声明位置很关键**：注解处理器是「编译期 + 按模块」生效，**且 `provided` 作用域不传递** ——
  只把 processor 放 `gu-li-mall-common-datasource` 里，业务模块给实体加 `@JoinChainModel` 也**不会**生成链式类。
  → 已放 **根 pom 的 `<dependencies>`（scope=provided）**：一句覆盖全部 13 个模块，且不进最终产物。
- 用法：实体加 `@JoinChainModel` → 编译期生成 `<实体包>.chain.<实体名>Chain`。
- 实测生成物（H2 验证工程的 User 实体，`target/generated-sources/annotations/verify/entity/chain/UserChain.java`）：

```java
package verify.entity.chain;
public class UserChain extends BaseChainModel<UserChain> {
    public static UserChain create() { return new UserChain(); }
    public UserChain id(Object val) { ... }        // 链式赋值（条件）
    public ChainFieldData _id() { ... }            // 字段元数据，供 select / join 使用
}
```

- 查询入口：`Joins.chain(UserChain.create())` → `JoinChainQueryWrapper`，支持
  `selectAll(chainModel)` / `selectAs` / `eq(fn)` / `oneToOneSelect` / `manyToManySelect` / `join(ChainFieldData, ChainFieldData, ...)`。
- 验证状态：**APT 生成已实测通过**（Boot 3.5.16 + MP 3.5.18 下 `mvn compile` 确实产出 `UserChain.java`）；
  链式查询的运行时用例待补（Lambda 模式已全量验证：join / 分页 join 均通过）。

## 四、谷粒商城多表场景映射（基于本仓库 6 份建表脚本）

### 4.1 场景总表

| #  | 模块/库  | 关联表（真实表名）                                                            | 关联类型         | 典型业务                                            |
|----|----------|-------------------------------------------------------------------------------|------------------|-----------------------------------------------------|
| 1  | pms 商品 | `pms_category_brand_relation` ← `pms_category` + `pms_brand`                  | 多对多（关联表） | 分类-品牌绑定管理                                   |
| 2  | pms 商品 | `pms_attr_attrgroup_relation` ← `pms_attr` + `pms_attr_group`                 | 多对多（关联表） | 属性-属性组维护                                     |
| 3  | pms 商品 | `pms_spu_info` + `pms_brand` + `pms_category` + `pms_spu_info_desc`           | 一对一/多对一    | SPU 详情（品牌名、分类名、图文描述）                |
| 4  | pms 商品 | `pms_sku_info` + `pms_sku_images` + `pms_sku_sale_attr_value`                 | 一对多           | SKU 列表（图片集、销售属性）                        |
| 5  | pms 商品 | `pms_product_attr_value` + `pms_attr`                                         | 多对一           | 商品规格参数回显                                    |
| 6  | oms 订单 | `oms_order` + `oms_order_item`                                                | 一对多           | 订单详情（商品行）                                  |
| 7  | oms 订单 | `oms_order` + `oms_payment_info`                                              | 多对一           | 订单支付状态                                        |
| 8  | wms 库存 | `wms_purchase` + `wms_purchase_detail`                                        | 一对多           | 采购单明细                                          |
| 9  | wms 库存 | `wms_ware_info` + `wms_ware_sku`                                              | 一对多           | 仓库-库存                                           |
| 10 | ums 会员 | `ums_member` + `ums_member_level`                                             | 多对一           | 会员等级信息                                        |
| 11 | ums 会员 | `ums_member` + `ums_member_receive_address`                                   | 一对多           | 收货地址列表                                        |
| 12 | sms 营销 | `sms_coupon` + `sms_coupon_spu_relation` / `sms_coupon_spu_category_relation` | 多对多（关联表） | 优惠券适用范围                                      |
| 13 | sms 秒杀 | `sms_seckill_session` + `sms_seckill_sku_relation`                            | 一对多           | 场次-商品                                           |
| 14 | 跨服务   | 商品上架链路：sku + 品牌 + 分类 + 属性 → ES 模型（`SkuEsModel`）              | 分布式组装       | 检索服务上架（注意：跨服务用 Feign，不走 SQL join） |

### 4.2 改造收益示例（场景 3 为例）

现状（课程风格）：查 SPU → 查品牌 → 查分类 → 查描述，**4 次单表查询 + 内存 Set**。
MPJ 后：一次 join 查询出 SPU + 品牌名 + 分类名 + 描述，Service 层代码量可减半以上，且类型安全（Lambda 引用实体字段，编译期查错）。

> 跨服务场景（#14）**不能用** MPJ（join 只解决单库内多表），保持 Feign 组装——这条是边界，别越界。

---

## 五、典型用法形态（示意，非完整代码）

> 仅展示 API 形态用于评估，落地代码由开发者编写。

**1. 一对一/多对一 join（查 SKU 带品牌名）**

```java
// SkuMapper extends JoinBaseMapper<SkuInfoEntity>
new MPJLambdaWrapper<>(SkuInfoEntity.class)
    .selectAll(SkuInfoEntity.class)
    .select(BrandEntity::getBrandName)          // 目标表字段
    .leftJoin(BrandEntity.class, BrandEntity::getBrandId,
              SkuInfoEntity::getBrandId)          // 关联条件
    .eq(SkuInfoEntity::getSkuId, skuId);
```

**2. 一对多（订单 + 订单项，结果映射集合）**

```java
new MPJLambdaWrapper<>(OrderEntity.class)
    .selectAll(OrderEntity.class)
    .selectCollection(OrderItemEntity.class, OrderDetailVo::getItems)  // 收集到子集合
    .leftJoin(OrderItemEntity.class, OrderItemEntity::getOrderId,
              OrderEntity::getOrderId)
    .eq(OrderEntity::getOrderId, orderId);
```

**3. 多对多（优惠券 + 关联商品）**

```java
new MPJLambdaWrapper<>(CouponEntity.class)
    .selectAll(CouponEntity.class)
    .leftJoin(CouponSpuRelationEntity.class, ...)   // 过关联表
    .leftJoin(SpuInfoEntity.class, ...)
    .eq(CouponEntity::getCouponId, couponId);
```

**4. 分页 join 依然可用**：`mapper.selectJoinPage(page, resultClass, wrapper)`（配合 MP 分页插件）。

---

## 六、注意事项 / 已知坑

1. **distinct / 结果去重**：多对多 join 会产生笛卡尔行，注意结果映射是否需 `selectCount`/去重处理。
2. **分页性能**：join 分页要确认 SQL 形态（MPJ 生成的是嵌套子查询分页还是 limit 直查），大表谨慎。
3. **别名与字段冲突**：两表同名字段要显式 `select` 指定，避免结果集覆盖。
4. **逻辑删除/多租户**：官方称兼容，但组合使用场景少，试点时要专门验证。
5. **`@TableField(exist=false)`** 实体扩展字段的行为需在试点中确认（影响 selectCollection 映射）。
6. **APT processor 与 lombok 顺序**：若用 2.x APT 链式模式，注解处理器配置要正确，否则编译期不生成链式类——**初期可只用 Lambda 模式（processor 可不引）**，降低风险。
7. **与手写 XML 的关系**：复杂报表/超级大 join 仍建议手写 SQL，MPJ 覆盖 80% 场景即可，别硬套。

---

## 七、落地建议

### 7.1 引入时机

1. 等路线 B 升级完成（Boot 3 + MP 3.5.x）后再引入——**不要混在版本升级里做**，出了问题难定位。
2. 优先级排在「阶段 2 工程化重构（补测试/CI）」之后，属于「代码风格/开发效率现代化」范畴。

### 7.2 试点计划（product 模块）

| 步骤 | 内容                                                                              | 验收                          |
|------|-----------------------------------------------------------------------------------|-------------------------------|
| 1    | pom 加 `mybatis-plus-join-boot-starter:2.1.0`（仅 Lambda 模式，先不引 processor） | 编译通过、应用启动            |
| 2    | 挑场景 3（SPU 详情）改造一个查询                                                  | join 查询结果正确、日志无报错 |
| 3    | 验证自动配置是否生效（Boot 3 风险点）                                             | `JoinWrappers` 不抛异常即通过 |
| 4    | 回归原接口（与旧实现结果比对）                                                    | 数据一致                      |
| 5    | 记录结果，决定是否全项目铺开                                                      | 结论写回本文档                |

> **试点状态（2026-11-07）**：已在等价工程（Boot 3.5.16 + MP 3.5.18 + MPJ 2.1.3 + H2）跑通步骤 1、3——join 单查与分页 join 均正常，自动配置生效；结论见本文档 **〇.1 实测更新**。已铺开：MPJ 入 `gu-li-mall-common-datasource`（6 个连库模块自动获赠）。


### 7.3 备选方案（Boot 3 验证不过时的退路）

| 方案                                                                               | 说明                                                                                   | 适合                               |
|------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------|------------------------------------|
| **B1：yulichang 版 MPJ**（`com.github.yulichang:mybatis-plus-join-boot3-starter`） | 同功能另一作者（注意：**与本文档主角不是同一项目**），有专门 boot3 starter，社区案例多 | 想要插件化 join 且 Boot 3          |
| **B2：core + 自配**（icu.mhb 版）                                                  | 引 `mybatis-plus-join-core` 手写 AutoConfiguration                                     | 想留在 icu.mhb 生态且能读源码      |
| **B3：手写 XML / @Select**                                                         | 谷粒商城原方案，MP 原生支持                                                            | 复杂报表类、少量高频查询；零新依赖 |

---

## 八、参考资料

- 官网（文档+安装+快速开始）：https://mpj.aicats.cc/
- Gitee：https://gitee.com/mhb0409/mybatis-plus-join
- GitHub：https://github.com/bobo667/mybatis-plus-join
- Maven Central 坐标核验：`icu.mhb:mybatis-plus-join-boot-starter:2.1.0`
- 谷粒商城表结构依据：本仓库 `doc/数据库/gu_li_mall_{pms,oms,wms,ums,sms,admin}.sql`