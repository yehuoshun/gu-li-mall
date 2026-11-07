package com.shun.gu_li_mall_common_datasource.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.shun.gu_li_mall_common_datasource.handler.MyMetaObjectHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 自动配置：分页 / 乐观锁 / 防全表更新删除 + 公共字段填充。
 *
 * <p>走 Boot 3 的 {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}
 * 注册（{@code spring.factories} 在 Boot 3 已废弃）。业务服务只要依赖本模块即自动生效，
 * <b>无需修改 {@code @SpringBootApplication} 的包扫描范围</b>——
 * 各服务的启动类扫的是自己的包（如 {@code com.shun.gu_li_mall_product}），扫不到本模块，
 * 所以必须以自动配置而非 {@code @Component} 的方式注册。</p>
 *
 * <p>各服务仍需在自己的启动类上写 {@code @MapperScan("com.shun.gu_li_mall_xxx.mapper")}，
 * Mapper 归属业务模块，公共模块不越权指定扫描路径。</p>
 *
 * @author yehuoshun
 */
@Configuration
@ConditionalOnClass(MybatisPlusInterceptor.class)
public class MybatisPlusAutoConfiguration {

    /**
     * 插件链。顺序有讲究：分页 → 乐观锁 → 防全表攻击，不可随意调换。
     *
     * <p>全部用 {@code @ConditionalOnMissingBean} 兜底：业务方若需要自定义（例如加多租户、
     * 改分页上限），自己声明同类型 Bean 即可覆盖本配置。</p>
     */
    @Bean
    @ConditionalOnMissingBean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 分页：方言 MySQL；未设 maxLimit，单页数量由业务自行控制（谷粒有批量导出类查询，硬限制易误伤）
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        // 乐观锁：实体带 @Version 字段时生效（配合 updateById / update(entity, wrapper) 自动加版本条件）
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        // 防全表更新/删除：拦截不带 where 的 update/delete，避免误操作清库
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());
        return interceptor;
    }

    /** 公共字段填充器（createTime / updateTime） */
    @Bean
    @ConditionalOnMissingBean
    public MetaObjectHandler metaObjectHandler() {
        return new MyMetaObjectHandler();
    }

}
