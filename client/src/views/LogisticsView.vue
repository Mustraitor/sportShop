<!-- src/views/orders/LogisticsView.vue -->
<template>
  <div class="logistics-page">
    <NavBar />

    <main class="main-content">
      <div class="container">
        <!-- 面包屑导航 -->
        <div class="breadcrumb-bar">
          <span class="link" @click="goToOrders">我的订单</span>
          <span class="separator">></span>
          <span class="link" @click="goToOrderDetail">订单详情</span>
          <span class="separator">></span>
          <span class="current">物流跟踪</span>
        </div>

        <!-- 加载中 -->
        <div v-if="loading" class="loading-state">
          <el-icon class="is-loading"><Loading /></el-icon>
          <span>加载物流信息中...</span>
        </div>

        <!-- 物流信息主体 -->
        <div v-else-if="logisticsData.orderNo" class="logistics-container">
          <!-- 头部卡片：订单号和物流状态 -->
          <div class="header-card">
            <div class="order-info">
              <span class="order-no">订单号：{{ logisticsData.orderNo }}</span>
              <span class="order-status" :class="statusClass">{{ logisticsData.statusText }}</span>
            </div>
            <div class="delivery-progress">
              <el-steps :active="currentStepIndex" align-center>
                <el-step
                  v-for="(step, idx) in logisticsData.steps"
                  :key="idx"
                  :title="step.title"
                  :description="step.time"
                />
              </el-steps>
            </div>
          </div>

          <!-- 物流公司及运单号卡片 -->
          <div class="carrier-card">
            <div class="carrier-info">
              <div class="carrier-name">
                🚚 {{ logisticsData.carrier }}
              </div>
              <div class="tracking-no">
                运单号：{{ logisticsData.trackingNo }}
                <el-button link type="primary" @click="copyTracking">复制</el-button>
              </div>
            </div>
            <div class="delivery-eta" v-if="logisticsData.estimatedTime">
              ⏰ 预计送达：{{ logisticsData.estimatedTime }}
            </div>
          </div>

          <!-- 物流轨迹时间线 -->
          <div class="timeline-card">
            <h4>物流轨迹</h4>
            <div class="timeline">
              <div
                v-for="(node, idx) in logisticsData.timeline"
                :key="idx"
                class="timeline-item"
                :class="{ 'is-latest': idx === 0 }"
              >
                <div class="timeline-dot"></div>
                <div class="timeline-content">
                  <div class="timeline-title">{{ node.status }}</div>
                  <div class="timeline-desc">{{ node.desc }}</div>
                  <div class="timeline-time">{{ node.time }}</div>
                </div>
              </div>
              <div v-if="!logisticsData.timeline?.length" class="no-timeline">
                暂无物流轨迹，请稍后再试。
              </div>
            </div>
          </div>

          <!-- 客服与帮助 -->
          <div class="help-card">
            <div class="help-item" @click="contactService">
              <el-icon><Service /></el-icon>
              <span>联系客服</span>
            </div>
            <div class="help-item" @click="viewCommonQuestions">
              <el-icon><QuestionFilled /></el-icon>
              <span>常见问题</span>
            </div>
            <div class="help-item" @click="reportIssue">
              <el-icon><Warning /></el-icon>
              <span>物流投诉</span>
            </div>
          </div>
        </div>

        <!-- 无物流信息或错误 -->
        <div v-else class="empty-state">
          <div class="empty-icon">🚚</div>
          <p>未查询到物流信息</p>
          <el-button type="primary" @click="goToOrderDetail">返回订单详情</el-button>
        </div>
      </div>
    </main>

    <footer class="site-footer">...</footer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Loading, Service, QuestionFilled, Warning } from '@element-plus/icons-vue'
import NavBar from '@/components/NavBar.vue'
import { getLogisticsDetail } from '@/api/order_1'


const route = useRoute()
const router = useRouter()

const loading = ref(true)
const logisticsData = ref({
  orderNo: '',
  statusText: '',
  status: '',
  carrier: '',
  trackingNo: '',
  estimatedTime: '',
  steps: [],        // 用于 el-steps 的步骤数组 [{ title, time }]
  timeline: []      // 详细轨迹 [{ status, desc, time }]
})

// 当前激活的步骤索引（用于 el-steps）
const currentStepIndex = computed(() => {
  // 根据物流状态文本匹配步骤索引，比如 "已签收" -> 最后一步
  const status = logisticsData.value.status
  const steps = logisticsData.value.steps
  if (!steps.length) return 0
  if (status === 'delivered') return steps.length - 1
  if (status === 'shipped') return 1
  return 0
})

const statusClass = computed(() => {
  const map = {
    pending: 'status-pending',
    shipped: 'status-shipped',
    delivered: 'status-delivered',
    problem: 'status-problem'
  }
  return map[logisticsData.value.status] || 'status-default'
})

// 复制运单号
const copyTracking = () => {
  if (logisticsData.value.trackingNo) {
    navigator.clipboard.writeText(logisticsData.value.trackingNo)
    ElMessage.success('运单号已复制')
  }
}

// 页面跳转方法
const goToOrders = () => router.push('/orders')
const goToOrderDetail = () => {
  if (logisticsData.value.orderNo) {
    router.push(`/order-detail/${route.params.orderId}`)
  } else {
    router.push('/orders')
  }
}

// 客服等操作
const contactService = () => ElMessage.info('联系客服（演示）')
const viewCommonQuestions = () => ElMessage.info('常见问题（演示）')
const reportIssue = () => ElMessage.info('物流投诉（演示）')

// 获取物流数据
// 获取物流数据
const fetchLogistics = async (orderId) => {
  loading.value = true
  try {
    const data = await getLogisticsDetail(orderId)
    if (data && data.orderNo) {
      logisticsData.value = data
    } else {
      // 如果没有物流数据，展示默认模拟数据（用于演示）
      console.log('该订单无物流信息，加载演示数据')
      logisticsData.value = {
        orderNo: `订单${orderId}（演示）`,
        statusText: '运输中',
        status: 'shipped',
        carrier: '顺丰速运',
        trackingNo: 'SF' + Date.now(),
        estimatedTime: '预计 2-3 天内送达',
        steps: [
          { title: '已下单', time: new Date().toLocaleString() },
          { title: '已付款', time: new Date().toLocaleString() },
          { title: '已发货', time: new Date().toLocaleString() },
          { title: '已签收', time: null }
        ],
        timeline: [
          {
            status: '已发货',
            desc: '您的订单已从仓库发出，等待揽收。',
            time: new Date().toLocaleString()
          }
        ]
      }
    }
  } catch (error) {
    console.error(error)
    ElMessage.error('获取物流信息失败')
    // 出错时也展示模拟数据
    logisticsData.value = { /* 同上模拟数据 */ }
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  const orderId = route.params.orderId
  if (orderId) {
    fetchLogistics(orderId)
  } else {
    loading.value = false
    ElMessage.error('无效的订单ID')
  }
})
</script>

<style scoped lang="scss">
$primary-color: #007fc3;
$border-light: #edf0f2;
$bg-gray: #f5f7fa;
$text-dark: #1f2f3a;
$text-light: #6c7a8e;
$status-shipped: #e67e22;
$status-delivered: #27ae60;

.logistics-page {
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
.breadcrumb-bar {
  margin-bottom: 24px;
  font-size: 14px;
  .link {
    color: $text-light;
    cursor: pointer;
    &:hover { color: $primary-color; }
  }
  .separator { margin: 0 8px; color: $text-light; }
  .current { color: $text-dark; font-weight: 500; }
}
.loading-state {
  text-align: center;
  padding: 80px;
  background: white;
  border-radius: 20px;
  color: $text-light;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
}
.header-card {
  background: white;
  border-radius: 20px;
  padding: 24px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
  .order-info {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    .order-no { font-weight: 500; color: $text-dark; }
    .order-status {
      font-weight: 600;
      &.status-shipped { color: $status-shipped; }
      &.status-delivered { color: $status-delivered; }
    }
  }
  .delivery-progress {
    :deep(.el-step__title) { font-size: 13px; }
    :deep(.el-step__description) { font-size: 12px; color: $text-light; }
  }
}
.carrier-card {
  background: white;
  border-radius: 20px;
  padding: 20px 24px;
  margin-bottom: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  .carrier-info {
    display: flex;
    align-items: center;
    gap: 24px;
    .carrier-name {
      display: flex;
      align-items: center;
      gap: 6px;
      font-weight: 500;
    }
    .tracking-no {
      color: $text-light;
      font-size: 14px;
    }
  }
  .delivery-eta {
    display: flex;
    align-items: center;
    gap: 6px;
    color: #e67e22;
    font-size: 14px;
  }
}
.timeline-card {
  background: white;
  border-radius: 20px;
  padding: 24px;
  margin-bottom: 20px;
  h4 {
    font-size: 18px;
    font-weight: 600;
    margin-bottom: 20px;
    border-left: 4px solid $primary-color;
    padding-left: 14px;
  }
  .timeline {
    .timeline-item {
      display: flex;
      gap: 16px;
      position: relative;
      padding-bottom: 28px;
      &:not(:last-child)::before {
        content: '';
        position: absolute;
        left: 10px;
        top: 22px;
        width: 2px;
        height: calc(100% - 16px);
        background: #e0e0e0;
      }
      .timeline-dot {
        flex-shrink: 0;
        width: 12px;
        height: 12px;
        border-radius: 50%;
        background: #bbb;
        margin-top: 6px;
        z-index: 1;
      }
      &.is-latest .timeline-dot {
        background: $primary-color;
        box-shadow: 0 0 0 3px rgba(0,127,195,0.2);
      }
      .timeline-content {
        .timeline-title {
          font-weight: 600;
          margin-bottom: 6px;
        }
        .timeline-desc {
          font-size: 13px;
          color: $text-light;
          margin-bottom: 4px;
        }
        .timeline-time {
          font-size: 12px;
          color: #999;
        }
      }
    }
    .no-timeline {
      text-align: center;
      padding: 40px;
      color: $text-light;
    }
  }
}
.help-card {
  background: white;
  border-radius: 20px;
  padding: 16px 24px;
  display: flex;
  gap: 32px;
  .help-item {
    display: flex;
    align-items: center;
    gap: 8px;
    color: $primary-color;
    cursor: pointer;
    font-size: 14px;
    &:hover { opacity: 0.8; }
  }
}
.empty-state {
  background: white;
  text-align: center;
  padding: 80px 20px;
  border-radius: 20px;
  color: $text-light;
  .empty-icon {
    font-size: 56px;
    margin-bottom: 16px;
  }
}
.site-footer {
  // 复用你已有的页脚样式，此处省略（实际保留原样式）
  background-color: #1a1a1a;
  color: #ccc;
  margin-top: 60px;
  padding: 40px 30px 20px;
  width: 100%;
}
@media (max-width: 768px) {
  .main-content { padding: 16px; }
  .carrier-card { flex-direction: column; align-items: flex-start; }
  .help-card { flex-wrap: wrap; gap: 16px; }
}
</style>