// src/api/order.js
import { ref } from 'vue'

// 状态映射辅助（在模拟数据中已包含）
const getStatusText = (status) => {
  const map = { pending: '待付款', paid: '待发货', shipped: '已发货', completed: '已完成', cancelled: '已取消' }
  return map[status] || '未知'
}
const getStatusClass = (status) => {
  const map = { pending: 'status-pending', paid: 'status-paid', shipped: 'status-shipped', completed: 'status-completed', cancelled: 'status-cancelled' }
  return map[status] || ''
}

// 模拟订单数据（包含 statusText / statusClass）
const mockOrders = ref([
  {
    id: 1,
    orderNo: 'XDP20260513001',
    createTime: '2026-05-13 15:42:18',
    payTime: null,
    shipTime: null,
    completeTime: null,
    status: 'pending',
    statusText: '待付款',
    statusClass: 'status-pending',
    totalPrice: 488.00,
    goodsTotal: 488.00,
    freight: 0,
    discount: 0,
    paymentMethod: null,
    address: {
      receiver: '张三',
      phone: '138****8000',
      fullAddress: '北京市朝阳区望京SOHO T1 1101室'
    },
    goods: [
      { id: 101, img: 'https://picsum.photos/80/80?random=1', name: '专业慢跑鞋 男女同款', spec: '42码 / 深空灰', price: 329.00, quantity: 1, skuCode: 'SP101' },
      { id: 102, img: 'https://picsum.photos/80/80?random=2', name: '户外速干防晒衣轻薄款', spec: 'L码 / 藏青色', price: 159.00, quantity: 1, skuCode: 'SP102' }
    ],
    logistics: null,
    timeline: [{ time: '2026-05-13 15:42:18', desc: '订单已提交' }],
    shopName: '型动派官方旗舰店',
    receiver: { name: '张三', phone: '138****8000', address: '北京市朝阳区望京SOHO T1 1101室' },
    delivery: { method: '京东快递', expectedDate: '2026-05-14', expectedTime: '09:00-21:00' },
    payment: { method: '在线支付', time: null },
    priceSummary: { goodsTotal: 488.00, freight: 0, promotion: 0, discount: 0 },
    jdBeans: 0,
    statusSteps: [
      { label: '提交订单', time: '2026-05-13 15:42:18', completed: true },
      { label: '付款成功', time: null, completed: false },
      { label: '商品出库', time: null, completed: false },
      { label: '等待收货', time: null, completed: false },
      { label: '完成', time: null, completed: false },
      { label: '评价', time: null, completed: false }
    ]
  },
  {
    id: 2,
    orderNo: 'XDP20260512002',
    createTime: '2026-05-12 09:20:33',
    payTime: null,
    shipTime: null,
    completeTime: null,
    status: 'pending',
    statusText: '待付款',
    statusClass: 'status-pending',
    totalPrice: 299.00,
    goodsTotal: 299.00,
    freight: 0,
    discount: 0,
    paymentMethod: null,
    address: {
      receiver: '李芳',
      phone: '159****5678',
      fullAddress: '上海市浦东新区世纪大道100号'
    },
    goods: [
      { id: 201, img: 'https://picsum.photos/80/80?random=3', name: '男士跑步鞋', spec: '黑色 / 42码', price: 299.00, quantity: 1, skuCode: 'SP201' }
    ],
    logistics: null,
    timeline: [{ time: '2026-05-12 09:20:33', desc: '订单已提交' }],
    shopName: '型动派官方旗舰店',
    receiver: { name: '李芳', phone: '159****5678', address: '上海市浦东新区世纪大道100号' },
    delivery: { method: '京东快递', expectedDate: '2026-05-13', expectedTime: '09:00-21:00' },
    payment: { method: '在线支付', time: null },
    priceSummary: { goodsTotal: 299.00, freight: 0, promotion: 0, discount: 0 },
    jdBeans: 0,
    statusSteps: [
      { label: '提交订单', time: '2026-05-12 09:20:33', completed: true },
      { label: '付款成功', time: null, completed: false },
      { label: '商品出库', time: null, completed: false },
      { label: '等待收货', time: null, completed: false },
      { label: '完成', time: null, completed: false },
      { label: '评价', time: null, completed: false }
    ]
  },
  {
    id: 3,
    orderNo: 'XDP20260510003',
    createTime: '2026-05-10 08:15:22',
    payTime: '2026-05-10 08:20:01',
    shipTime: null,
    completeTime: null,
    status: 'paid',
    statusText: '待发货',
    statusClass: 'status-paid',
    totalPrice: 428.00,
    goodsTotal: 428.00,
    freight: 0,
    discount: 0,
    paymentMethod: 1,
    address: {
      receiver: '王强',
      phone: '177****9999',
      fullAddress: '广州市天河区体育西路123号'
    },
    goods: [
      { id: 301, img: 'https://picsum.photos/80/80?random=4', name: '轻量运动背包', spec: '20L / 黑色', price: 189.00, quantity: 2, skuCode: 'SP301' },
      { id: 302, img: 'https://picsum.photos/80/80?random=5', name: '速干运动毛巾', spec: '灰色', price: 39.00, quantity: 2, skuCode: 'SP302' }
    ],
    logistics: null,
    timeline: [
      { time: '2026-05-10 08:15:22', desc: '订单已提交' },
      { time: '2026-05-10 08:20:01', desc: '订单已付款' }
    ],
    shopName: '型动派官方旗舰店',
    receiver: { name: '王强', phone: '177****9999', address: '广州市天河区体育西路123号' },
    delivery: { method: '京东快递', expectedDate: '2026-05-11', expectedTime: '09:00-21:00' },
    payment: { method: '微信支付', time: '2026-05-10 08:20:01' },
    priceSummary: { goodsTotal: 428.00, freight: 0, promotion: 0, discount: 0 },
    jdBeans: 0,
    statusSteps: [
      { label: '提交订单', time: '2026-05-10 08:15:22', completed: true },
      { label: '付款成功', time: '2026-05-10 08:20:01', completed: true },
      { label: '商品出库', time: null, completed: false },
      { label: '等待收货', time: null, completed: false },
      { label: '完成', time: null, completed: false },
      { label: '评价', time: null, completed: false }
    ]
  },
  {
    id: 4,
    orderNo: 'XDP20260508004',
    createTime: '2026-05-08 14:28:52',
    payTime: '2026-05-08 14:30:12',
    shipTime: '2026-05-09 10:15:00',
    completeTime: null,
    status: 'shipped',
    statusText: '已发货',
    statusClass: 'status-shipped',
    totalPrice: 198.00,
    goodsTotal: 198.00,
    freight: 0,
    discount: 0,
    paymentMethod: 2,
    address: {
      receiver: '陈丽',
      phone: '136****4521',
      fullAddress: '深圳市南山区科技园路10号'
    },
    goods: [
      { id: 401, img: 'https://picsum.photos/80/80?random=6', name: '速干运动T恤', spec: 'M码 / 藏蓝', price: 89.00, quantity: 2, skuCode: 'SP401' }
    ],
    logistics: {
      carrier: '京东快递',
      carrierPhone: '950616',
      trackingNo: 'JDVC36241914765',
      deliveryMan: '郝虹宇',
      deliveryPhone: '18636620774',
      deliveryMethod: '京东快递',
      status: '运输中',
      timeline: [
        { status: '派送中', desc: '您的订单正在配送途中（快递员：郝虹宇，电话：18636620774），请您耐心等待。', time: '2026-05-09 14:30:00' },
        { status: '已出库', desc: '您的订单已从仓库发出，准备配送。', time: '2026-05-09 10:15:00' }
      ]
    },
    timeline: [
      { time: '2026-05-08 14:28:52', desc: '订单已提交' },
      { time: '2026-05-08 14:30:12', desc: '订单已付款' },
      { time: '2026-05-09 10:15:00', desc: '商品已发货' }
    ],
    shopName: '型动派官方旗舰店',
    receiver: { name: '陈丽', phone: '136****4521', address: '深圳市南山区科技园路10号' },
    delivery: { method: '京东快递', expectedDate: '2026-05-10', expectedTime: '09:00-21:00' },
    payment: { method: '支付宝', time: '2026-05-08 14:30:12' },
    priceSummary: { goodsTotal: 198.00, freight: 0, promotion: 0, discount: 0 },
    jdBeans: 0,
    statusSteps: [
      { label: '提交订单', time: '2026-05-08 14:28:52', completed: true },
      { label: '付款成功', time: '2026-05-08 14:30:12', completed: true },
      { label: '商品出库', time: '2026-05-09 10:15:00', completed: true },
      { label: '等待收货', time: null, completed: false },
      { label: '完成', time: null, completed: false },
      { label: '评价', time: null, completed: false }
    ]
  },
  {
    id: 5,
    orderNo: 'XDP20260505005',
    createTime: '2026-05-05 10:03:19',
    payTime: null,
    shipTime: null,
    completeTime: null,
    status: 'cancelled',
    statusText: '已取消',
    statusClass: 'status-cancelled',
    totalPrice: 129.00,
    goodsTotal: 129.00,
    freight: 0,
    discount: 0,
    paymentMethod: null,
    address: {
      receiver: '赵磊',
      phone: '152****9876',
      fullAddress: '成都市武侯区锦城大道99号'
    },
    goods: [
      { id: 501, img: 'https://picsum.photos/80/80?random=7', name: '运动水壶 600ml', spec: '蓝色', price: 59.00, quantity: 1, skuCode: 'SP501' },
      { id: 502, img: 'https://picsum.photos/80/80?random=8', name: '防滑瑜伽垫', spec: '紫色', price: 70.00, quantity: 1, skuCode: 'SP502' }
    ],
    logistics: null,
    timeline: [
      { time: '2026-05-05 10:03:19', desc: '订单已提交' },
      { time: '2026-05-06 09:15:00', desc: '用户取消订单' }
    ],
    shopName: '型动派官方旗舰店',
    receiver: { name: '赵磊', phone: '152****9876', address: '成都市武侯区锦城大道99号' },
    delivery: { method: '京东快递', expectedDate: '2026-05-06', expectedTime: '09:00-21:00' },
    payment: { method: '在线支付', time: null },
    priceSummary: { goodsTotal: 129.00, freight: 0, promotion: 0, discount: 0 },
    jdBeans: 0,
    statusSteps: [
      { label: '提交订单', time: '2026-05-05 10:03:19', completed: true },
      { label: '付款成功', time: null, completed: false },
      { label: '商品出库', time: null, completed: false },
      { label: '等待收货', time: null, completed: false },
      { label: '完成', time: null, completed: false },
      { label: '评价', time: null, completed: false }
    ]
  }
])

// 获取订单列表（分页 + 状态筛选）
export const getOrderList = (params) => {
  return new Promise((resolve) => {
    setTimeout(() => {
      let list = [...mockOrders.value]
      const { status, pageNum = 1, pageSize = 5 } = params
      if (status && status !== 'all') {
        if (status === 'receiving') {
          list = list.filter(order => order.status === 'paid' || order.status === 'shipped')
        } else {
          list = list.filter(order => order.status === status)
        }
      }
      const total = list.length
      const start = (pageNum - 1) * pageSize
      const rows = list.slice(start, start + pageSize)
      resolve({ rows, total })
    }, 300)
  })
}

// 获取订单详情
export const getOrderDetail = (orderId) => {
  return new Promise((resolve) => {
    setTimeout(() => {
      const order = mockOrders.value.find(o => o.id === orderId)
      resolve(order || null)
    }, 200)
  })
}

// 取消订单
export const cancelOrderApi = (orderId) => {
  return new Promise((resolve) => {
    setTimeout(() => {
      const order = mockOrders.value.find(o => o.id === orderId)
      if (order && order.status === 'pending') {
        order.status = 'cancelled'
        order.statusText = '已取消'
        order.statusClass = 'status-cancelled'
        order.timeline.push({ time: new Date().toLocaleString(), desc: '用户取消订单' })
      }
      resolve({ success: true })
    }, 300)
  })
}

// 支付订单
export const payOrderApi = (orderId) => {
  return new Promise((resolve) => {
    setTimeout(() => {
      const order = mockOrders.value.find(o => o.id === orderId)
      if (order && order.status === 'pending') {
        order.status = 'paid'
        order.statusText = '待发货'
        order.statusClass = 'status-paid'
        order.payTime = new Date().toLocaleString()
        order.timeline.push({ time: order.payTime, desc: '订单已付款' })
      }
      resolve({ success: true })
    }, 300)
  })
}

// 确认收货
export const confirmReceiptApi = (orderId) => {
  return new Promise((resolve) => {
    setTimeout(() => {
      const order = mockOrders.value.find(o => o.id === orderId)
      if (order && order.status === 'shipped') {
        order.status = 'completed'
        order.statusText = '已完成'
        order.statusClass = 'status-completed'
        order.completeTime = new Date().toLocaleString()
        order.timeline.push({ time: order.completeTime, desc: '订单已完成' })
      }
      resolve({ success: true })
    }, 300)
  })
}

// 删除订单
export const deleteOrderApi = (orderId) => {
  return new Promise((resolve) => {
    setTimeout(() => {
      const index = mockOrders.value.findIndex(o => o.id === orderId)
      if (index !== -1) {
        mockOrders.value.splice(index, 1)
      }
      resolve({ success: true })
    }, 300)
  })
}

// 再次购买（加入购物车）
export const rebuyOrderApi = (orderId) => {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({ success: true })
    }, 300)
  })
}

// 获取未读消息数量（模拟）
export const getUnreadCount = () => {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({ count: 3 })
    }, 200)
  })
}