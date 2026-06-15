package org.server.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

/**
 * 💡 历史会话列表展示 VO（专供左侧历史列表渲染）
 */
@Data
@Builder
@NoArgsConstructor  // MyBatis/Jackson 序列化安全兜底
@AllArgsConstructor // 配合 @Builder 使用
public class ChatSessionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 💡 会话 ID（对应 ai_chat_session 表的 id，转成 String 扔给前端兼容性最好） */
    private String sessionId;

    /** 💡 会话标题（比如：展示用户的第一句提问，或者“关于跑步鞋的咨询”） */
    private String title;
}