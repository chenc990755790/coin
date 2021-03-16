package com.litesoftwares.coingecko.task;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MailService<T> {

    @Autowired
    private MailSender mailSender;


    @Autowired
    private SimpleMailMessage mailMessage;

    @Async
    public void sendMail(T text) {
        try {
            log.info("发送邮件");
            mailMessage.setText(text.toString());
            mailSender.send(mailMessage);
        } catch (MailException e) {
            log.error("发送失败", e);
        }
    }

}
