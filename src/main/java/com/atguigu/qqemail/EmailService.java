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
        // 步骤1：用工具类获取本地图片的Base64（避免邮件图片加载失败）
        String cuteBgBase64 = templateUtil.getLocalImageBase64("20200524001745_kz4xd.jpeg"); // 可爱图文件名
        String emoBgBase64 = templateUtil.getLocalImageBase64("20200524001745_kz4xd.jpeg");   // emo图文件名

        // 步骤2：调用工具类生成最终HTML内容
        String htmlContent = templateUtil.generateHtmlContent(
                aiQuote,       // 大模型文案
                isCute,        // 风格标识
                cuteBgBase64,  // 可爱背景图Base64
                emoBgBase64    // emo背景图Base64
        );

        int bgStart = htmlContent.indexOf("background-image: url(");
        int bgEnd = htmlContent.indexOf(")", bgStart) + 1;
        System.out.println("背景图HTML：" + htmlContent.substring(bgStart, bgEnd));

        // 步骤3：发送HTML邮件
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
        return isCute ? "今日份可爱请查收 🌸" : "深夜emo时刻 🌙";
    }
}
