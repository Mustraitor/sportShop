<script setup>
import { useRoute } from 'vue-router'
const route = useRoute()

const isActive = (path) => route.path.startsWith(path)
</script>
<template>
  <nav class="bottom-nav">
    <router-link to="/" class="item" :class="{ active: isActive('/') }">
      <svg viewBox="0 0 24 24" width="22" height="22" stroke="currentColor" fill="none" stroke-width="1.6">
        <path d="M3 9.5L12 3l9 6.5V21a1 1 0 0 1-1 1h-5v-6H10v6H5a1 1 0 0 1-1-1V9.5z" stroke-linejoin="round"/>
      </svg>
      <span>首页</span>
    </router-link>

    <router-link to="/cart" class="item" :class="{ active: isActive('/cart') }">
      <svg viewBox="0 0 24 24" width="22" height="22" stroke="currentColor" fill="none" stroke-width="1.6">
        <path d="M6 6h15l-1.5 9h-12zM6 6l-2-3H1" stroke-linecap="round" stroke-linejoin="round"/>
        <circle cx="9" cy="20" r="1.5" />
        <circle cx="17" cy="20" r="1.5" />
      </svg>
      <span>购物车</span>
    </router-link>

    <router-link to="/order" class="item" :class="{ active: isActive('/order') }">
      <svg viewBox="0 0 24 24" width="22" height="22" stroke="currentColor" fill="none" stroke-width="1.6">
        <path d="M7 3h10l1 4H6l1-4zM6 7h12v14H6V7z" stroke-linejoin="round"/>
      </svg>
      <span>订单</span>
    </router-link>

    <router-link to="/user" class="item" :class="{ active: isActive('/user') }">
      <svg viewBox="0 0 24 24" width="22" height="22" stroke="currentColor" fill="none" stroke-width="1.6">
        <circle cx="12" cy="8" r="4" />
        <path d="M4 21c1.5-4 4.5-6 8-6s6.5 2 8 6" stroke-linecap="round"/>
      </svg>
      <span>我的</span>
    </router-link>
  </nav>
</template>
<style scoped>
.bottom-nav {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 60px;
  background: #fff;
  display: flex;
  justify-content: space-around;
  align-items: center;
  border-top: 1px solid #ddd;
  z-index: 2000;
}

/* PC端隐藏 */
@media (min-width: 768px) {
  .bottom-nav {
    display: none;
  }
}

.item {
  font-size: 12px;
  color: #555;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
}

/* 选中状态：蓝色 + icon变蓝 + 下划线 */
.item.active {
  color: #007bff;
}

.item.active::after {
  content: "";
  width: 20px;
  height: 2px;
  background: #007bff;
  margin: 2px auto 0;
  border-radius: 2px;
}

/* 让 SVG 跟随文字颜色 */
.item svg {
  stroke: currentColor;
}
</style>