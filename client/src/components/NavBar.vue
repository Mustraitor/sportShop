<script setup>
import { ref, onMounted } from 'vue'
import { getUserList } from '@/api/user.js'

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
      <!-- <router-link to="/home">首页</router-link> -->
      <router-link to="/cart">购物车</router-link>
      <router-link to="/order">订单</router-link>
      <router-link to="/user">我的</router-link>
      <router-link to="/login">登录</router-link>
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
