package com.atguigu.qqemail;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

@Component
public class AnimeEmailTemplateUtil {

    // 模板文件路径（请确保该路径下存在对应的HTML模板）
    private static final String TEMPLATE_PATH = "templates/anime_email_template.html";

    /**
     * 读取HTML模板文件内容
     * @return 模板字符串
     * @throws IOException 当模板文件不存在或读取失败时抛出
     */
    private String readTemplate() throws IOException {
        // 从类路径加载模板文件
        ClassPathResource resource = new ClassPathResource(TEMPLATE_PATH);
        // 读取文件内容并转换为UTF-8字符串
        return Files.readString(Paths.get(resource.getURI()), StandardCharsets.UTF_8);
    }

    /**
     * 生成最终的邮件HTML内容（不含图片处理逻辑）
     * @param quote 大模型生成的文案内容
     * @param isCute 是否为可爱风格
     * @return 处理后的HTML字符串
     * @throws IOException 模板读取失败时抛出
     */
    public String generateHtmlContent(String quote, boolean isCute) throws IOException {
        // 1. 读取基础模板
        String html = readTemplate();

        // 2. 替换文案内容
        html = html.replace("[大模型生成的文案将显示在这里]", escapeHtml(quote));

        // 3. 切换风格（仅处理CSS类和标题，不涉及图片）
        if (isCute) {
            // 可爱风格配置
            html = html.replace("style-emo", ""); // 移除emo风格类
            html = html.replace("深夜emo时刻 🌙", "美少女的来信 🌸");
        } else {
            // Emo风格配置
            html = html.replace("style-cute", "style-emo"); // 切换风格类
            html = html.replace("美少女的来信 🌸", "深夜emo时刻 🌙");
            html = html.replace("二次元每日推送 ✨ | 用温暖治愈每一天", "月光漫过窗棂时，那些藏在年轮里的故事，正随着晚风轻轻洇入夜色");
        }
        return html;
    }

    /**
     * HTML特殊字符转义（防止文案中的特殊字符破坏HTML结构）
     * @param content 原始文案内容
     * @return 转义后的安全内容
     */
    private String escapeHtml(String content) {
        if (content == null) return "";
        return content.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
