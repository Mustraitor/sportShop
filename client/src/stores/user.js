import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
    const token = ref('')
    const userInfo = ref({})
    const isLoginVisible = ref(false)

    // ✅ 新增游客ID
    const guestId = ref(localStorage.getItem('guestId') || '')

    const showLogin = () => { isLoginVisible.value = true }
    const hideLogin = () => { isLoginVisible.value = false }

    const setLoginInfo = (newToken, user) => {
        token.value = newToken
        userInfo.value = user
        hideLogin() 
    }

    const clearLoginInfo = () => {
        token.value = ''
        userInfo.value = {}
    }

    // 初始化 guestId，只执行一次
    const initGuestId = () => {
        if (!guestId.value) {
            guestId.value = crypto.randomUUID()
            localStorage.setItem('guestId', guestId.value)
        }
    }

    return { 
        token, 
        userInfo, 
        isLoginVisible, 
        showLogin, 
        hideLogin, 
        setLoginInfo,
        clearLoginInfo,
        guestId,      //  guestId 全局状态
        initGuestId   //  初始化函数
    }
}, { persist: true })