package com.atguigu.qqemail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class EmailService {

    // 1. 注入工具类（Spring自动创建实例，直接用）
    @Autowired
    private AnimeEmailTemplateUtil templateUtil;

    // 2. 注入邮件发送工具和配置参数
    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail; // 发件人邮箱（从application.properties读）



    /**
     * 发送二次元风格邮件（核心方法）
     * @param aiQuote 大模型生成的文案
     * @param isCute true=可爱风格，false=emo风格
     */
    public void sendAnimeEmail(String aiQuote, boolean isCute,String toEmail) throws MessagingException, IOException {

        String htmlContent = templateUtil.generateHtmlContent(
                aiQuote,       // 大模型文案
                isCute);


        //发送HTML邮件
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromEmail);          // 发件人
        helper.setTo(toEmail);              // 收件人
        helper.setSubject(getEmailSubject(isCute)); // 邮件主题（按风格区分）
        helper.setText(htmlContent, true);  // 第二个参数true表示发送HTML内容


        mailSender.send(message); // 发送邮件
    }

    // 辅助方法：根据风格生成邮件主题
    private String getEmailSubject(boolean isCute) {
        return isCute ? "美少女的来信" : "深夜emo时刻 🌙";
    }
}
