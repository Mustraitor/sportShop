<script setup>
import { ref, computed } from 'vue'
import NavBar from '@/components/NavBar.vue'

// 模拟购物车数据
const cartItems = ref([
  {
    id: 1,
    image: 'https://via.placeholder.com/100x120?text=Product', 
    brand: 'DECATHLON',
    title: '运动舒缓三合一洗发洁面沐浴露',
    productCode: '5555473',
    size: '均码',
    price: 29.9,
    isChecked: false, // 默认未选中
    quantity: 1
  },
  {
    id: 2,
    image: 'https://via.placeholder.com/100x120?text=T-Shirt', 
    brand: 'QUECHUA',
    title: '男女同款速干圆领T恤 透气徒步上衣',
    productCode: '8546912',
    size: 'L', // 演示不同尺码
    price: 79.9,
    isChecked: true, // 演示默认选中状态
    quantity: 2  // 演示多件商品
  },
  {
    id: 3,
    image: 'https://via.placeholder.com/100x120?text=Yoga+Mat', // 实际项目中请替换为真实瑜伽垫图片链接
    brand: 'DOMYOS',
    title: '5mm加厚防滑环保TPE健身瑜伽垫',
    productCode: '3158746',
    size: '均码',
    price: 129.9,
    isChecked: false,
    quantity: 1
  },
  {
    id: 4,
    image: 'https://via.placeholder.com/100x120?text=Yoga+Mat', // 实际项目中请替换为真实瑜伽垫图片链接
    brand: 'DOMYOS',
    title: '5mm加厚防滑环保TPE健身瑜伽垫',
    productCode: '3158746',
    size: '均码',
    price: 129.9,
    isChecked: false,
    quantity: 1
  },
  {
    id: 5,
    image: 'https://via.placeholder.com/100x120?text=Yoga+Mat', // 实际项目中请替换为真实瑜伽垫图片链接
    brand: 'DOMYOS',
    title: '5mm加厚防滑环保TPE健身瑜伽垫',
    productCode: '3158746',
    size: '均码',
    price: 129.9,
    isChecked: false,
    quantity: 1
  },
  {
    id: 6,
    image: 'https://via.placeholder.com/100x120?text=Yoga+Mat', // 实际项目中请替换为真实瑜伽垫图片链接
    brand: 'DOMYOS',
    title: '5mm加厚防滑环保TPE健身瑜伽垫',
    productCode: '3158746',
    size: '均码',
    price: 129.9,
    isChecked: false,
    quantity: 1
  },
  {
    id: 7,
    image: 'https://via.placeholder.com/100x120?text=Yoga+Mat', // 实际项目中请替换为真实瑜伽垫图片链接
    brand: 'DOMYOS',
    title: '5mm加厚防滑环保TPE健身瑜伽垫',
    productCode: '3158746',
    size: '均码',
    price: 129.9,
    isChecked: false,
    quantity: 1
  }
])


// 计算属性：是否全选
const isAllSelected = computed({
  get: () => cartItems.value.every(item => item.isChecked),
  set: (value) => cartItems.value.forEach(item => item.isChecked = value)
})

// 计算属性：选中的商品列表
const selectedItems = computed(() => cartItems.value.filter(item => item.isChecked))

// 计算属性：合计金额
const totalPrice = computed(() => {
  return selectedItems.value.reduce((sum, item) => sum + item.price * item.quantity, 0).toFixed(2)
})

// 计算属性：选中商品总件数
const totalQuantity = computed(() => {
  return selectedItems.value.reduce((sum, item) => sum + item.quantity, 0)
})

// 方法：增加数量
const increment = (item) => {
  item.quantity++
}

// 方法：减少数量
const decrement = (item) => {
  if (item.quantity > 1) {
    item.quantity--
  }
}

// 方法：删除商品
const removeItem = (id) => {
  if (confirm('确定要删除该商品吗？')) {
    cartItems.value = cartItems.value.filter(item => item.id !== id)
  }
} 
</script>

<template>
  <div class="cart-container">
    <!-- 顶部导航栏 -->
    <NavBar/>

    <main class="main-content">
      <h2>购物车 ({{ cartItems.length }})</h2>

      <!-- 商品列表 -->
      <div class="cart-list">
        <div class="cart-item" v-for="item in cartItems" :key="item.id">
          <div class="item-select">
            <input type="checkbox" v-model="item.isChecked" />
          </div>

          <div class="item-image">
            <img :src="item.image" alt="product" />
          </div>

          <div class="item-info">
            <div class="brand">{{ item.brand }}</div>
            <div class="title">{{ item.title }}</div>
          </div>

          <div class="item-specs">
            <div class="code">货号: {{ item.productCode }}</div>
            <div class="size">尺码: {{ item.size }}</div>
            <div class="edit-link">修改颜色/尺码</div>
          </div>

          <div class="item-price">
            <div class="label">单价</div>
            <div class="value">¥{{ item.price.toFixed(2) }}</div>
          </div>

          <div class="item-quantity">
            <button @click="decrement(item)" :disabled="item.quantity <= 1">-</button>
            <span>{{ item.quantity }}</span>
            <button @click="increment(item)">+</button>
          </div>

          <div class="item-actions">
            <button class="icon-btn" title="收藏">♡</button>
            <button class="icon-btn" title="删除" @click="removeItem(item.id)">🗑️</button>
          </div>
        </div>
      </div>

      <!-- 底部结算栏 -->
      <div class="cart-footer">
        <div class="footer-left">
          <label class="select-all">
            <input type="checkbox" v-model="isAllSelected" />
            <span>全选</span>
          </label>
          <span class="total-count">商品件数 <strong>{{ totalQuantity }}</strong></span>
        </div>

        <div class="footer-right">
          <div class="total-price">
            合计 <span>¥{{ totalPrice }}</span>
          </div>
          <div class="tax-tip">所有产品价格均包含增值税</div>
          <button class="checkout-btn">结算</button>
        </div>
      </div>
    </main>
    <!-- 新增深色页脚 -->
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
          <p>© 2025 迪卡侬购物车示例 | 保留所有权利 | 本网站仅为演示项目</p>
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


.cart-container {
  max-width: 1600px;
  margin: 0 auto;
  background: #fff;
  min-height: 100vh;
}


.user-actions {
  /* 关键代码：自动占据左侧所有剩余空间，把自己推到最右边 */
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

/* 主体内容 */
.main-content {
  max-width: 1200px;
  margin: 0 auto;
  /* 添加这一行，数值需大于或等于 .header 的总高度 */
  padding-top: 80px;
  
}

.main-content h2 {
  margin-bottom: 20px;
  font-weight: normal;
}

/* 购物车列表项 */
.cart-list {
  border-top: 1px solid #eee;
}

.cart-item {
  display: flex;
  align-items: center;
  padding: 20px 0;
  border-bottom: 1px solid #eee;
  background-color: #f9f9f9; /* 浅灰背景 */
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

/* 底部结算栏 */
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
  background-color: #0075bf; /* 迪卡侬蓝 */
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

/* 新增页脚样式 */
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
</style>