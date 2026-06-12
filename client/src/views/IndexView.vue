<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { Swiper, SwiperSlide } from 'swiper/vue'
import { Navigation, Pagination, Autoplay } from 'swiper/modules'
import NavBar from '@/components/NavBar.vue'

import { useHomeStore } from '@/stores/home'
import { getProductList } from "@/api/product"
import { getCategoryList } from "@/api/category"

import 'swiper/css'
import 'swiper/css/navigation'
import 'swiper/css/pagination'

import banner1 from '@/assets/banner1.png'
import banner2 from '@/assets/banner2.png'
import banner3 from '@/assets/banner3.png'
import banner4 from '@/assets/banner4.png'
import banner5 from '@/assets/banner5.png'
import banner6 from '@/assets/banner6.png'

import { optimizeImage, defaultPlaceholder } from '@/utils/image'
import { ElPagination } from 'element-plus'

const homeStore = useHomeStore()
const router = useRouter()

// ==================== 1. 数据状态声明 ====================
const allProducts = ref([])     // 运动好物推荐列表（当前页）
const totalProducts = ref(0)    // 总数
const currentPage = ref(1)      // 当前页
const pageSize = 10             // 每页显示 10 个（2行*5列）
const seasonProducts = ref([])  // 当季品类跑马灯列表
const allCategory = ref([])     // 核心分类树
const isDataLoading = ref(false)
const currentSeasonTabIdx = ref(0)
const windowWidth = ref(typeof window !== 'undefined' ? window.innerWidth : 1200)

const bannerImages = ref([banner1, banner2, banner3, banner4, banner5, banner6])
const modules = [Navigation, Pagination, Autoplay]

// ==================== 2. 工具函数 ====================
const formatPrice = (price) => {
  const num = Number(price)
  return isNaN(num) ? price : num.toFixed(2)
}

const goDetail = (id) => {
  if (id) router.push(`/product/${id}`)
}

const handleImageError = (e) => {
  e.target.onerror = null
  e.target.src = defaultPlaceholder
}

const updateWindowWidth = () => {
  windowWidth.value = window.innerWidth
}

// ==================== 3. 后端分页请求 ====================
const fetchProducts = async (page = 1) => {
  isDataLoading.value = true
  try {
    const res = await getProductList({ page, size: pageSize, status: 1 })
    allProducts.value = res?.list || []
    totalProducts.value = res?.total || 0
    currentPage.value = page
  } catch (error) {
    allProducts.value = []
    totalProducts.value = 0
  }
  isDataLoading.value = false
}

// ==================== 4. 分类及当季品类 ====================
const fetchCategory = async () => {
  try {
    const res = await getCategoryList()
    allCategory.value = res || []
  } catch (error) {
    allCategory.value = []
  }
}

const loadSeasonProducts = async (tab) => {
  if (!tab?.id) return
  try {
    const res = await getProductList({ page: 1, size: 10, categoryId: tab.id, status: 1 })
    seasonProducts.value = res?.list || []
  } catch (error) {
    seasonProducts.value = []
  }
}

const buildRecommendedProducts = (categories, products) => {
  const directMap = {}
  products.forEach(p => {
    if (p.categoryId != null && p.mainImage && !directMap[p.categoryId]) {
      directMap[p.categoryId] = p
    }
  })

  const picked = new Set()
  const result = []

  categories.forEach(root => {
    ;(root.children || []).forEach(child => {
      const product = directMap[child.id]
      if (product && !picked.has(product.id)) {
        result.push(product)
        picked.add(product.id)
      }
    })
  })

  products.forEach(p => {
    if (!picked.has(p.id)) {
      result.push(p)
      picked.add(p.id) 
    }
  })

  return result
}

// ==================== 5. 计算属性 ====================
const recommendedProducts = computed(() => buildRecommendedProducts(allCategory.value, allProducts.value))
const seasonTabList = computed(() => {
  const target = allCategory.value.find(item => item.id === 1)
  return target?.children || []
})

const autoSlideNum = computed(() => {
  const w = windowWidth.value
  if (w < 640) return 2
  if (w < 992) return 3
  return 5
})

// ==================== 6. 交互函数 ====================
const switchSeasonTab = async (idx) => {
  if (currentSeasonTabIdx.value === idx) return
  currentSeasonTabIdx.value = idx
  await loadSeasonProducts(seasonTabList.value[idx])
}

const handlePageChange = (page) => {
  fetchProducts(page)
}

// ==================== 7. 生命周期 ====================
onMounted(async () => {
  await Promise.all([fetchProducts(currentPage.value), fetchCategory()])

  if (seasonTabList.value?.length > 0) {
    await loadSeasonProducts(seasonTabList.value[0])
  }

  window.addEventListener('resize', updateWindowWidth)
})

onUnmounted(() => {
  window.removeEventListener('resize', updateWindowWidth)
})
</script>

<template>
  <NavBar/>
  <div class="page-container">
    <main class="main-content-wrapper">
      <section class="deals-panel">   
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
                <img :src="img" :alt="'Banner ' + (index + 1)" class="banner-img" />
              </div>
            </swiper-slide>
          </swiper>
        </div>
        <div class="product-section">
          <h3 class="section-title">运动好物推荐</h3>
          <div class="product-grid">
            <div
              v-for="item in recommendedProducts"
              :key="item.id"
              class="product-item"
              @click="goDetail(item.id)"
            >
              <div class="product-icon">
                <img 
                  :src="optimizeImage(item.mainImage, 360)" 
                  loading="lazy" 
                  :alt="item.name" 
                  @error="handleImageError" 
                />
              </div>
              <div class="product-info">
                <div class="product-name">{{ item.name }}</div>
                <div class="product-price">¥{{ formatPrice(item.price) }}</div>
                <div v-if="item.status === 1" class="product-status">在售</div>
              </div>
            </div>
          </div>
              <!-- 分页控件 -->
          <el-pagination
            style="margin-top: 20px; text-align: center;"
            :current-page="currentPage"
            :page-size="pageSize"
            :total="totalProducts"
            layout="prev, pager, next"
            @current-change="handlePageChange"
          />
          <div class="season-category-wrap">
            <div class="season-top-title-wrap">
              <h3 class="section-title">当季品类</h3>
              <div class="season-tab-group">
                <span
                  v-for="(tab, idx) in seasonTabList"
                  :key="idx"
                  @click="switchSeasonTab(idx)"
                  class="season-tab-item"
                  :class="{ active: currentSeasonTabIdx === idx }"
                >
                  {{ tab.name }}
                </span>
              </div>
            </div>
            
            <div class="season-goods-box">
              <swiper
                :key="currentSeasonTabIdx"
                :modules="[Navigation]"
                :slides-per-view="autoSlideNum"
                :space-between="16"
                :navigation="true"
                class="season-goods-swiper"
              >
                <swiper-slide v-for="goods in seasonProducts" :key="goods.id">
                  <div class="season-goods-card" @click="goDetail(goods.id)">
                    <div class="goods-img-box">
                      <img 
                        :src="optimizeImage(goods.mainImage, 300)" 
                        loading="lazy" 
                        alt="商品图片" 
                        @error="handleImageError"
                      />
                    </div>
                    <div class="goods-name">{{ goods.name }}</div>
                    <div class="goods-price">¥{{ goods.price }}</div>
                  </div>
                </swiper-slide>
              </swiper>
            </div>
          </div>
        </div>
      </section>
    </main>

  </div>
</template>

<style scoped lang="scss">

.page-container {
  width: 100%;
  background-color: #f8f8f8;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.main-content-wrapper {
  flex: 1; 
  width: 100%;
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
}

.content-grid {
  display: grid;
  grid-template-columns: 220px 1fr;
  gap: 20px;
}

.left-sidebar {
  background-color: #f5f5f5;
  border-radius: 4px;
  overflow: visible;
  border: 1px solid #e8e8e8;
  position: relative;

  .category-list-container {
    padding: 10px 0;
  }

  .category-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 20px;
    font-size: 14px;
    color: #333;
    cursor: pointer;
    transition: all 0.2s;
    border-bottom: 1px solid #efefef;
    position: relative;

    &:last-child {
      border-bottom: none;
    }

    &:hover,
    &.active {
      background-color: #e8e8e8;
      color: #0056b3;
    }

    &.has-sub {
      color: #000;
      font-weight: 500;
    }

    &.separator {
      border-top: 1px solid #ddd;
      margin-top: 5px;
      padding-top: 15px;
      color: #999;
    }

    &.special {
      color: #d32f2f;
      font-weight: bold;

      &:hover {
        color: #b71c1c;
        background-color: #f5f5f5;
      }
    }

    .category-arrow {
      color: #999;
      font-size: 12px;
    }

    .sub-category-popup {
      position: absolute;
      left: 100%;
      top: 0;
      min-width: 600px;
      max-width: 800px;
      background: #fff;
      border: 1px solid #e8e8e8;
      border-left: none;
      border-radius: 0 4px 4px 0;
      box-shadow: 2px 2px 8px rgba(0, 0, 0, 0.1);
      padding: 20px;
      z-index: 1001;
      display: flex;
      flex-wrap: wrap;
      gap: 20px;

      .sub-category-group {
        flex: 1;
        min-width: 180px;

        .group-title {
          font-size: 14px;
          font-weight: bold;
          color: #333;
          margin-bottom: 12px;
          padding-bottom: 6px;
          border-bottom: 1px solid #eee;
        }

        .sub-category-items {
          display: flex;
          flex-wrap: wrap;
          gap: 8px 12px;

          .sub-category-item {
            font-size: 13px;
            color: #666;
            padding: 4px 8px;
            border-radius: 4px;
            cursor: pointer;
            transition: all 0.2s;
            white-space: nowrap;

            &:hover {
              background-color: #f0f7ff;
              color: #0056b3;
            }
          }
        }
      }
    }
  }
}
// 好物推荐
.product-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 15px;
  background: #fafafa;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid #eee;
  box-sizing: border-box;

  &:hover {
    transform: translateY(-1px);
     box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    border-color: #0056b3;
  }

  .product-icon {
    width: 100%;
    aspect-ratio: 1 / 1;
    max-width: 120px;
    max-height: 120px;
    margin-bottom: 10px;
    display: flex;
    justify-content: center;
    align-items: center;
    background: #fff;
    border-radius: 8px;
    overflow: hidden;

    img {
      max-width: 100%;
      max-height: 100%;
      object-fit: contain;
    }
  }

  .product-info {
    text-align: center;
    margin-top: 8px;

    .product-name {
      font-size: 14px;
      font-weight: 500;
      color: #222;
      text-align: center;
      display: -webkit-box;
      // -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
      height: 40px;
    }

    .product-price {
      font-size: 16px;
      color: #e60012;
      font-weight: bold;
      margin: 4px 0;
    }

    .product-status {
      font-size: 12px;
      color: #0056b3;
      background: #e6f0fa;
      padding: 2px 6px;
      border-radius: 4px;
      margin-top: 4px;
    }
  }
}

.category-panel {
  width: 220px;
}

.carousel-wrapper {
  width: 100%;
  max-width: 100%;
  margin: 0 auto;
  border-radius: 8px;
  overflow: hidden;
}

.my-swiper {
  width: 100%;
  max-width: 100%;
  height: 500px;
}

.slide-content {
  width: 100%;
  height: 100%;
  background-color: #f0f0f0;
  display: flex;
  justify-content: center;
  align-items: center;

  .banner-img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
  }
}

.product-section {
  margin-top: 30px;
  padding: 20px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);

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
    grid-template-columns: repeat(5, 1fr);
    padding: 2px 0;
    gap: 20px;
    overflow: hidden;
  }
}
// swiper
.carousel-wrapper {
  /* 导航按钮大小配置 */
  :deep(.swiper) {
    --swiper-navigation-size: 10px;
    --swiper-navigation-sides-offset: 10px;
  }

  /* 导航按钮基础样式：改为极简风格 */
  :deep(.swiper-button-prev),
  :deep(.swiper-button-next) {
    color: #ffffff; /* 默认为白色，在 Banner 图上更清晰 */
    width: 44px;
    height: 44px;
    background: rgba(0, 0, 0, 0.2); /* 仅用极淡的黑色遮罩，而不是突兀的白底 */
    border-radius: 50%;
    backdrop-filter: blur(4px);
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    opacity: 0; /* 默认隐藏 */
    transform: translateX(10px); /* 默认带一点偏移 */
  }
  
  :deep(.swiper-button-prev) { transform: translateX(-10px); }

  /* 交互效果：Hover 时显示并回到中心 */
  &:hover {
    :deep(.swiper-button-prev),
    :deep(.swiper-button-next) {
      opacity: 1;
      transform: translateX(0);
    }
  }

  /* 悬停时的精致交互 */
  :deep(.swiper-button-prev:hover),
  :deep(.swiper-button-next:hover) {
    background: rgba(0, 0, 0, 0.5) !important; /* 悬停时稍微加深，不需要蓝色 */
    transform: scale(1.1);
  }

  :deep(.swiper-button-disabled) {
    opacity: 0 !important;
  }
}

/* 分页器样式：改为更细小的圆点，符合极简感 */
:deep(.swiper-pagination-bullet) {
  width: 8px;
  height: 8px;
  background: rgba(255, 255, 255, 0.5);
  transition: all 0.3s;
}

:deep(.swiper-pagination-bullet-active) {
  background: #ffffff;
  width: 20px; /* 活跃态变长，更有质感 */
  border-radius: 4px;
}
// 品类
.season-category-wrap {
  margin-top: 40px;

  .season-top-title-wrap {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 22px;

    .section-title {
      margin-bottom: 0; // 成功将原本的行内样式收纳到这里
    }
  }

  .season-tab-group {
    display: flex;
    gap: 36px;

    .season-tab-item {
      font-size: 17px;
      color: #555;
      cursor: pointer;
      padding: 7px 0;
      position: relative;

      &.active {
        color: #0056b3;

        &::after {
          content: '';
          position: absolute;
          left: 0;
          bottom: 0;
          width: 100%;
          height: 2px;
          background: #0056b3;
        }
      }
    }
  }

  .season-goods-box {
    margin-top: 18px; // 成功将原本的行内样式收纳到这里
  }

  .season-banner-box {
    width: 100%;
    height: 330px;
    border-radius: 6px;
    overflow: hidden;
    position: relative;
    margin-bottom: 20px;

    .season-banner-img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  }

  .season-goods-swiper {
    width: 100%;
  }

  .season-goods-card {
    background: #fff;
    padding: 14px;
    cursor: pointer;

    .goods-img-box {
      width: 100%;
      aspect-ratio: 1 / 1;    
      background: #f5f5f5;   
      border-radius: 4px;
      overflow: hidden;
      justify-content: center;
      align-items: center;  

      img {
        width: 100%;         
        height: 100%;         
        display: block;
        object-fit: contain;
      }
    }

    .goods-name {
      font-size: 15px;
      font-weight: 500;
      margin-top: 10px;
    }

    .goods-tag {
      font-size: 12px;
      color: #666;
      line-height: 1.5;
      margin: 5px 0;
    }

    .goods-price {
      color: #e60012;
      font-weight: bold;
      font-size: 17px;
    }

    .goods-coupon {
      margin-top: 7px;
      font-size: 13px;
      color: #fff;
      background: linear-gradient(90deg, #ff3333, #ff7722);
      padding: 4px 7px;
      border-radius: 3px;
    }
  }
}


@media (max-width: 1200px) {
  .product-grid {
    grid-template-columns: repeat(4, 1fr);
    gap: 16px;
  }
}

@media (max-width: 768px) {
  .product-grid {
    grid-template-columns: repeat(3, 1fr);
    gap: 12px;
  }

  .product-item {
    padding: 10px;
  }

  .my-swiper {
    height: 200px;
  }

  .season-banner-box {
    height: 190px;
  }
}

@media (max-width: 480px) {
  .product-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 10px;
  }
}
</style>