package org.server.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatDTO {
    /** 会话ID */
    private String sessionId;

    /** 💡 新增：当前登录的用户ID（可不传，走游客模式） */
    private Long userId;

    /** 用户发送的文本内容 */
    @NotBlank(message = "消息内容不能为空")
    private String content;
}