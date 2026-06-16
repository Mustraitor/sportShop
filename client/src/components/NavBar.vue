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
    
    <div class="nav-left-modules">
      <div 
        class="header-action-btn category-btn"
        :class="{ active: route.path === '/product-category' }"
        @click="goTab('category')"
      >
        <span class="btn-icon"></span>商品分类
      </div>
      
      <router-link to="/AIChat" class="header-action-btn ai-recommend-btn">
        <span class="btn-icon ai-sparkle"></span>AI商品推荐
      </router-link> 
    </div>
    
    <div class="nav">
      <router-link to="/cart" class="nav-item">
        <div class="nav-icon cart-icon-wrapper">
          <div class="cart-badge" v-if="userStore.cartTotalCount > 0">
            {{ userStore.cartTotalCount > 99 ? '99+' : userStore.cartTotalCount }}
          </div>
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
            <path d="M8.2 8.5C8.2 5 9.9 2.8 12 2.8s3.8 2.3 3.8 5.7M5 20.5c.1.5.5.8 1 .8h12c.5 0 .9-.3 1-.8L21.1 10.8H2.9L5 20.5Z" />
          </svg>
          <div>购物车</div>
        </div>
      </router-link>

      <router-link v-show="userStore.token" to="/order" class="nav-item" >
        <div class="nav-icon">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" xmlns="http://www.w3.org/2000/svg">
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
            <div class="user-name">{{ userStore.userInfo?.nickName }}</div>
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

<style scoped lang="scss">
// ================= 全局变量定义 =================
$primary-blue: #0175b9;
$hover-blue: #0192e4;
$light-blue-bg: #f2f7fb;
$white: #ffffff;
$text-main: #333;
$border-color: #eee;
$danger-red: #ff4d4f;
$danger-bg: #fff1f0;

// ================= 统一按钮 Mixin =================
@mixin action-capsule {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 18px;
  font-size: 14px;
  font-weight: 500;
  color: $primary-blue;
  background-color: $white;
  border: 1px solid rgba($primary-blue, 0.3);
  border-radius: 20px;
  cursor: pointer;
  white-space: nowrap;
  text-decoration: none;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);

  .btn-icon {
    font-size: 14px;
  }

  // 悬停：蓝底白字并产生浮雕阴影
  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba($primary-blue, 0.2);
    color: $white;
    background: linear-gradient(135deg, $hover-blue 0%, $primary-blue 100%);
    border-color: transparent;
  }

  // 激活状态：纯蓝底白字
  &.active {
    color: $white;
    background: $primary-blue;
    border-color: transparent;
    box-shadow: 0 2px 8px rgba($primary-blue, 0.3);
  }
}

// ================= 头部整体布局 =================
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
  background-color: $white;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  margin-bottom: 20px;

  /* 左侧 Logo 区域 */
  .logo { 
    display: flex; 
    align-items: center; 
    gap: 12px; 
    cursor: pointer; 
    text-decoration: none;

    img { 
      height: 45px; 
      width: auto; 
    }
    
    span {
      font-family: "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", sans-serif;
      font-size: 22px; 
      font-weight: 500; 
      color: $primary-blue; 
      letter-spacing: 3px;
      -webkit-font-smoothing: antialiased; 
      font-style: italic;
    }
  }

  /* 中部功能区：分类与AI推荐 */
  .nav-left-modules {
    display: flex;
    align-items: center;
    gap: 15px;
    margin-left: 30px;
    flex: 1;

    // 应用统一的 Mixin 样式
    .header-action-btn {
      @include action-capsule;
    }

    // AI 特有的星星闪烁动画
    .ai-sparkle {
      animation: sparkle 2s infinite ease-in-out;
    }
  }

  /* 右侧导航项容器 */
  .nav { 
    display: flex; 
    align-items: center; 
    gap: 30px; 
    
    a { 
      text-decoration: none; 
      color: inherit; 
    }

    /* 统一图标+文字的垂直布局 */
    .nav-item, .nav-icon {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      color: $text-main;
      transition: opacity 0.2s;
      position: relative; 
      padding-bottom: 4px; 

      svg { 
        margin-bottom: 2px; 
      }

      div, &.user-name {
        font-size: 12px !important;
        line-height: 1;
        white-space: nowrap;
        margin-top: 4px;
      }
    }

    .nav-item {
      &:hover { 
        opacity: 0.7;
      }

      /* 伪元素动态下划线特效 */
      &::after {
        content: '';
        position: absolute;
        left: 0;
        bottom: 0; 
        width: 100%;
        height: 2px;
        background-color: $primary-blue;
        transform: scaleX(0);
        transform-origin: center; 
        transition: transform 0.3s ease;
      }

      &:hover::after {
        transform: scaleX(1);
      }
    }

    /* 购物车徽标专区 */
    .cart-icon-wrapper {
      position: relative;

      .cart-badge {
        position: absolute;
        top: -8px;       
        right: -10px;    
        background-color: $primary-blue; 
        color: $white;               
        font-size: 10px;           
        font-weight: bold;
        line-height: 1;
        padding: 3px 5px;          
        border-radius: 10px;       
        min-width: 16px;
        text-align: center;
        box-sizing: border-box;
        border: 1.5px solid $white;  
        pointer-events: none;      
      }
    }

    /* 下拉菜单区域 */
    .custom-avatar {
      width: 26px;
      height: 26px;
      border-radius: 50%;
      border: 1px solid $border-color;
      object-fit: cover;
    }

    .user-menu-container {
      position: relative; 

      &::after {
        content: '';
        position: absolute;
        top: 100%;
        left: 0;
        width: 100%;
        height: 15px; 
      }

      &:hover .dropdown-content { 
        display: block; 
      }

      .dropdown-content {
        display: none; 
        position: absolute;
        top: 100%;
        right: -10px; 
        margin-top: 15px;
        background-color: $white;
        min-width: 110px;
        box-shadow: 0 4px 12px rgba(0,0,0,0.1);
        border-radius: 8px;
        z-index: 100;
        padding: 8px 0;
        border: 1px solid $border-color;

        .dropdown-item {
          padding: 10px 0;
          font-size: 14px;
          color: $text-main;
          cursor: pointer;
          transition: background 0.2s;
          text-align: center;

          &:hover { 
            background-color: $light-blue-bg; 
            color: $primary-blue; 
          }

          &.logout {
            border-top: 1px solid $border-color;
            color: $danger-red;

            &:hover {
              background-color: $danger-bg;
            }
          }
        }
      }
    }
  }
}

// ================= 全局动画与响应式 =================
@keyframes sparkle {
  0%, 100% { transform: scale(1); opacity: 0.8; }
  50% { transform: scale(1.2); opacity: 1; }
}

@media (max-width: 900px) {
  .header { 
    padding: 0 15px; 
    
    .logo span { 
      display: none; 
    }
    
    .nav-left-modules {
      margin-left: 10px;
    }
  }
}
</style>