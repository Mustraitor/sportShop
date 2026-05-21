<script setup>
import { ref } from 'vue'
import { Swiper, SwiperSlide } from 'swiper/vue'
import { Navigation, Pagination, Autoplay } from 'swiper/modules'
import NavBar from '@/components/NavBar.vue'
// 引入 Swiper 样式
import 'swiper/css'
import 'swiper/css/navigation'
import 'swiper/css/pagination'

// 直接导入图片 (请确保 assets 目录下有对应的图片文件)
import banner1 from '@/assets/banner1.png'
import banner2 from '@/assets/banner2.png'
import banner3 from '@/assets/banner3.png'
import banner4 from '@/assets/banner4.png'
import banner5 from '@/assets/banner5.png'

// 使用实际导入的图片
const bannerImages = ref([
  { src: banner1 },
  { src: banner2 },
  { src: banner3 },
  { src: banner4 },
  { src: banner5 }
])

const modules = [Navigation, Pagination, Autoplay]

// 搜索功能占位
const searchText = ref('')
const handleSearch = () => {
  console.log('搜索:', searchText.value)
}

// 新增：商品展示数据
const productItems = ref([
  { id: 1, image: new URL('@/assets/product1.png', import.meta.url).href, category: '智能设备' },
  { id: 2, image: new URL('@/assets/product2.png', import.meta.url).href, category: '篮球装备' },
  { id: 3, image: new URL('@/assets/product3.png', import.meta.url).href, category: '运动袜品' },
  { id: 4, image: new URL('@/assets/product4.png', import.meta.url).href, category: '游泳装备' },
  { id: 5, image: new URL('@/assets/product5.png', import.meta.url).href, category: '健康监测' },
  { id: 6, image: new URL('@/assets/product6.png', import.meta.url).href, category: '智能穿戴' },
  { id: 7, image: new URL('@/assets/product7.png', import.meta.url).href, category: '户外运动' },
])
</script>

<template>
  <div class="page-container">
    
    <NavBar/>
    <!-- 2. 主体内容区域 (左侧分类留白 + 右侧轮播) -->
    <div class="main-content-wrapper">
      
      <!-- 模拟电商布局：左侧占位(分类列表)，右侧轮播 -->
      <div class="content-grid">
        
        <!-- 左侧：分类列表 (已改为运动服饰相关) -->
        <div class="left-sidebar">
          <div class="category-header">全部分类</div>
          <ul class="category-list">
            <li>运动服饰</li>
            <li>运动鞋袜</li>
            <li>男士</li>
            <li>女士</li>
          </ul>
        </div>

        <!-- 右侧：轮播图 -->
        <div class="carousel-wrapper">
          <swiper
            :modules="modules"
            :slides-per-view="1"
            :space-between="0"
            :loop="true"
            :autoplay="{ delay: 4000, disableOnInteraction: false }"
            :pagination="{ clickable: true }"
            :navigation="true"
            class="my-swiper"
          >
            <swiper-slide v-for="(img, index) in bannerImages" :key="index">
              <div class="slide-content">
                <img :src="img.src" :alt="'Banner ' + (index + 1)" class="banner-img" />
              </div>
            </swiper-slide>
          </swiper>
        </div>
      </div>

      <!--商品分类展示-->
      <div class="product-section">
        <h3 class="section-title">推荐商品分类</h3>
        <div class="product-grid">
          <div
           v-for="item in productItems"
           :key="item.id"
           class="product-item"
          >
           <div class="product-icon">
            <img :src="item.image" :alt="item.category" />
           </div>
           <div class="productcategory">
            {{ item.category}} 
           </div>
        </div>
      </div>
    </div>

  </div>
  </div>
  <div class="test">
dasd
  </div>
</template>

<style scoped>
.test {
  width: 50px;
  height: 50px;
  background-color: blue;
  border-radius: 20%;
}
/* 全局容器 */
.page-container {
  width: 100%;
  background-color: #f8f8f8;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

/* --- 1. 导航栏样式 --- */
.navbar {
  width: 100%;
  background-color: #ffffff;
  color: #333;
  height: 70px;
  display: flex;
  justify-content: center;
  align-items: center;
  box-shadow: 0 2px 4px rgba(0,0,0,0.05);
  position: sticky;
  top: 0;
  z-index: 1000;
}

.nav-container {
  width: 100%;
  max-width: 1200px;
  padding: 0 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* 左侧组合区域 */
.nav-group-left {
  display: flex;
  align-items: center;
  gap: 0; /* 蓝框紧贴 Logo */
}

/* 品牌 Logo */
.brand-logo {
  padding-right: 15px;
  display: flex;
  align-items: center;
}

.logo-img {
  height: 30px;
  width: auto;
}

/* 选项卡 - 蓝底白字块 */
.nav-tabs {
  display: flex;
  background-color: #0056b3; /* 蓝色背景 */
  border-radius: 4px;
  overflow: hidden;
}

.nav-tab {
  color: #ffffff;
  text-decoration: none;
  font-size: 14px;
  padding: 10px 20px;
  border-right: 1px solid rgba(255, 255, 255, 0.2); /* 细微分割线 */
  transition: color 0.2s, background-color 0.2s; /* 颜色过渡动画 */
  cursor: pointer;
}

.nav-tab:last-child {
  border-right: none;
}

.nav-tab:hover {
  background-color: #4a8bc2; /* 悬停时颜色变浅 */
}

.nav-tab.active {
  background-color: #004494; /* 激活状态颜色加深 */
  font-weight: bold;
}

/* 右侧区域 */
.right-area {
  display: flex;
  align-items: center;
  gap: 20px;
}

.search-box {
  display: flex;
  border: 1px solid #ddd;
  border-radius: 4px;
  overflow: hidden;
}

.search-input {
  border: none;
  padding: 8px 12px;
  width: 200px;
  outline: none;
  font-size: 14px;
}

.search-btn {
  background-color: #0056b3; /* 搜索按钮改为蓝色 */
  color: white;
  border: none;
  padding: 8px 16px;
  cursor: pointer;
  font-size: 14px;
  transition: background 0.2s;
}

.search-btn:hover {
  background-color: #4a8bc2;
}

.user-actions {
  display: flex;
  gap: 15px;
}

.user-actions a {
  color: #333;
  text-decoration: none;
  font-size: 14px;
}

.user-actions a:hover {
  color: #0056b3;
}

/* --- 2. 主体布局 (类似参考图) --- */
.main-content-wrapper {
  flex: 1;
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.content-grid {
  display: grid;
  grid-template-columns: 200px 1fr; /* 左侧分类栏宽度固定，右侧自适应 */
  gap: 20px;
}

/* 左侧分类栏 */
.left-sidebar {
  background-color: #1a1a1a;
  color: white;
  border-radius: 4px;
  overflow: hidden;
}

.category-header {
  padding: 15px 20px;
  font-size: 16px;
  font-weight: bold;
  border-bottom: 1px solid #333;
}

.category-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.category-list li {
  padding: 12px 20px;
  border-bottom: 1px solid #333;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.2s;
}

.category-list li:hover {
  background-color: #333;
}

.category-list li:last-child {
  border-bottom: none;
}

/* 轮播图 */
.carousel-wrapper {
  width: 100%;
  border-radius: 8px;
  overflow: hidden;
}

.my-swiper {
  width: 100%;
  height: 350px; /* 调整轮播图高度，使其更合理 */
}

.slide-content {
  width: 100%;
  height: 100%;
  background-color: #f0f0f0; /* 设置背景色，填补图片两侧的空白 */
  display: flex;             /* 使用 flex 居中图片 */
  justify-content: center;   /* 水平居中 */
  align-items: center;       /* 垂直居中 */
}

.banner-img {
  max-width: 100%;
  max-height: 100%;
  width: auto;
  height: auto;
  object-fit: contain;
  display: block;
}

/* 商品展示区域 */
.product-section {
  margin-top: 30px; /* 增加与轮播图的间距 */
  padding: 20px;
  background-color: #ffffff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}

.section-title {
  font-size: 20px;
  font-weight: bold;
  color: #333;
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 2px solid #0056b3;
  display: inline-block;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 20px;
}

.product-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 15px;
  background-color: #fafafa;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid #eee;
}

.product-item:hover {
  transform: translateY(-3px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
  border-color: #0056b3;
}

.product-icon {
  width: 80px;
  height: 80px;
  margin-bottom: 10px;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #ffffff;
  border-radius: 8px;
  overflow: hidden;
}

.product-icon img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.product-category {
  font-size: 14px;
  color: #666;
  text-align: center;
}

/* 轮播组件样式微调 */
:deep(.swiper-pagination-bullet-active) {
  background-color: #0056b3;
}

:deep(.swiper-button-prev),
:deep(.swiper-button-next) {
  color: #333;
  background: rgba(255,255,255,0.5);
  width: 40px;
  height: 40px;
  border-radius: 50%;
}

:deep(.swiper-button-prev:after),
:deep(.swiper-button-next:after) {
  font-size: 18px;
}

/* 响应式适配 */
@media screen and (max-width: 992px) {
  .content-grid {
    grid-template-columns: 1fr; /* 小屏幕变成单列 */
  }
  
  .left-sidebar {
    display: none; /* 隐藏左侧分类，或者改成折叠菜单 */
  }
}

@media screen and (max-width: 768px) {
  .nav-container {
    flex-wrap: wrap;
    height: auto;
    padding: 10px;
  }
  
  .nav-group-left {
    width: 100%;
    justify-content: center;
    margin-bottom: 10px;
  }
  
  .nav-tabs {
    flex-wrap: wrap;
    justify-content: center;
  }
  
  .nav-tab {
    padding: 6px 10px;
    font-size: 11px;
  }
  
  .right-area {
    width: 100%;
    justify-content: center;
    flex-wrap: wrap;
  }
  
  .search-box {
    width: 100%;
    max-width: 280px;
  }
  
  .search-input {
    flex: 1;
    width: auto;
  }
  
  .my-swiper {
    height: 200px;
  }
  
  .product-grid {
    grid-template-columns: repeat(4, 1fr);
    gap: 10px;
  }
  
  .product-icon {
    width: 60px;
    height: 60px;
  }
}
</style>

