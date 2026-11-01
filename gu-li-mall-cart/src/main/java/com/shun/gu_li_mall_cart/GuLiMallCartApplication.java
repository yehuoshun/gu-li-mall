package com.shun.gu_li_mall_cart;

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
public class GuLiMallCartApplication {

    public static void main(String[] args) {
        SpringApplication.run(GuLiMallCartApplication.class, args);
    }

}
