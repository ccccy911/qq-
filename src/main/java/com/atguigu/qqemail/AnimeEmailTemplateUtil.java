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

    // 模板文件路径（固定放在resources/templates目录下）
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
     * 生成最终的邮件HTML内容
     * @param quote 大模型生成的文案内容
     * @param isCute 是否为可爱风格
     * @param cuteImageUrl 可爱风格背景图URL或Base64
     * @param emoImageUrl emo风格背景图URL或Base64
     * @return 处理后的HTML字符串
     * @throws IOException 模板读取失败时抛出
     */
    public String generateHtmlContent(String quote, boolean isCute,
                                      String cuteImageUrl, String emoImageUrl) throws IOException {
        // 1. 读取基础模板
        String html = readTemplate();

        // 2. 替换文案内容
        html = html.replace("[大模型生成的文案将显示在这里]", escapeHtml(quote));

        // 3. 替换背景图片和风格
        if (isCute) {
            // 可爱风格配置
            html = html.replace("可爱风格背景图URL", cuteImageUrl);
            html = html.replace("style-emo", ""); // 移除emo风格类
        } else {
            // Emo风格配置
            html = html.replace("可爱风格背景图URL", emoImageUrl);
            html = html.replace("style-cute", "style-emo"); // 切换风格类
            html = html.replace("今日份可爱请查收 🌸", "深夜emo时刻 🌙");
        }

        return html;
    }

    /**
     * 将本地图片转换为Base64编码（避免邮件图片加载问题）
     * @param imageName 图片文件名（放在resources/static/images目录下）
     * @return Base64编码的图片字符串（可直接用于img标签或background-image）
     * @throws IOException 图片读取失败时抛出
     */
    public String getLocalImageBase64(String imageName) throws IOException {
        // 加载本地图片文件
        ClassPathResource imageResource = new ClassPathResource("static/images/" + imageName);
        // 读取图片字节数组
        byte[] imageBytes = Files.readAllBytes(Paths.get(imageResource.getURI()));
        // 转换为Base64编码并拼接前缀
        return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(imageBytes);
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

