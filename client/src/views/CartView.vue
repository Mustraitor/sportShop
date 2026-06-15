<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus' 
import NavBar from '@/components/NavBar.vue'
import { cartApi } from '@/api/cart'
import { useUserStore } from '@/stores/user'
import request from '@/utils/request'
import SkuDrawer from '@/components/SkuDrawer.vue' 
import { optimizeImage } from '@/utils/image'
import ModernDialog from '@/components/ModernDialog.vue' 

const router = useRouter()
const userStore = useUserStore()

// ---------- 响应式数据 ----------
const loading = ref(false)
const cartList = ref([])
const totalCount = ref(0)
const checkedTotalAmount = ref(0)

// 猜你喜欢板块对应的响应式数据
const randomLoading = ref(false)
const randomProducts = ref([])

// ---------- 计算属性 ----------
const isAllChecked = computed({
  get() {
    if (cartList.value.length === 0) return false
    return cartList.value.every(item => item.isChecked)
  },
  set(val) {
    toggleSelectAll(val)
  }
})

// ---------- 购物车图片格式化 ----------
const formatCartItem = (item) => ({
  ...item,
  isChecked: item.checked === 1,
  mainImage: optimizeImage(item.mainImage, 200)
})

// 洗牌算法（随机打乱推荐商品）
const shuffleArray = (array) => {
  const arr = [...array]
  for (let i = arr.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [arr[i], arr[j]] = [arr[j], arr[i]]
  }
  return arr
}

// ---------- 获取“猜你喜欢”数据 ----------
const fetchRandomProducts = async () => {
  randomLoading.value = true
  try {
    const res = await request.get('/product/list', {
      params: { page: 1, size: 50 }
    })
    const list = res.list || res.records || []
    
    const processedList = list.map(item => {
      return { 
        ...item, 
        mainImage: optimizeImage(item.mainImage, 400) 
      }
    })
    const shuffled = shuffleArray(processedList)
    randomProducts.value = shuffled.slice(0, 8)
  } catch (err) {
    console.error('随机商品加载失败', err)
  } finally {
    randomLoading.value = false
  }
}

// ---------- 获取购物车数据 ----------
const fetchCart = async () => {
  try {
    userStore.initGuestId()
    const res = await cartApi.getCartList()
    const dataObj = res.data ? res.data : res;
    
    cartList.value = (dataObj.list || []).map(formatCartItem)
    totalCount.value = dataObj.total || 0
    checkedTotalAmount.value = dataObj.checkedTotalAmount || 0
    
    userStore.updateCartCount(totalCount.value)
  } catch (err) {
    console.error('获取购物车失败', err)
  }
}

// ---------- 更新数量 ----------
const updateQuantity = async (item, action) => {
  if (action === 'sub' && item.quantity <= 1) {
    removeItem(item.skuId)
    return
  }
  if (action === 'add' && item.quantity >= item.stock) {
    alert(`库存不足，最多可购买 ${item.stock} 件`)
    return
  }
  try {
    await cartApi.updateCartQuantity(item.skuId, action)
    await fetchCart() 
  } catch (err) {
    console.error('修改数量失败', err)
  }
}

// ---------- 勾选/取消勾选 ----------
const updateChecked = async (item, checkedVal) => {
  const newChecked = checkedVal ? 1 : 0
  try {
    await cartApi.toggleCheckItem(item.skuId, newChecked)
    await fetchCart() 
  } catch (err) {
    console.error('修改勾选状态失败', err)
  }
}

// ---------- 删除商品 ----------
const removeItem = async (skuId) => {
  ElMessage.success('删除成功')
  try {
    await cartApi.deleteCartItem(skuId)
    await fetchCart()
  } catch (err) {
    console.error('删除失败:', err)
  }
}

// ---------- 全选/全不选 ----------
const toggleSelectAll = async (isSelected) => {
  if (cartList.value.length === 0) return
  const promises = cartList.value.map(item => {
    if (item.isChecked !== isSelected) {
      const newChecked = isSelected ? 1 : 0
      return cartApi.toggleCheckItem(item.skuId, newChecked)
    }
    return Promise.resolve()
  })
  try {
    await Promise.all(promises)
    await fetchCart()
  } catch (err) {
    console.error('全选控制失败', err)
  }
}

// ---------- 结算跳转 ----------
const checkout = async () => {
  const selected = cartList.value.filter(item => item.isChecked)
  if (selected.length === 0) {
    ElMessage.warning('请先选择要结算的商品')
    return
  }

  if (!userStore.token) {
    userStore.isLoginVisible = true
    return
  }

  const cartIdsStr = selected.map(item => item.skuId).join(',')

  router.push({
    path: '/checkout',
    query: { cartIds: cartIdsStr }
  })
}

// ---------- 跳转商品详情 ----------
const goToProductDetail = (productId) => {
  router.push(`/product/${productId}`)
}

// 猜你喜欢：加入购物车逻辑
const addToCartFromRandom = (product, e) => {
  e.stopPropagation() 
  console.log('=== 当前点击的推荐商品原始数据 ===', product)
  
  currentSelectedProduct.value = product
  skuDrawerVisible.value = true
}

// 控制清空操作的弹窗状态
const showClearDialog = ref(false)
const isClearLoading = ref(false)

const openClearConfirm = () => {
  showClearDialog.value = true
}

const handleClearConfirm = async () => {
  isClearLoading.value = true
  try {
    await cartApi.clearCart()
    await fetchCart() 
    ElMessage.success('购物车已清空')
    showClearDialog.value = false 
  } catch (err) {
    console.error('清空失败', err)
  } finally {
    isClearLoading.value = false
  }
}

// ---------- 控制规格抽屉响应式状态 ----------
const skuDrawerVisible = ref(false)
const currentSelectedProduct = ref(null)
const cartLoading = ref(false)

const handleSkuCartConfirm = async (skuData) => {
  if (!skuData.productId) {
    console.error('【前端拦截】无法获取到商品的 productId，传递的数据为：', skuData)
    ElMessage.error('商品数据异常，请刷新页面重试')
    return
  }

  cartLoading.value = true
  try {

    await cartApi.addToCart({
      productId: skuData.productId,
      skuId: skuData.skuId,
      quantity: skuData.quantity
    })
    
    ElMessage.success('商品已成功加入购物车！')
    skuDrawerVisible.value = false // 关闭抽屉
    await fetchCart()
    
  } catch (error) {
    console.error('加入购物车失败:', error)
    ElMessage.error('加入购物车失败，请重试')
  } finally {
    cartLoading.value = false
  }
}

// ---------- 生命周期初始化 ----------
onMounted(async () => {
  userStore.initGuestId()
  await Promise.all([
    fetchCart(),
    fetchRandomProducts()
  ])
})
</script>

<template>
  <div class="cart-container">
    <NavBar />
    <main class="main-content">
      <h2>购物车 ({{ totalCount }})</h2>

      <div v-if="loading" class="loading">加载中...</div>

      <div v-else-if="cartList.length === 0" class="empty-cart">
        购物车还是空的，去逛逛吧～
      </div>

      <div v-else class="cart-list">
        <div 
          class="cart-item" 
          v-for="item in cartList" 
          :key="item.skuId"
          :style="loading ? 'pointer-events: none; opacity: 0.6;' : ''"
        >
          <div class="item-select">
            <input 
              type="checkbox" 
              v-model="item.isChecked" 
              @change="updateChecked(item, $event.target.checked)" 
            />
          </div>

          <div class="item-image" @click="goToProductDetail(item.productId)">
            <img :src="item.mainImage" alt="product" />
          </div>

          <div class="item-info">
            <div class="brand">{{ item.productName ? item.productName.split(' ')[0] : '' }}</div>
            <div class="title">{{ item.productName }}</div>
          </div>

          <div class="item-specs">
            <div class="code">SKU ID: {{ item.skuId }}</div>
            <div class="size">规格: {{ item.skuName }}</div>
          </div>

          <div class="item-price">
            <div class="label">单价</div>
            <div class="value">¥{{ Number(item.price).toFixed(2) }}</div>
          </div>

          <div class="item-quantity">
            <button @click="updateQuantity(item, 'sub')" :disabled="item.quantity <= 1 || loading">-</button>
            <span>{{ item.quantity }}</span>
            <button @click="updateQuantity(item, 'add')" :disabled="item.quantity >= item.stock || loading">+</button>
          </div>

          <div class="item-actions">
            <button class="icon-btn" title="删除" @click="removeItem(item.skuId)">🗑️</button>
          </div>
        </div>
      </div>

      <div class="cart-footer" v-if="cartList.length > 0">
        <div class="footer-left">
          <label class="select-all">
            <input type="checkbox" v-model="isAllChecked" />
            <span>全选</span>
          </label>
          <span class="total-count">商品总种类：<strong>{{ totalCount }}</strong></span>
          <button class="clear-btn" :disabled="loading" @click="openClearConfirm">清空购物车</button>
          
          <ModernDialog
            v-model="showClearDialog"
            title="清空购物车"
            confirm-text="确认清空"
            :loading="isClearLoading"
            @confirm="handleClearConfirm"
          >
            <div style="padding: 10px 0; color: #606266;">
              清空购物车后将无法恢复，确定要移除所有商品吗？
            </div>
          </ModernDialog>
        </div>

        <div class="footer-right">
          <div class="total-price">
            合计 <span>¥{{ Number(checkedTotalAmount).toFixed(2) }}</span>
          </div>
          <div class="tax-tip">所有产品价格均包含增值税</div>
          <button class="checkout-btn" :disabled="loading" @click="checkout">结算</button>
        </div>
      </div>
    </main>

    <section class="recommend-section">
      <div class="recommend-header">
        <h3>猜你喜欢</h3>
        <span class="recommend-subtitle">随机发现好物</span>
      </div>

      <div v-if="randomLoading" class="loading">商品加载中…</div>

      <div v-else-if="randomProducts.length > 0" class="recommend-grid">
        <div 
          class="product-card" 
          v-for="product in randomProducts" 
          :key="product.id" 
          @click="goToProductDetail(product.id)"
        >
          <div class="card-img">
            <img :src="product.mainImage" :alt="product.name || product.title" />
            <button class="quick-add-btn" @click.stop="addToCartFromRandom(product, $event)">
              加入购物车
            </button>
          </div>
          <div class="card-info">
            <p class="card-name">{{ product.name || product.title }}</p>
            <p class="card-price">¥{{ (Number(product.price) || 0).toFixed(2) }}</p>
          </div>
        </div>
      </div>
      
      <div v-else class="empty-recommend">暂无推荐商品</div>
    </section>

    <SkuDrawer 
      v-model:show="skuDrawerVisible"
      :product="currentSelectedProduct"
      :submitting="cartLoading"
      @confirm="handleSkuCartConfirm"
    />
  </div>
</template>
<style lang="scss" scoped>
/* 优雅的颜色与变量定义 */
$primary-color: #0075bf;
$primary-hover: #005a9e;
$danger-color: #e00000;
$danger-hover: #b80000;
$bg-color: #f7f8fa;
$text-main: #222222;
$text-muted: #8c8c8c;
$border-color: #eaeaea;
$radius: 8px;
$transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);

/* 全局容器背景优化 */
.cart-container {
  max-width: 1600px;
  margin: 0 auto;
  background-color: $bg-color;
  min-height: 100vh;
  padding-bottom: 40px; /* 减少底部过长留白 */
}

/* 主体内容区卡片化 */
.main-content {
  max-width: 1200px;
  margin: 24px auto 0;
  padding: 24px;
  background: #ffffff;
  border-radius: $radius;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);

  h2 {
    margin-bottom: 20px;
    font-size: 20px;
    font-weight: 600;
    color: $text-main;
    border-left: 4px solid $primary-color;
    padding-left: 10px;
    line-height: 1;
  }
}

/* 列表渲染美化 */
.cart-list {
  border-top: 1px solid $border-color;
}

.cart-item {
  display: flex;
  align-items: center;
  padding: 16px 12px; /* 紧凑间距 */
  border-bottom: 1px solid $border-color;
  background-color: #ffffff;
  transition: $transition;
  border-radius: 4px;
  margin: 4px 0;

  &:hover {
    background-color: #fafbfc;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.02);
  }

  /* 各种弹性盒单元格自适应优化 */
  .item-select {
    flex: 0.4;
    display: flex;
    justify-content: center;
    
    input[type="checkbox"] {
      cursor: pointer;
      width: 16px;
      height: 16px;
      accent-color: $primary-color;
    }
  }

  .item-image {
    flex: 1;
    cursor: pointer;
    overflow: hidden;
    display: flex;
    justify-content: center;

    img {
      width: 80px;
      height: 90px;
      object-fit: contain;
      background: #fff;
      border: 1px solid $border-color;
      border-radius: 4px;
      transition: $transition;

      &:hover {
        transform: scale(1.05);
        border-color: $primary-color;
      }
    }
  }

  .item-info {
    flex: 2.5;
    padding: 0 16px;

    .brand {
      font-weight: 600;
      font-size: 12px;
      margin-bottom: 4px;
      color: $primary-color;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }

    .title {
      font-size: 14px;
      line-height: 1.4;
      color: $text-main;
      font-weight: 500;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }
  }

  .item-specs {
    flex: 1.5;
    font-size: 13px;
    color: $text-muted;
    line-height: 1.6;

    .code {
      font-family: monospace;
      font-size: 12px;
    }
  }

  .item-price {
    flex: 1;
    text-align: center;

    .label {
      font-size: 11px;
      color: $text-muted;
      margin-bottom: 2px;
    }

    .value {
      font-weight: 600;
      font-size: 15px;
      color: $text-main;
    }
  }

  /* 数量选择器精致化 */
  .item-quantity {
    flex: 1.2;
    display: flex;
    align-items: center;
    justify-content: center;

    button {
      width: 28px;
      height: 28px;
      border: 1px solid #dcdfe6;
      background: #fff;
      cursor: pointer;
      font-size: 16px;
      display: flex;
      align-items: center;
      justify-content: center;
      transition: $transition;
      border-radius: 4px;

      &:first-of-type {
        border-top-right-radius: 0;
        border-bottom-right-radius: 0;
      }
      &:last-of-type {
        border-top-left-radius: 0;
        border-bottom-left-radius: 0;
        border-left: none;
      }
      &:first-of-type + span + button {
        border-left: none; /* 边缘合并 */
      }

      &:hover:not(:disabled) {
        color: $primary-color;
        border-color: $primary-color;
        background-color: #f0f7ff;
      }

      &:disabled {
        color: #c0c4cc;
        background-color: #f5f7fa;
        cursor: not-allowed;
      }
    }

    span {
      width: 36px;
      height: 28px;
      display: flex;
      align-items: center;
      justify-content: center;
      border-top: 1px solid #dcdfe6;
      border-bottom: 1px solid #dcdfe6;
      font-size: 13px;
      font-weight: 500;
      background: #fff;
    }
  }

  .item-actions {
    flex: 0.8;
    display: flex;
    justify-content: center;

    .icon-btn {
      background: none;
      border: none;
      cursor: pointer;
      font-size: 16px;
      padding: 6px;
      border-radius: 50%;
      transition: $transition;

      &:hover {
        background-color: #fff0f0;
        transform: scale(1.1);
      }
    }
  }
}

/* 吸底结算栏高级感 */
.cart-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 24px;
  padding: 16px 20px;
  background: #fcfdfe;
  border: 1px solid $border-color;
  border-radius: $radius;

  .footer-left {
    display: flex;
    align-items: center;

    .select-all {
      display: flex;
      align-items: center;
      cursor: pointer;
      margin-right: 24px;
      font-size: 14px;
      font-weight: 500;

      input {
        margin-right: 6px;
        width: 16px;
        height: 16px;
        accent-color: $primary-color;
      }
    }

    .total-count {
      font-size: 14px;
      color: #555;

      strong {
        color: $primary-color;
        margin: 0 4px;
      }
    }

    .clear-btn {
      margin-left: 24px;
      background: none;
      border: 1px solid #dcdfe6;
      padding: 5px 14px;
      font-size: 13px;
      cursor: pointer;
      border-radius: 4px;
      color: #606266;
      transition: $transition;

      &:hover:not(:disabled) {
        color: $danger-color;
        border-color: #fbc4c4;
        background-color: #fff0f0;
      }
      &:disabled {
        cursor: not-allowed;
        opacity: 0.6;
      }
    }
  }

  .footer-right {
    display: flex;
    align-items: center; /* 改为横向紧凑布局 */
    gap: 24px;

    .price-block {
      text-align: right;
    }

    .total-price {
      font-size: 14px;
      color: #333;

      span {
        color: $danger-color;
        font-weight: 700;
        font-size: 26px;
        margin-left: 6px;
      }
    }

    .tax-tip {
      font-size: 12px;
      color: $text-muted;
      margin-top: 2px;
    }

    .checkout-btn {
      background-color: $primary-color;
      color: white;
      border: none;
      padding: 12px 48px;
      font-size: 16px;
      font-weight: 600;
      cursor: pointer;
      border-radius: 4px;
      box-shadow: 0 4px 12px rgba(0, 117, 191, 0.2);
      transition: $transition;

      &:hover:not(:disabled) {
        background-color: $primary-hover;
        box-shadow: 0 6px 16px rgba(0, 117, 191, 0.3);
        transform: translateY(-1px);
      }
      
      &:active {
        transform: translateY(1px);
      }
      
      &:disabled {
        background-color: #a0cfff;
        cursor: not-allowed;
        box-shadow: none;
      }
    }
  }
}

/* 猜你喜欢卡片区（独立卡片化） */
.recommend-section {
  max-width: 1200px;
  margin: 32px auto 0; /* 压缩模块与模块之间的过大间距 */
  padding: 24px;
  background: #ffffff;
  border-radius: $radius;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);

  .recommend-header {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 20px;
    border-left: 4px solid #ff6b00;
    padding-left: 10px;

    h3 {
      font-size: 20px;
      font-weight: 600;
      color: $text-main;
    }

    .recommend-subtitle {
      font-size: 13px;
      color: $text-muted;
    }
  }
}

.recommend-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px; /* 稍微缩紧卡片间隙，更显精致 */
}

.product-card {
  background: #fff;
  border-radius: 6px;
  overflow: hidden;
  transition: $transition;
  border: 1px solid #f0f0f0;
  cursor: pointer;
  display: flex;
  flex-direction: column;

  &:hover {
    box-shadow: 0 8px 20px rgba(0, 0, 0, 0.08);
    transform: translateY(-4px);
    border-color: rgba($primary-color, 0.2);
  }

  .card-img {
    position: relative;
    overflow: hidden;
    background: #fafafa;
    aspect-ratio: 1; /* 正方形展示更标准紧凑 */

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      transition: transform 0.4s cubic-bezier(0.4, 0, 0.2, 1);
    }
  }

  &:hover {
    .card-img img {
      transform: scale(1.05);
    }
    .quick-add-btn {
      opacity: 1;
      transform: translate(-50%, 0);
    }
  }

  .quick-add-btn {
    position: absolute;
    bottom: 12px;
    left: 50%;
    transform: translate(-50%, 8px); /* 向上浮出效果 */
    background: rgba($primary-color, 0.95);
    color: #fff;
    border: none;
    padding: 8px 18px;
    border-radius: 20px;
    font-size: 12px;
    font-weight: 500;
    opacity: 0;
    backdrop-filter: blur(4px);
    transition: $transition;
    cursor: pointer;
    white-space: nowrap;
    box-shadow: 0 4px 10px rgba(0, 0, 0, 0.15);

    &:hover {
      background: $primary-hover;
      transform: translate(-50%, -1px) scale(1.05);
    }
  }

  .card-info {
    padding: 12px;
    flex: 1;
    display: flex;
    flex-direction: column;
    justify-content: space-between;

    .card-name {
      font-size: 13px;
      color: #333;
      margin-bottom: 6px;
      line-height: 1.4;
      font-weight: 500;
      height: 36px; /* 固定两行高度，防止排版错位 */
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }

    .card-price {
      font-size: 16px;
      font-weight: 700;
      color: $danger-color;
      margin-bottom: 4px;
    }

    .card-meta {
      font-size: 11px;
      color: $text-muted;
      display: flex;
      align-items: center;

      .sold {
        color: #ff6b00;
        background: #fff5ed;
        padding: 2px 6px;
        border-radius: 4px;
      }

      .new-tag {
        background: #e8f4fd;
        color: $primary-color;
        padding: 2px 6px;
        border-radius: 4px;
        font-weight: 500;
      }
    }
  }
}

/* 全局公共状态（加载、空状态） */
.loading, .empty-cart {
  text-align: center;
  padding: 48px 20px; /* 压缩过长的空白高度 */
  font-size: 14px;
  color: $text-muted;
}

/* 底部 Footer 样式美化 */
.site-footer {
  background-color: #1a1a1a;
  color: #aaa;
  margin-top: 48px;
  padding: 40px 24px 24px;
  width: 100%;

  .footer-inner {
    max-width: 1200px;
    margin: 0 auto;
  }

  .footer-links {
    display: flex;
    flex-wrap: wrap;
    justify-content: space-between;
    gap: 24px;
    border-bottom: 1px solid #2e2e2e;
    padding-bottom: 24px;
    margin-bottom: 20px;
  }

  .link-group {
    flex: 1;
    min-width: 140px;

    h4 {
      color: #fff;
      font-size: 14px;
      margin-bottom: 12px;
      font-weight: 600;
    }

    a {
      display: block;
      color: #999;
      text-decoration: none;
      font-size: 12px;
      line-height: 2;
      transition: $transition;

      &:hover {
        color: $primary-color;
      }
    }
  }

  .footer-copyright {
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: 15px;
    font-size: 12px;
    color: #666;

    .payment-icons span {
      margin-left: 12px;
      font-size: 11px;
      background: #2a2a2a;
      padding: 4px 10px;
      border-radius: 4px;
      color: #999;
    }
  }
}


@media (max-width: 1024px) {
  .main-content, .recommend-section {
    margin: 16px;
    padding: 16px;
  }
}

@media (max-width: 992px) {
  /* 猜你喜欢：平滑降为 3 列 */
  .recommend-grid { 
    grid-template-columns: repeat(3, 1fr); 
    gap: 12px;
  }
  
  /* 结算脚部：由横向改为纵向两层流式布局 */
  .cart-footer {
    flex-direction: column;
    gap: 16px;
    align-items: stretch; /* 拉伸占满宽度 */
    
    .footer-left {
      justify-content: space-between;
      width: 100%;
    }
    
    .footer-right { 
      width: 100%; 
      justify-content: space-between; 
      border-top: 1px dashed $border-color;
      padding-top: 14px;
      gap: 12px;
    }
  }
}

@media (max-width: 768px) {
  /* 网页底部大尾栏自适应 */
  .site-footer {
    padding: 30px 16px 16px;
    margin-top: 32px;
    
    .footer-links { 
      flex-direction: column; 
      gap: 16px; 
    }
    .footer-copyright { 
      flex-direction: column; 
      text-align: center; 
      gap: 10px;
      
      .payment-icons {
        display: flex;
        flex-wrap: wrap;
        justify-content: center;
        gap: 6px;
        span { margin-left: 0; }
      }
    }
  }

  /* 🎯 购物车卡片：核心移动端流式改造 */
  .cart-item {
    position: relative;
    flex-wrap: wrap; /* 允许内部元素换行 */
    align-items: flex-start;
    padding: 12px 8px;
    gap: 8px;

    /* 勾选框绝对定位到左上角 */
    .item-select {
      flex: none;
      position: absolute;
      top: 12px;
      left: 8px;
      z-index: 2;
    }

    /* 图片缩紧并腾出左侧勾选框空间 */
    .item-image {
      flex: none;
      width: 75px;
      margin-left: 24px; 
      img {
        width: 70px;
        height: 80px;
      }
    }

    /* 商品标题、规格撑满右侧剩余空间 */
    .item-info {
      flex: 1;
      min-width: 150px;
      padding: 0 4px;
      
      .title {
        font-size: 13px;
        -webkit-line-clamp: 2; /* 依然保持最多两行 */
      }
    }

    /* 隐藏非核心纯代码，精简手机屏幕 */
    .item-specs {
      flex: none;
      width: 100%;
      margin-left: 99px; /* 精确对齐右侧文本起点 */
      margin-top: -12px; /* 紧贴标题下方 */
      
      .code { display: none; } /* 手机端隐藏冗长的 SKU ID */
      .size { font-size: 12px; }
    }

    /* 移动端视角的金额：改成醒目的靠左排列 */
    .item-price {
      flex: none;
      text-align: left;
      margin-left: 99px; 
      margin-top: 4px;

      .label { display: none; } /* 隐藏“单价”字样标签 */
      .value {
        color: $danger-color;
        font-size: 14px;
        &::before { content: '单价: '; font-size: 11px; color: $text-muted; font-weight: normal; }
      }
    }

    /* 数量加减器：右下角极佳的操作区布局 */
    .item-quantity {
      flex: none;
      margin-left: auto; /* 自动推到最右侧 */
      margin-top: 2px;
      
      button {
        width: 26px;
        height: 26px;
        font-size: 14px;
      }
      span {
        width: 30px;
        height: 26px;
        font-size: 12px;
      }
    }

    /* 删除图标：移至右上角绝对定位 */
    .item-actions {
      flex: none;
      position: absolute;
      top: 8px;
      right: 4px;
      
      .icon-btn {
        padding: 4px;
        font-size: 14px;
        &:hover { background: none; } /* 移动端无 hover 态，取消背景色 */
      }
    }
  }
}

@media (max-width: 640px) {
  /* 猜你喜欢：小屏手机降为经典的双列瀑布流比例 */
  .recommend-grid { 
    grid-template-columns: repeat(2, 1fr); 
    gap: 8px;
  }
  
  .product-card {
    .card-info {
      padding: 8px;
      .card-name { font-size: 12px; height: 34px; }
      .card-price { font-size: 14px; }
    }
    /* 移动设备无法便捷 Hover，让加入购物车按钮在手机卡片上始终处于半漂浮触控态 */
    .quick-add-btn {
      opacity: 1 !important;
      transform: translate(-50%, 0) !important;
      width: 85%;
      text-align: center;
      padding: 6px 0;
      bottom: 8px;
      font-size: 11px;
    }
  }
}

@media (max-width: 480px) {
  /* 极端窄屏（如小屏 iPhone）下的结算字号微调 */
  .cart-footer .footer-right .total-price span {
    font-size: 20px;
  }
  .cart-footer .footer-right .checkout-btn {
    padding: 10px 24px;
    font-size: 14px;
  }
}
</style>