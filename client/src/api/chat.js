import request from '@/utils/request' // 指向你的 axios 配置文件路径

/**
 * 发送聊天消息给 AI 导购助手
 * @param {Object} data 
 * @param {string} data.content - 用户发送的聊天内容
 * @param {string} data.sessionId - 会话ID（比如你的 "666"）
 * @returns {Promise<Object>} 返回一个 Promise，resolve 后直接拿到后端 data 域的数据：{ text: string, products: Array }
 */
export function sendChatMessage(data) {
  return request({
    url: '/chat/send', 
    method: 'post',
    data
  })
}

/**
 * 获取指定会话的历史聊天记录
 * @param {string} sessionId - 会话ID
 * @returns {Promise<Array>} 返回一个 Promise，resolve 后直接拿到后端包装类里的 List<ChatVO> 数组
 */
export function getChatHistory(sessionId) {
  return request({
    // 使用模板字符串把路径参数拼到 URL 后面，与后端的 @GetMapping("/history/{sessionId}") 完美对齐
    url: `/chat/history/${sessionId}`, 
    method: 'get'
  })
}

/**
 * 💡 新增：获取当前登录用户的所有历史会话列表
 * @returns {Promise<Array>} 返回一个 Promise，与后端的 @GetMapping("/sessions") 对齐，返回 List<ChatSessionVO>
 */
export function getChatSessions() {
  return request({
    url: '/chat/sessions',
    method: 'get'
  })
}