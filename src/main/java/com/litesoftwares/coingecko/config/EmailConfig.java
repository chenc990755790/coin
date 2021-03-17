package com.litesoftwares.coingecko.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@Configuration
@Data
public class EmailConfig {

    @Bean
    @ConfigurationProperties(prefix = "spring.mail")
    public MailSender javaMailSender() {
        JavaMailSenderImpl javaMailSender = new JavaMailSenderImpl();
        return javaMailSender;
    }

    @Bean
    @ConfigurationProperties(prefix = "mess")
    public SimpleMailMessage message() {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setSubject("突破新高");
        return message;
    }

}
