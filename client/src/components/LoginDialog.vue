<script setup>
import { ref, reactive } from 'vue'
import { useUserStore } from '@/stores/user'
import { loginApi, registerApi } from '@/api/user'   // 导入 loginApi 和 registerApi
const userStore = useUserStore()
const showTabs = true
const activeTab = ref('account')
const loading = ref(false)
/** 表单数据 */
const loginForm = reactive({
  username: '',
  password: ''
})

/** 账号登录 */
const handleLogin = async () => {
  if (loading.value) return
  loading.value = true

  try {
    let guestIdToMerge = null
    const storeStr = localStorage.getItem('user-store')
    if (storeStr) {
      try {
        const storeObj = JSON.parse(storeStr)
        guestIdToMerge = storeObj.guestId 
      } catch (e) {
        console.error('解析 user-store 失败', e)
      }
    }

    const resData = await loginApi({
      username: loginForm.username,
      password: loginForm.password,
      guestId: guestIdToMerge // 这里传给后端，后端就能正确合并了
    })

    // 登录成功后，调用 Store 的逻辑进行状态清理
    userStore.setLoginInfo(resData.token, {
      userId: resData.userId,
      userName: resData.userName
    })

    loginForm.username = ''
    loginForm.password = ''
  } catch (err) {
    console.error('登录失败:', err)
  } finally {
    loading.value = false
  }
}

/** 短信登录（预留） */
const handleSMSLogin = async () => {
  console.log('短信登录功能待实现')
}

const registerForm = reactive({
  username: '',
  phone: '',
  code: '',
  password: '',
  confirmPwd: ''
})

const handleRegister = async () => {
  // 前端校验（保持不变）
  if (!registerForm.username || !registerForm.password) {
    alert('请填写完整信息')
    return
  }
  if (registerForm.password !== registerForm.confirmPwd) {
    alert('两次密码输入不一致')
    return
  }
  if (registerForm.password.length < 6) {
    alert('密码长度至少6位')
    return
  }

  try {
    // 调用注册 API - 增加 confirmPassword 字段
    await registerApi({
      username: registerForm.username,
      password: registerForm.password,
      confirmPassword: registerForm.confirmPwd   
    })
    alert('注册成功，请登录')
    // 清空表单
    registerForm.username = ''
    registerForm.password = ''
    registerForm.confirmPwd = ''
    // 切换回登录页
    activeTab.value = 'account'
  } catch (err) {
    console.error('注册失败:', err)
    alert(err.response?.data?.msg || '注册失败，请稍后重试')
  }
}
</script>
<template>
  <Transition name="fade">
    <div 
      v-if="userStore.isLoginVisible"
      class="modal-overlay"
      @click.self="userStore.hideLogin"
    >
      <div class="login-card">
        <!-- 关闭按钮 -->
        <button class="close-btn" @click="userStore.hideLogin">×</button>

        <!-- === Tabs（可隐藏） === -->
        <div v-if="showTabs" class="login-tabs">
          <div 
            :class="['tab', activeTab === 'account' && 'active']"
            @click="activeTab = 'account'"
          >
            账号登录
          </div>

          <div 
            :class="['tab', activeTab === 'sms' && 'active']"
            @click="activeTab = 'sms'"
          >
            短信登录
          </div>
        </div>

        <!-- 标题 -->
        <!-- <div class="login-header">
          <h2>欢迎回来</h2>
          <p>登录以开启您的“型动”之旅</p>
        </div> -->

        <!-- === 账号密码登录 === -->
        <form
          v-if="activeTab === 'account'"
          @submit.prevent="handleLogin"
          class="login-form"
        >
          <div class="input-group">
            <label>用户名</label>
            <input
              v-model="loginForm.username"
              type="text"
              placeholder="请输入用户名"
              required
            />
          </div>

          <div class="input-group">
            <label>密码</label>
            <input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              required
            />
            <span class="forgot-pwd">忘记密码？</span>
          </div>

          <button type="submit" class="submit-btn" :disabled="loading">
            <span>登录</span>
            <!-- <span v-else class="loader"></span> -->
          </button>
          <div class="login-footer">
          还没有账号？<span class="register-link"  :class="['tab', activeTab === 'register' && 'active']"
            @click="activeTab = 'register'">立即注册</span>
        </div>
        </form>

        <!-- === 短信登录（预留） === -->
        <form
          v-else-if="activeTab === 'sms'"
          @submit.prevent="handleSMSLogin"
          class="login-form"
        >
          <div class="input-group">
            <label>手机号</label>
            <input type="text" placeholder="请输入手机号" />
          </div>

          <div class="input-group">
            <label>验证码</label>
            <div class="sms-row">
              <input type="text" placeholder="短信验证码" />
              <button class="send-btn" type="button">发送</button>
            </div>
          </div>

          <button type="submit" class="submit-btn">登录</button>
        </form>
        <!-- === 注册账号 === -->
        <form
          v-else-if="activeTab === 'register'"
          @submit.prevent="handleRegister"
          class="login-form"
        >
          <div class="input-group">
            <label>用户名</label>
            <input v-model="registerForm.username" type="text" placeholder="请输入用户名" />
          </div>
          <div class="input-group">
            <label>密码</label>
            <input v-model="registerForm.password" type="password" placeholder="请输入密码" />
          </div>

          <div class="input-group">
            <label>确认密码</label>
            <input v-model="registerForm.confirmPwd" type="password" placeholder="请再次输入密码" />
          </div>

          <button type="submit" class="submit-btn">
            注册
          </button>
        </form>
        <!-- 底部提示 -->

      </div>
    </div>
  </Transition>
</template>



<style scoped>
/* 淡入淡出动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 遮罩层 */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(5px);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 3000;
}

/* 登录卡片 */
.login-card {
  background: #fff;
  width: 100%;
  max-width: 420px;
  padding: 36px 40px;
  border-radius: 22px;
  position: relative;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.12);
}

/* 关闭按钮 */
.close-btn {
  position: absolute;
  top: 14px;
  right: 16px;
  background: transparent;
  border: none;
  font-size: 22px;
  cursor: pointer;
  color: #999;
  transition: 0.2s;
}
.close-btn:hover {
  color: var(--main-blue);
}

/* Tabs */
.login-tabs {
  display: flex;
  justify-content: center;
  margin-bottom: 22px;
  gap: 20px;
}
.tab {
  padding: 6px 14px;
  border-radius: 14px;
  cursor: pointer;
  font-size: 14px;
  color: #666;
  transition: 0.2s;
}
.tab.active {
  background: var(--main-blue);
  color: #fff;
}

/* 标题区 */
.login-header {
  text-align: center;
  margin-bottom: 28px;
}
.login-header h2 {
  margin: 0;
  font-size: 22px;
  color: var(--text-main);
}
.login-header p {
  margin: 4px 0 0;
  font-size: 14px;
  color: var(--text-sub);
}

/* 输入框组 */
.input-group {
  margin-bottom: 18px;
  position: relative;
}
label {
  display: block;
  font-size: 14px;
  margin-bottom: 6px;
  color: var(--text-main);
}
input {
  width: 100%;
  padding: 12px 14px;
  border-radius: 12px;
  border: 1px solid #ddd;
  background: var(--input-bg);
  transition: 0.2s;
}
input:focus {
  border-color: var(--main-blue);
  box-shadow: 0 0 0 2px rgba(0, 114, 255, 0.15);
}

/* 忘记密码 */
.forgot-pwd {
  font-size: 12px;
  color: var(--main-blue);
  position: absolute;
  right: 0;
  bottom: -20px;
  cursor: pointer;
}

/* 按钮 */
.submit-btn {
  width: 100%;
  padding: 12px 0;
  background: var(--main-blue);
  border: none;
  border-radius: 14px;
  color: #fff;
  font-size: 16px;
  cursor: pointer;
  margin-top: 8px;
  transition: 0.2s;
}
.submit-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}
.submit-btn:hover:not(:disabled) {
  background: var(--main-blue-dark);
}

/* 加载动画 */
.loader {
  width: 18px;
  height: 18px;
  border: 3px solid #fff;
  border-top-color: transparent;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* 短信验证码布局 */
.sms-row {
  display: flex;
  gap: 10px;
}
.send-btn {
  min-width: 70px;
  padding: 0 10px;
  border: none;
  background: var(--main-blue);
  color: #fff;
  border-radius: 12px;
  cursor: pointer;
  transition: 0.2s;
}
.send-btn:hover {
  background: var(--main-blue-dark);
}

/* 底部 */
.login-footer {
  margin-top: 20px;
  text-align: center;
  font-size: 14px;
  color: var(--text-sub);
}
.register-link {
  color: var(--main-blue);
  cursor: pointer;
  margin-left: 4px;
}
</style>