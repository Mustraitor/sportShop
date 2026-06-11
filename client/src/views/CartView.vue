<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import NavBar from '@/components/NavBar.vue'
// 1. 引入全局封装好的 API 与 Store 模块
import { cartApi } from '@/api/cart'
import { orderApi } from '@/api/order' 
import { useUserStore } from '@/stores/user'
import { addressApi } from '@/api/address'

const router = useRouter()
const userStore = useUserStore()

// ---------- 常量定义 ----------
const OSS_BASE_URL = 'https://sportshop-pictures.oss-cn-beijing.aliyuncs.com/'

// ---------- 辅助函数 ----------
const formatImageUrl = (rawPath) => {
  if (!rawPath) return ''
  if (rawPath.startsWith('http')) return rawPath
  
  let pathOnly = rawPath
  let cleaned = pathOnly.replace(/\/upload\/\d+\//, '/upload/')
  if (cleaned === pathOnly) {
    cleaned = cleaned.replace(/^\d+\//, '')
  }
  if (!cleaned.startsWith('/upload/')) {
    cleaned = '/upload/' + cleaned.replace(/^\/+/, '')
  }
  const base = OSS_BASE_URL.replace(/\/$/, '')
  return base + cleaned
}

// ---------- 响应式数据 ----------
const loading = ref(false)
const cartList = ref([])
const totalCount = ref(0)
const checkedTotalAmount = ref(0)
const activeAddressId = ref(1) // 💡 临时模拟一个地址ID，供创建订单接口做必填项验证

const isAllChecked = computed({
  get() {
    if (cartList.value.length === 0) return false
    return cartList.value.every(item => item.isChecked)
  },
  set(val) {
    toggleSelectAll(val)
  }
})

// 数据格式化
const formatCartItem = (item) => ({
  ...item,
  isChecked: item.checked === 1,
  mainImage: formatImageUrl(item.mainImage)
})

// ---------- 获取购物车数据 ----------
const fetchCart = async () => {
  loading.value = true
  try {
    userStore.initGuestId()

    const res = await cartApi.getCartList()
  
    // console.log('--- 购物车接口返回原始数据 ---', res)
    const dataObj = res.data ? res.data : res;
    
    cartList.value = (dataObj.list || []).map(formatCartItem)
    totalCount.value = dataObj.total || 0
    checkedTotalAmount.value = dataObj.checkedTotalAmount || 0
    
    userStore.updateCartCount(totalCount.value)
  } catch (err) {
    console.error('获取购物车失败', err)
  } finally {
    loading.value = false
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
    loading.value = true 
    await cartApi.updateCartQuantity(item.skuId, action)
    await fetchCart() 
  } catch (err) {
    console.error('修改数量失败', err)
  } finally {
    loading.value = false
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

const addressList = ref([])
const paymentMethod = ref(1) // 默认微信支付

const checkout = async () => {
  // 1. 过滤购物车中被勾选的商品
  const selected = cartList.value.filter(item => item.isChecked)
  if (selected.length === 0) {
    ElMessage.warning('请先选择要结算的商品')
    return
  }

  // 2. 登录拦截检查
  if (!userStore.token) {
    userStore.isLoginVisible = true
    return
  }

  // 3. 把选中的所有 skuId 用逗号拼接成字符串，例如 "101,102"
  const cartIdsStr = selected.map(item => item.skuId).join(',')

  // 4. 带着商品参数，理直气壮地跳转到你的 Checkout 结算页
  router.push({
    path: '/checkout',
    query: { cartIds: cartIdsStr }
  })
}

// ---------- 跳转商品详情 ----------
const goToProductDetail = (productId) => {
  router.push(`/product/${productId}`)
}

// ---------- 生命周期初始化 ----------
onMounted(async () => {
  userStore.initGuestId()
  await fetchCart()
})
</script>

<template>
  <!-- 模板内容与原版完全一致，无需任何改动 -->
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
            <img :src="item.mainImage || 'https://via.placeholder.com/100x120?text=No+Image'" alt="product" />
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
          <button class="clear-btn" :disabled="loading" @click="clearCart">清空购物车</button>
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
    <!-- 随机商品展示 -->
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
              :src="product.mainImage || 'https://via.placeholder.com/260x320?text=Product'"
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

      <!-- <div v-if="!randomLoading && randomProducts.length === 0" class="empty-recommend">
        暂无商品
      </div> -->
    </section>

    <footer class="site-footer">
      <div class="footer-inner">
        <div class="footer-links">
          <div class="link-group">
            <h4>购物指南</h4>
            <a href="#">购物流程</a>
            <a href="#">会员权益</a>
            <a href="#">积分规则</a>
          </div>
          <div class="link-group">
            <h4>支付与配送</h4>
            <a href="#">支付方式</a>
            <a href="#">配送说明</a>
            <a href="#">运费政策</a>
          </div>
          <div class="link-group">
            <h4>售后服务</h4>
            <a href="#">退换货政策</a>
            <a href="#">保修服务</a>
            <a href="#">联系客服</a>
          </div>
          <div class="link-group">
            <h4>关于我们</h4>
            <a href="#">品牌故事</a>
            <a href="#">门店查询</a>
            <a href="#">加入我们</a>
          </div>
          <div class="link-group">
            <h4>隐私与法律</h4>
            <a href="#">隐私政策</a>
            <a href="#">服务条款</a>
            <a href="#">Cookie声明</a>
          </div>
        </div>
        <div class="footer-copyright">
          <p>© 2025 购物车示例 | 保留所有权利 | 本网站仅为演示项目</p>
          <div class="payment-icons">
            <span>微信支付</span>
            <span>支付宝</span>
            <span>银联</span>
            <span>Visa</span>
          </div>
        </div>
      </div>
    </footer>
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