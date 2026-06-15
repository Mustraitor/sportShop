package org.server.service;

import org.server.dto.ChatDTO;
import org.server.vo.ChatSessionVO;
import org.server.vo.ChatVO;
import java.util.List;

public interface ChatService {
    /**
     * 发送消息并获取 AI 回复
     * @param chatDto 包含 sessionId 和 content
     * @return ChatVO 包含 AI 回复和推荐商品列表
     */
    ChatVO sendMessage(ChatDTO chatDto);

    /**
     * 获取指定会话的历史记录
     * @param sessionId 会话ID
     * @return 历史消息列表，封装为 ChatVO 列表
     */
    List<ChatVO> getHistory(String sessionId);

    List<ChatSessionVO> getSessionsByUserId(Long userId);
}