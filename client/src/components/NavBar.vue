<script setup>
import { ref, onMounted } from 'vue'
import { logoutApi, getUserList } from '@/api/user.js'
import { useUserStore } from '@/stores/user' 
import { useRouter } from 'vue-router'

const userStore = useUserStore() 
const router = useRouter()
const userList = ref([])
// 请求后端接口
const getUsers = async () => {
  try {
    const data = await getUserList()
    userList.value = data
    console.log('后端返回数据：', data)
  } catch (error) {
    console.error(error)
  }
} 

const handleLogout = async () => {
  try {
    await logoutApi()
  } catch (err) {
    console.error('后端退出记录失败', err)
  } finally {
    userStore.clearLoginInfo()
    router.push('/')
  }
}
onMounted(() => {
  getUsers()
})
</script>
<template>
  <header class="header">
    <!-- 左侧 Logo -->
    <div class="logo">
      <img src="/favicon.ico" alt="logo" />
      <span>型动派</span>
    </div>
    <!-- 中间搜索 -->
    <div class="search">
      <input type="text" placeholder="搜索商品..." />
      <button>搜索</button>
    </div>
    <!-- 右侧导航 -->
    <div class="nav">
      <router-link to="/cart">购物车</router-link>
      <router-link to="/order">订单</router-link>
      <router-link to="/user">我的</router-link>

      <span v-if="!userStore.token" class="login-link" @click="userStore.showLogin()">
        登录
      </span>

      <el-dropdown v-else trigger="click">
        <div class="avatar-wrapper">
          <el-avatar 
            :size="30" 
            :src="userStore.userInfo.avatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'" 
          />
          <span class="user-name">{{ userStore.userInfo.userName }}</span>
        </div>
        
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item @click="router.push('/user')">个人中心</el-dropdown-item>
            <el-dropdown-item @click="router.push('/order')">我的订单</el-dropdown-item>
            <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </header>
  <!-- <img src="https://sportshop-pictures.oss-cn-beijing.aliyuncs.com/%E7%BA%B8%E4%B8%8A%E7%9A%84%E9%AD%94%E6%B3%95%E4%BD%BF/20.jpg" alt=""/> -->
  <!-- <a href="https://sportshop-pictures.oss-cn-beijing.aliyuncs.com/%E7%BA%B8%E4%B8%8A%E7%9A%84%E9%AD%94%E6%B3%95%E4%BD%BF/20.jpg">666</a> -->
 
  <!-- 测试前后端数据库连通性 -->
  <div>
    <h2>用户列表测试</h2>
    <div v-for="item in userList" :key="item.userId">
      {{ item.userName }}
    </div>
  </div>

</template>
<style scoped>
/* 基础变量 */
:root {
  --primary-blue: #3643ba;   /* 迪卡侬品牌蓝 */
  --bg-gray: #f5f4f5;        /* 搜索框背景灰色 */
  --text-main: #333333;      /* 主文字颜色 */
  --border-color: #eeeeee;
}

/* Header 容器 */
.header {
  height: 80px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 40px;
  border-bottom: 1px solid var(--border-color);
  font-family: "Helvetica Neue", Helvetica, Arial, "PingFang SC", sans-serif;
}

/* 左侧 Logo 区 */
.logo {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
}

.logo img {
  width: 90px; 
  height: auto;
}

.logo span {
  font-size: 22px;
  font-weight: 800;
  color: var(--primary-blue);
  letter-spacing: 1px;
}

.search {
  display: flex;
  align-items: center;
  background: var(--bg-gray);
  border-radius: 25px;
  padding: 4px 18px;
  flex: 0 1 450px; 
  border: 1px solid black;
  margin: 0 30px;
  transition: all 0.3s ease;
}


.search:focus-within {
  background: #fff;
  box-shadow: 0 0 0 1px var(--primary-blue);
}

.search input {
  flex: 1;
  height: 36px;
  background: transparent;
  border: none;
  outline: none;
  padding: 0 10px;
  font-size: 14px;
  color: var(--text-main);
}

.search button {
  background: transparent;
  border: none;
  color: #666;
  font-weight: 600;
  cursor: pointer;
  padding: 0 5px;
  font-size: 14px;
}

.search button:hover {
  color: var(--primary-blue);
}


.nav {
  display: flex;
  align-items: center;
  gap: 25px;
}


.nav a {
  text-decoration: none;
  color: var(--text-main);
  font-size: 15px;
  font-weight: 500;
  position: relative;
  transition: color 0.2s;
}

.nav a:hover {
  color: var(--primary-blue);
}

.nav a[to="/login"] {
  font-weight: 600;
}


@media (max-width: 900px) {
  .header {
    padding: 0 15px;
  }
  .search {
    flex: 1;
    margin: 0 15px;
  }
  .logo span {
    display: none; 
  }
}

@media (max-width: 600px) {
  .search {
    display: none;
  }
}
</style>
