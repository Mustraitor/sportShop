<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getProductDetail } from '@/api/product'
import { cartApi } from '@/api/cart'
import { useUserStore } from '@/stores/user'

// 💡 优化 1：引入智能图片路由工具与默认占位图
import { optimizeImage, defaultPlaceholder } from '@/utils/image'

const props = defineProps({
  id: {
    type: [String, Number],
    required: true
  }
})

// ---------- 全局状态定义 ----------
const userStore = useUserStore()

// ---------- 响应式数据 (Refs) ----------
const loading = ref(true)
const loadError = ref(false)
const currentImageIndex = ref(0)
const selectedSkuId = ref(null)
const quantity = ref(1)

// 购物车角标数量直接联动 Pinia 状态机，实现全局实时同步
const cartCount = computed(() => userStore.cartCount)

const showImageZoom = ref(false)
const showReviewImageModal = ref(false)
const currentReviewImage = ref('')
const activeFilter = ref('all')

const product = ref({
  id: null,
  name: '',
  description: '',
  price: 0,
  stock: 0,
  status: 1,
  mainImage: '',
  images: [],
  skus: [],
  rating: 4.5,
  reviewCount: 245
})

// 模拟评价数据原样保留
const reviews = ref([
  {
    id: 1,
    userName: '跑步爱好者',
    date: '2023-10-15',
    rating: 5,
    content: '鞋子非常轻便，透气性很好，跑了10公里脚也不觉得闷。缓震效果也不错，对膝盖友好。',
    images: ['https://images.unsplash.com/photo-1542291026-7eec264c27ff?ixlib=rb-4.0.3&auto=format&fit=crop&w=400&q=80']
  },
  { id: 2, userName: '运动新手', date: '2023-10-10', rating: 4, content: '外观很时尚，穿着舒适。尺码标准，按平时尺码购买即可。唯一不足是白色不太耐脏。', images: [] },
  {
    id: 3,
    userName: '健身达人',
    date: '2023-10-05',
    rating: 5,
    content: '性价比很高的一款跑鞋，无论是跑步还是日常穿着都很舒适。已经推荐给朋友了。',
    images: [
      'https://images.unsplash.com/photo-1606107557195-0e29a4b5b4aa?ixlib=rb-4.0.3&auto=format&fit=crop&w=400&q=80',
      'https://images.unsplash.com/photo-1600185365483-26d7a4cc7519?ixlib=rb-4.0.3&auto=format&fit=crop&w=400&q=80'
    ]
  },
  { id: 4, userName: '马拉松选手', date: '2023-09-28', rating: 4, content: '作为训练鞋很合适，轻量化的设计让长距离训练更轻松。不过对于专业比赛来说，缓冲可能稍显不足。', images: [] },
  { id: 5, userName: '日常通勤者', date: '2023-09-20', rating: 5, content: '不仅适合运动，日常穿搭也很棒。很百搭，舒适度可以穿一整天。', images: [] }
])

const reviewFilters = ref([
  { label: '全部', value: 'all', count: 245 },
  { label: '好评', value: 'good', count: 210 },
  { label: '中评', value: 'medium', count: 25 },
  { label: '差评', value: 'bad', count: 10 },
  { label: '有图', value: 'hasImage', count: 89 }
])

// ---------- 计算属性 (Computed) ----------

/**
 * 💡 优化 2：画廊图片集计算属性重构
 * 提取主图及副图时，直接通过 optimizeImage 处理成目标路由地址（宽度传 800 以保证大图清晰度）
 */
const galleryImages = computed(() => {
  const imgs = []
  
  // 处理主图
  if (product.value.mainImage) {
    imgs.push(optimizeImage(product.value.mainImage, 800))
  }
  
  // 处理副图组
  ;(product.value.images || []).forEach(img => {
    if (img) {
      const routedImg = optimizeImage(img, 800)
      if (!imgs.includes(routedImg)) imgs.push(routedImg)
    }
  })
  
  return imgs.length ? imgs : [defaultPlaceholder]
})

const currentImage = computed(() => galleryImages.value[currentImageIndex.value] || defaultPlaceholder)

const selectedSku = computed(() => {
  if (!selectedSkuId.value || !product.value.skus?.length) return null
  return product.value.skus.find(s => s.id === selectedSkuId.value) || null
})

const displayPrice = computed(() => {
  const price = selectedSku.value?.price ?? product.value.price
  return Number(price) || 0
})

const displayStock = computed(() => selectedSku.value ? selectedSku.value.stock : (product.value.stock || 0))

const maxQuantity = computed(() => Math.min(displayStock.value, 99))

const isOffShelf = computed(() => product.value.status === 0)

const canPurchase = computed(() => !isOffShelf.value && displayStock.value > 0)

const filteredReviews = computed(() => {
  const list = reviews.value
  if (activeFilter.value === 'good') return list.filter(r => r.rating >= 4).slice(0, 3)
  if (activeFilter.value === 'medium') return list.filter(r => r.rating === 3).slice(0, 3)
  if (activeFilter.value === 'bad') return list.filter(r => r.rating <= 2).slice(0, 3)
  if (activeFilter.value === 'hasImage') return list.filter(r => r.images && r.images.length > 0).slice(0, 3)
  return list.slice(0, 3)
})

// ---------- 异步业务方法 (Arrow Functions) ----------

// 1. 获取商品详情
const fetchProduct = async () => {
  loading.value = true
  loadError.value = false
  currentImageIndex.value = 0
  quantity.value = 1
  try {
    const data = await getProductDetail(props.id)
    if (!data) {
      loadError.value = true
      return
    }
    
    // 💡 优化 3：确保副图字段类型安全，若为字符串则切为数组
    let sanitizedImages = data.images
    if (typeof sanitizedImages === 'string') {
      try {
        sanitizedImages = JSON.parse(sanitizedImages)
      } catch (err) {
        sanitizedImages = sanitizedImages.split(',').filter(Boolean)
      }
    }

    product.value = {
      ...data,
      images: Array.isArray(sanitizedImages) ? sanitizedImages : [],
      rating: 4.5,
      reviewCount: 245
    }
    if (data.skus?.length) {
      const firstAvailable = data.skus.find(s => s.stock > 0) || data.skus[0]
      selectedSkuId.value = firstAvailable?.id ?? null
    } else {
      selectedSkuId.value = null
    }
  } catch (e) {
    loadError.value = true
  } finally {
    loading.value = false
  }
}

// 2. 获取并更新全局购物车数量
const fetchCartCount = async () => {
  try {
    userStore.initGuestId()
    const res = await cartApi.getCartList()
    const total = res?.total || res?.data?.total || 0
    userStore.updateCartCount(total)
  } catch (err) {
    console.error('获取购物车数量失败', err)
  }
}

// 3. 加入购物车
const addToCart = async () => {
  if (!canPurchase.value) {
    alert(isOffShelf.value ? '商品已下架' : '商品已缺货')
    return
  }
  if (!selectedSkuId.value) {
    alert('请选择规格')
    return
  }

  const payload = {
    productId: product.value.id,
    skuId: selectedSkuId.value,
    quantity: quantity.value
  }

  try {
    await cartApi.addToCart(payload)
    await fetchCartCount() 
    alert(`已成功添加 ${quantity.value} 件商品到购物车`)
  } catch (err) {
    alert(err?.response?.data?.msg || '添加失败')
  }
}

// ---------- 页面交互方法 (Arrow Functions) ----------
const changeImage = (index) => {
  currentImageIndex.value = index
}

const selectSku = (sku) => {
  if (!sku || isSkuDisabled(sku)) return
  selectedSkuId.value = sku.id
  if (quantity.value > sku.stock) {
    quantity.value = Math.max(1, sku.stock)
  }
}

const isSkuDisabled = (sku) => !sku || sku.stock <= 0

const increaseQuantity = () => {
  if (quantity.value < maxQuantity.value) quantity.value++
}

const decreaseQuantity = () => {
  if (quantity.value > 1) quantity.value--
}

const validateQuantity = () => {
  if (quantity.value < 1) {
    quantity.value = 1
  } else if (quantity.value > maxQuantity.value) {
    quantity.value = maxQuantity.value
  }
}

const buyNow = () => {
  if (!canPurchase.value) {
    alert(isOffShelf.value ? '商品已下架' : '商品已缺货')
    return
  }
  alert(`立即购买 ${quantity.value} 件商品，正在跳转到订单结算页面...`)
}

const formatDate = (dateString) => {
  const date = new Date(dateString)
  return `${date.getFullYear()}-${(date.getMonth() + 1).toString().padStart(2, '0')}-${date.getDate().toString().padStart(2, '0')}`
}

const changeFilter = (filterValue) => {
  activeFilter.value = filterValue
}

const showReviewImage = (img) => {
  currentReviewImage.value = img
  showReviewImageModal.value = true
}

const showAllReviews = () => {
  alert('正在加载并显示全部历史评价')
}

// 💡 优化 4：图片裂开的处理函数升级为默认占位图地址
const onImgError = (e) => {
  e.target.onerror = null
  e.target.src = defaultPlaceholder
}

// ---------- 生命周期与监听器 ----------
watch(() => props.id, () => {
  fetchProduct()
}, { immediate: true })

onMounted(() => {
  fetchCartCount()
})
</script>

<template>
  <div class="product-detail" v-loading="loading">
    <div v-if="loadError" class="load-error">
      <p>商品不存在或已下架</p>
      <router-link to="/" class="back-link">返回首页</router-link>
    </div>

    <template v-else-if="!loading && product.id">
    <div class="top-nav">
      <div class="nav-left">
        <router-link to="/" class="back-link">
          <span class="back-icon">←</span> 返回首页
        </router-link>
      </div>
      <div class="nav-center">商品详情</div>
      <div class="nav-right">
        <router-link to="/cart" class="cart-link">
          <span class="cart-icon">🛒</span>
          <span v-if="cartCount > 0" class="cart-count">{{ cartCount }}</span>
        </router-link>
      </div>
    </div>

    <div class="product-gallery">
      <div class="main-image">
        <img :src="currentImage" :alt="product.name" @click="showImageZoom = true" @error="onImgError" />
      </div>
      <div class="thumbnail-list" v-if="galleryImages.length > 1">
        <div
            v-for="(img, index) in galleryImages"
            :key="index"
            class="thumbnail-item"
            :class="{ active: currentImageIndex === index }"
            @click="changeImage(index)"
        >
          <img :src="img" :alt="'商品图片' + (index+1)" @error="onImgError" />
        </div>
      </div>
    </div>

    <div class="product-info">
      <div class="product-header">
        <h2 class="product-name">{{ product.name }}</h2>
        <div class="product-rating">
          <div class="stars">
            <span v-for="n in 5" :key="n" class="star" :class="{ filled: n <= product.rating }">★</span>
          </div>
          <span class="rating-text">{{ product.rating.toFixed(1) }}分</span>
          <span class="review-count">({{ product.reviewCount }}条评价)</span>
        </div>
      </div>

      <div class="product-price">
        <span class="current-price">¥{{ displayPrice.toFixed(2) }}</span>
        <span v-if="isOffShelf" class="off-shelf-tag">已下架</span>
      </div>

      <div class="product-specs">
        <div class="spec-item">
          <span class="spec-label">商品编号：</span>
          <span class="spec-value">{{ product.id }}</span>
        </div>
        <div class="spec-item">
          <span class="spec-label">库存：</span>
          <span class="spec-value" :class="{ low: displayStock < 10 }">
            {{ displayStock > 0 ? `有货(${displayStock}件)` : '缺货' }}
          </span>
        </div>
      </div>

      <div class="product-options" v-if="product.skus && product.skus.length">
        <div class="option-item">
          <div class="option-label">规格</div>
          <div class="option-values">
            <span
                v-for="sku in product.skus"
                :key="sku.id"
                class="option-value"
                :class="{
                  selected: selectedSkuId === sku.id,
                  disabled: isSkuDisabled(sku)
                }"
                @click="selectSku(sku)"
            >
              {{ sku.skuName }}
            </span>
          </div>
        </div>
      </div>

      <div class="quantity-selector">
        <div class="quantity-label">购买数量：</div>
        <div class="quantity-control">
          <button
              class="quantity-btn"
              :disabled="quantity <= 1"
              @click="decreaseQuantity"
          >-</button>
          <input
              type="number"
              v-model.number="quantity"
              min="1"
              :max="maxQuantity"
              class="quantity-input"
              @change="validateQuantity"
          />
          <button
              class="quantity-btn"
              :disabled="quantity >= maxQuantity"
              @click="increaseQuantity"
          >+</button>
        </div>
        <div class="stock-info">最多可购买{{ maxQuantity }}件</div>
      </div>

      <div class="action-buttons">
        <button
            class="btn add-to-cart"
            :disabled="!canPurchase"
            @click="addToCart"
        >
          <span class="btn-icon">🛒</span> 加入购物车
        </button>
        <button
            class="btn buy-now"
            :disabled="!canPurchase"
            @click="buyNow"
        >
          立即购买
        </button>
      </div>

      <div class="product-description">
        <h3>商品详情</h3>
        <div class="description-content">{{ product.description || '暂无商品描述' }}</div>
      </div>
    </div>

    <div class="product-reviews">
      <div class="reviews-header">
        <h3>用户评价 ({{ product.reviewCount }})</h3>
        <div class="overall-rating">
          <div class="rating-score">{{ product.rating.toFixed(1) }}</div>
          <div class="rating-stars">
            <div class="stars">
              <span v-for="n in 5" :key="n" class="star" :class="{ filled: n <= product.rating }">★</span>
            </div>
            <div class="rating-text">{{ product.rating.toFixed(1) }}分</div>
          </div>
        </div>
      </div>

      <div class="review-filters">
        <span
            v-for="filter in reviewFilters"
            :key="filter.label"
            class="review-filter"
            :class="{ active: activeFilter === filter.value }"
            @click="changeFilter(filter.value)"
        >
          {{ filter.label }}({{ filter.count }})
        </span>
      </div>

      <div class="review-list">
        <div v-for="review in filteredReviews" :key="review.id" class="review-item">
          <div class="reviewer-info">
            <div class="reviewer-avatar">{{ review.userName.charAt(0) }}</div>
            <div class="reviewer-details">
              <div class="reviewer-name">{{ review.userName }}</div>
              <div class="review-date">{{ formatDate(review.date) }}</div>
            </div>
            <div class="review-rating">
              <span v-for="n in 5" :key="n" class="star" :class="{ filled: n <= review.rating }">★</span>
            </div>
          </div>
          <div class="review-content">{{ review.content }}</div>
          <div v-if="review.images && review.images.length > 0" class="review-images">
            <img
                v-for="(img, index) in review.images"
                :key="index"
                :src="img"
                :alt="'评价图片' + (index+1)"
                class="review-image"
                @click="showReviewImage(img)"
            />
          </div>
        </div>
      </div>

      <div class="more-reviews" v-if="product.reviewCount > 3">
        <button class="btn more-reviews-btn" @click="showAllReviews">
          查看全部{{ product.reviewCount }}条评价
        </button>
      </div>
    </div>

    <div class="bottom-bar">
      <div class="bottom-bar-left">
        <router-link to="/" class="bottom-icon">
          <span class="icon">🏠</span>
          <span class="text">首页</span>
        </router-link>
        <router-link to="/cart" class="bottom-icon">
          <span class="icon">🛒</span>
          <span class="text">购物车</span>
          <span v-if="cartCount > 0" class="cart-badge">{{ cartCount }}</span>
        </router-link>
      </div>
      <div class="bottom-bar-right">
        <button class="bottom-btn add-to-cart-btn" @click="addToCart">加入购物车</button>
        <button class="bottom-btn buy-now-btn" @click="buyNow">立即购买</button>
      </div>
    </div>

    <div v-if="showImageZoom" class="image-zoom-modal" @click="showImageZoom = false">
      <div class="modal-content" @click.stop>
        <button class="close-modal" @click="showImageZoom = false">×</button>
        <img :src="currentImage" :alt="product.name" class="zoomed-image" @error="onImgError" />
      </div>
    </div>

    <div v-if="showReviewImageModal" class="image-zoom-modal" @click="showReviewImageModal = false">
      <div class="modal-content" @click.stop>
        <button class="close-modal" @click="showReviewImageModal = false">×</button>
        <img :src="currentReviewImage" alt="评价图片" class="zoomed-image" />
      </div>
    </div>
    </template>
  </div>
</template>


<style scoped>
/* 样式与原文件完全一致，请保留您原有的所有样式代码 */
/* 这里仅做一个示例，实际请复制您之前文件中的完整样式 */
.product-detail {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, sans-serif;
  max-width: 1200px;
  margin: 0 auto;
  padding-bottom: 80px;
  background-color: #f8f9fa;
  min-height: 300px;
}

.load-error {
  text-align: center;
  padding: 80px 20px;
  color: #666;
}

.load-error p {
  margin-bottom: 16px;
  font-size: 16px;
}

/* 顶部导航 */
.top-nav {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background-color: white;
  border-bottom: 1px solid #eaeaea;
  position: sticky;
  top: 0;
  z-index: 100;
}

.nav-center {
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.back-link, .cart-link {
  display: flex;
  align-items: center;
  text-decoration: none;
  color: #333;
  font-size: 16px;
}

.back-icon {
  margin-right: 4px;
  font-size: 20px;
}

.cart-link {
  position: relative;
}

.cart-count {
  position: absolute;
  top: -8px;
  right: -8px;
  background-color: #ff4444;
  color: white;
  border-radius: 50%;
  width: 18px;
  height: 18px;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 商品图片区域 */
.product-gallery {
  background-color: white;
  padding: 16px;
  margin-bottom: 12px;
}

.main-image {
  width: 100%;
  height: 350px;
  overflow: hidden;
  border-radius: 8px;
  margin-bottom: 12px;
  cursor: zoom-in;
}

.main-image img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.thumbnail-list {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding-bottom: 8px;
}

.thumbnail-item {
  flex: 0 0 80px;
  height: 80px;
  border-radius: 4px;
  overflow: hidden;
  border: 2px solid transparent;
  cursor: pointer;
  opacity: 0.7;
  transition: all 0.2s;
}

.thumbnail-item.active {
  border-color: #007bff;
  opacity: 1;
}

.thumbnail-item:hover {
  opacity: 1;
}

.thumbnail-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* 商品信息 */
.product-info {
  background-color: white;
  padding: 16px;
  margin-bottom: 12px;
  border-radius: 8px;
}

.product-header {
  margin-bottom: 12px;
}

.product-name {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin-bottom: 8px;
  line-height: 1.4;
}

.product-rating {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #666;
}

.stars {
  display: flex;
  gap: 2px;
}

.star {
  color: #ddd;
  font-size: 16px;
}

.star.filled {
  color: #ffc107;
}

.product-price {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #eee;
}

.current-price {
  font-size: 24px;
  font-weight: 700;
  color: #ff4444;
}

.original-price {
  font-size: 16px;
  color: #999;
  text-decoration: line-through;
}

.off-shelf-tag {
  background-color: #999;
  color: white;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 14px;
  font-weight: 600;
}

.product-specs {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid #eee;
  font-size: 14px;
  color: #666;
}

.spec-item {
  display: flex;
}

.spec-label {
  width: 80px;
  color: #999;
}

.spec-value.low {
  color: #ff4444;
  font-weight: 600;
}

/* 商品规格选择 */
.product-options {
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid #eee;
}

.option-item {
  margin-bottom: 16px;
}

.option-item:last-child {
  margin-bottom: 0;
}

.option-label {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 8px;
}

.option-values {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.option-value {
  padding: 8px 16px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  color: #333;
  cursor: pointer;
  transition: all 0.2s;
  min-width: 60px;
  text-align: center;
}

.option-value:hover {
  border-color: #007bff;
  color: #007bff;
}

.option-value.selected {
  border-color: #007bff;
  background-color: #e6f2ff;
  color: #007bff;
  font-weight: 600;
}

.option-value.disabled {
  background-color: #f5f5f5;
  color: #ccc;
  cursor: not-allowed;
  border-color: #eee;
  text-decoration: line-through;
}

.option-value.disabled:hover {
  border-color: #eee;
  color: #ccc;
}

/* 购买数量 */
.quantity-selector {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid #eee;
}

.quantity-label {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.quantity-control {
  display: flex;
  align-items: center;
  gap: 0;
}

.quantity-btn {
  width: 40px;
  height: 40px;
  background-color: #f8f9fa;
  border: 1px solid #ddd;
  font-size: 18px;
  color: #333;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.quantity-btn:hover:not(:disabled) {
  background-color: #e9ecef;
}

.quantity-btn:disabled {
  color: #ccc;
  cursor: not-allowed;
  background-color: #f8f9fa;
}

.quantity-input {
  width: 60px;
  height: 40px;
  border: 1px solid #ddd;
  border-left: none;
  border-right: none;
  text-align: center;
  font-size: 16px;
  color: #333;
  -moz-appearance: textfield;
}

.quantity-input::-webkit-outer-spin-button,
.quantity-input::-webkit-inner-spin-button {
  -webkit-appearance: none;
  margin: 0;
}

.stock-info {
  font-size: 14px;
  color: #666;
}

/* 操作按钮 */
.action-buttons {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.btn {
  flex: 1;
  padding: 16px;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.btn:disabled {
  background-color: #ccc;
  color: #666;
  cursor: not-allowed;
}

.add-to-cart {
  background-color: #fff;
  color: #007bff;
  border: 1px solid #007bff;
}

.add-to-cart:hover:not(:disabled) {
  background-color: #f0f7ff;
}

.buy-now {
  background-color: #007bff;
  color: white;
}

.buy-now:hover:not(:disabled) {
  background-color: #0056b3;
}

/* 商品描述 */
.product-description {
  background-color: white;
  padding: 20px 16px;
  margin-bottom: 12px;
  border-radius: 8px;
}

.product-description h3 {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #eee;
}

.description-content {
  font-size: 15px;
  line-height: 1.6;
  color: #444;
}

.description-content h4 {
  font-size: 16px;
  margin: 16px 0 8px 0;
  color: #333;
}

.description-content ul {
  margin-left: 20px;
  margin-bottom: 16px;
}

.description-content li {
  margin-bottom: 6px;
}

/* 用户评价 */
.product-reviews {
  background-color: white;
  padding: 20px 16px;
  border-radius: 8px;
}

.reviews-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #eee;
}

.reviews-header h3 {
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.overall-rating {
  display: flex;
  align-items: center;
  gap: 12px;
}

.rating-score {
  font-size: 36px;
  font-weight: 700;
  color: #ff6b00;
}

.rating-stars {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.review-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid #eee;
}

.review-filter {
  padding: 8px 16px;
  border: 1px solid #ddd;
  border-radius: 20px;
  font-size: 14px;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
}

.review-filter:hover {
  border-color: #007bff;
  color: #007bff;
}

.review-filter.active {
  border-color: #007bff;
  background-color: #e6f2ff;
  color: #007bff;
  font-weight: 600;
}

.review-list {
  margin-bottom: 20px;
}

.review-item {
  padding: 16px 0;
  border-bottom: 1px solid #f0f0f0;
}

.review-item:last-child {
  border-bottom: none;
}

.reviewer-info {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.reviewer-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: #007bff;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 18px;
}

.reviewer-details {
  flex: 1;
}

.reviewer-name {
  font-size: 15px;
  font-weight: 600;
  color: #333;
  margin-bottom: 4px;
}

.review-date {
  font-size: 12px;
  color: #999;
}

.review-rating {
  display: flex;
  gap: 2px;
}

.review-content {
  font-size: 15px;
  line-height: 1.5;
  color: #444;
  margin-bottom: 12px;
}

.review-images {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.review-image {
  width: 80px;
  height: 80px;
  border-radius: 4px;
  object-fit: cover;
  cursor: pointer;
  transition: transform 0.2s;
}

.review-image:hover {
  transform: scale(1.05);
}

.more-reviews {
  text-align: center;
  padding-top: 20px;
  border-top: 1px solid #eee;
}

.more-reviews-btn {
  width: auto;
  padding: 12px 32px;
  background-color: #f8f9fa;
  color: #333;
  border: 1px solid #ddd;
  border-radius: 20px;
  font-size: 15px;
}

.more-reviews-btn:hover {
  background-color: #e9ecef;
}

/* 底部操作栏 */
.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background-color: white;
  border-top: 1px solid #eaeaea;
  padding: 12px 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  z-index: 1000;
  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.1);
}

.bottom-bar-left {
  display: flex;
  gap: 24px;
}

.bottom-icon {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-decoration: none;
  color: #666;
  position: relative;
}

.bottom-icon .icon {
  font-size: 20px;
  margin-bottom: 4px;
}

.bottom-icon .text {
  font-size: 12px;
}

.cart-badge {
  position: absolute;
  top: -5px;
  right: -5px;
  background-color: #ff4444;
  color: white;
  border-radius: 50%;
  width: 18px;
  height: 18px;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.bottom-bar-right {
  display: flex;
  gap: 12px;
  flex: 1;
  max-width: 300px;
}

.bottom-btn {
  flex: 1;
  padding: 12px;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.add-to-cart-btn {
  background-color: #fff;
  color: #007bff;
  border: 1px solid #007bff;
}

.add-to-cart-btn:hover {
  background-color: #f0f7ff;
}

.buy-now-btn {
  background-color: #007bff;
  color: white;
}

.buy-now-btn:hover {
  background-color: #0056b3;
}

/* 图片放大模态框 */
.image-zoom-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.9);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  padding: 20px;
}

.modal-content {
  position: relative;
  max-width: 90%;
  max-height: 90%;
}

.close-modal {
  position: absolute;
  top: -40px;
  right: 0;
  background: none;
  border: none;
  color: white;
  font-size: 40px;
  cursor: pointer;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
}

.zoomed-image {
  max-width: 100%;
  max-height: 80vh;
  object-fit: contain;
}

/* 响应式设计 */
@media (min-width: 768px) {
  .product-detail {
    padding-bottom: 0;
  }

  .top-nav {
    padding: 16px 24px;
  }

  .product-gallery {
    display: flex;
    gap: 20px;
    padding: 24px;
  }

  .main-image {
    flex: 1;
    height: 400px;
  }

  .thumbnail-list {
    flex-direction: column;
    width: 100px;
    overflow-y: auto;
    overflow-x: hidden;
  }

  .thumbnail-item {
    flex: 0 0 80px;
    width: 80px;
  }

  .product-info, .product-description, .product-reviews {
    padding: 24px;
    margin-bottom: 16px;
  }

  .action-buttons {
    max-width: 400px;
    margin-left: auto;
    margin-right: auto;
  }

  .bottom-bar {
    display: none; /* 在桌面端隐藏底部操作栏 */
  }
}
</style>