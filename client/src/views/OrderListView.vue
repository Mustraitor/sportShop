<script setup>
import { ref, computed } from 'vue'
import NavBar from '@/components/NavBar.vue'
// 订单状态筛选
const tabs = ref(['全部', '待付款', '待发货', '已完成', '已取消'])
const activeTab = ref(0)

// 模拟订单数据（后续可替换为接口请求）
const orderList = ref([
  {
    id: 1,
    orderNo: 'XDP20260513001',
    createTime: '2026-05-13 15:42:18',
    status: 'pending',
    statusText: '待付款',
    statusClass: 'status-pending',
    totalPrice: 488.00,
    goods: [
      {
        id: 101,
        img: 'https://via.placeholder.com/80',
        name: '专业慢跑鞋 男女同款',
        spec: '42码 / 深空灰',
        price: 329.00
      },
      {
        id: 102,
        img: 'https://via.placeholder.com/80',
        name: '户外速干防晒衣轻薄款',
        spec: 'L码 / 藏青色',
        price: 159.00
      }
    ]
  },
  {
    id: 2,
    orderNo: 'XDP20260512002',
    createTime: '2026-05-12 09:20:33',
    status: 'paid',
    statusText: '已付款 待发货',
    statusClass: 'status-paid',
    totalPrice: 299.00,
    goods: [
      {
        id: 201,
        img: 'https://via.placeholder.com/80',
        name: '男士跑步鞋',
        spec: '黑色 / 42码',
        price: 299.00
      }
    ]
  }
])

// 根据当前选中的标签过滤订单
const filteredOrderList = computed(() => {
  if (activeTab.value === 0) {
    return orderList.value
  }
  const statusMap = ['all', 'pending', 'paid', 'completed', 'cancelled']
  const targetStatus = statusMap[activeTab.value]
  return orderList.value.filter(order => order.status === targetStatus)
})

// 切换筛选标签
const changeTab = (index) => {
  activeTab.value = index
}
</script>
<template>
  <div class="order-list-page">
    <!-- 顶部导航栏（和首页风格保持一致） -->
    <NavBar/>
    <!-- 页面主体 -->
    <main class="main-content">
      <div class="container">
        <h1 class="page-title">我的订单</h1>

        <!-- 订单状态筛选 -->
        <div class="filter-tabs">
          <button
            v-for="(tab, index) in tabs"
            :key="index"
            :class="{ active: activeTab === index }"
            @click="changeTab(index)"
          >
            {{ tab }}
          </button>
        </div>

        <!-- 订单列表 -->
        <div class="order-list">
          <div class="order-item" v-for="order in filteredOrderList" :key="order.id">
            <!-- 订单头部 -->
            <div class="order-header">
              <div class="order-info">
                <span class="order-no">订单号：{{ order.orderNo }}</span>
                <span class="order-time">下单时间：{{ order.createTime }}</span>
              </div>
              <div class="order-status" :class="order.statusClass">
                {{ order.statusText }}
              </div>
            </div>

            <!-- 商品列表 -->
            <div class="goods-list">
              <div class="goods-item" v-for="item in order.goods" :key="item.id">
                <img :src="item.img" alt="商品图片" class="goods-img" />
                <div class="goods-info">
                  <p class="goods-name">{{ item.name }}</p>
                  <p class="goods-spec">{{ item.spec }}</p>
                </div>
                <div class="goods-price">¥{{ item.price.toFixed(2) }}</div>
              </div>
            </div>

            <!-- 订单底部 -->
            <div class="order-footer">
              <div class="total-info">
                共 {{ order.goods.length }} 件商品，实付：
                <span class="total-price">¥{{ order.totalPrice.toFixed(2) }}</span>
              </div>
              <div class="btn-group">
                <button v-if="order.status === 'pending'" class="btn btn-cancel">取消订单</button>
                <button v-if="order.status === 'pending'" class="btn btn-primary">立即支付</button>
                <button v-if="order.status === 'paid'" class="btn btn-secondary">查看物流</button>
                <button class="btn btn-secondary">联系客服</button>
              </div>
            </div>
          </div>

          <!-- 空状态提示 -->
          <div v-if="filteredOrderList.length === 0" class="empty-state">
            <p>暂无订单记录</p>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>



<style scoped>
/* 全局样式重置（和项目保持一致） */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: "Microsoft Yahei", Arial, sans-serif;
  background-color: #f5f5f5;
}

/* 顶部导航栏 */
.site-header {
  background-color: #fff;
  border-bottom: 1px solid #eee;
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-inner {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
}

.logo-icon {
  width: 48px;
  height: 48px;
}

.logo-text {
  font-size: 20px;
  font-weight: bold;
  color: #333;
}

.search-bar {
  display: flex;
  align-items: center;
  flex: 0 1 400px;
}

.search-bar input {
  flex: 1;
  padding: 10px 16px;
  border: 1px solid #ddd;
  border-radius: 24px 0 0 24px;
  outline: none;
  font-size: 14px;
}

.search-btn {
  padding: 10px 20px;
  background-color: #007fc3;
  color: #fff;
  border: 1px solid #007fc3;
  border-radius: 0 24px 24px 0;
  cursor: pointer;
  font-size: 14px;
}

.nav-links {
  display: flex;
  gap: 24px;
  align-items: center;
}

.nav-link {
  color: #333;
  text-decoration: none;
  font-size: 14px;
}

.nav-link.active {
  color: #007fc3;
  font-weight: bold;
}

/* 页面主体 */
.main-content {
  padding: 40px 20px;
}

.container {
  max-width: 1000px;
  margin: 0 auto;
}

.page-title {
  font-size: 24px;
  color: #333;
  margin-bottom: 20px;
}

.filter-tabs {
  display: flex;
  background: #fff;
  border-radius: 8px;
  margin-bottom: 20px;
  overflow: hidden;
  border: 1px solid #eee;
}

.filter-tabs button {
  flex: 1;
  border: none;
  background: transparent;
  padding: 15px;
  font-size: 14px;
  color: #666;
  cursor: pointer;
  transition: all 0.3s ease;
}

.filter-tabs button:hover {
  background-color: #f5f5f5;
}

.filter-tabs button.active {
  background-color: #007fc3;
  color: #fff;
}

.order-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.order-item {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #eee;
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background-color: #fafafa;
  border-bottom: 1px solid #eee;
}

.order-info {
  display: flex;
  gap: 20px;
  font-size: 14px;
  color: #666;
}

.order-no {
  color: #333;
}

.order-status {
  font-size: 14px;
  font-weight: bold;
}

.status-pending {
  color: #ff9800;
}

.status-paid {
  color: #007fc3;
}

.status-completed {
  color: #4caf50;
}

.status-cancelled {
  color: #999;
}

.goods-list {
  padding: 16px 20px;
}

.goods-item {
  display: flex;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f5f5f5;
}

.goods-item:last-child {
  border-bottom: none;
}

.goods-img {
  width: 80px;
  height: 80px;
  border-radius: 4px;
  object-fit: cover;
  margin-right: 16px;
  background-color: #f5f5f5;
}

.goods-info {
  flex: 1;
}

.goods-name {
  font-size: 14px;
  color: #333;
  margin-bottom: 4px;
}

.goods-spec {
  font-size: 12px;
  color: #999;
}

.goods-price {
  font-size: 14px;
  color: #333;
  font-weight: bold;
}

.order-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-top: 1px solid #eee;
}

.total-info {
  font-size: 14px;
  color: #666;
}

.total-price {
  color: #e53935;
  font-size: 16px;
  font-weight: bold;
  margin-left: 8px;
}

.btn-group {
  display: flex;
  gap: 10px;
}

.btn {
  padding: 8px 16px;
  border-radius: 4px;
  border: 1px solid #ccc;
  background: #fff;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s ease;
}

.btn:hover {
  opacity: 0.9;
}

.btn-cancel {
  color: #666;
}

.btn-secondary {
  color: #007fc3;
  border-color: #007fc3;
}

.btn-primary {
  background-color: #007fc3;
  color: #fff;
  border-color: #007fc3;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #999;
  background-color: #fff;
  border-radius: 8px;
  border: 1px solid #eee;
}
</style>