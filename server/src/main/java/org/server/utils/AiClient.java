package org.server.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
public class AiClient {

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${ai.api-key}")
    private String apiKey;

    @Value("${ai.api-url}")
    private String apiUrl;

    public AiClient(OkHttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    public CompletableFuture<String> chatAsync(String userContent) {
        CompletableFuture<String> future = new CompletableFuture<>();

        try {
            // 1. 使用 ObjectMapper 构建 JSON 结构
            ObjectNode root = objectMapper.createObjectNode();
            root.put("model", "gpt-4o-mini");

            ArrayNode messages = root.putArray("messages");

            // 添加系统提示词 (system prompt)
            ObjectNode systemMsg = messages.addObject();
            systemMsg.put("role", "system");
            systemMsg.put("content", "你是一个专业的户外导购助手。当用户询问商品时，请务必以 JSON 格式回复，不要输出额外文字。格式必须为纯JSON，如下：{\"text\": \"回复内容\", \"product_keywords\": [\"关键词1\", \"关键词2\"]}。除此之外不要输出任何 Markdown 格式或多余解释。");

            // 添加用户消息 (user prompt)
            ObjectNode userMsg = messages.addObject();
            userMsg.put("role", "user");
            userMsg.put("content", userContent);

            // 2. 转换为字符串
            String jsonBody = objectMapper.writeValueAsString(root);

            log.info("准备发送给 AI 的 JSON: {}", jsonBody);

            RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json"));
            Request request = new Request.Builder()
                    .url(apiUrl)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .post(body)
                    .build();

            // 3. 执行请求
            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    log.error("网络请求发生异常", e);
                    future.completeExceptionally(e);
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        String errorBody = (response.body() != null) ? response.body().string() : "No response body";
                        log.error("AI API 返回错误，状态码: {}, 详情: {}", response.code(), errorBody);
                        future.completeExceptionally(new IOException("API Error: " + response.code() + ", Body: " + errorBody));
                    } else {
                        String result = response.body() != null ? response.body().string() : "";
                        future.complete(result);
                    }
                }
            });
        } catch (Exception e) {
            log.error("AI 客户端执行出错", e);
            future.completeExceptionally(e);
        }

        return future;
    }
}