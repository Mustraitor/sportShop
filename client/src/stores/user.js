import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
    const token = ref('')
    const userInfo = ref({})
    const isLoginVisible = ref(false)
    const cartTotalCount = ref(0)

    const guestId = ref('')

    const showLogin = () => { isLoginVisible.value = true }
    const hideLogin = () => { isLoginVisible.value = false }

    // 初始化 guestId
    const initGuestId = () => {
        if (!guestId.value && !token.value) {
            if (typeof crypto !== 'undefined' && crypto.randomUUID) {
                guestId.value = crypto.randomUUID()
            } else {
                // 回退方案：时间戳 + 随机字符串
                guestId.value = 'g_' + Date.now().toString(36) + Math.random().toString(36).substring(2, 7)
            }
        }
        return guestId.value
    }

    // 登录成功：保存信息、合并购物车、清除游客标识
    const setLoginInfo = async (newToken, user) => {
        token.value = newToken
        userInfo.value = user
        hideLogin() 
        guestId.value = '' 
    }

    // 退出登录：清除所有状态，并为下一次未登录访问准备新的 guestId
    const clearLoginInfo = () => {
        token.value = ''
        userInfo.value = {}
        cartTotalCount.value = 0
        guestId.value = '' 
        localStorage.removeItem('user-store')
        initGuestId()
    }

    const updateCartCount = (count) => {
        cartTotalCount.value = count
    }

    return { 
        token, 
        userInfo, 
        isLoginVisible, 
        showLogin, 
        hideLogin, 
        setLoginInfo,
        clearLoginInfo,
        guestId,      
        initGuestId,  
        cartTotalCount,
        updateCartCount
    }
}, { 
    persist: {
        key: 'user-store',
        paths: ['token', 'userInfo', 'guestId', "cartTotalCount"] 
    }
})