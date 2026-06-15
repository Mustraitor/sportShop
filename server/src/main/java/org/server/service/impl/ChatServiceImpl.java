package org.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.server.dto.ChatDTO;
import org.server.entity.AiChatMessage;
import org.server.entity.AiChatSession;
import org.server.mapper.AiChatMessageMapper;
import org.server.mapper.AiChatSessionMapper;
import org.server.service.ChatService;
import org.server.service.ProductService;
import org.server.utils.AiClient;
import org.server.vo.ChatSessionVO;
import org.server.vo.ChatVO;
import org.server.vo.ProductVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * AI 导购聊天服务实现
 */
@Slf4j
@Service
public class ChatServiceImpl implements ChatService {

    private final AiChatMessageMapper messageMapper;
    private final AiChatSessionMapper sessionMapper;
    private final ProductService productService;
    private final ObjectMapper objectMapper;
    private final AiClient aiClient;

    // 构造器注入
    public ChatServiceImpl(AiChatMessageMapper messageMapper,
                           AiChatSessionMapper sessionMapper,
                           ProductService productService,
                           ObjectMapper objectMapper,
                           AiClient aiClient) {
        this.messageMapper = messageMapper;
        this.sessionMapper = sessionMapper;
        this.productService = productService;
        this.objectMapper = objectMapper;
        this.aiClient = aiClient;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatVO sendMessage(ChatDTO chatDto) {
        String sessionId = chatDto.getSessionId();
        Long longSessionId = Long.valueOf(sessionId);

        // ======= 1. 真正激活 ai_chat_session 表（加入多端用户关联逻辑） =======
        QueryWrapper<AiChatSession> sessionWrapper = new QueryWrapper<>();
        sessionWrapper.eq("id", longSessionId);
        Long count = sessionMapper.selectCount(sessionWrapper);

        if (count == 0) {
            log.info("发现新会话，正在自动创建并持久化 Session: {}", sessionId);
            AiChatSession newSession = new AiChatSession();
            newSession.setId(longSessionId);

            // 1. 优先获取当前登录的用户ID
            Long currentUserId = chatDto.getUserId();
            if (currentUserId == null) {
                // 如果项目中配置了 BaseContext 拦截器，也可以尝试从上下文中捞一把
                // currentUserId = BaseContext.getCurrentId();
            }

            // 2. 💡 提取用户发送的第一条消息内容，作为动态标题
            String firstMsg = chatDto.getContent(); // 👈 获取前端传过来的 content
            String dynamicTitle = "";

            if (firstMsg != null && !firstMsg.trim().isEmpty()) {
                dynamicTitle = firstMsg.trim();
                // 如果第一句话太长，截取前 14 个字并加上省略号，防止前端侧边栏撑爆
                if (dynamicTitle.length() > 14) {
                    dynamicTitle = dynamicTitle.substring(0, 14) + "...";
                }
            } else {
                // 极端兜底情况（比如用户发了个空消息或者纯表情）
                dynamicTitle = currentUserId == null ? "游客的新会话" : "新导购会话_" + sessionId;
            }

            // 3. 💡 双保险与赋值
            if (currentUserId == null) {
                newSession.setUserId(0L);
                // 游客会话也可以用第一句话作标题，如果你想保留游客标识，可以写成: "【游客】" + dynamicTitle
                newSession.setSessionTitle(dynamicTitle);
            } else {
                newSession.setUserId(currentUserId);
                newSession.setSessionTitle(dynamicTitle); // 👈 告别“用户3的AI对话”，直接换成动态标题！
            }

            newSession.setCreatedAt(LocalDateTime.now());
            newSession.setUpdatedAt(LocalDateTime.now());
            sessionMapper.insert(newSession);
        } else {
            log.info("激活已有历史会话 Session: {}", sessionId);
            // 更新最后活跃时间
            AiChatSession updateSession = new AiChatSession();
            updateSession.setId(longSessionId);
            updateSession.setUpdatedAt(LocalDateTime.now());
            sessionMapper.updateById(updateSession);
        }

        // ======= 2. 保存用户消息 =======
        AiChatMessage userMsg = new AiChatMessage();
        userMsg.setSessionId(Long.valueOf(sessionId));
        userMsg.setRole("user");
        userMsg.setContent(chatDto.getContent());
        userMsg.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(userMsg);

        String aiText = "抱歉，导购小助手暂时无法回复，请稍后再试。";
        List<ProductVO.Simple> recommendedProducts = new ArrayList<>();

        // ======= 3. 调用 AI 并解析 JSON =======
        try {
            CompletableFuture<String> future = aiClient.chatAsync(chatDto.getContent());
            String responseBody = future.get(); // 阻塞等待 AI 响应

            JsonNode root = objectMapper.readTree(responseBody);
            String aiContent = root.path("choices").get(0).path("message").path("content").asText();

            // 清理 Markdown 标记
            String jsonStr = aiContent.replaceAll("```json", "").replaceAll("```", "").trim();

            // 解析 AI 返回的 JSON
            JsonNode aiData = objectMapper.readTree(jsonStr);
            aiText = aiData.path("text").asText(); // 更新回复文案
            JsonNode keywordsNode = aiData.path("product_keywords");

            // 只有 AI 指定了关键词，才进行检索
            if (keywordsNode.isArray() && keywordsNode.size() > 0) {
                for (JsonNode node : keywordsNode) {
                    String keyword = node.asText();
                    log.info("AI 建议搜索关键词: {}", keyword);

                    // 调用服务层检索商品
                    List<ProductVO.Simple> products = productService.searchByKeyword(keyword);
                    if (products != null) {
                        recommendedProducts.addAll(products);
                    }
                }
                // 优化：根据商品 ID 进行去重，防止多个关键词查出相同的商品
                recommendedProducts = recommendedProducts.stream()
                        .distinct()
                        .toList();
            }
        } catch (Exception e) {
            log.error("AI 调用或解析过程发生异常: {}", e.getMessage(), e);
        }

        // ======= 4. 保存 AI 回复消息 =======
        AiChatMessage aiMsg = new AiChatMessage();
        aiMsg.setSessionId(Long.valueOf(sessionId));
        aiMsg.setRole("ai");
        aiMsg.setContent(aiText);
        aiMsg.setCreatedAt(LocalDateTime.now());

        try {
            aiMsg.setProductsJson(objectMapper.writeValueAsString(recommendedProducts));
        } catch (Exception e) {
            log.error("商品 JSON 序列化失败", e);
            aiMsg.setProductsJson("[]");
        }
        messageMapper.insert(aiMsg);

        // ======= 5. 返回带 role 标识的 ChatVO 给前端 =======
        return ChatVO.builder()
                .role("ai")
                .text(aiText)
                .products(recommendedProducts)
                .build();
    }

    /**
     * 💡 关键点 4：将入参由 Long 统一改为 String，保持全局会话ID的数据类型一致
     */
    @Override
    public List<ChatVO> getHistory(String sessionId) {
        QueryWrapper<AiChatMessage> wrapper = new QueryWrapper<>();
        // 这里的列名为 session_id 对应 ai_chat_message 表中的字段
        wrapper.eq("session_id", sessionId)
                .orderByAsc("created_at");
        List<AiChatMessage> messages = messageMapper.selectList(wrapper);

        return messages.stream().map(msg -> {
            List<ProductVO.Simple> products = new ArrayList<>();
            String productsJson = msg.getProductsJson();

            // 如果数据库里存了商品 JSON，解析它
            if (productsJson != null && !productsJson.isEmpty() && !"[]".equals(productsJson)) {
                try {
                    products = objectMapper.readValue(productsJson, new TypeReference<List<ProductVO.Simple>>() {});
                } catch (Exception e) {
                    log.error("解析历史商品 JSON 失败: {}", productsJson, e);
                    products = Collections.emptyList();
                }
            }

            // 返回带完整前端所需信息的 ChatVO 历史数据
            return ChatVO.builder()
                    .id(msg.getId())
                    .role(msg.getRole())
                    .text(msg.getContent())
                    .products(products)
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    public List<ChatSessionVO> getSessionsByUserId(Long userId) {
        // 1. 如果用户未登录，BaseContext 捞出来可能是 null，这里做个兜底拦截
        if (userId == null) {
            log.warn("查询历史会话失败：用户未登录");
            return Collections.emptyList();
        }

        // 2. 构建 MyBatis-Plus 查询条件
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AiChatSession> wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.eq(AiChatSession::getUserId, userId)
                // 💡 核心：按最后更新时间倒序排序，让最近聊过天的会话顶在最上面
                .orderByDesc(AiChatSession::getUpdatedAt);

        // 3. 执行查询
        List<AiChatSession> sessionEntities = sessionMapper.selectList(wrapper);

        // 4. 将持久层 Entity 转化为前端需要的 VO 结构
        return sessionEntities.stream()
                .map(session -> ChatSessionVO.builder()
                        .sessionId(session.getId().toString()) // 💡 Long 转 String，完美契合你前端的 "666" 风格
                        .title(session.getSessionTitle())     // 拿到你在数据库里存的会话标题
                        .build())
                .collect(Collectors.toList());
    }
}