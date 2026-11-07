package com.shun.gu_li_mall_coupon;

import org.springframework.boot.SpringApplication;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 启动类
 *
 * @author yehuoshun
 */
@SpringBootApplication
@MapperScan("com.shun.gu_li_mall_coupon.mapper")
@EnableDiscoveryClient
public class GuLiMallCouponApplication {

    public static void main(String[] args) {
        SpringApplication.run(GuLiMallCouponApplication.class, args);
    }

}
