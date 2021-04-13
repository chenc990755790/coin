package com.litesoftwares.coingecko.task;

import com.litesoftwares.coingecko.domain.PriceOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;
import java.io.UnsupportedEncodingException;
import java.util.List;


@Service
@Slf4j
public class MailService {

    @Autowired
    private JavaMailSenderImpl mailSender;

    @Autowired
    private SimpleMailMessage mailMessage;
    @Autowired
    TemplateEngine templateEngine;

    @Async
    public void sendMail(String text, boolean onlyOwn) {
        try {
            log.info("发送邮件");
            mailMessage.setText(text);
            if (onlyOwn) {
                sendMailOnlyOwe();
            }
            mailSender.send(mailMessage);
            log.info("邮件发送成功");
        } catch (MailException e) {
            log.error("发送失败", e);
        }
    }

    public void sendMailOnlyOwe() {
        mailMessage.setSubject("火币专有");
        mailMessage.setTo(mailMessage.getTo()[0]);
    }

    public void sendhtmlmail(List<PriceOrder> priceOrderList) throws MessagingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);
        helper.setSubject("突破新高");
        helper.setFrom(mailMessage.getFrom());
        helper.setTo(mailMessage.getTo());
        Context context = new Context();
        context.setVariable("orderList", priceOrderList);
        String process = templateEngine.process("priceorder.html", context);
        // 第二个参数true表示这是一个html文本
        helper.setText(process, true);
        mailSender.send(mimeMessage);
    }


}
