package com.atguigu.qqemail;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.codec.binary.Base64;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Component
public class XunfeiAiClient {

    // 从配置文件读取参数
    @Value("${xunfei.appid}")
    private String appid;
    @Value("${xunfei.api-key}")
    private String apiKey;
    @Value("${xunfei.api-secret}")
    private String apiSecret;
    @Value("${xunfei.url}")
    private String url;
    @Value("${xunfei.model}")
    private String model;

    // 存储大模型返回的结果
    private StringBuilder resultBuilder = new StringBuilder();

    /**
     * 调用讯飞星火生成文案
     * @param style 文案风格：早八元气满满/晚上12点的深夜emo
     * @return 生成的文案内容
     */
    public String generateQuote(Integer style) {
        try {
            // 1. 生成鉴权签名（讯飞必须的步骤，用于身份验证）
            String authUrl = getAuthUrl(url, apiKey, apiSecret);
            // 替换HTTP协议为WebSocket（讯飞要求）
            String websocketUrl = authUrl.replace("http://", "ws://").replace("https://", "wss://");

            // 2. 构建请求参数（提示词和模型配置）
            JSONObject request = buildRequest(style);

            // 3. 建立WebSocket连接并发送请求
            resultBuilder.setLength(0); // 清空上次结果
            CountDownLatch latch = new CountDownLatch(1); // 用于等待结果返回
            WebSocketClient client = new WebSocketClient(new URI(websocketUrl)) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    // 连接成功后发送请求
                    send(request.toString());
                }

                @Override
                public void onMessage(String message) {
                    System.out.println(message + "1");
                    // 处理返回的消息
                    parseResponse(message, latch);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    // 连接关闭时释放等待
                    if (latch.getCount() > 0) {
                        latch.countDown();
                    }
                }

                @Override
                public void onError(Exception ex) {
                    ex.printStackTrace();
                    latch.countDown(); // 出错时也释放等待
                }
            };

            client.connect();
            // 等待结果返回（最多等10秒）
            latch.await(10, TimeUnit.SECONDS);
            client.close();

            // 4. 返回生成的文案（如果为空则返回默认值）
            return resultBuilder.length() > 0 ? resultBuilder.toString() : "今日文案生成失败啦～";

        } catch (Exception e) {
            e.printStackTrace();
            return "调用星火API出错：" + e.getMessage();
        }
    }

    /**
     * 生成讯飞要求的鉴权签名（核心步骤）
     */
    private String getAuthUrl(String url, String apiKey, String apiSecret) throws Exception {
        URI uri = new URI(url);
        // 生成时间戳（RFC1123格式）
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        String date = sdf.format(new Date());

        // 构建签名字符串
        String signatureOrigin = "host: " + uri.getHost() + "\n" +
                "date: " + date + "\n" +
                "GET " + uri.getPath() + " HTTP/1.1";

        // 使用HmacSHA256加密
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec spec = new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(spec);
        byte[] signature = mac.doFinal(signatureOrigin.getBytes(StandardCharsets.UTF_8));
        String signatureBase64 = Base64.encodeBase64String(signature);

        // 拼接最终的鉴权URL
        return url + "?authorization=" + Base64.encodeBase64String(
                ("api_key=\"" + apiKey + "\", algorithm=\"hmac-sha256\", headers=\"host date request-line\", signature=\"" + signatureBase64 + "\"").getBytes(StandardCharsets.UTF_8)
        ) + "&date=" + URLEncoder.encode(date, "UTF-8") + "&host=" + uri.getHost();
    }

    /**
     * 构建请求参数（根据讯飞星火的格式要求）
     */
    private JSONObject buildRequest(Integer style) {
        JSONObject request = new JSONObject();

        // 1. 基础配置（匹配文档的header结构）
        JSONObject header = new JSONObject();
        header.put("app_id", appid); // 你的appid
        header.put("uid", UUID.randomUUID().toString().substring(0, 10)); // 用户唯一标识（自定义）
        request.put("header", header);

        // 2. 参数配置（补充tools参数，可选）
        JSONObject parameter = new JSONObject();
        JSONObject chat = new JSONObject();
        chat.put("domain", "x1"); // x1-32k版本固定值
        chat.put("temperature", 0.7); // 随机性
        chat.put("max_tokens", 100); // 最大生成字数
        chat.put("presence_penalty", 1); // 文档示例中的参数，控制重复内容
        chat.put("frequency_penalty", 0.02); // 文档示例中的参数，控制高频词
        chat.put("top_k", 5); // 文档示例中的参数，控制候选词范围

        // 可选：启用网络搜索（如果需要实时信息，如景点、天气等，建议添加）
        JSONArray tools = new JSONArray();
        JSONObject tool = new JSONObject();
        tool.put("type", "web_search");
        JSONObject webSearch = new JSONObject();
        webSearch.put("enable", true); // 开启网络搜索
        webSearch.put("search_mode", "normal");
        tool.put("web_search", webSearch);
        tools.add(tool);
        chat.put("tools", tools); // 添加到chat参数中

        parameter.put("chat", chat);
        request.put("parameter", parameter);

        // 3. 修复payload.message.text结构（数组类型，包含role和content）
        JSONObject payload = new JSONObject();
        JSONObject message = new JSONObject();

        // 关键修正：text是数组，每个元素包含role和content
        JSONArray textArray = new JSONArray();
        JSONObject textItem = new JSONObject();
        textItem.put("role", "user"); // 角色：用户
        if(style == 1){
            textItem.put("content", "直接随机回复我一句少女风元气满满的短文案");
        }
        else if(style == 0){
            textItem.put("content", "直接随机回复我一句深夜emo的短文案，分手伤感");
        }

        textArray.add(textItem); // 放入数组

        message.put("text", textArray); // text字段是数组
        payload.put("message", message);
        request.put("payload", payload);

        return request;
    }


    /**
     * 解析讯飞返回的结果（修复空指针异常）
     */
    private void parseResponse(String message, CountDownLatch latch) {
        JSONObject json = JSON.parseObject(message);

        // 1. 先判断是否有错误（非0表示出错）
        JSONObject header = json.getJSONObject("header");
        if (header != null && header.getInteger("code") != 0) {
            String errorMsg = header.getString("message");
            System.err.println("API返回错误：" + errorMsg);
            latch.countDown(); // 出错时释放等待
            return;
        }

        // 2. 解析生成的内容（核心逻辑）
        if (json.containsKey("payload")) {
            JSONObject payload = json.getJSONObject("payload");
            if (payload.containsKey("choices")) {
                JSONObject choices = payload.getJSONObject("choices");
                // 提取text数组（大模型返回的内容片段）
                JSONArray textArray = choices.getJSONArray("text");
                if (textArray != null && !textArray.isEmpty()) {
                    // 每个元素是包含content的对象
                    JSONObject textItem = textArray.getJSONObject(0);
                    String content = textItem.getString("content");
                    if (content != null && !content.isEmpty()) {
                        resultBuilder.append(content); // 拼接内容片段
                    }
                }

                // 3. 判断是否生成结束（增加非空判断，避免空指针）
                Integer isEnd = choices.getInteger("is_end");
                if (isEnd != null && isEnd == 1) { // 先判断isEnd不为null，再比较
                    latch.countDown(); // 通知等待线程：生成完成
                }
            }
        }
    }

}
