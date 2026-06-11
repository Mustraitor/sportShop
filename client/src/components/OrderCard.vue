<template>
  <div class="order-card" @click="$emit('click', order)">
    <div class="card-header">
      <div class="order-meta">
        <span class="order-no">订单号：{{ order.id }}</span>
        <span class="order-time">下单时间：{{ order.createdAt?.slice(0, 19) || '暂无时间' }}</span>
      </div>
      <div class="status-wrapper">
        <div class="order-status" :class="orderApi.getStatusClass(order.status)">
          {{ orderApi.getStatusText(order.status) }}
        </div>
      </div>
    </div>

    <div class="goods-section">
      <div
        v-for="item in (showAll ? (order.items || []) : (order.items || []).slice(0, 2))"
        :key="item.id || item.skuId"
        class="goods-item"
      >
        <img 
          :src="item.mainImage || item.picUrl || item.img ? formatImageUrl(item.mainImage || item.picUrl || item.img) : 'https://sportshop-pictures.oss-cn-beijing.aliyuncs.com/upload/default.png'" 
          class="goods-img" 
        />
        
        <div class="goods-detail">
          <div class="goods-name">{{ item.productName }}</div>
          <div class="goods-spec">{{ item.skuName || '默认规格' }}</div>
        </div>
        
        <div class="goods-price-info">
          <div class="goods-price">¥{{ (item.price || 0).toFixed(2) }}</div>
          <div class="goods-quantity">x{{ item.quantity }}</div>
        </div>
      </div>
      
      <div
        v-if="getOrderItems.length > 2"
        class="toggle-goods"
        @click.stop="showAll = !showAll"
      >
        {{ showAll ? '收起商品 ▲' : `展开全部商品 (${getOrderItems.length}件) ▼` }}
      </div>
    </div>

    <div v-if="order.address || order.receiverName" class="address-info">
      📦 收货人：{{ order.receiverName || order.address?.receiver }} {{ order.receiverPhone || order.address?.phone }}
    </div>

    <div class="card-footer">
      <div class="total-info">
        共 {{ totalQuantity }} 件商品，实付：
        <span class="total-price">¥{{ (order.payAmount || order.totalAmount || 0).toFixed(2) }}</span>
      </div>
      <div class="action-buttons" @click.stop>
        
        <template v-if="order.status === 0">
          <button class="btn btn-cancel" @click="$emit('delete', order)">删除订单</button>
          <button class="btn btn-primary" @click="$emit('pay', order)">立即支付</button>
        </template>
        
        <template v-else-if="order.status === 1">
          <button class="btn btn-secondary" @click="$emit('logistics', order)">查看物流</button>
          <button class="btn btn-primary" @click="$emit('confirm', order)">确认收货</button>
        </template>
        
        <template v-else-if="order.status === 3">
          <button class="btn btn-cancel" @click="$emit('delete', order)">删除订单</button>
          <button class="btn btn-secondary" @click="$emit('rebuy', order)">再次购买</button>
          <button class="btn btn-secondary" @click="$emit('review', order)">写评价</button>
        </template>
        
        <template v-else-if="order.status === 2">
          <button class="btn btn-cancel" @click="$emit('delete', order)">删除订单</button>
          <button class="btn btn-primary" @click="$emit('rebuy', order)">重新购买</button>
        </template>

        <button class="btn btn-outline" @click="$emit('contact', order)">联系客服</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { orderApi } from '@/api/order' 

const props = defineProps({ 
  order: { type: Object, required: true } 
})

defineEmits([
  'click', 'pay', 'logistics', 'confirm', 'rebuy', 'review', 'delete', 'contact'
])

const showAll = ref(false)
const OSS_BASE_URL = 'https://sportshop-pictures.oss-cn-beijing.aliyuncs.com/'

// 图片路径格式化
const formatImageUrl = (rawPath) => {
  if (!rawPath) return ''
  if (rawPath.startsWith('http')) return rawPath
  return OSS_BASE_URL.replace(/\/$/, '') + '/' + rawPath.replace(/^\/+/, '')
}

// 统一使用接口的 items 字段
const getOrderItems = computed(() => {
  return props.order.items || []
})

// 计算总件数
const totalQuantity = computed(() => {
  return getOrderItems.value.reduce((sum, g) => sum + (g.quantity || 0), 0)
})
</script>

<style scoped lang="scss">
.order-card {
  background: white;
  border-radius: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: transform 0.2s, box-shadow 0.2s;
  cursor: pointer;
  overflow: hidden;
  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 12px 24px rgba(0, 0, 0, 0.08);
  }
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  padding: 18px 28px;
  background: #fafcfd;
  border-bottom: 1px solid #edf0f2;
  .order-meta {
    font-size: 15px;
    color: #6c7a8e;
    .order-no {
      color: #1f2f3a;
      font-weight: 500;
      margin-right: 28px;
    }
  }
  .status-wrapper {
    display: flex;
    align-items: center;
    gap: 12px;
  }
  .order-status {
    font-size: 15px;
    font-weight: 600;
    &.status-pending { color: #e67e22; }
    &.status-paid { color: #007fc3; }
    &.status-shipped { color: #2c9e6e; }
    &.status-completed { color: #27ae60; }
    &.status-cancelled { color: #95a5a6; }
  }
}
.goods-section {
  padding: 12px 28px;
  .goods-item {
    display: flex;
    align-items: center;
    gap: 20px;
    padding: 18px 0;
    border-bottom: 1px solid #f0f2f4;
    &:last-child { border-bottom: none; }
    .goods-img {
      width: 80px;
      height: 80px;
      background: #f0f2f4;
      border-radius: 12px;
      object-fit: cover;
    }
    .goods-detail {
      flex: 1;
      .goods-name {
        font-weight: 500;
        margin-bottom: 8px;
        font-size: 16px;
      }
      .goods-spec {
        font-size: 13px;
        color: #8a9aad;
      }
    }
    .goods-price-info {
      text-align: right;
      .goods-price {
        font-weight: 600;
        color: #e53935;
        font-size: 16px;
      }
      .goods-quantity {
        font-size: 13px;
        color: #8a9aad;
        margin-left: 6px;
      }
    }
  }
  .toggle-goods {
    text-align: center;
    padding: 10px 0;
    color: #007fc3;
    font-size: 13px;
    cursor: pointer;
    border-top: 1px solid #f0f2f4;
    &:hover {
      background: #fafcfd;
    }
  }
}
.address-info {
  padding: 12px 28px;
  background: #fef7e0;
  font-size: 14px;
  color: #b86f00;
  border-top: 1px solid #f0e5c0;
  border-bottom: 1px solid #f0e5c0;
}
.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  padding: 18px 28px;
  background: white;
  border-top: 1px solid #edf0f2;
  .total-info {
    font-size: 15px;
    color: #5f6b7a;
    .total-price {
      font-size: 20px;
      font-weight: 700;
      color: #e53935;
      margin-left: 8px;
    }
  }
  .action-buttons {
    display: flex;
    gap: 14px;
    flex-wrap: wrap;
    .btn {
      padding: 8px 20px;
      border-radius: 40px;
      font-size: 14px;
      font-weight: 500;
      border: 1px solid #cdd6df;
      background: white;
      color: #2c3e50;
      cursor: pointer;
      transition: all 0.2s;
      &.btn-primary {
        background: #007fc3;
        border-color: #007fc3;
        color: white;
        &:hover { background: #005fa3; }
      }
      &.btn-secondary {
        border-color: #007fc3;
        color: #007fc3;
        &:hover { background: #e6f2fa; }
      }
      &.btn-cancel {
        color: #7a8a9e;
        &:hover {
          background: #f8f9fa;
          border-color: #cbd3dc;
        }
      }
      &.btn-outline {
        border-color: #cdd6df;
        &:hover { background: #f8f9fa; }
      }
    }
  }
}
</style>