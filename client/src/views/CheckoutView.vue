<template>
  <div class="checkout-page">
    <NavBar />

    <main class="main-content">
      <div class="container">
        <div class="breadcrumb-bar">
          <span class="link" @click="handleBack">
            {{ isDirectPayMode ? '我的订单' : '购物车' }}
          </span>
          <span class="separator">></span>
          <span class="current">{{ isDirectPayMode ? '收银台付款' : '确认订单' }}</span>
        </div>

        <div v-if="loading" class="loading-state">
          <el-skeleton :rows="5" animated />
        </div>

        <div v-else>
          <div class="address-card">
            <div class="card-header">
              <h3 class="title-with-line">收货地址</h3>
              <el-button type="primary" link class="text-blue-modern" @click="showAddressDrawer = true">
                管理/切换地址 <el-icon><ArrowRight /></el-icon>
              </el-button>
            </div>
            
            <div v-if="addressList.length === 0" class="empty-address" @click="showAddressDrawer = true">
              <el-icon class="icon-plus"><Plus /></el-icon> 暂无收货地址，请点击添加新地址
            </div>
            
            <div v-else class="address-single-display" @click="showAddressDrawer = true">
              <div v-if="selectedAddress" class="addr-main-box">
                <div class="addr-main-info">
                  <span class="name">{{ selectedAddress.name }}</span>
                  <span class="phone">{{ selectedAddress.phone }}</span>
                  <div v-if="selectedAddress.isDefault === 1 || selectedAddress.isDefault === true" class="default-badge">默认</div>
                </div>
                <div class="addr-detail-info">
                  {{ selectedAddress.province }} {{ selectedAddress.city }} {{ selectedAddress.district }} {{ selectedAddress.detailAddress || selectedAddress.detail }}
                </div>
              </div>
              <div v-else class="empty-address">
                请点击选择一个收货地址
              </div>
              <el-icon class="arrow-right-icon"><ArrowRight /></el-icon>
            </div>
          </div>

          <div class="goods-card">
            <h3>{{ isDirectPayMode ? '订单商品快照' : '商品清单' }}</h3>
            <div class="goods-list">
              <div v-for="item in cartItems" :key="item.skuId || item.id" class="goods-item">
                <img :src="item.mainImage || item.picUrl" class="goods-img" />
                <div class="goods-info">
                  <div class="goods-name">{{ item.productName }}</div>
                  <div class="goods-spec">{{ item.skuName }}</div>
                </div>
                <div class="goods-price">¥{{ (item.price || 0).toFixed(2) }}</div>
                <div class="goods-quantity">x{{ item.quantity }}</div>
                <div class="goods-subtotal">¥{{ ((item.price || 0) * (item.quantity || 0)).toFixed(2) }}</div>
              </div>
            </div>
          </div>

          <div class="payment-card">
            <h3>支付方式</h3>
            <div class="payment-options">
              <el-radio-group v-model="paymentMethod" class="modern-radio-group">
                <el-radio-button label="1">微信支付</el-radio-button>
                <el-radio-button label="2">支付宝</el-radio-button>
                <el-radio-button label="3">银联支付</el-radio-button>
              </el-radio-group>
            </div>
          </div>

          <div class="summary-card">
            <div class="summary-row" v-if="!isDirectPayMode">
              <span>商品总金额</span>
              <span class="price-num">¥{{ totalGoodsAmount.toFixed(2) }}</span>
            </div>
            <div class="summary-row" v-if="!isDirectPayMode">
              <span>运费</span>
              <span class="price-num">¥{{ freight.toFixed(2) }}</span>
            </div>
            <div class="summary-row" v-if="!isDirectPayMode && discount > 0">
              <span>优惠减免</span>
              <span class="price-num discount">-¥{{ discount.toFixed(2) }}</span>
            </div>
            <div class="summary-row total">
              <span>实付款</span>
              <span class="final-price">¥{{ finalAmount.toFixed(2) }}</span>
            </div>
          </div>

          <div class="action-bar">
            <el-button type="primary" size="large" class="btn-submit-modern" @click="submitOrder" :loading="submitting">
              {{ isDirectPayMode ? '立即支付' : '提交订单' }}
            </el-button>
          </div>
        </div>
      </div>
    </main>

    <AddressDrawer 
      v-model:show="showAddressDrawer" 
      v-model="selectedAddressId"
      @refresh="fetchAddressList"
    />
  </div>
  <ModernDialog
  v-model="payDialogVisible"
  title="支付确认"
  confirm-text="立即支付"
  cancel-text="暂不支付"
  :loading="submitting"
  @confirm="handleRealPay"
  @cancel="handleCancelAndCreate"
>
  <p>您确定要发起支付吗？若选择暂不支付，订单依然会被创建，您可以后续付款。</p>
</ModernDialog>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowRight, Plus } from '@element-plus/icons-vue'
import NavBar from '@/components/NavBar.vue'
import AddressDrawer from '@/components/AddressDrawer.vue'
import { cartApi } from '@/api/cart'
import { orderApi } from '@/api/order'
import { addressApi } from '@/api/address'
import ModernDialog from '@/components/ModernDialog.vue' // 💡 自定义万能弹窗组件

const router = useRouter()
const route = useRoute()

// ==========================================
// 1. 核心状态响应式变量
// ==========================================
const orderId = ref(null)             
const isDirectPayMode = ref(false)     

const selectedCartIds = ref([])        
const cartItems = ref([])              
const loading = ref(true)              
const submitting = ref(false)          
const payDialogVisible = ref(false)   // 🎯 新增：控制现代化自定义弹窗的哨兵开关

// ==========================================
// 2. 地址模块状态
// ==========================================
const addressList = ref([])            
const selectedAddressId = ref(null)    
const showAddressDrawer = ref(false)

const selectedAddress = computed(() => {
  const list = addressList.value?.data || addressList.value || []
  return list.find(a => a.id === selectedAddressId.value) || null
})

// ==========================================
// 3. 费用与支付配置
// ==========================================
const paymentMethod = ref('1')         
const freight = ref(0)                 
const discount = ref(0)                
const directPayAmount = ref(0)         

// ==========================================
// 4. 计算属性 (Computed)
// ==========================================
const totalGoodsAmount = computed(() => {
  return cartItems.value.reduce((sum, item) => sum + (item.price || 0) * (item.quantity || 0), 0)
})

const finalAmount = computed(() => {
  if (isDirectPayMode.value) {
    return directPayAmount.value
  }
  return totalGoodsAmount.value + freight.value - discount.value
})

// ==========================================
// 5. 核心路由回退跳转控制
// ==========================================
const handleBack = () => {
  if (isDirectPayMode.value) {
    router.push('/order')
  } else {
    router.push('/cart')
  }
}

// ==========================================
// 6. 核心数据请求流
// ==========================================
const fetchExistingOrderDetails = async (id) => {
  try {
    const detail = await orderApi.getOrderDetail(id)
    const data = detail.data || detail
    directPayAmount.value = data.payAmount ?? data.totalAmount ?? 0
    cartItems.value = (data.items || []).map(item => ({
      id: item.id,
      mainImage: item.mainImage || 'https://sportshop-pictures.oss-cn-beijing.aliyuncs.com/upload/default.png',
      productName: item.productName,
      skuName: item.skuName,
      price: item.price,
      quantity: item.quantity
    }))
  } catch (error) {
    console.error('获取订单详情失败:', error)
    ElMessage.error('订单详情获取失败')
    router.push('/order')
  }
}

const fetchSelectedCartItems = async () => {
  try {
    const res = await cartApi.getCartList()
    const allItems = res.list || res.data || []
    const selected = allItems.filter(item => selectedCartIds.value.includes(item.skuId))
    cartItems.value = selected
    
    if (cartItems.value.length === 0) {
      ElMessage.warning('请选择要结算的商品')
      router.push('/cart')
    }
  } catch (error) {
    console.error('获取商品信息失败:', error)
    router.push('/cart')
  }
}

const fetchAddressList = async () => {
  try {
    const res = await addressApi.getAddressList()
    const rawList = res.data || res || []
    addressList.value = rawList
    
    const defaultAddr = rawList.find(a => a.isDefault === 1 || a.isDefault === true)
    selectedAddressId.value = defaultAddr ? defaultAddr.id : (rawList[0]?.id || null)
  } catch (error) {
    console.error('获取地址列表失败:', error)
  }
}

// ==========================================
// 7. 创单支付复合控制流 (🎯 已改造为拦截分流模式)
// ==========================================

/**
 * A. 用户点击底部的“提交订单/支付”大按钮：触发拦截校验
 */
const submitOrder = async () => {
  if (!selectedAddressId.value) {
    ElMessage.warning('请选择收货地址')
    return
  }
  if (cartItems.value.length === 0) {
    ElMessage.warning('结算商品不能为空')
    return
  }
  
  // 💡 满足条件，不直接发起创单，而是先呼出你的现代化精美弹窗
  payDialogVisible.value = true
}

/**
 * B. 🟢 弹窗中点击【立即支付】按钮
 */
const handleRealPay = async () => {
  submitting.value = true
  payDialogVisible.value = false // 立刻收起弹窗
  const payMethodInt = parseInt(paymentMethod.value, 10) || 1

  try {
    if (isDirectPayMode.value) {
      // 模式 1: 如果是已有订单直接支付
      await orderApi.payOrder({
        orderId: orderId.value,
        paymentMethod: payMethodInt
      })
      ElMessage.success('支付成功！')
      router.push('/order')
    } else {
      // 模式 2: 从购物车进来的复合流：先创单，再付款
      const orderData = {
        addressId: selectedAddressId.value,
        cartIds: selectedCartIds.value,
        paymentMethod: payMethodInt
      }

      const res = await orderApi.createOrder(orderData)
      const targetData = res.data || res

      if (targetData && (targetData.orderId || targetData.id)) {
        // 创建成功，顺滑拉起支付
        await orderApi.payOrder({
          orderId: targetData.orderId || targetData.id, 
          paymentMethod: payMethodInt
        })
        ElMessage.success('支付成功！')
        router.push('/order')
      } else {
        ElMessage.error(res.msg || '订单创建失败')
      }
    }
  } catch (error) {
    console.error('支付处理异常:', error)
    ElMessage.error('支付失败，请稍后前往订单中心重试')
  } finally {
    submitting.value = false
  }
}

/**
 * C. 🟡 弹窗中点击【取消 / 暂不支付】按钮
 */
const handleCancelAndCreate = async () => {
  payDialogVisible.value = false // 收起弹窗

  // 边界保护：如果是已有订单去支付的模式，直接退回列表页，不需要重复生成新订单
  if (isDirectPayMode.value) {
    ElMessage.info('已取消支付')
    router.push('/order')
    return
  }

  submitting.value = true
  const payMethodInt = parseInt(paymentMethod.value, 10) || 1
  
  try {
    const orderData = {
      addressId: selectedAddressId.value,
      cartIds: selectedCartIds.value,
      paymentMethod: payMethodInt
    }

    // 🎯 核心需求：只调用创建订单接口，不进行后续的 payOrder 支付
    const res = await orderApi.createOrder(orderData)
    const targetData = res.data || res

    if (targetData && (targetData.orderId || targetData.id)) {
      ElMessage.success('订单已成功创建，请尽快前往订单中心付款')
      router.push('/order') // 🏃 成功创建订单后跳转至订单页
    } else {
      ElMessage.error(res.msg || '订单创建失败')
    }
  } catch (error) {
    console.error('取消支付时创建订单失败:', error)
    ElMessage.error('创建订单失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

// ==========================================
// 8. 生命周期加载
// ==========================================
onMounted(async () => {
  await fetchAddressList()

  if (route.query.orderId) {
    orderId.value = Number(route.query.orderId)
    isDirectPayMode.value = true
    await fetchExistingOrderDetails(orderId.value)
    loading.value = false
    return 
  }

  const ids = route.query.cartIds
  if (!ids) {
    ElMessage.warning('未选择结算商品')
    router.push('/cart')
    return
  }

  selectedCartIds.value = ids.split(',').map(Number)
  await fetchSelectedCartItems()
  loading.value = false
})
</script>

<style scoped lang="scss">
.checkout-page {
  background-color: #f7f9fa;
  min-height: 100vh;
  padding-bottom: 60px;
}
.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}
.breadcrumb-bar {
  padding: 20px 0;
  font-size: 14px;
  color: #606266;
  .link {
    cursor: pointer;
    transition: color 0.2s;
    &:hover { color: #007fc3; font-weight: 500; }
  }
  .separator { margin: 0 8px; color: #c0c4cc; }
  .current { color: #303133; font-weight: 600; }
}

/* 🎯 统一传承你发我的图里面的高级精美蓝白地址卡片样式 */
.address-card, .goods-card, .payment-card, .summary-card {
  background: #ffffff;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 20px;
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.02);
  h3 { font-size: 16px; color: #1f2f3a; margin-top: 0; margin-bottom: 20px; font-weight: 600; }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  h3 { margin: 0; }
}

.text-blue-modern {
  color: #007fc3;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 亮点：精美的单选高亮框 */
.address-single-display {
  border: 1px solid #007fc3;
  background-color: #f0f9ff;
  border-radius: 12px;
  padding: 18px 24px;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: relative;
  transition: all 0.2s;
  
  &:hover {
    background-color: #e6f5ff;
  }

  .addr-main-info {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 8px;
    .name { font-size: 16px; font-weight: 600; color: #1f2f3a; }
    .phone { font-size: 14px; color: #5f6b7a; }
  }
  .addr-detail-info {
    font-size: 14px;
    color: #6c7a8e;
  }
  .default-badge {
    font-size: 11px;
    color: #df4343;
    background: #fef0f0;
    border: 1px solid #fde2e2;
    padding: 1px 6px;
    border-radius: 4px;
  }
  .arrow-right-icon {
    color: #007fc3;
    font-size: 18px;
  }
}

.empty-address {
  border: 1px dashed #dcdfe6;
  border-radius: 12px;
  padding: 32px;
  text-align: center;
  color: #909399;
  cursor: pointer;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  &:hover {
    border-color: #007fc3;
    color: #007fc3;
  }
}

/* 商品清单表格化样式 */
.goods-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.goods-item {
  display: flex;
  align-items: center;
  padding-bottom: 16px;
  border-bottom: 1px solid #f2f6fc;
  &:last-child { border-bottom: none; padding-bottom: 0; }
  .goods-img { width: 80px; height: 80px; border-radius: 8px; object-fit: cover; margin-right: 16px; border: 1px solid #eee; }
  .goods-info { flex: 1; .goods-name { font-size: 14px; color: #303133; font-weight: 500; } .goods-spec { font-size: 12px; color: #909399; margin-top: 4px; } }
  .goods-price, .goods-quantity, .goods-subtotal { width: 100px; text-align: right; font-size: 14px; color: #606266; }
  .goods-subtotal { font-weight: 600; color: #303133; }
}

/* 现代化蓝白支付方式 Radio */
.modern-radio-group {
  :deep(.el-radio-button__inner) {
    padding: 12px 28px;
    font-size: 14px;
  }
  :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
    background-color: #007fc3;
    border-color: #007fc3;
    box-shadow: -1px 0 0 0 #007fc3;
  }
}

/* 🎯 已经为你改造为整体“靠右定位”，但内部“文字左对齐”的优雅结算卡片样式 */
.summary-card {
  display: flex;
  flex-direction: column;
  align-items: flex-start; /* 💡 保持内部每一行的子元素从左开始排 */
  margin-left: auto;       /* 🎯 核心修改：利用自动外边距，把整个卡片整体推到页面的最右边 */
  gap: 12px;
  
  .summary-row {
    display: flex;
    width: 280px;            /* 固定宽度，确保上下长短一致 */
    justify-content: flex-start; /* 内部内容靠左对齐 */
    gap: 16px;               /* 标签和金额之间的间距 */
    font-size: 14px;
    color: #606266;

    /* 让左边的文本（如“商品总金额”）固定宽度，这样后面的冒号和金额就能对得整整齐齐 */
    span:first-child {
      min-width: 85px; 
      color: #5f6b7a;
    }

    .price-num { 
      color: #303133; 
      font-weight: 600; 
      &.discount { color: #f56c6c; } 
    }

    &.total {
      width: 100%; 
      margin-top: 8px;
      padding-top: 16px;
      border-top: 1px solid #ebeef5; /* 分割线 */
      font-size: 16px;
      color: #303133;
      font-weight: bold;
      align-items: center;

      span:first-child {
        color: #303133;
      }
      .final-price { 
        color: #df4343; 
        font-size: 22px; 
        font-weight: 700;
      }
    }
  }
}

.action-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 24px;
  .btn-submit-modern {
    background-color: #007fc3;
    border-color: #007fc3;
    padding: 24px 48px;
    font-size: 16px;
    font-weight: 600;
    border-radius: 8px;
    &:hover { background-color: #00669e; border-color: #00669e; }
  }
}
</style>