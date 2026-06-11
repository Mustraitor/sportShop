<script setup>
import { onMounted } from 'vue'
import { logoutApi } from '@/api/user.js'
import { useUserStore } from '@/stores/user' 
import { useRouter, useRoute } from 'vue-router'
import SearchModal from "@/components/SearchModal.vue";
import { cartApi } from '@/api/cart'

const userStore = useUserStore() 
const router = useRouter()
const route = useRoute()

const initGuest = () => {
  userStore.initGuestId() 
}

const fetchCartCount = async () => {
  // console.log('正在请求购物车数量，身份标识：', userStore.token || userStore.guestId)
  const res = await cartApi.getCartList()
  userStore.updateCartCount(res.total)
}

// 3. 退出逻辑
const handleLogout = async () => {
  try {
    await logoutApi()
  } catch (err) {
    console.error('后端退出记录失败', err)
  } finally {
    userStore.clearLoginInfo()
    // 退出后重新初始化一下游客身份，并拉取游客的购物车数量
    userStore.initGuestId()
    fetchCartCount()
    router.push('/')
  }
}

const goTab = (tab) => {
  // 商品分类跳转到独立的分类页
  if (tab === 'category') {
    router.push('/product-category')
    return
  }
  homeStore.setTab(tab)
  if (router.currentRoute.value.path !== '/') {
    router.push('/')
  }
}

onMounted(() => {
  initGuest()
  fetchCartCount()
})
</script>
<template>
  <header class="header">
    <router-link to="/" class="logo">
      <img src="/favicon.ico" alt="型动派 logo" />
      <span>型动派</span>
    </router-link>
        <div class="home-tabs">
      <span
        class="home-tab"
        :class="{ active: route.path === '/product-category' }"
        @click="goTab('category')"
      >商品分类</span>
    </div>
    <SearchModal />
    
    <div class="nav">
      <router-link to="/cart" class="nav-item">
        <div class="nav-icon cart-icon-wrapper">
          <div class="cart-badge" v-if="userStore.cartTotalCount > 0">
            {{ userStore.cartTotalCount > 99 ? '99+' : userStore.cartTotalCount }}
          </div>
          <svg
            width="20"
            height="20"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="1.5"
            xmlns="http://www.w3.org/2000/svg"
            aria-hidden="true"
          >
            <path d="M8.2 8.5C8.2 5 9.9 2.8 12 2.8s3.8 2.3 3.8 5.7M5 20.5c.1.5.5.8 1 .8h12c.5 0 .9-.3 1-.8L21.1 10.8H2.9L5 20.5Z" />
          </svg>
          <div>购物车</div>
        </div>
      </router-link>

      <router-link v-show="userStore.token" to="/order" class="nav-item" >
        <div class="nav-icon">
          <svg 
            width="24" 
            height="24" 
            viewBox="0 0 24 24" 
            fill="none" 
            stroke="currentColor" 
            stroke-width="1.5" 
            xmlns="http://www.w3.org/2000/svg"
          >
            <path d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9h6m-6-4h6" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          <div>订单</div>
        </div>
      </router-link>

      <div v-if="!userStore.token" class="nav-item" @click="userStore.showLogin()">
          <div class="nav-icon">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <circle cx="12" cy="7" r="3" />
              <path d="M5 20c0-4 3-6 7-6s7 2 7 6" />
            </svg>
            <div>登录</div>
          </div>
      </div>

      <div v-else class="user-menu-container">
          <div class="nav-icon avatar-trigger">
            <img 
              class="custom-avatar" 
              :src="userStore.userInfo?.avatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'" 
              alt="avatar"
            />
            <div class="user-name">{{ userStore.userInfo?.userName }}</div>
          </div>
          <div class="dropdown-content">
            <div class="dropdown-item" @click="router.push('/profile')">个人中心</div>
            <div class="dropdown-item" @click="router.push('/order')">我的订单</div>
            <div class="dropdown-item logout" @click="handleLogout">退出登录</div>
          </div>
      </div>
    </div>
  </header>
</template>
<style scoped>
/* 头部整体布局 */
.header {
  position: sticky;
  top: 0;
  left: 0;
  width: 100%;
  z-index: 1000; 
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 40px;
  height: 80px;
  background-color: #fff;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  margin-bottom: 20px;
}

/* 左侧 Logo 区域 */
.logo { 
  display: flex; 
  align-items: center; 
  gap: 12px; 
  cursor: pointer; 
  text-decoration: none;
}
.logo img { 
  height: 45px; 
  width: auto; 
}
.logo span {
  font-family: "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", sans-serif;
  font-size: 22px; 
  font-weight: 500; 
  color: var(--main-blue-dark, #0175b9); 
  letter-spacing: 3px;
  -webkit-font-smoothing: antialiased; 
  font-style: italic;
}

/* 右侧导航项容器 */
.nav { 
  display: flex; 
  align-items: center; 
  gap: 30px; 
}
.nav a { 
  text-decoration: none; 
  color: inherit; 
}

/* 统一图标+文字的垂直布局类 */
.nav-item, .nav-icon {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #333;
  transition: opacity 0.2s;
  /* 为伪元素下划线提供定位基准 */
  position: relative; 
  padding-bottom: 4px; 
}

.nav-item:hover { 
  opacity: 0.7;
}

/* 伪元素动态下划线特效 */
.nav-item::after {
  content: '';
  position: absolute;
  left: 0;
  bottom: 0; 
  width: 100%;
  height: 2px;
  background-color: #0175b9;
  transform: scaleX(0);
  transform-origin: center; 
  transition: transform 0.3s ease;
}

/* 悬停时激活下划线 */
.nav-item:hover::after {
  transform: scaleX(1);
}

/* 图标和文字的间距 */
.nav-icon svg { margin-bottom: 2px; }

/* 统一文字部分样式 */
.nav-icon div, .user-name {
  font-size: 12px !important;
  line-height: 1;
  white-space: nowrap;
  margin-top: 4px;
}
/* 搜索框左侧选项卡 */
.home-tabs {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: 30px;
}
.home-tab {
  font-size: 15px;
  color: #333;
  cursor: pointer;
  padding: 8px 16px;
  border-radius: 20px;
  white-space: nowrap;
  transition: all 0.25s;
}
.home-tab:hover {
  color: #0175b9;
  background: #f2f7fb;
}
.home-tab.active {
  color: #fff;
  background: #0175b9;
  font-weight: 600;
}

/* ================= 购物车徽标专区 ================= */
.cart-icon-wrapper {
  position: relative;
}

.cart-badge {
  position: absolute;
  top: -8px;       
  right: -10px;    
  background-color: #0175b9; /* 蓝色徽标 */
  color: #fff;               
  font-size: 10px;           
  font-weight: bold;
  line-height: 1;
  padding: 3px 5px;          
  border-radius: 10px;       
  min-width: 16px;
  text-align: center;
  box-sizing: border-box;
  border: 1.5px solid #fff;  /* 白边防止视觉黏连 */
  pointer-events: none;      /* 不阻挡鼠标点击 */
}
/* ================================================== */

/* 自定义头像图片样式 */
.custom-avatar {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  border: 1px solid #eee;
  object-fit: cover;
}

/* 下拉菜单区域 */
.user-menu-container {
  position: relative; 
}

.dropdown-content {
  display: none; 
  position: absolute;
  top: 100%;
  right: -10px; 
  margin-top: 15px;
  background-color: #fff;
  min-width: 110px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
  border-radius: 8px;
  z-index: 100;
  padding: 8px 0;
  border: 1px solid var(--border-color, #eee);
}

/* 透明伪元素：防止鼠标刚离开头像，菜单就消失 */
.user-menu-container::after {
  content: '';
  position: absolute;
  top: 100%;
  left: 0;
  width: 100%;
  height: 15px; 
}

/* 鼠标悬停显示弹窗 */
.user-menu-container:hover .dropdown-content { display: block; }

.dropdown-item {
  padding: 10px 0;
  font-size: 14px;
  color: #333;
  cursor: pointer;
  transition: background 0.2s;
  text-align: center;
}

.dropdown-item:hover { 
  background-color: var(--bg-gray, #f5f7fa); 
  color: var(--primary-blue, #0175b9); 
}

.dropdown-item.logout {
  border-top: 1px solid var(--border-color, #eee);
  color: #ff4d4f;
}
.dropdown-item.logout:hover {
  background-color: #fff1f0;
}

/* 响应式调整 */
@media (max-width: 900px) {
  .header { padding: 0 15px; }
  .logo span { display: none; }
}
</style>