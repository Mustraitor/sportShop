import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user' // 导入你的 Pinia store
import router from '@/router' // 导入路由用于跳转

// 创建 axios 实例
const service = axios.create({
  // 建议在 .env.development 中配置 VITE_API_BASE_URL = /api
  baseURL: import.meta.env.VITE_API_BASE_URL, 
  timeout: 5000
})

// 1. 请求拦截：自动携带 Token
service.interceptors.request.use(
  config => {
    const userStore = useUserStore()
    const token = userStore.token // 从 Pinia 拿，它是响应式的
    if (token) {
      // 核心：加上 Bearer 前缀，注意有个空格
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
        userStore.clearLoginInfo() 
        userStore.showLogin()
      }
      
      ElMessage.error(res.msg || '请求失败')
      return Promise.reject(res)
    }
    return res.data
  },
  error => {
    // 处理网络错误或服务器 500
    const msg = error.response?.data?.msg || '网络连接异常'
    ElMessage.error(msg)
    return Promise.reject(error)
  }
)

export default service