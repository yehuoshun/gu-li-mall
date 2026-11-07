# gu-li-mall-common-datasource

数据源基座模块：MyBatis-Plus + MySQL 驱动 + MPJ（多表 join 插件）。仅**连接 MySQL 的业务模块**依赖本模块
（product / member / coupon / ware / order / seckill）；gateway、search、cart、third-part **不要**依赖。

## 一、模块提供什么

| 内容 | 说明 |
|---|---|
| `BaseEntity` | id（自增）+ createTime / updateTime 自动填充。**故意不含 version / deleted**（每个字段都会进 SQL，没这列的表会报错） |
| `MyMetaObjectHandler` | 公共字段填充器（`strictInsertFill`，不覆盖已有值） |
| `MybatisPlusAutoConfiguration` | 分页 / 乐观锁 / 防全表更新删除插件 + 填充器；经 `AutoConfiguration.imports` 注册，**业务服务无需改包扫描** |
| MPJ | `icu.mhb:mybatis-plus-join-boot-starter`，多表 join（Lambda 版）；仅 Lambda 模式，未引 APT processor |

依赖方仍需在自己的启动类上写 `@MapperScan("com.shun.gu_li_mall_xxx.mapper")`。

## 二、MPJ 用法（已实测，Boot 3.5.16 + MP 3.5.18 + MPJ 2.1.3）

```java
// 1. Mapper 由 BaseMapper 换成 JoinBaseMapper
public interface SpuInfoMapper extends JoinBaseMapper<SpuInfo> { }

// 2. Service 层直接写 join（不用 XML）
JoinLambdaWrapper<SpuInfo> wrapper = new JoinLambdaWrapper<>(SpuInfo.class);
wrapper.selectAll()                                                    // 主表全字段
       .leftJoin(Brand.class, Brand::getBrandId, SpuInfo::getBrandId)   // 关联条件
       .select(Brand::getBrandName)                                     // 关联表字段
       .end()                                                           // 回到主 wrapper
       .eq(SpuInfo::getSpuId, spuId);

List<SpuInfoVo> list = spuInfoMapper.joinSelectList(wrapper, SpuInfoVo.class);   // 列表
// 分页：spuInfoMapper.joinSelectPage(new Page<>(1, 10), wrapper, SpuInfoVo.class)
```

实测生成 SQL：

```sql
SELECT t_user.name,t_user.dept_id,t_user.id,t_dept.dept_name
FROM t_user t_user LEFT JOIN t_dept t_dept ON t_dept.id = t_user.dept_id
WHERE (t_user.id = ?)
```

结果映射：扁平 VO 字段名与列别名一致即可自动映射（`t_dept.dept_name` → `deptName`）。

## 三、坑（实测踩到 / 官方已知）

1. **join 目标实体必须有 Mapper**：只给主表建 Mapper 时，`leftJoin(Brand.class, ...)` 直接 NPE
   （`TableInfo` 为 null）。每个参与 join 的实体都要有对应 Mapper（哪怕只继承 `BaseMapper`）。
2. **分页 count 会去掉 join**：`optimizeJoin=true`（默认）时，count 语句形如 `SELECT COUNT(*) FROM t_user t_user`，
   一对多 join 会**少算/多算总数**。一对多分页需 `new PaginationInnerInterceptor(DbType.MYSQL).setOptimizeJoin(false)` + `distinct()`。
3. **多对多 join 会产生笛卡尔行**，注意 `distinct()` 与去重逻辑。
4. **跨服务查询不要用 MPJ**（join 只解决单库多表）；跨服务保持 Feign 组装。
5. 复杂报表类 SQL 仍建议手写 XML，MPJ 覆盖常规 join 场景即可。
