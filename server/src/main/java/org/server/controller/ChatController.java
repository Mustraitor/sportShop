package org.server.controller;

import jakarta.validation.Valid;
import org.server.common.Result;
import org.server.dto.ChatDTO;
import org.server.utils.BaseContext;
import org.server.vo.ChatSessionVO;
import org.server.vo.ChatVO;
import org.server.service.ChatService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI 导购聊天控制器
 */
@RestController
@RequestMapping("/chat")
public class ChatController {

    // 使用构造器注入，无需 @Autowired，避免报错且易于测试
    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * 发送聊天信息接口
     * @param chatDto 请求数据（包含会话ID和用户消息）
     * @return Result包装的聊天响应数据
     */
    @PostMapping("/send")
    public Result<ChatVO> send(@Valid @RequestBody ChatDTO chatDto) {
        if (chatDto.getContent() == null || chatDto.getContent().trim().isEmpty()) {
            return Result.error("消息内容不能为空");
        }

        // 💡 借鉴购物车设计：从当前线程上下文中获取真实登录用户 ID
        Long userId = BaseContext.getCurrentId();
        if (userId != null) {
            chatDto.setUserId(userId); // 自动塞入 DTO 传入 Service
        }

        return Result.success(chatService.sendMessage(chatDto));
    }

    /**
     * 获取指定会话的聊天记录接口
     * @param sessionId 会话ID
     * @return 历史消息列表
     */
    @GetMapping("/history/{sessionId}")
    public Result<List<ChatVO>> getHistory(@PathVariable String sessionId) {
        return Result.success(chatService.getHistory(sessionId));
    }
    @GetMapping("/sessions")
    public Result<List<ChatSessionVO>> getSessions() {
        // 从当前线程上下文拿到用户 ID，只查他自己的历史会话
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return Result.error("用户未登录");
        }

        // 去 service 层查询：SELECT session_id, max(content) as title FROM chat_message WHERE user_id = #{userId} GROUP BY session_id
        List<ChatSessionVO> sessions = chatService.getSessionsByUserId(userId);
        return Result.success(sessions);
    }
}