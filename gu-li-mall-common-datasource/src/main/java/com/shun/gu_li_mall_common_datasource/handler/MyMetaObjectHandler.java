package com.shun.gu_li_mall_common_datasource.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;

import java.time.LocalDateTime;

/**
 * 公共字段自动填充：createTime / updateTime。
 *
 * <p>用 {@code strictInsertFill} / {@code strictUpdateFill} 而非 {@code setFieldValByName}：
 * 前者只在实体**确实声明了**带 {@code fill} 的字段时才写入，不会给无关实体凭空加字段；
 * 且字段已有值时不覆盖（便于数据迁移/补数据时手工指定时间）。</p>
 *
 * <p>本类不挂 {@code @Component}——由 {@code MybatisPlusAutoConfiguration} 以 Bean 方式注册，
 * 这样依赖本模块的服务无需做包扫描即可生效。</p>
 *
 * @author yehuoshun
 */
public class MyMetaObjectHandler implements MetaObjectHandler {

    private static final String CREATE_TIME = "createTime";
    private static final String UPDATE_TIME = "updateTime";

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        this.strictInsertFill(metaObject, CREATE_TIME, LocalDateTime.class, now);
        this.strictInsertFill(metaObject, UPDATE_TIME, LocalDateTime.class, now);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, UPDATE_TIME, LocalDateTime.class, LocalDateTime.now());
    }

}
