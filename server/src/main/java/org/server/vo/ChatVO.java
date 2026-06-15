package org.server.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor  // 💡 建议加上，MyBatis/Jackson 序列化时更安全
@AllArgsConstructor // 💡 使用 @Builder 时最好配套加上全参构造器
public class ChatVO {

    /** 💡 消息主键 ID（对应 ai_chat_message 表的 id，给前端 v-for 作 key） */
    private Long id;

    /** 💡 角色类型: "user" 或 "ai"（前端靠它分左右气泡） */
    private String role;

    /** AI 回复或用户发送的文本 */
    private String text;

    /** 推荐的商品列表 */
    private List<ProductVO.Simple> products;
}