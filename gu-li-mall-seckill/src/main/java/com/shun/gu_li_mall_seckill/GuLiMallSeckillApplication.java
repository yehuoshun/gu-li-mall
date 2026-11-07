package com.shun.gu_li_mall_seckill;

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
@MapperScan("com.shun.gu_li_mall_seckill.mapper")
@EnableDiscoveryClient
public class GuLiMallSeckillApplication {

    public static void main(String[] args) {
        SpringApplication.run(GuLiMallSeckillApplication.class, args);
    }

}
