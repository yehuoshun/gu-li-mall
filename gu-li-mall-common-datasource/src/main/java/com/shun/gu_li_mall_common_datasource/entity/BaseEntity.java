package com.shun.gu_li_mall_common_datasource.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体基类：主键 + 创建/更新时间（自动填充）。
 *
 * <p>故意保持最小字段集合——{@code BaseEntity} 的每个字段都会被 MP 写进 SQL，
 * 所以 <b>不要</b>把 {@code version}（乐观锁）、{@code deleted}（逻辑删除）等
 * 「部分表才有」的列塞进来，否则没这列的表会当场 SQL 报错。
 * 需要乐观锁的实体请自建 {@code @Version} 字段，逻辑删除请用 {@code @TableLogic}。</p>
 *
 * <p>时间字段类型统一 {@link LocalDateTime}，配合
 * {@link com.shun.gu_li_mall_common_datasource.handler.MyMetaObjectHandler} 自动填充，
 * 业务代码无需手写 setCreateTime / setUpdateTime。</p>
 *
 * @author yehuoshun
 */
@Getter
@Setter
public class BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键：数据库自增（对应建表语句的 bigint auto_increment） */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 创建时间：插入时自动填充 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间：插入与更新时自动填充 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

}
