<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import NavBar from '@/components/NavBar.vue'
import OrderCard from '@/components/OrderCard.vue'
import { orderApi } from '@/api/order' // 引入重构后的命名空间对象
import { cartApi } from '@/api/cart' // 引入重构后的命名空间对象

const router = useRouter()

// 状态标签页配置
const statusTabs = [
  { label: '全部', value: 'all' },
  { label: '待付款', value: 'pending' },
  { label: '待收货', value: 'receiving' },
  { label: '已完成', value: 'completed' }
]

// 统一使用 activeStatus 作为当前选中的标签状态值
const activeStatus = ref('all')

const activeStatusLabel = computed(() => {
  const tab = statusTabs.find(t => t.value === activeStatus.value)
  return tab ? tab.label : '全部'
})

// 分页与加载状态
const currentPage = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const orderList = ref([])
const total = ref(0) // 后端返回的总条数

const totalPages = computed(() => {
  return Math.ceil(total.value / pageSize.value) || 1
})

// 未读消息数量
const unreadCount = ref(0)
const fetchUnreadCount = async () => {
  try {
    const res = await orderApi.getUnreadCount()
    unreadCount.value = res ? (res.count ?? res) : 0
  } catch (error) {
    console.error('获取未读消息失败', error)
  }
}

/**
 * 获取并加载订单列表数据
 */
const fetchOrders = async () => {
  loading.value = true
  
  // 1. 映射前端的英文 Tab 状态到后端的数字
  let statusParam = null
  if (activeStatus.value === 'pending') {
    statusParam = 0 // 待支付
  } else if (activeStatus.value === 'receiving') {
    statusParam = 1 // 待收货
  } else if (activeStatus.value === 'completed') {
    statusParam = 3 // 已完成
  }

  // 2. 严格按照后端 OrderDTO.PageReq 的字段名组装对象
  const params = {
    page: currentPage.value,       // 对应后端的 page
    pageSize: pageSize.value,     // 对应后端的 pageSize
  }

  // 3. 后端规定“不传代表查全部”，所以只有非空时才把 status 加进去
  if (statusParam !== null) {
    params.status = statusParam
  }

  try {
    // 拦截器已解包，res 直接就是后端的 OrderVO.PageResult 核心数据对象
    const res = await orderApi.getOrderList(params)
    console.log('1. 后端返回的原始列表数据:', res);
    
    // 4. 移除 if (res.code === 200)，直接大胆组装数据
    if (res && res.list) {
      // 利用 Promise.all 并发调用详情接口，把残缺的数据补全
      const fullOrderList = await Promise.all(
        res.list.map(async (order) => {
          try {
            // 拿着列表里每个订单的 id 去查完整的详情 (对齐你之前的 getOrderDetail)
            const detailData = await orderApi.getOrderDetail(order.id)
            
            // 把详情里的完整数据（包含 price, items, userId 等）合并覆盖到原订单中
            return {
              ...order,
              ...detailData
            }
          } catch (err) {
            console.error(`自动获取订单(ID: ${order.id})详情失败，降级使用列表数据`, err)
            return order 
          }
        })
      )

      orderList.value = fullOrderList
      total.value = res.total || 0
    } else {
      orderList.value = []
      total.value = 0
    }
  } catch (error) {
    console.error("加载订单列表失败:", error)
  } finally {
    loading.value = false
  }
}

// 切换状态 Tab
const changeStatus = (status) => {
  activeStatus.value = status
  currentPage.value = 1 // 切换标签时重置到第一页
  fetchOrders()
}

// 切换分页
const changePage = (page) => {
  currentPage.value = page
  fetchOrders()
}

const goToDetail = (order) => {
  console.log('====== 1. 列表页触发跳转 ======\n', {
    '整个order对象': order,
    '传过去的订单ID': order?.id,
    'ID的数据类型': typeof order?.id
  })
  router.push({ name: 'OrderDetail', params: { id: order.id } })
}

// ==================== 订单动作操作方法 ====================

const handlePayOrder = async (order) => {
  try {
    router.push({ 
      path: '/checkout', 
      query: { orderId: order.id } 
    })
  } catch (e) {
    console.error('支付失败:', e)
  }
}

const handleViewLogistics = (order) => {
  if (order.trackingNo || order.courierCompany) {
    ElMessage.info(`物流公司：${order.courierCompany || '暂无'}，运单号：${order.trackingNo || '暂无'}`)
  } else {
    ElMessage.info('暂无物流信息')
  }
}

const handleConfirmReceipt = async (order) => {
  try {
    await orderApi.confirmReceipt(order.id)
    ElMessage.success('确认收货成功')
    fetchOrders()
  } catch (e) {
    console.error('确认收货失败:', e)
  }
}

//  列表页再次购买：接收当前卡片的 orderItem 对象
const handleRebuy = async (orderItem) => {
  try {
    // 1. 安全防御：校验订单内是否有商品明细
    if (!orderItem || !orderItem.items || orderItem.items.length === 0) {
      ElMessage.warning('该订单无可购买的商品');
      return;
    }

    // 2. 映射出请求 promise 数组
    const promises = orderItem.items.map((item, index) => {
      
      // 提取并确保拿到核心的 skuId
      const finalSkuId = item.skuId || item.id;
      
      const requestData = {
        productId: item.productId || null, // 如果列表对象里有 productId 就带上，没有就传 null
        skuId: Number(finalSkuId),
        quantity: Number(item.quantity || 1)
      };

      return cartApi.addToCart(requestData);
    });

    // 3. 并发执行所有 Axios 请求
    await Promise.all(promises);
    
    ElMessage.success('商品已成功重新加入购物车！');

    router.push('/cart');
  } catch (error) {
    ElMessage.error('加入购物车失败，请稍后重试');
  }
};

const handleWriteReview = (order) => {
  ElMessage.info(`评价订单 ${order.id} 的商品`)
}

// 💡 核心修改 2：彻底弃用 cancelOrder，换上全新的真正后端逻辑删除接口
const handleDeleteOrder = async (order) => {
  try {
    // 1. 调用刚刚在 api/order.js 里面定义好的真实 delete 动作
    await orderApi.deleteOrder(order.id)
    ElMessage.success('订单已删除')
    
    // 2. 💡 完美体验升级：前端不等刷新，先提前在数组里把这条删掉，达成视觉上的“瞬间蒸发”
    orderList.value = orderList.value.filter(item => item.id !== order.id)
    total.value = Math.max(0, total.value - 1)
    
    // 3. 静默重新拉取后端分页列表，对齐最新计数
    fetchOrders()
  } catch (e) {
    console.error('删除订单失败:', e)
  }
}

const handleContactService = (order) => {
  ElMessage.info(`联系客服，订单ID：${order.id}`)
}

onMounted(() => {
  fetchOrders()
  fetchUnreadCount()
})
</script>

<template>
  <div class="order-list-page">
    <NavBar />

    <main class="main-content">
      <div class="container">
        <div class="page-header-bar">
          <h1 class="page-title">我的订单</h1>
          <div class="unread-badge" @click="goToMessages">
            <span v-if="unreadCount > 0" class="badge">{{ unreadCount }}</span>
          </div>
        </div>

        <div class="filter-tabs">
          <button
            v-for="tab in statusTabs"
            :key="tab.value"
            :class="{ active: activeStatus === tab.value }"
            @click="changeStatus(tab.value)"
          >
            {{ tab.label }}
          </button>
        </div>

        <div v-if="loading" class="loading-state">加载中...</div>
        <div v-else-if="orderList.length" class="order-list">
          <OrderCard
            v-for="order in orderList"
            :key="order.id"
            :order="order"
            @click="goToDetail(order)" 
            @pay="handlePayOrder"
            @logistics="handleViewLogistics"
            @confirm="handleConfirmReceipt"
            @rebuy="handleRebuy"
            @review="handleWriteReview"
            @delete="handleDeleteOrder"
            @contact="handleContactService"
          />
          
          <div v-if="totalPages > 1" class="pagination">
            <button :disabled="currentPage === 1" @click="changePage(currentPage - 1)">上一页</button>
            <span>第 {{ currentPage }} / {{ totalPages }} 页</span>
            <button :disabled="currentPage === totalPages" @click="changePage(currentPage + 1)">下一页</button>
          </div>
        </div>
        
        <div v-else class="empty-state">
          <div class="empty-icon">📭</div>
          <p>暂无{{ activeStatusLabel }}订单</p>
          <!-- <button class="btn btn-primary" @click="changeStatus('all')">查看全部订单</button> -->
        </div>
      </div>
    </main>
  </div>
</template>



<style scoped lang="scss">
// 新增页面头部栏样式
.page-header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  .page-title {
    font-size: 28px;
    font-weight: 600;
    color: #1f2f3a;
    margin: 0;
  }
  .unread-badge {
    position: relative;
    cursor: pointer;
    .msg-icon {
      font-size: 24px;
      color: #666;
    }
    .badge {
      position: absolute;
      top: -6px;
      right: -10px;
      background-color: #ff4444;
      color: white;
      border-radius: 20px;
      font-size: 12px;
      padding: 2px 6px;
      min-width: 18px;
      text-align: center;
    }
  }
}
.order-list-page {
  background: #f5f7fa;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}
.main-content {
  flex: 1;
  max-width: 1400px;
  width: 100%;
  margin: 0 auto;
  padding: 32px 40px;
}
.container {
  width: 100%;
}
.page-title {
  font-size: 28px;
  font-weight: 600;
  margin-bottom: 24px;
  color: #1f2f3a;
}
.filter-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 28px;
  button {
    padding: 8px 24px;
    border-radius: 40px;
    font-size: 14px;
    font-weight: 500;
    background: white;
    border: 1px solid #e2e6ea;
    color: #4a5b6e;
    cursor: pointer;
    transition: all 0.2s;
    &:hover {
      border-color: #007fc3;
      color: #007fc3;
    }
    &.active {
      background: #007fc3;
      border-color: #007fc3;
      color: white;
      box-shadow: 0 2px 8px rgba(0,127,195,0.2);
    }
  }
}
.order-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.pagination {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-top: 40px;
  align-items: center;
  button {
    padding: 8px 20px;
    border: 1px solid #ccc;
    background: white;
    border-radius: 30px;
    cursor: pointer;
    &:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }
  }
}
.empty-state {
  background: white;
  text-align: center;
  padding: 80px 20px;
  border-radius: 20px;
  color: #8a9aad;
  .empty-icon {
    font-size: 56px;
    margin-bottom: 16px;
  }
}
.loading-state {
  text-align: center;
  padding: 80px;
  color: #8a9aad;
}
.btn-primary {
  background: #007fc3;
  border-color: #007fc3;
  color: white;
  padding: 8px 20px;
  border-radius: 40px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  &:hover {
    background: #005fa3;
  }
}
// 页脚样式
.site-footer {
  background-color: #1a1a1a;
  color: #ccc;
  margin-top: 60px;
  padding: 40px 30px 20px;
  width: 100%;
  .footer-inner {
    max-width: 1400px;
    margin: 0 auto;
    padding: 0 40px;
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
      h4 {
        color: #fff;
        font-size: 16px;
        margin-bottom: 15px;
        font-weight: 500;
      }
      a {
        display: block;
        color: #aaa;
        text-decoration: none;
        font-size: 13px;
        line-height: 1.8;
        cursor: pointer;
        &:hover { color: #0075bf; }
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
  .main-content {
    padding: 20px 16px;
  }
  .site-footer .footer-inner {
    padding: 0 16px;
  }
  .filter-tabs button {
    padding: 6px 16px;
  }
  .card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
  .goods-item {
    flex-wrap: wrap;
    gap: 12px;
  }
  .goods-price-info {
    margin-left: auto;
  }
  .card-footer {
    flex-direction: column;
    align-items: flex-end;
    gap: 12px;
  }
  .action-buttons {
    width: 100%;
    justify-content: flex-end;
  }
  .footer-links {
    flex-direction: column;
    gap: 20px;
  }
  .footer-copyright {
    flex-direction: column;
    text-align: center;
  }
}
</style>