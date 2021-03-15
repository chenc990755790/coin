package com.litesoftwares.coingecko;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Slf4j
public class CoinApplication {
    public static void main(String[] args) {
        log.info("application start ------");
        SpringApplication.run(CoinApplication.class,args);
    }
}
