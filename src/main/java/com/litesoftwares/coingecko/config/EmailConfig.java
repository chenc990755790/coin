package com.litesoftwares.coingecko.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.SimpleMailMessage;

@Configuration
@Data
public class EmailConfig {

    @Bean
    @ConfigurationProperties(prefix = "mess")
    public SimpleMailMessage message() {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setSubject("突破新高");
        return message;
    }

}
