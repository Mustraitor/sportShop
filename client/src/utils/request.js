import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user' 
import router from '@/router' 

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL, 
  timeout: 5000
})

// 1. 请求拦截：自动携带 Token
service.interceptors.request.use(
  config => {
    const userStore = useUserStore()
    const token = userStore.token 
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => Promise.reject(error)
)

// 2. 响应拦截：统一处理业务状态码
service.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      if (res.code === 401) {
        const userStore = useUserStore()
        userStore.clearLoginInfo() // 清除 Pinia 缓存和 localStorage
        userStore.showLogin()      // 弹出登录框
        ElMessage.error('登录已过期，请重新登录')
        return Promise.reject(res)
      }
      
      ElMessage.error(res.msg || '请求失败')
      return Promise.reject(res)
    }
    // 保持你原有的返回结构，确保你的业务代码不会断层
    return res.data
  },
  error => {
    // 【新增核心】：如果后端拦截器直接抛出了标准的 HTTP 401 状态码，在这里进行捕获！
    if (error.response && error.response.status === 401) {
      const userStore = useUserStore()
      userStore.clearLoginInfo() // 清除 Pinia 缓存和 localStorage
      userStore.showLogin()      // 弹出登录框
      ElMessage.error('登录已过期，请重新登录')
      return Promise.reject(error)
    }

    // 处理其他网络错误或服务器 500
    const msg = error.response?.data?.msg || '网络连接异常'
    ElMessage.error(msg)
    return Promise.reject(error)
  }
)

export default service