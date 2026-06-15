<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getProductDetail } from '@/api/product'
import { cartApi } from '@/api/cart'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus' 
import { submitReview, getProductReviews } from '@/api/review'
import { ShoppingCart } from '@element-plus/icons-vue'

// 引入智能图片路由工具与默认占位图
import { optimizeImage, defaultPlaceholder } from '@/utils/image'

const router = useRouter()
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

// 💡 整合：新增评价模块专属的响应式状态
const reviewsLoading = ref(false)
const submittingReview = ref(false)
const reviews = ref([]) // 存储从后端拉取的真实评价数组
const REVIEW_PAGE_SIZE = 3 // 评价每页显示条数
const reviewCurrentPage = ref(1) // 评价当前页码
const reviewFetchSize = ref(100) // 每次从后端获取的最大评价基数

// 💡 整合：评价表单绑定的响应式对象
const reviewForm = ref({
  reviewType: 'good',
  rating: 5,
  content: ''
})

// 💡 整合：动态过滤标签（数量初始化为 0，由后端数据动态统计更新）
const reviewFilters = ref([
  { label: '全部', value: 'all', count: 0 },
  { label: '好评', value: 'good', count: 0 },
  { label: '中评', value: 'medium', count: 0 },
  { label: '差评', value: 'bad', count: 0 },
  { label: '有图', value: 'hasImage', count: 0 }
])

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
  rating: 0,       // 动态计算总分
  reviewCount: 0   // 动态统计总数
})

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

// 💡 整合：根据当前选中的过滤分类（全部/好评/中评/差评/有图）筛选出对应的评价列表
const filteredReviewList = computed(() => {
  const list = reviews.value
  if (activeFilter.value === 'good') return list.filter(r => r.rating >= 4)
  if (activeFilter.value === 'medium') return list.filter(r => r.rating === 3)
  if (activeFilter.value === 'bad') return list.filter(r => r.rating <= 2)
  if (activeFilter.value === 'hasImage') return list.filter(r => r.images && r.images.length > 0)
  return list
})

// 💡 整合：对过滤后的评价列表实施前端逻辑切片分页
const filteredReviews = computed(() => {
  const start = (reviewCurrentPage.value - 1) * REVIEW_PAGE_SIZE
  return filteredReviewList.value.slice(start, start + REVIEW_PAGE_SIZE)
})

// 💡 整合：获取过滤后的评价总条数，供给分页组件使用
const reviewTotalCount = computed(() => filteredReviewList.value.length)

// ---------- 内部公共辅助核心方法 ----------

// 💡 整合：根据获取到的全量评价数据，动态更新过滤分类标签的统计数量
const updateReviewFilters = () => {
  const list = reviews.value
  reviewFilters.value = [
    { label: '全部', value: 'all', count: list.length },
    { label: '好评', value: 'good', count: list.filter(r => r.rating >= 4).length },
    { label: '中评', value: 'medium', count: list.filter(r => r.rating === 3).length },
    { label: '差评', value: 'bad', count: list.filter(r => r.rating <= 2).length },
    { label: '有图', value: 'hasImage', count: list.filter(r => r.images && r.images.length > 0).length }
  ]
}

// 💡 整合：根据评论列表的星级动态计算当前商品的综合平均得分
const updateOverallRating = () => {
  const list = reviews.value
  if (!list.length) {
    product.value.rating = 0
    return
  }
  const sum = list.reduce((acc, r) => acc + r.rating, 0)
  product.value.rating = sum / list.length
}

// 💡 整合：重置评价表单及状态
const resetReviewForm = () => {
  reviewForm.value = {
    reviewType: 'good',
    rating: 5,
    content: ''
  }
  reviewCurrentPage.value = 1
  activeFilter.value = 'all'
}

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
      rating: 0,
      reviewCount: 0
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

// 💡 整合：异步获取商品真实的评价列表
const fetchReviews = async () => {
  reviewsLoading.value = true
  try {
    const data = await getProductReviews(props.id, {
      page: 1,
      size: reviewFetchSize.value
    })
    reviews.value = (data?.list || []).map(item => ({
      ...item,
      // 提取纯日期部分：比如 '2026-06-14 09:40:00' -> '2026-06-14'
      date: item.createdAt ? item.createdAt.split(' ')[0] : '',
      images: item.images || []
    }))
    product.value.reviewCount = data?.total || 0
    
    // 重新计算筛选标签数量和商品综合评分
    updateReviewFilters()
    updateOverallRating()

    // 防错保护：确保当前页码不会超出最大切片页数
    const maxPage = Math.max(1, Math.ceil(reviewTotalCount.value / REVIEW_PAGE_SIZE))
    if (reviewCurrentPage.value > maxPage) {
      reviewCurrentPage.value = maxPage
    }
  } catch (e) {
    console.error('获取评价失败', e)
  } finally {
    reviewsLoading.value = false
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
// 3. 加入购物车 (使用 ElMessage)
const addToCart = async () => {
  // 1. 基础状态校验
  if (!canPurchase.value) {
    ElMessage.warning(isOffShelf.value ? '该商品已下架' : '该商品已缺货')
    return
  }
  
  // 2. 规格选中校验
  if (!selectedSkuId.value) {
    ElMessage.warning('请先选择商品规格')
    return
  }

  const payload = {
    productId: product.value.id,
    skuId: selectedSkuId.value,
    quantity: quantity.value
  }

  try {
    await cartApi.addToCart(payload)
    
    ElMessage.success(`已成功添加 ${quantity.value} 件商品到购物车`)
    
    await fetchCartCount() 
  
  } catch (err) {
    // 5. 错误捕获：从后端获取错误提示，若无则使用兜底文案
    const errorMsg = err?.response?.data?.msg || err?.message || '添加购物车失败，请重试'
    ElMessage.error(errorMsg)
  }
}

// 💡 整合：提交用户发表的全新评价
const handleSubmitReview = async () => {
  if (!userStore.token) {
    userStore.showLogin()
    ElMessage.warning('请先登录后再评价')
    return
  }

  const { reviewType, rating, content } = reviewForm.value

  if (!content.trim()) {
    ElMessage.warning('请填写评价内容')
    return
  }

  // if (reviewType === 'good' && rating < 4) {
  //   ElMessage.warning('好评请选择 4~5 星')
  //   return
  // }

  // if (reviewType === 'bad' && rating > 2) {
  //   ElMessage.warning('差评请选择 1~2 星')
  //   return
  // }

  submittingReview.value = true
  try {
    await submitReview({
      productId: Number(props.id),
      rating,
      content: content.trim()
    })
    ElMessage.success('评价成功')
    
    // 提交成功后重置表单并刷新评价列表
    reviewForm.value.content = ''
    reviewForm.value.rating = reviewType === 'good' ? 5 : 1
    activeFilter.value = 'all'
    reviewCurrentPage.value = 1
    await fetchReviews()
  } catch (e) {
    // 拦截器已处理统一异常捕获
  } finally {
    submittingReview.value = false
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

// const buyNow = () => {
//   if (!canPurchase.value) {
//     alert(isOffShelf.value ? '商品已下架' : '商品已缺货')
//     return
//   }
//   alert(`立即购买 ${quantity.value} 件商品，正在跳转到订单结算页面...`)
// }

// 💡 整合：升级后的日期格式化函数，提供健壮的防御机制
const formatDate = (dateString) => {
  if (!dateString) return ''
  const date = new Date(dateString)
  if (Number.isNaN(date.getTime())) return dateString
  return `${date.getFullYear()}-${(date.getMonth() + 1).toString().padStart(2, '0')}-${date.getDate().toString().padStart(2, '0')}`
}

// 💡 整合：点击过滤按钮切换分类标签
const changeFilter = (filterValue) => {
  activeFilter.value = filterValue
  reviewCurrentPage.value = 1
}

// 切换表单的好评/差评快捷键，自动绑定合理的预设星级
const changeReviewType = (type) => {
  reviewForm.value.reviewType = type
  reviewForm.value.rating = type === 'good' ? 5 : 1
}

// 在表单里手动点击星星评分
const setReviewRating = (rating) => {
  reviewForm.value.rating = rating
}

const showReviewImage = (img) => {
  currentReviewImage.value = img
  showReviewImageModal.value = true
}

// 处理评论组件页码切换动作
const handleReviewPageChange = (page) => {
  reviewCurrentPage.value = page
}

// 图片裂开的处理函数升级为默认占位图地址
const onImgError = (e) => {
  e.target.onerror = null
  e.target.src = defaultPlaceholder
}

const goBack = () => {
  if (window.history.length > 1) {
    router.back() 
  } else {
    router.push('/')
  }
}

// ---------- 生命周期与监听器 ----------
watch(() => props.id, async () => {
  resetReviewForm()
  await fetchProduct()
  // 💡 整合：商品 ID 变化时联动重新获取后端真实的评价列表
  await fetchReviews()
}, { immediate: true })

onMounted(() => {
  fetchCartCount()
})
</script>

<template>
  <div class="product-detail" v-loading="loading">
    <div v-if="loadError" class="load-error">
      <div class="error-icon">📭</div>
      <p>商品不存在或已下架</p>
      <router-link to="/" class="back-link-btn">返回首页</router-link>
    </div>

    <template v-else-if="!loading && product.id">
      <div class="top-nav">
        <div class="nav-left">
          <div class="back-action" @click="goBack">
            <span class="back-icon">←</span> 返回
          </div>
        </div>
        <div class="nav-center">商品详情</div>
        <div class="nav-right">
          <router-link to="/cart" class="cart-action">
            <el-icon class="cart-icon"><ShoppingCart /></el-icon>
            <span v-if="cartCount > 0" class="cart-count">{{ cartCount }}</span>
          </router-link>
        </div>
      </div>

      <div class="product-core-container">
        <div class="product-gallery card-shadow">
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

        <div class="product-info card-shadow">
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

          <div class="product-price-box">
            <span class="current-price">¥{{ displayPrice.toFixed(2) }}</span>
            <span v-if="isOffShelf" class="off-shelf-tag">已下架</span>
          </div>

          <div class="product-specs-box">
            <div class="spec-item">
              <span class="spec-label">商品编号</span>
              <span class="spec-value code-font">{{ product.id }}</span>
            </div>
            <div class="spec-item">
              <span class="spec-label">库存状态</span>
              <span class="spec-value" :class="{ low: displayStock < 10 }">
                <span class="stock-dot" :class="{ empty: displayStock === 0 }"></span>
                {{ displayStock > 0 ? `有货 (${displayStock}件)` : '缺货' }}
              </span>
            </div>
          </div>

          <div class="product-options" v-if="product.skus && product.skus.length">
            <div class="option-item">
              <div class="option-label">规格选择</div>
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
            <div class="quantity-label">购买数量</div>
            <div class="quantity-controls-wrapper">
              <div class="quantity-control">
                <button class="quantity-btn" :disabled="quantity <= 1" @click="decreaseQuantity">-</button>
                <input
                    type="number"
                    v-model.number="quantity"
                    min="1"
                    :max="maxQuantity"
                    class="quantity-input"
                    @change="validateQuantity"
                />
                <button class="quantity-btn" :disabled="quantity >= maxQuantity" @click="increaseQuantity">+</button>
              </div>
              <div class="stock-info">最多可购买 {{ maxQuantity }} 件</div>
            </div>
          </div>

          <div class="action-buttons desktop-only">
            <button class="btn add-to-cart" :disabled="!canPurchase" @click="addToCart">加入购物车</button>
            <!-- <button class="btn buy-now" :disabled="!canPurchase" @click="buyNow">立即购买</button> -->
          </div>
        </div>
      </div>

      <div class="product-details-container">
        <div class="product-description card-shadow">
          <h3 class="section-title">商品详情</h3>
          <div class="description-content">{{ product.description || '暂无商品描述' }}</div>
        </div>

        <div class="product-reviews card-shadow">
          <div class="reviews-header">
            <h3 class="section-title">用户评价 ({{ product.reviewCount }})</h3>
            <div class="overall-rating">
              <div class="rating-score">{{ product.rating.toFixed(1) }}</div>
              <div class="rating-stars">
                <div class="stars">
                  <span v-for="n in 5" :key="n" class="star" :class="{ filled: n <= product.rating }">★</span>
                </div>
                <div class="rating-text">综合评分</div>
              </div>
            </div>
          </div>

          <div class="review-form">
            <h4 class="review-form-title">发表评价</h4>
            <div class="review-form-row">
              <span class="review-form-label">商品评分</span>
              <div class="star-input">
                <span
                    v-for="n in 5"
                    :key="n"
                    class="star star-clickable"
                    :class="{ filled: n <= reviewForm.rating }"
                    @click="setReviewRating(n)"
                >★</span>
                <span class="star-input-text">{{ reviewForm.rating }} 分</span>
              </div>
            </div>
            <div class="review-form-row review-form-row-column">
              <span class="review-form-label">评价内容</span>
              <textarea
                  v-model="reviewForm.content"
                  class="review-textarea"
                  placeholder="请分享您对商品的使用感受..."
                  maxlength="500"
                  rows="3"
              />
              <div class="review-textarea-count">{{ reviewForm.content.length }}/500</div>
            </div>
            <div class="review-form-actions">
              <button class="btn submit-review-btn" :disabled="submittingReview" @click="handleSubmitReview">
                {{ submittingReview ? '提交中...' : '提交评价' }}
              </button>
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
              {{ filter.label }} ({{ filter.count }})
            </span>
          </div>

          <div class="review-list" v-loading="reviewsLoading">
            <div v-if="!filteredReviews.length && !reviewsLoading" class="review-empty">
              <span class="empty-icon">📝</span>
              <p>暂无评价，快来抢沙发吧~</p>
            </div>

            <div v-for="review in filteredReviews" :key="review.id" class="review-item">
              <div class="reviewer-info">
                <div class="reviewer-avatar">{{ review.userName?.charAt(0) || '用' }}</div>
                <div class="reviewer-details">
                  <div class="reviewer-name">{{ review.userName || '匿名用户' }}</div>
                  <div class="review-date">{{ formatDate(review.date || review.createdAt) }}</div>
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
                    :alt="'评价图片' + (index + 1)"
                    class="review-image"
                    @click="showReviewImage(img)"
                />
              </div>
            </div>
          </div>

          <div class="review-pagination" v-if="reviewTotalCount > REVIEW_PAGE_SIZE">
            <el-pagination
                v-model:current-page="reviewCurrentPage"
                :page-size="REVIEW_PAGE_SIZE"
                :total="reviewTotalCount"
                layout="prev, pager, next"
                background
                @current-change="handleReviewPageChange"
            />
          </div>
        </div>
      </div>

      <div class="bottom-bar mobile-only">
        <div class="bottom-bar-left">
          <!-- <router-link to="/" class="bottom-icon">
            <span class="icon">🏠</span>
            <span class="text">首页</span>
          </router-link>
          <router-link to="/cart" class="bottom-icon">
            <span class="icon">🛒</span>
            <span class="text">购物车</span>
            <span v-if="cartCount > 0" class="cart-badge">{{ cartCount }}</span>
          </router-link> -->
        </div>
        <div class="bottom-bar-right">
          <button class="bottom-btn add-to-cart-btn" @click="addToCart">加入购物车</button>
          <!-- <button class="bottom-btn buy-now-btn" @click="buyNow">立即购买</button> -->
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

<style scoped lang="scss">

$primary-color: #1890ff;    
$primary-hover: #40a9ff;    
$primary-light: #e6f7ff;     
$danger-color: #ff4d4f;       
$star-color: #faad14;       
$bg-color: #f0f2f5;           
$card-bg: #ffffff;         
$text-main: #262626;        
$text-sub: #595959;        
$text-muted: #8c8c8c;         
$border-color: #f0f0f0;     
$border-radius: 12px;       
$transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);

/* --- 基础布局 --- */
.product-detail {
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
  background-color: $bg-color;
  min-height: 100vh;
  padding-bottom: 20px;
}

.card-shadow {
  background-color: $card-bg;
  border-radius: $border-radius;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04);
  padding: 24px;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: $text-main;
  margin-bottom: 20px;
  display: flex;
  align-items: center;

  &::before {
    content: '';
    display: inline-block;
    width: 4px;
    height: 18px;
    background-color: $primary-color;
    border-radius: 2px;
    margin-right: 10px;
  }
}

/* --- 错误提示 --- */
.load-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
  color: $text-sub;

  .error-icon {
    font-size: 64px;
    margin-bottom: 16px;
  }
  p { font-size: 16px; margin-bottom: 24px; }

  .back-link-btn {
    padding: 10px 24px;
    background-color: $primary-color;
    color: white;
    border-radius: 20px;
    text-decoration: none;
    transition: $transition;
    &:hover { background-color: $primary-hover; }
  }
}

/* --- 顶部导航 --- */
.top-nav {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background-color: $card-bg;
  box-shadow: 0 1px 4px rgba(0,0,0,0.02);
  position: sticky;
  top: 0;
  z-index: 100;

  .nav-center {
    font-size: 18px;
    font-weight: 600;
    color: $text-main;
  }

  .back-action, .cart-action {
    display: flex;
    align-items: center;
    color: $text-main;
    font-size: 15px;
    cursor: pointer;
    text-decoration: none;
    transition: $transition;
    &:hover { color: $primary-color; }
  }

  .cart-action {
    position: relative;
    font-size: 20px;
    .cart-count {
      position: absolute;
      top: -6px; right: -10px;
      background-color: $danger-color;
      color: white;
      border-radius: 10px;
      padding: 0 5px;
      font-size: 12px;
      line-height: 16px;
      font-weight: 600;
    }
  }
}

/* --- 核心信息区容器 (Web/Mobile 关键分界点) --- */
.product-core-container {
  max-width: 1200px;
  margin: 20px auto;
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding: 0 16px;
}

/* 左侧画廊 */
.product-gallery {
  .main-image {
    width: 100%;
    aspect-ratio: 1 / 1;
    border-radius: 8px;
    overflow: hidden;
    margin-bottom: 16px;
    cursor: zoom-in;
    background: #fcfcfc;
    border: 1px solid $border-color;

    img {
      width: 100%; height: 100%;
      object-fit: contain;
      transition: transform 0.3s;
      &:hover { transform: scale(1.02); }
    }
  }

  .thumbnail-list {
    display: flex; gap: 12px; overflow-x: auto;
    padding-bottom: 8px;
    &::-webkit-scrollbar { height: 4px; }
    &::-webkit-scrollbar-thumb { background: #ddd; border-radius: 2px; }
  }

  .thumbnail-item {
    flex: 0 0 68px; height: 68px;
    border-radius: 6px;
    border: 2px solid transparent;
    cursor: pointer;
    overflow: hidden;
    transition: $transition;

    img { width: 100%; height: 100%; object-fit: cover; }
    &:hover { opacity: 0.8; }
    &.active { border-color: $primary-color; box-shadow: 0 0 0 2px $primary-light; }
  }
}

/* 右侧信息 */
.product-info {
  display: flex;
  flex-direction: column;

  .product-name {
    font-size: 22px; line-height: 1.4; color: $text-main;
    font-weight: 600; margin-bottom: 12px;
  }

  .product-rating {
    display: flex; align-items: center; gap: 8px; font-size: 13px; color: $text-sub;
  }

  .star { color: #e8e8e8; font-size: 16px; &.filled { color: $star-color; } }

  .product-price-box {
    margin: 20px 0; padding: 16px;
    background: $primary-light;
    border-radius: 8px;
    display: flex; align-items: center; gap: 12px;

    .current-price { font-size: 28px; font-weight: 700; color: $primary-color; }
    .off-shelf-tag { background: $text-muted; color: white; padding: 2px 8px; border-radius: 4px; font-size: 12px; }
  }

  .product-specs-box {
    display: flex; flex-direction: column; gap: 12px;
    padding-bottom: 20px; border-bottom: 1px dashed $border-color;
    margin-bottom: 20px; font-size: 14px;

    .spec-item { display: flex; align-items: center; }
    .spec-label { width: 80px; color: $text-muted; }
    .spec-value { 
      color: $text-main; display: flex; align-items: center;
      &.code-font { font-family: monospace; }
      &.low { color: $danger-color; font-weight: 500; }
    }
    .stock-dot {
      display: inline-block; width: 6px; height: 6px; border-radius: 50%;
      background-color: #52c41a; margin-right: 6px;
      &.empty { background-color: $danger-color; }
    }
  }

  /* 规格选择 */
  .option-label, .quantity-label {
    font-size: 14px; color: $text-muted; margin-bottom: 12px; width: 80px; flex-shrink: 0;
  }
  
  .option-item { display: flex; flex-direction: column; margin-bottom: 24px; }
  
  .option-values { display: flex; flex-wrap: wrap; gap: 10px; }

  .option-value {
    padding: 8px 20px; background: #fafafa; border: 1px solid #d9d9d9;
    border-radius: 6px; font-size: 14px; color: $text-main; cursor: pointer; transition: $transition;

    &:hover:not(.disabled) { border-color: $primary-hover; color: $primary-hover; }
    &.selected { border-color: $primary-color; background-color: $primary-light; color: $primary-color; font-weight: 500; }
    &.disabled { background: #f5f5f5; color: #bfbfbf; cursor: not-allowed; border-color: #e8e8e8; }
  }

  /* 数量控制 */
  .quantity-selector {
    display: flex; flex-direction: column; margin-bottom: 30px;
    .quantity-controls-wrapper { display: flex; align-items: center; gap: 16px; }
  }

  .quantity-control {
    display: flex; align-items: center; border: 1px solid #d9d9d9; border-radius: 6px; overflow: hidden;
    .quantity-btn {
      width: 36px; height: 36px; background: #fafafa; border: none; font-size: 18px; color: $text-sub;
      cursor: pointer; transition: $transition;
      &:hover:not(:disabled) { background: #e6e6e6; color: $primary-color; }
      &:disabled { color: #d9d9d9; cursor: not-allowed; }
    }
    .quantity-input {
      width: 50px; height: 36px; text-align: center; border: none; border-left: 1px solid #d9d9d9; border-right: 1px solid #d9d9d9; font-size: 14px; color: $text-main;
      -moz-appearance: textfield;
      &::-webkit-outer-spin-button, &::-webkit-inner-spin-button { -webkit-appearance: none; margin: 0; }
      &:focus { outline: none; }
    }
  }
  .stock-info { font-size: 13px; color: $text-muted; }
}

/* 按钮基类 */
.btn {
  padding: 0 24px; height: 44px; border-radius: 22px; font-size: 16px; font-weight: 500;
  cursor: pointer; transition: $transition; border: none; display: inline-flex; align-items: center; justify-content: center;
  &:disabled { opacity: 0.6; cursor: not-allowed; }
}

.action-buttons {
  display: flex; gap: 16px; margin-top: auto;
  .add-to-cart { flex: 1; background: $primary-light; color: $primary-color; border: 1px solid $primary-color; }
  .add-to-cart:hover:not(:disabled) { background: white; }
  .buy-now { flex: 1; background: $primary-color; color: white; box-shadow: 0 4px 12px rgba(24, 144, 255, 0.3); }
  .buy-now:hover:not(:disabled) { background: $primary-hover; transform: translateY(-1px); }
}

/* --- 底部容器 (详情与评价) --- */
.product-details-container {
  max-width: 1200px; margin: 0 auto 40px; padding: 0 16px; display: flex; flex-direction: column; gap: 20px;
}

.description-content {
  font-size: 15px; line-height: 1.8; color: $text-sub;
  /* 此处如需富文本，可配合 v-html 及对应样式 */
}

/* 评价面板 */
.overall-rating {
  display: flex; align-items: center; gap: 16px; padding: 20px; background: #fafafa; border-radius: 8px; margin-bottom: 24px;
  .rating-score { font-size: 40px; font-weight: bold; color: $primary-color; line-height: 1; }
  .rating-stars { display: flex; flex-direction: column; gap: 4px; .rating-text { font-size: 13px; color: $text-muted; } }
}

.review-form {
  border: 1px solid $border-color; padding: 20px; border-radius: 8px; margin-bottom: 24px; background: white;
  .review-form-title { font-size: 16px; margin-bottom: 16px; color: $text-main; }
  .review-form-row { display: flex; align-items: center; gap: 16px; margin-bottom: 16px; &.review-form-row-column { flex-direction: column; align-items: stretch; } }
  .review-form-label { width: 70px; color: $text-sub; font-size: 14px; }
  .star-clickable { cursor: pointer; transition: transform 0.2s; &:hover { transform: scale(1.15); } }
  .star-input {
  display: flex;
  align-items: center;
  gap: 4px;

  /* 默认状态：鼠标没放上去，也没被点击时，全部星星为灰色 */
  .star-clickable {
    font-size: 24px;
    color: #e8e8e8; /* 默认灰色 */
    cursor: pointer;
    transition: all 0.15s ease;

    /* 被点击选中后的高亮状态（通过后端绑定的 filled 类） */
    &.filled {
      color: #faad14; /* 金黄色 */
    }
  }

  &:hover {
    .star-clickable {
      color: #faad14; /* 只要鼠标移入容器，先把所有星星变黄 */
    }
  }

  .star-clickable:hover ~ .star-clickable {
    color: #e8e8e8; /* 然后让当前鼠标悬浮的星星【后面】的所有兄弟星星恢复成灰色 */
  }
  
  /* 鼠标悬浮时给星星加个微微放大的微动效，更有现代感 */
  .star-clickable:hover {
    transform: scale(1.2);
  }

  .star-input-text {
    color: #1890ff; /* 现代化蓝风格数字提示 */
    font-weight: 500;
    font-size: 14px;
    margin-left: 10px;
  }
}
  .review-textarea {
    padding: 12px; border: 1px solid #d9d9d9; border-radius: 6px; font-size: 14px; resize: vertical; transition: $transition;
    &:focus { border-color: $primary-color; box-shadow: 0 0 0 2px $primary-light; outline: none; }
  }
  .review-textarea-count { text-align: right; font-size: 12px; color: $text-muted; margin-top: 4px; }
  .review-form-actions { display: flex; justify-content: flex-end; .submit-review-btn { background: $primary-color; color: white; height: 36px; padding: 0 20px; font-size: 14px; border-radius: 6px; } }
}

.review-filters {
  display: flex; flex-wrap: wrap; gap: 12px; margin-bottom: 24px;
  .review-filter {
    padding: 6px 16px; border-radius: 16px; background: #fafafa; color: $text-sub; font-size: 13px; cursor: pointer; transition: $transition; border: 1px solid transparent;
    &:hover { background: $primary-light; color: $primary-color; }
    &.active { background: $primary-color; color: white; font-weight: 500; }
  }
}

.review-empty { text-align: center; padding: 40px 0; color: $text-muted; .empty-icon { font-size: 48px; display: block; margin-bottom: 12px; opacity: 0.5; } }

.review-item {
  padding: 20px 0; border-bottom: 1px solid $border-color;
  &:last-child { border-bottom: none; }
  .reviewer-info { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
  .reviewer-avatar { width: 36px; height: 36px; border-radius: 50%; background: linear-gradient(135deg, $primary-color, $primary-hover); color: white; display: flex; align-items: center; justify-content: center; font-size: 16px; }
  .reviewer-details { flex: 1; .reviewer-name { font-size: 14px; color: $text-main; margin-bottom: 2px; } .review-date { font-size: 12px; color: $text-muted; } }
  .review-content { font-size: 14px; line-height: 1.6; color: $text-main; margin-bottom: 12px; }
  .review-images { display: flex; gap: 10px; flex-wrap: wrap; .review-image { width: 72px; height: 72px; border-radius: 6px; object-fit: cover; cursor: zoom-in; border: 1px solid #f0f0f0; } }
}

.review-pagination { display: flex; justify-content: flex-end; margin-top: 20px; }

/* --- 移动端吸底条 --- */
.bottom-bar {
  position: fixed; 
  bottom: 0; 
  left: 0; 
  right: 0;
  background: white; 
  border-top: 1px solid $border-color; 
  padding: 8px 16px; 
  padding-bottom: calc(8px + env(safe-area-inset-bottom));
  display: flex; 
  justify-content: space-between; 
  align-items: center; 
  z-index: 1000; 
  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.05);

  .bottom-bar-left { 
    display: flex; 
    gap: 20px; 
  }

  .bottom-icon { 
    display: flex; 
    flex-direction: column; 
    align-items: center; 
    color: $text-sub; 
    text-decoration: none; 
    position: relative; 
    font-size: 11px; 
    
    .icon { 
      font-size: 20px; 
      margin-bottom: 2px; 
    } 
  }

  .cart-badge { 
    position: absolute; 
    top: -2px; 
    right: -8px; 
    background: $danger-color; 
    color: white; 
    border-radius: 10px; 
    padding: 0 4px; 
    font-size: 10px; 
    line-height: 14px; 
    border: 1px solid white; 
  }

  /* 右侧按钮包裹容器 */
  .bottom-bar-right { 
    display: flex; 
    gap: 8px; 
    flex: 1; 
    justify-content: flex-end; 
    max-width: 260px; 
    margin-left: 10px; 
  }

  /* 按钮基础样式与居中配置 */
  .bottom-btn {
    flex: 1; 
    height: 40px; 
    border-radius: 20px; 
    border: none; 
    font-size: 14px; 
    font-weight: 500;
    cursor: pointer;
    
    /* 💡 核心：确保按钮内的文字在水平与垂直方向上完美居中 */
    display: inline-flex;
    justify-content: center;
    align-items: center;
    transition: background-color 0.2s ease, opacity 0.2s ease;

    &:active {
      opacity: 0.85; /* 点击时的微交互反馈 */
    }

    &.add-to-cart-btn { 
      background: $primary-light; 
      color: $primary-color; 
    }

    &.buy-now-btn { 
      background: $primary-color; 
      color: white; 
    }
  }
}

/* 放大图片模态框 */
.image-zoom-modal {
  position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.85); display: flex; align-items: center; justify-content: center; z-index: 2000; backdrop-filter: blur(4px);
  .modal-content { position: relative; max-width: 90vw; max-height: 90vh; }
  .close-modal { position: absolute; top: -40px; right: 0; background: none; border: none; color: white; font-size: 32px; cursor: pointer; }
  .zoomed-image { max-width: 100%; max-height: 85vh; border-radius: 8px; object-fit: contain; }
}

/* --- 响应式规则 (Web/Mobile 适配) --- */
.desktop-only { display: none; }
.mobile-only { display: flex; }

@media (min-width: 768px) {
  .product-detail { padding-bottom: 40px; }
  .desktop-only { display: flex; }
  .mobile-only { display: none; }

  .product-core-container { flex-direction: row; align-items: stretch; margin-top: 24px; }
  .product-gallery { flex: 0 0 45%; position: sticky; top: 80px; height: fit-content; }
  .product-info { flex: 1; }
  
  .option-item { flex-direction: row; align-items: flex-start; }
  .quantity-selector { flex-direction: row; align-items: center; }
  .option-label, .quantity-label { margin-top: 8px; margin-bottom: 0; }
}
</style>