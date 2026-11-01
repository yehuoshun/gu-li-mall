package com.shun.gu_li_mall_coupon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 启动类
 *
 * @author yehuoshun
 */
@SpringBootApplication
@EnableDiscoveryClient
public class GuLiMallCouponApplication {

    public static void main(String[] args) {
        SpringApplication.run(GuLiMallCouponApplication.class, args);
    }

}
