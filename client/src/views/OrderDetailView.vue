<template>
  <div class="order-detail-page">
    <NavBar />

    <main class="main-content">
      <div class="container">
        <div class="breadcrumb-bar">
          <div class="breadcrumb">
            <span class="link" @click="goToOrders">我的订单</span>
            <span class="separator">></span>
            <span class="current">订单详情</span>
          </div>
        </div>

        <div v-if="loading" class="loading-state">加载订单详情...</div>

        <div v-else-if="order.orderNo">
          <div class="status-card">
            <div class="status-header">
              <div class="status-title">
                <span class="status-text">{{ order.statusText }}</span>
                <span class="order-no">订单号：{{ order.orderNo }}</span>
              </div>
              <div class="status-actions">
                <button class="btn-text" @click="handlePrintOrder">打印订单</button>
                <button class="btn-text" @click="handleViewInvoice">查看发票详情</button>
              </div>
            </div>
            <div class="status-message" v-if="order.status === 3 || order.status === 'completed'">
              订单已完成，感谢您在型动派购物，欢迎您对本次交易及所购商品进行评价。
            </div>
            <div class="status-message" v-else-if="order.status === 2 || order.status === 'cancelled'">
              订单已取消，如有疑问请联系客服。
            </div>
            <div class="status-message" v-else-if="order.status === 0 || order.status === 'pending'">
              订单待付款，请及时完成支付。
            </div>
            <div class="status-message" v-else-if="order.status === 1 || order.status === 'receiving'">
              订单已付款，正在为您极速配送。
            </div>
            
            <div class="status-steps">
              <div v-for="(step, idx) in order.statusSteps" :key="idx" class="step" :class="{ completed: step.completed, active: idx === getCurrentStepIndex() }">
                <div class="step-icon">{{ step.completed ? '✓' : idx === getCurrentStepIndex() ? '●' : '○' }}</div>
                <div class="step-label">{{ step.label }}</div>
                <div class="step-time" v-if="step.time">{{ step.time }}</div>
              </div>
            </div>
          </div>

          <div class="logistics-card" v-if="order.logistics && order.logistics.timeline && order.logistics.timeline.length">
            <div class="logistics-header">
              <div class="carrier-info">
                <span class="carrier-name">{{ order.logistics.carrier }}</span>
                <span class="carrier-phone">承运人电话：{{ order.logistics.carrierPhone }}</span>
              </div>
              <div class="tracking-info">
                货运单号：{{ order.logistics.trackingNo }}
                <a href="javascript:;" @click="copyTracking">复制</a>
              </div>
            </div>
            <div class="timeline">
              <div v-for="(event, idx) in order.logistics.timeline" :key="idx" class="timeline-item">
                <div class="timeline-dot"></div>
                <div class="timeline-content">
                  <div class="timeline-title">{{ event.status }}</div>
                  <div class="timeline-desc">{{ event.desc }}</div>
                  <div class="timeline-time">{{ event.time }}</div>
                </div>
              </div>
            </div>
            <div class="delivery-info" v-if="order.logistics.deliveryMan">
              送货方式：{{ order.delivery.method }}<br>
              您的订单已{{ order.logistics.status }}，如有疑问请联系配送员
              【{{ order.logistics.deliveryMan }}，{{ order.logistics.deliveryPhone }}】确认。
            </div>
          </div>

          <div class="info-grid">
            <div class="info-card">
              <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px;">
                <h4 style="margin: 0;">收货人信息</h4>
                <el-button 
                  v-if="order.status === 0 || order.status === 'pending'" 
                  type="primary" 
                  link 
                  @click="openAddressSelector"
                >
                  修改地址 <el-icon><ArrowRight /></el-icon>
                </el-button>
              </div>
              <p>收货人：{{ order.receiver?.name || '--' }}</p>
              <p>手机号码：{{ order.receiver?.phone || '--' }}</p>
              <p>地址：{{ order.receiver?.address || '--' }}</p>
            </div>
            <AddressDrawer 
              v-model:show="showAddressDrawer" 
              v-model="selectedAddressId"
              @refresh="fetchAddressList"
            />
            <div class="info-card">
              <h4>配送信息</h4>
              <p>配送方式：{{ order.delivery?.method }}</p>
              <p>期望送货日期：{{ order.delivery?.expectedDate || '--' }}</p>
              <p>期望配送时间：{{ order.delivery?.expectedTime || '--' }}</p>
            </div>
            <div class="info-card">
              <h4>付款信息</h4>
              <p>付款方式：{{ order.payment?.method }}</p>
              <p>付款时间：{{ order.payment?.time || '--' }}</p>
            </div>
          </div>
          <div class="goods-card">
            <div class="shop-info">
              <span class="shop-name">{{ order.shopName }}</span>
            </div>
            <div class="goods-table">
              <div class="goods-header">
                <div class="col-goods">商品</div>
                <div class="col-sku">商品编号</div>
                <div class="col-price">单价</div>
                <div class="col-quantity">数量</div>
                <div class="col-actions">操作</div>
              </div>
              <div class="goods-row" v-for="item in order.goods" :key="item.id">
                <div class="col-goods">
                  <img :src="getGoodsImage(item.img)" class="goods-img" />
                  <div class="goods-info">
                    <div class="goods-name">{{ item.name }}</div>
                    <div class="goods-spec">{{ item.spec }}</div>
                  </div>
                </div>
                <div class="col-sku">{{ item.skuCode || '--' }}</div>
                <div class="col-price">¥{{ (item.price || 0).toFixed(2) }}</div>
                <div class="col-quantity">{{ item.quantity }}</div>
                <div class="col-beans">--</div>
                <div class="col-actions">
                  <a href="javascript:;" @click="handleAfterSale(item)">申请售后</a>
                  <a href="javascript:;" @click="handleReview(item)">查看评价晒单</a>
                </div>
              </div>
            </div>
            <div class="goods-footer">
              <div class="promise-tags">
                <span>支持30天无理由退货</span>
                <span>买贵双倍赔</span>
              </div>
              <div class="price-detail">
                <div>商品总价：¥{{ (order.priceSummary?.goodsTotal || 0).toFixed(2) }}</div>
                <div>运费：+¥{{ (order.priceSummary?.freight || 0).toFixed(2) }}</div>
                <div>促销立减：-¥{{ (order.priceSummary?.promotion || 0).toFixed(2) }}</div>
                <div>补贴优惠：-¥{{ (order.priceSummary?.discount || 0).toFixed(2) }}</div>
                <div class="total-pay">实付款：¥{{ (order.totalPrice || 0).toFixed(2) }}</div>
              </div>
            </div>
          </div>

        </div>

        <div v-else class="empty-state">
          <div class="empty-icon">📭</div>
          <p>订单不存在</p>
          <button class="btn btn-primary" @click="goToOrders">返回订单列表</button>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowRight } from '@element-plus/icons-vue'
import NavBar from '@/components/NavBar.vue'
import AddressDrawer from '@/components/AddressDrawer.vue'
import { orderApi } from '@/api/order'
import { addressApi } from '@/api/address'
import { generateDisplayOrderNo, getStatusText } from '@/utils/format'
import { optimizeImage } from '@/utils/image'

const route = useRoute()
const router = useRouter()
const OSS_BASE_URL = 'https://sportshop-pictures.oss-cn-beijing.aliyuncs.com/'

// 订单响应式数据结构兜底
const order = ref({
  orderNo: '',
  statusText: '',
  status: '',
  totalPrice: 0,
  goods: [],
  logistics: null,
  receiver: { name: '', phone: '', address: '' },
  delivery: { method: '', expectedDate: '', expectedTime: '' },
  payment: { method: '', time: '' },
  priceSummary: { goodsTotal: 0, freight: 0, promotion: 0, discount: 0 },
  shopName: '',
  jdBeans: 0,
  statusSteps: []
})

const loading = ref(true)
const showAddressDrawer = ref(false)
const selectedAddressId = ref(null)
const addressList = ref([])

// 获取收货地址列表 (仅供抽屉组件里的列表渲染高亮使用)
const fetchAddressList = async () => {
  try {
    const res = await addressApi.getAddressList()
    addressList.value = res.data || res || []
  } catch (err) {
    console.error('获取地址列表失败:', err)
  }
}

// 打开地址选择抽屉
const openAddressSelector = async () => {
  await fetchAddressList()
  // 🎯 高亮对齐：让抽屉里的高亮圆圈，直接匹配当前订单存储的最新外键地址 ID
  selectedAddressId.value = order.value._backendAddressId
  showAddressDrawer.value = true
}

// 🎯 核心逻辑 1：监听选中的地址变更
watch(selectedAddressId, async (newAddressId) => {
  // 严格约束：只有当抽屉打开、地址 ID 存在、处于待付款状态时，才允许调用同步修改
  if (!showAddressDrawer.value || !newAddressId || !order.value._realBackendId) return
  if (order.value.status !== 0 && order.value.status !== 'pending') return

  try {
    // 1. 调用你的业务接口，后端会将新地址信息完整覆盖写入 orders 表对应的 receiver_xxx 字段
    await orderApi.updateOrderAddress({
      orderId: order.value._realBackendId,
      addressId: newAddressId
    })
    ElMessage.success('收货地址修改成功！')
    
    // 2. 核心闭环：立刻重新拉取一遍订单详情接口，吃进后端更新后的全新订单快照，实现无缝切换展示！
    await refreshOrderDetail(order.value._realBackendId)
  } catch (error) {
    console.error('同步修改订单地址失败:', error)
    ElMessage.error('修改收货地址失败，请稍后重试')
  }
})

// 统一拉取/刷新订单信息
const refreshOrderDetail = async (orderId) => {
  try {
    const apiRes = await orderApi.getOrderDetail(orderId)
    let res = (apiRes && apiRes.data) ? apiRes.data : apiRes
    
    // 执行结构映射适配
    const mappedResult = mapBackendToFrontend(res)
    if (mappedResult) {
      order.value = mappedResult
      // 同步高亮所需的外键 ID
      selectedAddressId.value = mappedResult._backendAddressId
    } else {
      ElMessage.error('订单不存在')
    }
  } catch (err) {
    console.error('加载订单失败:', err)
    ElMessage.error('获取订单详情失败')
  }
}

// 商品图片路径动态解析
const getGoodsImage = (rawPath) => {
  if (!rawPath) return `${OSS_BASE_URL}upload/default.png`
  const fullUrl = rawPath.startsWith('http') 
    ? rawPath 
    : `${OSS_BASE_URL.replace(/\/$/, '')}/${rawPath.replace(/^\/+/, '')}`
  return optimizeImage(fullUrl, 400)
}

const getCurrentStepIndex = () => {
  const steps = order.value.statusSteps || []
  if (!steps.length) return 0
  for (let i = steps.length - 1; i >= 0; i--) {
    if (steps[i].completed) return i + 1
  }
  return 0
}

const goToOrders = () => { router.push('/order') }

/**
 * 🎯 核心逻辑 2：数据映射适配器 (Backend -> Frontend)
 * 前端视图彻底不再依赖实时地址库，全部数据来源于后端查出的 orders 主表快照
 */
const mapBackendToFrontend = (raw) => {
  if (!raw) return null

  const frontendGoods = (raw.items || []).map(item => ({
    id: item.id || item.skuId,
    name: item.productName || '未知商品',
    spec: item.skuName || '默认规格',
    skuCode: item.skuId ? String(item.skuId) : '--',
    price: item.price ?? (raw.totalAmount || 0),
    quantity: item.quantity || 1,
    img: item.mainImage || item.picUrl || '' 
  }))

  const orderTime = raw.createdAt ? raw.createdAt.replace('T', ' ').slice(0, 19) : '暂无时间'
  const realId = raw.id || raw.orderId

  return {
    orderNo: generateDisplayOrderNo(realId), 
    _realBackendId: realId, 
    _backendAddressId: raw.addressId, // 记录来源 addressId 以供抽屉组件绑定高亮
    statusText: getStatusText(raw.status),
    status: raw.status,
    totalPrice: raw.payAmount ?? raw.totalAmount ?? 0,
    shopName: '型动派官方自营店',
    jdBeans: 10,
    
    statusSteps: [
      { label: '提交订单', time: orderTime, completed: true },
      { label: '规范付款', time: raw.status > 0 ? orderTime : '', completed: raw.status > 0 && raw.status !== 2 },
      { label: '宝贝出库', time: raw.status === 1 || raw.status === 3 ? orderTime : '', completed: raw.status === 1 || raw.status === 3 },
      { label: '确认收货', time: raw.status === 3 ? orderTime : '', completed: raw.status === 3 }
    ],
    logistics: raw.status === 1 || raw.status === 3 ? {
      carrier: '京东快递',
      carrierPhone: '950616',
      trackingNo: 'JD00987654321',
      status: '送达',
      timeline: [
        { status: '派送中', desc: '您的订单正在由京东配送员极速派送中', time: orderTime },
        { status: '已发货', desc: '卖家已发货，包裹准备出库', time: orderTime }
      ]
    } : null,

    // 🎯 完美承接：直接展现后端后端拼装好的 receiver 快照对象数据！
    receiver: {
      name: raw.receiver?.name || '未记录收货人',
      phone: raw.receiver?.phone || '----',
      address: raw.receiver?.address || '未记录详细地址'
    },
    delivery: {
      method: '京东快递（支持夜间收货）',
      expectedDate: '工作日、双休日均可送货',
      expectedTime: '09:00-21:00'
    },
    payment: {
      method: raw.paymentType === 1 ? '微信支付' : raw.paymentType === 2 ? '支付宝' : '在线支付',
      time: raw.status > 0 && raw.status !== 2 ? orderTime : '--'
    },
    priceSummary: {
      goodsTotal: raw.totalAmount || 0,
      freight: 0,
      promotion: 0,
      discount: (raw.totalAmount - raw.payAmount) > 0 ? (raw.totalAmount - raw.payAmount) : 0
    },
    goods: frontendGoods
  }
}

onMounted(async () => {
  const orderIdRaw = route.params.id || route.query.orderId || route.query.id
  if (!orderIdRaw) {
    loading.value = false
    ElMessage.error('无效的订单ID')
    return
  }

  try {
    const orderId = Number(orderIdRaw)
    await refreshOrderDetail(orderId)
  } finally {
    loading.value = false
  }
})
</script>

<style scoped lang="scss">
$primary-color: #007fc3;
$primary-hover: #005fa3;
$border-light: #edf0f2;
$bg-gray: #f5f7fa;
$bg-card: white;
$text-dark: #1f2f3a;
$text-light: #6c7a8e;
$price-color: #e53935;
$status-completed: #27ae60;
$timeline-dot-color: #007fc3;
$timeline-line-color: #e0e0e0;

.order-detail-page {
  background: $bg-gray;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.main-content {
  flex: 1;
  max-width: 1200px;
  width: 100%;
  margin: 0 auto;
  padding: 24px 20px;
}

.container {
  width: 100%;
}

/* 面包屑栏 + 未读消息 */
.breadcrumb-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  .breadcrumb {
    font-size: 14px;
    .link {
      color: $text-light;
      cursor: pointer;
      &:hover { color: $primary-color; }
    }
    .separator { margin: 0 8px; color: $text-light; }
    .current { color: $text-dark; font-weight: 500; }
  }
  .unread-badge {
    position: relative;
    cursor: pointer;
    .msg-icon { font-size: 24px; }
    .badge {
      position: absolute;
      top: -6px;
      right: -10px;
      background: #ff4444;
      color: white;
      border-radius: 20px;
      font-size: 12px;
      padding: 2px 6px;
    }
  }
}

/* 状态卡片 */
.status-card {
  background: $bg-card;
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
  .status-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    margin-bottom: 12px;
    .status-title {
      .status-text {
        font-size: 18px;
        font-weight: 600;
        color: $status-completed;
        margin-right: 20px;
      }
      .order-no {
        font-size: 14px;
        color: $text-light;
      }
    }
    .status-actions {
      .btn-text {
        background: none;
        border: none;
        color: $primary-color;
        cursor: pointer;
        margin-left: 16px;
        font-size: 14px;
        &:hover { text-decoration: underline; }
      }
    }
  }
  .status-message {
    background: #f8f9fa;
    padding: 12px;
    border-radius: 8px;
    font-size: 14px;
    color: $text-light;
    margin-bottom: 20px;
  }
  .status-steps {
    display: flex;
    justify-content: space-between;
    flex-wrap: wrap;
    .step {
      flex: 1;
      text-align: center;
      position: relative;
      &:not(:last-child)::after {
        content: '';
        position: absolute;
        top: 20px;
        right: -50%;
        width: 100%;
        height: 2px;
        background: #e0e0e0;
        z-index: 0;
      }
      .step-icon {
        width: 40px;
        height: 40px;
        line-height: 40px;
        border-radius: 50%;
        background: #e0e0e0;
        color: white;
        margin: 0 auto 8px;
        position: relative;
        z-index: 1;
      }
      .step-label {
        font-size: 13px;
        color: $text-light;
      }
      .step-time {
        font-size: 12px;
        color: #999;
      }
      &.completed .step-icon {
        background: $status-completed;
      }
      &.active .step-icon {
        background: $primary-color;
      }
    }
  }
}

/* 物流卡片 */
.logistics-card {
  background: $bg-card;
  border-radius: 16px;
  padding: 20px 24px;
  margin-bottom: 20px;
  .logistics-header {
    display: flex;
    justify-content: space-between;
    flex-wrap: wrap;
    margin-bottom: 20px;
    padding-bottom: 12px;
    border-bottom: 1px solid $border-light;
    .carrier-info {
      .carrier-name { font-weight: 600; margin-right: 20px; }
      .carrier-phone { font-size: 13px; color: $text-light; }
    }
    .tracking-info {
      font-size: 13px;
      a { margin-left: 12px; color: $primary-color; cursor: pointer; }
    }
  }
  .timeline {
    margin: 20px 0;
    .timeline-item {
      display: flex;
      gap: 16px;
      position: relative;
      padding-bottom: 24px;
      &:not(:last-child)::before {
        content: '';
        position: absolute;
        left: 10px;
        top: 22px;
        width: 2px;
        height: calc(100% - 16px);
        background: $timeline-line-color;
      }
      .timeline-dot {
        flex-shrink: 0;
        width: 12px;
        height: 12px;
        border-radius: 50%;
        background: $timeline-dot-color;
        margin-top: 6px;
        z-index: 1;
      }
      .timeline-content {
        .timeline-title { font-weight: 600; margin-bottom: 6px; }
        .timeline-desc { font-size: 13px; color: $text-light; margin-bottom: 4px; }
        .timeline-time { font-size: 12px; color: #999; }
      }
    }
  }
  .delivery-info {
    background: #fef7e0;
    padding: 12px;
    border-radius: 8px;
    font-size: 13px;
    color: #b86f00;
  }
}

/* 三列信息卡片 */
.info-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  margin-bottom: 20px;
  .info-card {
    background: $bg-card;
    border-radius: 16px;
    padding: 16px 20px;
    h4 {
      font-size: 16px;
      font-weight: 600;
      margin-bottom: 12px;
      border-left: 4px solid $primary-color;
      padding-left: 10px;
    }
    p {
      margin: 8px 0;
      font-size: 14px;
      color: $text-dark;
    }
  }
}

/* 商品卡片 */
.goods-card {
  background: $bg-card;
  border-radius: 16px;
  padding: 20px 24px;
  margin-bottom: 20px;
  .shop-info {
    padding-bottom: 12px;
    border-bottom: 1px solid $border-light;
    margin-bottom: 16px;
    .shop-name { font-weight: 600; }
  }
  .goods-table {
    width: 100%;
    .goods-header {
      display: grid;
      grid-template-columns: 4fr 1.5fr 1fr 1fr 1fr 1.5fr;
      background: #f8f9fa;
      padding: 12px 0;
      font-weight: 600;
      font-size: 13px;
      color: $text-light;
      border-radius: 8px;
      margin-bottom: 8px;
    }
    .goods-row {
      display: grid;
      grid-template-columns: 4fr 1.5fr 1fr 1fr 1fr 1.5fr;
      align-items: center;
      padding: 16px 0;
      border-bottom: 1px solid $border-light;
      .col-goods {
        display: flex;
        gap: 12px;
        .goods-img {
          width: 60px;
          height: 60px;
          border-radius: 8px;
          object-fit: cover;
        }
        .goods-info {
          .goods-name { font-weight: 500; margin-bottom: 4px; }
          .goods-spec { font-size: 12px; color: $text-light; }
        }
      }
      .col-actions a {
        display: block;
        font-size: 12px;
        color: $primary-color;
        margin-bottom: 4px;
        cursor: pointer;
        &:hover { text-decoration: underline; }
      }
    }
  }
  .goods-footer {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-top: 20px;
    flex-wrap: wrap;
    .promise-tags {
      font-size: 12px;
      color: #f90;
      span { margin-right: 20px; }
    }
    .price-detail {
      text-align: right;
      font-size: 14px;
      line-height: 1.8;
      .total-pay {
        font-size: 18px;
        font-weight: bold;
        color: $price-color;
      }
    }
  }
}

/* 额外提示 */
.extra-tips {
  display: flex;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  .tip-item {
    background: #fef7e0;
    border-radius: 12px;
    padding: 10px 16px;
    font-size: 13px;
    color: #b86f00;
    cursor: pointer;
    .beans {
      color: #ff6b00;
      font-weight: 600;
      margin: 0 4px;
    }
  }
}

/* 加载中和空状态 */
.loading-state, .empty-state {
  text-align: center;
  padding: 80px 20px;
  background: white;
  border-radius: 20px;
  color: #8a9aad;
}
.empty-icon {
  font-size: 56px;
  margin-bottom: 16px;
}
.btn-primary {
  background: $primary-color;
  border: none;
  color: white;
  padding: 8px 20px;
  border-radius: 40px;
  cursor: pointer;
  transition: all 0.2s;
  &:hover {
    background: $primary-hover;
  }
}

/* 页脚 */
.site-footer {
  background-color: #1a1a1a;
  color: #ccc;
  margin-top: 60px;
  padding: 40px 30px 20px;
  width: 100%;
  .footer-inner {
    max-width: 1200px;
    margin: 0 auto;
    padding: 0 20px;
  }
  .footer-links {
    display: flex;
    flex-wrap: wrap;
    justify-content: space-between;
    gap: 30px;
    border-bottom: 1px solid #333;
    padding-bottom: 30px;
    margin-bottom: 20px;
    .link-group {
      flex: 1;
      min-width: 120px;
      h4 { color: #fff; font-size: 16px; margin-bottom: 15px; font-weight: 500; }
      a { display: block; color: #aaa; text-decoration: none; font-size: 13px; line-height: 1.8; cursor: pointer; &:hover { color: #0075bf; } }
    }
  }
  .footer-copyright {
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: 15px;
    font-size: 12px;
    color: #777;
    .payment-icons span {
      margin-left: 15px;
      font-size: 12px;
      background: #333;
      padding: 4px 8px;
      border-radius: 4px;
      color: #ddd;
    }
  }
}

@media (max-width: 768px) {
  .main-content { padding: 16px; }
  .info-grid { grid-template-columns: 1fr; }
  .goods-header, .goods-row {
    grid-template-columns: 1fr !important;
    gap: 8px;
    .col-goods { flex-direction: column; }
  }
  .status-steps { flex-direction: column; gap: 16px; }
  .status-steps .step:not(:last-child)::after { display: none; }
  .extra-tips { flex-direction: column; }
  .footer-links { flex-direction: column; gap: 20px; }
  .footer-copyright { flex-direction: column; text-align: center; }
}
</style>