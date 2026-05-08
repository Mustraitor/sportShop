<template>
  <Transition name="fade">
    <div v-if="userStore.isLoginVisible" class="modal-overlay" @click.self="userStore.hideLogin">
      <div class="login-card">
        <button class="close-btn" @click="userStore.hideLogin">×</button>
        
        <div class="login-header">
          <h2>欢迎回来</h2>
          <p>登录以开启您的“型动”之旅</p>
        </div>

        <form @submit.prevent="handleLogin" class="login-form">
          <div class="input-group">
            <label>用户名</label>
            <input 
              v-model="loginForm.username" 
              type="text" 
              placeholder="请输入您的用户名" 
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
            <span v-if="!loading">登录</span>
            <span v-else class="loader"></span>
          </button>
        </form>

        <div class="login-footer">
          还没有账号？<span class="register-link">立即注册</span>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useUserStore } from '@/stores/user'
import { loginApi } from '@/api/user'

const userStore = useUserStore()
const loading = ref(false)
const loginForm = reactive({ username: '', password: '' })

const handleLogin = async () => {
  if (loading.value) return
  loading.value = true
  try {
    const resData = await loginApi(loginForm)
    userStore.setLoginInfo(resData.token, {
      userId: resData.userId,
      userName: resData.userName
    })
    userStore.hideLogin()
  } catch (err) {
    console.error(err)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* 迪卡侬风格配色 */
:root {
  --decathlon-blue: #0072b8;
  --text-main: #222;
  --text-sub: #757575;
  --bg-gray: #f2f4f5;
}

/* 动画 */
.fade-enter-active, .fade-leave-active { transition: opacity 0.3s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 2000;
  backdrop-filter: blur(4px); /* 现代化模糊感 */
}

.login-card {
  background: #fff;
  width: 100%;
  max-width: 420px;
  padding: 48px;
  position: relative;
  box-shadow: 0 10px 25px rgba(0,0,0,0.1);
  border-radius: 2px; /* 迪卡侬风格较硬，不用大圆角 */
}

.close-btn {
  position: absolute;
  right: 20px;
  top: 15px;
  background: none;
  border: none;
  font-size: 28px;
  cursor: pointer;
  color: #ccc;
}

.login-header h2 {
  font-size: 24px;
  color: #222;
  margin-bottom: 8px;
  font-weight: 800;
  text-transform: uppercase; /* 迪卡侬特色：大写感 */
}

.login-header p {
  font-size: 14px;
  color: #757575;
  margin-bottom: 32px;
}

.input-group {
  margin-bottom: 24px;
  display: flex;
  flex-direction: column;
}

.input-group label {
  font-size: 12px;
  font-weight: 700;
  margin-bottom: 8px;
  color: #222;
}

.input-group input {
  padding: 12px 16px;
  border: 1px solid #d1d1d1;
  background: #fcfcfc;
  font-size: 14px;
  transition: all 0.2s;
}

.input-group input:focus {
  outline: none;
  border-color: #0072b8;
  background: #fff;
}

.forgot-pwd {
  font-size: 12px;
  color: #0072b8;
  text-align: right;
  margin-top: 8px;
  cursor: pointer;
}

.submit-btn {
  width: 100%;
  padding: 14px;
  background: #0072b8;
  color: #fff;
  border: none;
  font-weight: 700;
  font-size: 16px;
  cursor: pointer;
  margin-top: 16px;
  transition: background 0.2s;
}

.submit-btn:hover {
  background: #005a91;
}

.submit-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.login-footer {
  margin-top: 32px;
  text-align: center;
  font-size: 13px;
  color: #757575;
}

.register-link {
  color: #0072b8;
  font-weight: 700;
  cursor: pointer;
  text-decoration: underline;
}

/* 简单的加载动画 */
.loader {
  width: 20px;
  height: 20px;
  border: 2px solid #fff;
  border-bottom-color: transparent;
  border-radius: 50%;
  display: inline-block;
  animation: rotation 1s linear infinite;
}

@keyframes rotation {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
</style>