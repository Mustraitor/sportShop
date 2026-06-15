<script setup>
import { ref, reactive, onUnmounted, watch } from 'vue'
import { useUserStore } from '@/stores/user'
import { loginApi, registerApi } from '@/api/user'
import { sendSmsCodeApi, smsLoginApi } from '@/api/user' 
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
const showTabs = true
const activeTab = ref('account')
const loading = ref(false)

const loginForm = reactive({
  username: '',
  password: ''
})

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
        console.error(e)
      }
    }

    const resData = await loginApi({
      username: loginForm.username,
      password: loginForm.password,
      guestId: guestIdToMerge
    })

    userStore.setLoginInfo(resData.token, {
      userId: resData.userId,
      userName: resData.userName,
      nickName: resData.nickName
    })
    
    ElMessage.success('登录成功')
    userStore.hideLogin()
  } catch (err) {
    console.error(err)
    ElMessage.error(err.response?.data?.msg || '登录失败，请检查账号密码') 
  } finally {
    loading.value = false
  }
}

const smsForm = reactive({
  phone: '',
  code: ''
})

const countdown = ref(0)
let timer = null

const sendSmsCode = async () => {
  const phoneReg = /^1[3-9]\d{9}$/
  if (!smsForm.phone) {
    ElMessage.warning('请输入手机号')
    return
  }
  if (!phoneReg.test(smsForm.phone)) {
    ElMessage.warning('手机号格式不正确')
    return
  }
  if (countdown.value > 0) {
    ElMessage.warning(`请等待 ${countdown.value} 秒后再试`)
    return
  }

  try {
    await sendSmsCodeApi({
      phoneNumber: smsForm.phone,
      scene: 'login'
    })
    ElMessage.success('验证码已发送')
    
    countdown.value = 60
    timer = setInterval(() => {
      if (countdown.value <= 1) {
        clearInterval(timer)
        countdown.value = 0
      } else {
        countdown.value--
      }
    }, 1000)
  } catch (err) {
    console.error(err)
    ElMessage.error(err.response?.data?.msg || '发送失败，请稍后重试')
  }
}

const handleSMSLogin = async () => {
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
        console.error(e) 
      }
    }

    const resData = await smsLoginApi({
      phone: smsForm.phone,
      code: smsForm.code,
      guestId: guestIdToMerge
    })

    userStore.setLoginInfo(resData.token, {
      userId: resData.userId,
      userName: resData.userName,
      nickName: resData.nickName
    })

    ElMessage.success('登录成功')
    userStore.hideLogin()
  } catch (err) {
    console.error(err)
    ElMessage.error(err.response?.data?.msg || '登录失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const registerForm = reactive({
  username: '',
  phone: '',
  code: '',
  password: '',
  confirmPwd: ''
})

const handleRegister = async () => {
  if (!registerForm.username || !registerForm.password) {
    ElMessage.warning('请填写完整信息')
    return
  }
  if (registerForm.password !== registerForm.confirmPwd) {
    ElMessage.warning('两次密码输入不一致')
    return
  }
  if (registerForm.password.length < 6) {
    ElMessage.warning('密码长度至少6位')
    return
  }

  try {
    await registerApi({
      username: registerForm.username,
      password: registerForm.password,
      confirmPassword: registerForm.confirmPwd   
    })
    ElMessage.success('注册成功，请登录')
    activeTab.value = 'account'
  } catch (err) {
    console.error(err)
    ElMessage.error(err.response?.data?.msg || '注册失败，请稍后重试')
  }
}

const clearAllForms = () => {
  loginForm.username = ''
  loginForm.password = ''
  
  smsForm.phone = ''
  smsForm.code = ''
  
  registerForm.username = ''
  registerForm.password = ''
  registerForm.confirmPwd = ''
  registerForm.phone = ''
  registerForm.code = ''
  
  activeTab.value = 'account'
}

watch(
  () => userStore.isLoginVisible,
  (newVal) => {
    if (!newVal) {
      clearAllForms()
    }
  }
)

onUnmounted(() => {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
})
</script>
<template>
  <Transition name="fade">
    <div 
      v-if="userStore.isLoginVisible"
      class="modal-overlay"
      @click.self="userStore.hideLogin"
    >
      <div class="login-card">
        <button class="close-btn" @click="userStore.hideLogin">×</button>

        <div v-if="showTabs && activeTab !== 'register'" class="login-tabs">
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

        <form
          v-if="activeTab === 'account'"
          @submit.prevent="handleLogin"
          class="login-form"
        >
          <div class="input-group">
            <label>用户名</label>
            <input v-model="loginForm.username" type="text" placeholder="请输入用户名" required />
          </div>

          <div class="input-group">
            <label>密码</label>
            <input v-model="loginForm.password" type="password" placeholder="请输入密码" required />
            <span class="forgot-pwd">忘记密码？</span>
          </div>

          <button type="submit" class="submit-btn" :disabled="loading">
            <span>登录</span>
          </button>
          
          <div class="login-footer">
            还没有账号？<span class="register-link" @click="activeTab = 'register'">立即注册</span>
          </div>
        </form>

        <form
          v-else-if="activeTab === 'sms'"
          @submit.prevent="handleSMSLogin"
          class="login-form"
        >
          <div class="input-group">
            <label>手机号</label>
            <input v-model="smsForm.phone" type="tel" placeholder="请输入手机号" required />
          </div>

          <div class="input-group">
            <label>验证码</label>
            <div class="sms-row">
              <input v-model="smsForm.code" type="text" placeholder="短信验证码" required />
              <button
                type="button"
                class="send-btn"
                :disabled="countdown > 0"
                @click="sendSmsCode"
              >
                {{ countdown > 0 ? `${countdown}秒后重试` : '发送验证码' }}
              </button>
            </div>
          </div>

          <button type="submit" class="submit-btn" :disabled="loading">
            {{ loading ? '登录中...' : '登录' }}
          </button>
        </form>
        
        <form
          v-else-if="activeTab === 'register'"
          @submit.prevent="handleRegister"
          class="login-form"
        >
          <div class="input-group">
            <label>用户名</label>
            <input v-model="registerForm.username" type="text" placeholder="请输入用户名" required />
          </div>
          <div class="input-group">
            <label>密码</label>
            <input v-model="registerForm.password" type="password" placeholder="请输入密码" required />
          </div>

          <div class="input-group">
            <label>确认密码</label>
            <input v-model="registerForm.confirmPwd" type="password" placeholder="请再次输入密码" required />
          </div>

          <button type="submit" class="submit-btn">
            注册
          </button>

          <div class="login-footer">
            已有账号？<span class="register-link" @click="activeTab = 'account'">返回登录</span>
          </div>
        </form>

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