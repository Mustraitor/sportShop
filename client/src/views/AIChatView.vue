<script setup>
import { ref, nextTick, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { sendChatMessage, getChatHistory, getChatSessions } from '@/api/chat' 
import { ElMessage } from 'element-plus'
import { optimizeImage } from '@/utils/image' 

const router = useRouter()

// ===== 侧边栏拖拽调宽 =====
const chatWidth = ref(400) 
const isResizing = ref(false)

const startResizing = () => {
  isResizing.value = true
  document.addEventListener('mousemove', handleMouseMove)
  document.addEventListener('mouseup', stopResizing)
  document.body.style.cursor = 'col-resize' 
}

const handleMouseMove = (e) => {
  if (!isResizing.value) return
  const newWidth = Math.max(300, Math.min(800, e.clientX))
  chatWidth.value = newWidth
}

const stopResizing = () => {
  isResizing.value = false
  document.removeEventListener('mousemove', handleMouseMove)
  document.removeEventListener('mouseup', stopResizing)
  document.body.style.cursor = 'default'
}

const chatList = ref([])
const inputMsg = ref('')
const chatContainer = ref(null)
const isTyping = ref(false)

// 生成纯数字字符串时间戳，确保能被后端的 Long.valueOf() 完美解析
const sessionId = ref(localStorage.getItem('current_chat_session_id') || String(Date.now())) 

// ===== 历史会话状态（已剔除 Mock） =====
const showHistoryDrawer = ref(false)
const sessionList = ref([]) // 初始为空，由后端持久层数据注入

// ===== 右侧商品区 / 移动端抽屉状态 =====
const recommendedProducts = ref([])
const isSearching = ref(false) 
const showMobileDrawer = ref(false) 

// 返回首页逻辑
const goHome = () => {
  router.push('/')
}

// 滚动到底部
const scrollToBottom = async () => {
  await nextTick()
  if (chatContainer.value) {
    chatContainer.value.scrollTop = chatContainer.value.scrollHeight
  }
}

// 打开/关闭商品抽屉（移动端）
const openDrawer = () => { showMobileDrawer.value = true }
const closeDrawer = () => { showMobileDrawer.value = false }

/**
 * 💡 新增方法：从后端异步拉取最新的会话列表
 */
const fetchSessions = async () => {
  try {
    const res = await getChatSessions()
    // 自动适配你的前端 axios 响应拦截器
    sessionList.value = res || [] 
  } catch (err) {
    console.error('实时获取历史会话列表失败:', err)
  }
}

// ===== 💡 核心逻辑：从后端恢复/同步历史聊天纪录 =====
const initChatRoom = async () => {
  isSearching.value = true
  try {
    const history = await getChatHistory(sessionId.value)
    
    if (history && history.length > 0) {
      chatList.value = history.map(item => ({
        id: item.id,
        role: item.role,
        text: item.text,
        hasProducts: item.products && item.products.length > 0,
        products: item.products || []
      }))
      
      const lastAiMsg = [...chatList.value].reverse().find(m => m.role === 'ai' && m.hasProducts)
      if (lastAiMsg) {
        recommendedProducts.value = lastAiMsg.products
      }
    } else {
      // 全新空会话初始状态
      chatList.value = [
        {
          id: 1,
          role: 'ai',
          text: '您好！我是型动派AI智能导购 🏃‍♂️，请告诉我您的运动偏好或需求。\n（💡 提示：您可以发送“我想登山”或“推荐几款慢跑鞋”来体验右侧的商品推荐功能）',
          hasProducts: false,
          products: []
        }
      ]
      recommendedProducts.value = []
    }
    scrollToBottom()
  } catch (err) {
    console.error('加载历史聊天记录失败:', err)
    ElMessage.error('聊天历史记录同步失败')
  } finally {
    isSearching.value = false
  }
}

// 💡 切换会话
const handleSwitchSession = (id) => {
  sessionId.value = id
  localStorage.setItem('current_chat_session_id', id)
  showHistoryDrawer.value = false
  initChatRoom()
}

// 💡 用户新增会话（开启干净的全新聊天）
const handleNewChat = () => {
  // 生成纯数字时间戳字符串，适配后端 Long 类型的会话主键
  const newId = String(Date.now()) 
  handleSwitchSession(newId)
  ElMessage.success('已开启新的导购会话')
}

// 💡 点击历史气泡下的按钮，联动右侧商品看板
const handleShowMessageProducts = (msg) => {
  if (msg.products && msg.products.length > 0) {
    recommendedProducts.value = msg.products
    if (window.innerWidth <= 768) {
      openDrawer()
    }
  }
}

// 挂载时恢复界面
onMounted(() => {
  fetchSessions() // 👈 进页面先拉取左侧会话菜单
  initChatRoom()   // 👈 同步右侧聊天窗口详情
})

// ===== 发送消息核心逻辑 =====
const handleSend = async () => {
  const text = inputMsg.value.trim()
  if (!text) return

  chatList.value.push({ 
    id: Date.now(), 
    role: 'user', 
    text: text, 
    hasProducts: false,
    products: []
  })
  inputMsg.value = ''
  scrollToBottom()

  isTyping.value = true
  isSearching.value = true
  scrollToBottom()

  try {
    const res = await sendChatMessage({
      content: text,
      sessionId: sessionId.value
    })

    isTyping.value = false
    isSearching.value = false

    const hasProducts = res.products && res.products.length > 0
    chatList.value.push({
      id: Date.now() + 1,
      role: res.role || 'ai', 
      text: res.text,
      hasProducts: hasProducts,
      products: res.products || []
    })

    recommendedProducts.value = res.products || []

    // 💡 核心闭环点：发完消息且后端插入会话表后，立马触发一次左侧列表刷新
    await fetchSessions()

    if (!hasProducts && (text.includes('产品') || text.includes('推荐') || text.includes('产品吗没有'))) {
       ElMessage.info('小派在库里没找到完全符合的商品，您可以换个词试试～')
    }

    if (hasProducts && window.innerWidth <= 768) {
      showMobileDrawer.value = true
    }

    scrollToBottom()

  } catch (err) {
    isTyping.value = false
    isSearching.value = false
    
    chatList.value.push({ 
      id: Date.now() + 2, 
      role: 'ai', 
      text: '抱歉，小派刚刚走神了，请稍后再试。', 
      hasProducts: false,
      products: []
    })
    scrollToBottom()
  }
}
</script>

<template>
  <div class="ai-page-layout">
    
    <div class="history-mask" :class="{ 'show': showHistoryDrawer }" @click="showHistoryDrawer = false"></div>
    <div class="history-drawer" :class="{ 'open': showHistoryDrawer }">
      <div class="drawer-header">
        <h3>历史导购会话</h3>
        <button class="close-drawer-btn" @click="showHistoryDrawer = false">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="icon-xs">
            <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>
      <button class="new-chat-btn" @click="handleNewChat">
        <span class="icon">＋</span> 开启新会话
      </button>
      <div class="session-list">
        <div 
          v-for="session in sessionList" 
          :key="session.sessionId" 
          :class="['session-item', { 'active': sessionId === session.sessionId }]"
          @click="handleSwitchSession(session.sessionId)"
        >
          <span class="session-icon">💬</span>
          <span class="session-text" :title="session.title">{{ session.title }}</span>
        </div>
      </div>
    </div>

    <div class="chat-section" :style="{ width: chatWidth + 'px', minWidth: '320px' }">
      <header class="chat-header">
        <div class="header-left">
          <button class="menu-btn" @click="showHistoryDrawer = true" title="查看历史记录">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="icon-sm">
              <path stroke-linecap="round" stroke-linejoin="round" d="M4 6h16M4 12h16M4 18h16" />
            </svg>
            历史
          </button>
          <button class="back-btn" @click="goHome">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="icon-sm">
              <path stroke-linecap="round" stroke-linejoin="round" d="M15 19l-7-7 7-7" />
            </svg>
            首页
          </button>
        </div>
        <div class="header-title">型动派 AI 导购</div>
        <div class="header-right">
          <span class="session-badge">ID: {{ sessionId }}</span>
        </div>
      </header>

      <div class="chat-container" ref="chatContainer">
        <div v-for="msg in chatList" :key="msg.id" :class="['chat-row', msg.role]">
          <div v-if="msg.role === 'ai'" class="avatar ai-avatar">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M9.75 17L9 20l-1 1h8l-1-1-.75-3M3 13h18M5 17h14a2 2 0 002-2V5a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
            </svg>
          </div>
          
          <div class="msg-content">
            <div class="bubble">{{ msg.text }}</div>
            
            <button 
              v-if="msg.role === 'ai' && msg.hasProducts" 
              class="mobile-view-btn" 
              @click="handleShowMessageProducts(msg)"
            >
              <span class="btn-icon">🛍️</span> 查看当前推荐商品
            </button>
          </div>

          <div v-if="msg.role === 'user'" class="avatar user-avatar">我</div>
        </div>

        <div v-show="isTyping" class="chat-row ai">
          <div class="avatar ai-avatar">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M9.75 17L9 20l-1 1h8l-1-1-.75-3M3 13h18M5 17h14a2 2 0 002-2V5a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
            </svg>
          </div>
          <div class="msg-content">
            <div class="bubble typing-indicator">
              <span></span><span></span><span></span>
            </div>
          </div>
        </div>
      </div>

      <div class="input-area">
        <div class="input-wrapper">
          <input 
            v-model="inputMsg" 
            @keyup.enter="handleSend" 
            placeholder="告诉小派运动需求，如夏天爬山穿的鞋..." 
          />
          <button class="send-btn" @click="handleSend" :disabled="!inputMsg.trim() || isTyping">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="icon-send">
              <path stroke-linecap="round" stroke-linejoin="round" d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8" />
            </svg>
          </button>
        </div>
      </div>
    </div>

    <div class="resizer" @mousedown="startResizing" :class="{ 'active': isResizing }"></div>
    
    <div class="drawer-mask" :class="{ 'show': showMobileDrawer }" @click="closeDrawer"></div>
    <div class="product-section" :class="{ 'drawer-open': showMobileDrawer }">
      <div class="product-header">
        <div class="drawer-handler" @click="closeDrawer"></div>
        <div class="header-title-flex">
          <h2>为您推荐的商品</h2>
          <span class="count-tag" v-if="recommendedProducts.length > 0">共 {{ recommendedProducts.length }} 款精品</span>
        </div>
      </div>

      <div class="product-content">
        <div v-if="isSearching" class="status-box">
          <div class="loading-spinner"></div>
          <p>正在为您在海量商品中检索...</p>
        </div>

        <div v-else-if="recommendedProducts.length === 0" class="status-box empty">
          <svg viewBox="0 0 24 24" fill="none" stroke="#cfd8dc" stroke-width="1.5">
            <path stroke-linecap="round" stroke-linejoin="round" d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4"/>
          </svg>
          <p>告诉AI您的需求，智能导购推荐将呈现在此</p>
        </div>

        <div v-else class="product-grid">
          <div v-for="item in recommendedProducts" :key="item.id" class="product-card" @click="router.push(`/product/${item.id}`)">
            <div class="img-wrap">
              <img :src="optimizeImage(item.mainImage || item.image, 400)" :alt="item.name" loading="lazy" />
            </div>
            <div class="card-info">
              <p class="p-name" :title="item.name">{{ item.name }}</p>
              <div class="p-bottom">
                <p class="p-price"><span class="currency">￥</span>{{ item.price }}</p>
                <button class="view-detail-btn" @click.stop="router.push(`/product/${item.id}`)">
                  详情
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

  </div>
</template>

<style lang="scss" scoped>
// ===== 现代蓝白主题全局变量 =====
$brand-blue: #0084ff;
$brand-blue-hover: #1a91ff;
$brand-blue-light: #e6f3ff;
$bg-main: #f4f6f9;
$bg-chat: #f8fafc;
$text-main: #2c3e50;
$text-muted: #909399;
$border-color: #e4e7ed;
$radius-lg: 16px;
$radius-md: 10px;
$shadow-sm: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
$shadow-md: 0 4px 20px 0 rgba(0, 0, 0, 0.08);

// SVG 图标通用微调
.icon-sm { width: 16px; height: 16px; }
.icon-xs { width: 14px; height: 14px; }

// ===== 全局分栏布局 =====
.ai-page-layout {
  display: flex;
  width: 100%;
  height: 100vh;
  background: $bg-main;
  overflow: hidden;
  font-family: -apple-system, BlinkMacSystemFont, "Helvetica Neue", Arial, sans-serif;

  // 左右区域拖拽手柄
  .resizer {
    width: 4px;
    height: 100%;
    background: darken($bg-main, 2%);
    cursor: col-resize;
    transition: all 0.2s ease;
    z-index: 20;

    &:hover,
    &.active {
      width: 6px;
      background: $brand-blue;
      box-shadow: 0 0 8px rgba(0, 132, 255, 0.4);
    }
  }

  .chat-section {
    flex-shrink: 0;
    background: #ffffff;
    display: flex;
    flex-direction: column;
    z-index: 10;
  }

  .product-section {
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow: hidden;
  }
}

// ===== 左侧：高级历史抽屉组件 =====
.history-mask {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: rgba(15, 23, 42, 0.3);
  backdrop-filter: blur(2px);
  z-index: 999;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  
  &.show { opacity: 1; pointer-events: auto; }
}

.history-drawer {
  position: fixed;
  top: 0;
  left: -300px;
  width: 300px;
  height: 100vh;
  background: #ffffff;
  box-shadow: 4px 0 25px rgba(0, 0, 0, 0.08);
  z-index: 1000;
  display: flex;
  flex-direction: column;
  transition: left 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  padding: 20px;
  box-sizing: border-box;

  &.open { left: 0; }

  .drawer-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    
    h3 {
      margin: 0;
      font-size: 16px;
      color: $text-main;
      font-weight: 600;
    }
    
    .close-drawer-btn {
      background: #f4f6f9;
      border: none;
      width: 28px;
      height: 28px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      color: $text-muted;
      transition: all 0.2s;
      &:hover { background: #e4e7ed; color: #333; }
    }
  }

  .new-chat-btn {
    width: 100%;
    padding: 12px;
    background: $brand-blue;
    color: white;
    border: none;
    border-radius: $radius-md;
    font-size: 14px;
    font-weight: 500;
    cursor: pointer;
    margin-bottom: 20px;
    box-shadow: 0 4px 12px rgba(0, 132, 255, 0.2);
    transition: all 0.2s ease;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 6px;

    &:hover {
      background: $brand-blue-hover;
      transform: translateY(-1px);
      box-shadow: 0 6px 16px rgba(0, 132, 255, 0.3);
    }
  }

  .session-list {
    flex: 1;
    overflow-y: auto;
    padding-right: 2px;

    &::-webkit-scrollbar { width: 4px; }
    &::-webkit-scrollbar-thumb { background: #e4e7ed; border-radius: 2px; }

    .session-item {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 12px 14px;
      border-radius: $radius-md;
      background: $bg-main;
      margin-bottom: 10px;
      cursor: pointer;
      font-size: 14px;
      color: #4a5568;
      border: 1px solid transparent;
      transition: all 0.25s ease;

      .session-icon { font-size: 14px; opacity: 0.8; }
      .session-text {
        flex: 1;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }

      &:hover {
        background: $brand-blue-light;
        color: $brand-blue;
      }

      &.active {
        background: #ffffff;
        color: $brand-blue;
        border-color: rgba($brand-blue, 0.3);
        font-weight: 600;
        box-shadow: $shadow-sm;
        position: relative;
        
        &::before {
          content: '';
          position: absolute;
          left: 0;
          top: 25%;
          height: 50%;
          width: 4px;
          background: $brand-blue;
          border-radius: 0 4px 4px 0;
        }
      }
    }
  }
}

// ===== 中间：核心聊天面板 =====
.chat-section {
  border-right: 1px solid $border-color;
  box-shadow: 4px 0 15px rgba(0, 0, 0, 0.01);
}

.chat-header {
  height: 64px;
  background: #ffffff;
  border-bottom: 1px solid $border-color;
  color: $text-main;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 16px;

  .header-left {
    display: flex;
    align-items: center;
    gap: 8px;

    .menu-btn, .back-btn {
      background: #ffffff;
      border: 1px solid $border-color;
      padding: 6px 12px;
      border-radius: 20px;
      cursor: pointer;
      font-size: 13px;
      color: #4a5568;
      display: flex;
      align-items: center;
      gap: 4px;
      transition: all 0.2s ease;

      &:hover {
        background: $bg-main;
        border-color: darken($border-color, 5%);
        color: $brand-blue;
      }
    }
  }

  .header-title {
    font-weight: 600;
    font-size: 16px;
    letter-spacing: 0.5px;
  }

  .session-badge {
    background: $brand-blue-light;
    color: $brand-blue;
    padding: 4px 10px;
    border-radius: 20px;
    font-size: 11px;
    font-weight: 500;
  }
}

.chat-container {
  flex: 1;
  padding: 24px 16px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 22px;
  background: $bg-chat;
}

.chat-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  width: 100%;
  animation: fadeIn 0.3s ease;

  &.user { justify-content: flex-end; }
  &.ai { justify-content: flex-start; }
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: bold;
  flex-shrink: 0;
  box-shadow: $shadow-sm;
}

.ai-avatar {
  background: linear-gradient(135deg, $brand-blue, #00c6ff);
  color: #fff;
  padding: 8px;
  box-sizing: border-box;
}

.user-avatar {
  background: #ffffff;
  color: $brand-blue;
  border: 1px solid $border-color;
}

.msg-content {
  max-width: 75%;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
}

.bubble {
  padding: 12px 16px;
  border-radius: 18px;
  line-height: 1.6;
  font-size: 14px;
  white-space: pre-wrap;
  word-break: break-all;
  box-shadow: 0 1px 2px rgba(0,0,0,0.02);
}

.chat-row.ai .bubble {
  background: #ffffff;
  border: 1px solid rgba(228, 231, 237, 0.7);
  border-top-left-radius: 4px;
  color: #2c3e50;
  box-shadow: $shadow-sm;
}

.chat-row.user .bubble {
  background: linear-gradient(135deg, $brand-blue, #0077e6);
  color: #ffffff;
  border-top-right-radius: 4px;
}

// 联动商品按钮样式
.mobile-view-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 14px;
  margin-top: 4px;
  background: #ffffff;
  color: $brand-blue;
  border: 1px solid rgba($brand-blue, 0.3);
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  box-shadow: $shadow-sm;
  transition: all 0.2s ease;

  &:hover {
    background: $brand-blue;
    color: white;
    border-color: $brand-blue;
    transform: translateY(-1px);
  }
}

// 输入框底座
.input-area {
  padding: 16px;
  background: #ffffff;
  border-top: 1px solid $border-color;

  .input-wrapper {
    display: flex;
    background: $bg-main;
    border-radius: 24px;
    padding: 4px 4px 4px 16px;
    align-items: center;
    border: 1px solid transparent;
    transition: all 0.2s ease;

    &:focus-within {
      background: #ffffff;
      border-color: $brand-blue;
      box-shadow: 0 0 0 3px rgba(0, 132, 255, 0.1);
    }
  }

  input {
    flex: 1;
    height: 36px;
    background: transparent;
    border: none;
    outline: none;
    font-size: 14px;
    color: $text-main;

    &::placeholder { color: $text-muted; }
  }

  .send-btn {
    width: 36px;
    height: 36px;
    background: $brand-blue;
    color: #fff;
    border: none;
    border-radius: 50%;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.2s ease;

    .icon-send { width: 16px; height: 16px; margin-left: 2px; }

    &:hover:not(:disabled) {
      background: $brand-blue-hover;
      transform: scale(1.05);
    }

    &:disabled {
      background: #dcdfe6;
      cursor: not-allowed;
    }
  }
}

// ===== 右侧：商品网格看板区域 =====
.product-section {
  background: $bg-main;
}

.product-header {
  padding: 24px 30px 16px;

  .header-title-flex {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  h2 {
    margin: 0;
    font-size: 18px;
    color: $text-main;
    font-weight: 600;
  }

  .count-tag {
    font-size: 12px;
    background: #ffffff;
    padding: 2px 8px;
    border-radius: 12px;
    color: $text-muted;
    border: 1px solid $border-color;
  }
}

.product-content {
  flex: 1;
  padding: 0 30px 30px 30px;
  overflow-y: auto;
}

.status-box {
  height: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: $text-muted;
  font-size: 14px;
  gap: 12px;

  &.empty svg {
    width: 64px;
    height: 64px;
    opacity: 0.7;
  }
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(210px, 1fr));
  gap: 20px;
}

.product-card {
  background: #ffffff;
  border-radius: $radius-lg;
  overflow: hidden;
  box-shadow: $shadow-sm;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  display: flex;
  flex-direction: column;
  border: 1px solid rgba(228, 231, 237, 0.5);

  &:hover {
    transform: translateY(-4px);
    box-shadow: $shadow-md;
    border-color: rgba($brand-blue, 0.2);
  }

  .img-wrap {
    width: 100%;
    aspect-ratio: 1 / 1;
    background: #ffffff;
    padding: 12px;
    box-sizing: border-box;
    border-bottom: 1px solid #f2f6fc;

    img {
      width: 100%;
      height: 100%;
      object-fit: contain;
      transition: transform 0.3s ease;
    }
  }
  
  &:hover .img-wrap img {
    transform: scale(1.03);
  }

  .card-info {
    padding: 14px;
    display: flex;
    flex-direction: column;
    flex: 1;
    justify-content: space-between;

    .p-name {
      font-size: 13px;
      color: $text-main;
      font-weight: 500;
      margin: 0 0 12px 0;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
      height: 40px;
      line-height: 1.5;
    }

    .p-bottom {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    .p-price {
      font-size: 18px;
      font-weight: 700;
      color: #ff4d4f;
      margin: 0;
      
      .currency { font-size: 12px; font-weight: 500; }
    }
  }

  .view-detail-btn {
    padding: 5px 14px;
    background: $brand-blue-light;
    color: $brand-blue;
    border: none;
    border-radius: 20px;
    font-size: 12px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.2s ease;

    &:hover {
      background: $brand-blue;
      color: #ffffff;
    }
  }
}

// ===== 打字机动画闪烁指示器 =====
.typing-indicator {
  display: flex;
  gap: 4px;
  align-items: center;
  padding: 4px 8px;

  span {
    width: 6px;
    height: 6px;
    background: $text-muted;
    border-radius: 50%;
    animation: bounce 1.4s infinite ease-in-out both;

    &:nth-child(1) { animation-delay: -0.32s; }
    &:nth-child(2) { animation-delay: -0.16s; }
  }
}

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(6px); }
  to { opacity: 1; transform: translateY(0); }
}

// =========================================
// 📱 移动端响应式高级适配适配
// =========================================
@media screen and (max-width: 768px) {
  .ai-page-layout {
    display: block;
    height: 100dvh;
    position: relative;
    
    .resizer { display: none; }
    
    .chat-section {
      width: 100% !important;
      height: 100%;
      border: none;
    }
  }

  .msg-content { max-width: 85%; }

  // 移动端底部遮罩与拉伸抽屉
  .drawer-mask {
    display: block;
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background: rgba(15, 23, 42, 0.4);
    backdrop-filter: blur(1px);
    z-index: 90;
    opacity: 0;
    visibility: hidden;
    transition: opacity 0.3s;

    &.show { opacity: 1; visibility: visible; }
  }

  .product-section {
    position: fixed;
    bottom: 0;
    left: 0;
    width: 100%;
    height: 75vh;
    background: $bg-main;
    border-radius: 24px 24px 0 0;
    z-index: 100;
    box-shadow: 0 -8px 30px rgba(0, 0, 0, 0.15);
    transform: translateY(100%);
    transition: transform 0.4s cubic-bezier(0.16, 1, 0.3, 1);

    &.drawer-open { transform: translateY(0); }
  }

  .product-header {
    padding: 12px 20px 12px;
    background: #ffffff;
    border-radius: 24px 24px 0 0;
    border-bottom: 1px solid $border-color;
  }

  .drawer-handler {
    display: block;
    width: 36px;
    height: 5px;
    background: #e4e7ed;
    border-radius: 3px;
    margin: 0 auto 12px auto;
    cursor: pointer;
  }

  .product-content { padding: 15px 15px 20px 15px; }
  .product-grid { grid-template-columns: repeat(2, 1fr); gap: 12px; }
  .product-card .card-info .p-price { font-size: 16px; }
  
  .input-area {
    padding-bottom: calc(12px + env(safe-area-inset-bottom));
  }
}
</style>