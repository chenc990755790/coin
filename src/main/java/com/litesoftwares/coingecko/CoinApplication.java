package com.litesoftwares.coingecko;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@Slf4j
@EnableAsync
@EnableScheduling
public class CoinApplication {

    public static void main(String[] args) {
        log.info("application start ------");
        SpringApplication.run(CoinApplication.class,args);
    }
}
