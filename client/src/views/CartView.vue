<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import NavBar from '@/components/NavBar.vue'
import { cartApi } from '@/api/cart'
import { useUserStore } from '@/stores/user'
import { optimizeImage } from '@/utils/image'
import ModernDialog from '@/components/ModernDialog.vue' 

const router = useRouter()
const userStore = useUserStore()

// ---------- 响应式数据 ----------
const loading = ref(false)
const cartList = ref([])
const totalCount = ref(0)
const checkedTotalAmount = ref(0)

// 猜你喜欢板块对应的响应式数据（补充声明以防模板报错）
const randomLoading = ref(false)
const randomProducts = ref([])

const isAllChecked = computed({
  get() {
    if (cartList.value.length === 0) return false
    return cartList.value.every(item => item.isChecked)
  },
  set(val) {
    toggleSelectAll(val)
  }
})

// 🎯 核心修改：弃用原来的老 formatImageUrl，直接调用你的智能路由工具
const formatCartItem = (item) => ({
  ...item,
  isChecked: item.checked === 1,
  // 传入单品单价和图片，购物车列表一般可以用较小宽度（比如 200）来节省带宽
  mainImage: optimizeImage(item.mainImage, 200)
})

// ---------- 获取购物车数据 ----------
const fetchCart = async () => {
  // loading.value = true
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
  } finally {
    // loading.value = false
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
  // 🎯 【核心修改】：删掉了 finally 块里的 loading.value = false
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
  if (!confirm('确定要删除该商品吗？')) return
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
  loading.value = true
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
  } finally {
    loading.value = false
  }
}

// ---------- 清空购物车 ----------
const clearCart = async () => {
  if (!confirm('清空购物车后将无法恢复，确定清空吗？')) return
  try {
    await cartApi.clearCart()
    await fetchCart()
    alert('购物车已清空')
  } catch (err) {
    console.error('清空失败', err)
  }
}

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

// 模拟猜你喜欢里的加入购物车逻辑
const addToCartFromRandom = async (product, e) => {
  e.stopPropagation() // 阻止触发卡片跳转详情
  try {
    // 假设随机推荐商品默认添加其第一个默认 skuId
    const targetSkuId = product.skuId || product.id 
    await cartApi.addToCart({
      productId: product.id,
      skuId: targetSkuId,
      quantity: 1
    })
    ElMessage.success('成功加入购物车！')
    await fetchCart()
  } catch (err) {
    console.error('加入购物车失败', err)
  }
}

// 控制清空操作的弹窗状态
const showClearDialog = ref(false)
const isClearLoading = ref(false)

// 按钮点击：开启弹窗
const openClearConfirm = () => {
  showClearDialog.value = true
}

// 弹窗确认：执行 API
const handleClearConfirm = async () => {
  isClearLoading.value = true
  try {
    await cartApi.clearCart()
    await fetchCart(true) // 静默刷新数据
    ElMessage.success('购物车已清空')
    showClearDialog.value = false // 关闭弹窗
  } catch (err) {
    console.error('清空失败', err)
  } finally {
    isClearLoading.value = false
  }
}
// ---------- 生命周期初始化 ----------
onMounted(async () => {
  userStore.initGuestId()
  await fetchCart()
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

      <div v-else class="recommend-grid">
        <div class="product-card" v-for="product in randomProducts" :key="product.id" @click="goToProductDetail(product.id)">
          <div class="card-img">
            <img
              :src="optimizeImage(product.mainImage, 400)"
              :alt="product.name || product.title"
            />
            <button class="quick-add-btn" @click="(e) => addToCartFromRandom(product, e)">
              加入购物车
            </button>
          </div>
          <div class="card-info">
            <p class="card-name">{{ product.name || product.title }}</p>
            <p class="card-price">¥{{ (product.price || 0).toFixed(2) }}</p>
            <div class="card-meta">
              <span v-if="product.soldCount || product.sold_count" class="sold">
                {{ product.soldCount || product.sold_count }}+人已购买
              </span>
              <span v-else class="new-tag">新品</span>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
/* 原有样式完全保留，未做任何修改 */
.cart-container {
  max-width: 1600px;
  margin: 0 auto;
  background: #fff;
  min-height: 100vh;
}

.user-actions {
  margin-left: auto;
}
.user-actions span {
  margin-right: 20px;
  font-size: 14px;
  cursor: pointer;
}

.cart-icon {
  position: relative;
}

.badge {
  background: #e00000;
  color: white;
  border-radius: 50%;
  padding: 2px 6px;
  font-size: 12px;
  position: absolute;
  top: -8px;
  right: -10px;
}

.main-content {
  max-width: 1200px;
  margin: 0 auto;
  padding-top: 20px;
}

.main-content h2 {
  margin-bottom: 20px;
  font-weight: normal;
}

.cart-list {
  border-top: 1px solid #eee;
}

.cart-item {
  display: flex;
  align-items: center;
  padding: 20px 0;
  border-bottom: 1px solid #eee;
  background-color: #f9f9f9;
}

.item-select {
  flex: 0.5;
  display: flex;
  justify-content: center;
}

.item-image {
  flex: 1;
}

.item-image img {
  width: 80px;
  height: 100px;
  object-fit: contain;
  background: #fff;
  border: 1px solid #eee;
}

.item-info {
  flex: 2;
  padding-left: 20px;
}

.item-info .brand {
  font-weight: bold;
  margin-bottom: 10px;
  color: #666;
}

.item-info .title {
  font-size: 16px;
  line-height: 1.5;
}

.item-specs {
  flex: 1.5;
  font-size: 14px;
  color: #666;
  line-height: 1.8;
}

.item-specs .edit-link {
  color: #333;
  text-decoration: underline;
  cursor: pointer;
  margin-top: 5px;
  display: inline-block;
}

.item-price {
  flex: 1;
  text-align: center;
}

.item-price .label {
  font-size: 12px;
  color: #999;
  margin-bottom: 5px;
}

.item-price .value {
  font-weight: bold;
  font-size: 16px;
}

.item-quantity {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.item-quantity button {
  width: 30px;
  height: 30px;
  border: 1px solid #ddd;
  background: #fff;
  cursor: pointer;
  font-size: 18px;
  line-height: 1;
}

.item-quantity button:disabled {
  color: #ccc;
  cursor: not-allowed;
}

.item-quantity span {
  width: 40px;
  text-align: center;
  font-size: 14px;
}

.item-actions {
  flex: 1;
  display: flex;
  justify-content: center;
  gap: 10px;
}

.icon-btn {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 18px;
}

.cart-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 20px;
  padding: 15px 0;
  border-top: 2px solid #333;
}

.footer-left {
  display: flex;
  align-items: center;
}

.select-all {
  display: flex;
  align-items: center;
  cursor: pointer;
  margin-right: 20px;
}

.select-all input {
  margin-right: 5px;
}

.total-count {
  font-size: 14px;
  color: #666;
}

.total-count strong {
  color: #e00000;
  margin-left: 5px;
}

.footer-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.total-price {
  font-size: 18px;
  margin-bottom: 5px;
}

.total-price span {
  color: #e00000;
  font-weight: bold;
  font-size: 24px;
  margin-left: 10px;
}

.tax-tip {
  font-size: 12px;
  color: #999;
  margin-bottom: 10px;
}

.checkout-btn {
  background-color: #0075bf;
  color: white;
  border: none;
  padding: 10px 40px;
  font-size: 16px;
  cursor: pointer;
  border-radius: 2px;
}

.checkout-btn:hover {
  background-color: #005a9e;
}

/* 随机商品展示 */
.recommend-section {
  max-width: 1200px;
  margin: 50px auto 0;
  padding: 0 20px;
}

.recommend-header {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 24px;
}

.recommend-header h3 {
  font-size: 24px;
  font-weight: 600;
  color: #111;
}

.recommend-subtitle {
  font-size: 14px;
  color: #999;
}

.recommend-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.product-card {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  transition: box-shadow 0.2s, transform 0.2s;
  border: 1px solid #f0f0f0;
  cursor: pointer;
}

.product-card:hover {
  box-shadow: 0 6px 18px rgba(0,0,0,0.08);
  transform: translateY(-2px);
}

.card-img {
  position: relative;
  overflow: hidden;
  background: #fafafa;
  aspect-ratio: 0.8;
}

.card-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.product-card:hover .card-img img {
  transform: scale(1.04);
}

.quick-add-btn {
  position: absolute;
  bottom: 10px;
  left: 50%;
  transform: translateX(-50%);
  background: #0075bf;
  color: #fff;
  border: none;
  padding: 8px 20px;
  border-radius: 20px;
  font-size: 13px;
  opacity: 0;
  transition: opacity 0.2s;
  cursor: pointer;
  white-space: nowrap;
}

.product-card:hover .quick-add-btn {
  opacity: 1;
}

.quick-add-btn:hover {
  background: #005a9e;
}

.card-info {
  padding: 12px 16px 16px;
}

.card-name {
  font-size: 14px;
  color: #333;
  margin-bottom: 8px;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-price {
  font-size: 18px;
  font-weight: 700;
  color: #e00000;
  margin-bottom: 6px;
}

.card-meta {
  font-size: 12px;
  color: #999;
  display: flex;
  align-items: center;
}

.sold {
  color: #ff6b00;
}

.new-tag {
  background: #e8f4fd;
  color: #0075bf;
  padding: 1px 8px;
  border-radius: 10px;
}

.empty-recommend {
  text-align: center;
  padding: 40px;
  color: #aaa;
  font-size: 14px;
}

.site-footer {
  background-color: #1a1a1a;
  color: #ccc;
  margin-top: 60px;
  padding: 40px 30px 20px;
  width: 100%;
}

.footer-inner {
  max-width: 1200px;
  margin: 0 auto;
}

.footer-links {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 30px;
  border-bottom: 1px solid #333;
  padding-bottom: 30px;
  margin-bottom: 20px;
}

.link-group {
  flex: 1;
  min-width: 120px;
}

.link-group h4 {
  color: #fff;
  font-size: 16px;
  margin-bottom: 15px;
  font-weight: 500;
}

.link-group a {
  display: block;
  color: #aaa;
  text-decoration: none;
  font-size: 13px;
  line-height: 1.8;
  transition: color 0.2s;
}

.link-group a:hover {
  color: #0075bf;
  text-decoration: underline;
}

.footer-copyright {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 15px;
  font-size: 12px;
  color: #777;
}

.payment-icons span {
  margin-left: 15px;
  font-size: 12px;
  background: #333;
  padding: 4px 8px;
  border-radius: 4px;
  color: #ddd;
}

@media (max-width: 768px) {
  .footer-links {
    flex-direction: column;
    gap: 20px;
  }
  .footer-copyright {
    flex-direction: column;
    text-align: center;
  }
  .payment-icons span {
    margin: 0 5px;
  }
}

.loading, .empty-cart {
  text-align: center;
  padding: 60px 20px;
  font-size: 16px;
  color: #666;
}

.clear-btn {
  margin-left: 20px;
  background: none;
  border: 1px solid #ddd;
  padding: 4px 12px;
  cursor: pointer;
  border-radius: 2px;
  color: #666;
}

.clear-btn:hover {
  background: #f5f5f5;
}

@media (max-width: 992px) {
  .recommend-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 640px) {
  .recommend-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>