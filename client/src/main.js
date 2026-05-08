import { createApp } from 'vue'
import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate' // 引入插件

import App from './App.vue'
import router from './router'

import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

const app = createApp(App)
const pinia = createPinia()

pinia.use(piniaPluginPersistedstate) // 使用插件

app.use(pinia)
app.use(router)
app.use(ElementPlus)

app.mount('#app')