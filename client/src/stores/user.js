import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
    const token = ref('')
    const userInfo = ref({})
    const isLoginVisible = ref(false)

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

    return { 
        token, 
        userInfo, 
        isLoginVisible, 
        showLogin, 
        hideLogin, 
        setLoginInfo,
        clearLoginInfo 
    }
}, { persist: true })