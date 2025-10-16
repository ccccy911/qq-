package com.atguigu.qqemail;


import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

// 开启定时任务功能
@Component
@EnableScheduling // 开启定时任务
public class EmailScheduler {

    // 1. 注入邮件服务（间接使用工具类）
    @Autowired
    private EmailService emailService;

    // 2. 注入大模型客户端（用于生成文案）
    @Autowired
    private XunfeiAiClient xunfeiAiClient;

    private final String toemail = "3576471609@qq.com";


    // 每天8点发送可爱风格邮件（Cron表达式：秒 分 时 日 月 周）
    @Scheduled(cron = "0/30 * * * * ?")
    public void sendCuteEmail() {
        try {
            // 步骤1：调用大模型生成可爱风格文案
            String cuteQuote = xunfeiAiClient.generateQuote(1);
            // 步骤2：调用邮件服务发送（true=可爱风格）
            emailService.sendAnimeEmail(cuteQuote, true,toemail);
            System.out.println("可爱风格邮件发送成功！");
        } catch (MessagingException | IOException e) {
            System.err.println("可爱风格邮件发送失败：" + e.getMessage());
            e.printStackTrace();
        }
    }


    // 每天0点发送emo风格邮件
    @Scheduled(cron = "0 0 0 * * ?")
    public void sendEmoEmail() {
        try {
            // 步骤1：调用大模型生成emo风格文案
            String emoQuote = xunfeiAiClient.generateQuote(0);
            // 步骤2：调用邮件服务发送（false=emo风格）
            emailService.sendAnimeEmail(emoQuote, false,toemail);
            System.out.println("emo风格邮件发送成功！");
        } catch (MessagingException | IOException e) {
            System.err.println("emo风格邮件发送失败：" + e.getMessage());
            e.printStackTrace();
        }
    }
}
