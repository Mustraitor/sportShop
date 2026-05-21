<template>
  <div class="product-detail">
    <!-- 顶部导航 -->
    <div class="top-nav">
      <div class="nav-left">
        <router-link to="/" class="back-link">
          <span class="back-icon">←</span> 返回首页
        </router-link>
      </div>
      <div class="nav-center">商品详情</div>
      <div class="nav-right">
        <router-link to="/cart" class="cart-link">
          <span class="cart-icon">🛒</span>
          <span v-if="cartCount > 0" class="cart-count">{{ cartCount }}</span>
        </router-link>
      </div>
    </div>

    <!-- 商品主图与缩略图 -->
    <div class="product-gallery">
      <div class="main-image">
        <img :src="currentImage" :alt="product.name" @click="showImageZoom = true" />
      </div>
      <div class="thumbnail-list">
        <div
            v-for="(img, index) in product.images"
            :key="index"
            class="thumbnail-item"
            :class="{ active: currentImageIndex === index }"
            @click="changeImage(index)"
        >
          <img :src="img" :alt="'商品图片' + (index+1)" />
        </div>
      </div>
    </div>

    <!-- 商品信息 -->
    <div class="product-info">
      <div class="product-header">
        <h2 class="product-name">{{ product.name }}</h2>
        <div class="product-rating">
          <div class="stars">
            <span v-for="n in 5" :key="n" class="star" :class="{ filled: n <= product.rating }">★</span>
          </div>
          <span class="rating-text">{{ product.rating.toFixed(1) }}分</span>
          <span class="review-count">({{ product.reviewCount }}条评价)</span>
        </div>
      </div>

      <div class="product-price">
        <span class="current-price">¥{{ product.currentPrice.toFixed(2) }}</span>
        <span v-if="product.originalPrice" class="original-price">¥{{ product.originalPrice.toFixed(2) }}</span>
        <span v-if="product.discount" class="discount-tag">{{ product.discount }}折</span>
      </div>

      <div class="product-specs">
        <div class="spec-item">
          <span class="spec-label">商品编号：</span>
          <span class="spec-value">{{ product.sku }}</span>
        </div>
        <div class="spec-item">
          <span class="spec-label">品牌：</span>
          <span class="spec-value">{{ product.brand }}</span>
        </div>
        <div class="spec-item">
          <span class="spec-label">库存：</span>
          <span class="spec-value" :class="{ low: product.stock < 10 }">
            {{ product.stock > 0 ? `有货(${product.stock}件)` : '缺货' }}
          </span>
        </div>
      </div>

      <!-- 商品规格选择 -->
      <div class="product-options">
        <div class="option-item" v-for="(option, index) in product.options" :key="index">
          <div class="option-label">{{ option.label }}</div>
          <div class="option-values">
            <span
                v-for="value in option.values"
                :key="value"
                class="option-value"
                :class="{
                selected: selectedOptions[option.label] === value,
                disabled: isOptionDisabled(option.label, value)
              }"
                @click="selectOption(option.label, value)"
            >
              {{ value }}
            </span>
          </div>
        </div>
      </div>

      <!-- 购买数量 -->
      <div class="quantity-selector">
        <div class="quantity-label">购买数量：</div>
        <div class="quantity-control">
          <button
              class="quantity-btn"
              :disabled="quantity <= 1"
              @click="decreaseQuantity"
          >-</button>
          <input
              type="number"
              v-model.number="quantity"
              min="1"
              :max="maxQuantity"
              class="quantity-input"
              @change="validateQuantity"
          />
          <button
              class="quantity-btn"
              :disabled="quantity >= maxQuantity"
              @click="increaseQuantity"
          >+</button>
        </div>
        <div class="stock-info">最多可购买{{ maxQuantity }}件</div>
      </div>

      <!-- 操作按钮 -->
      <div class="action-buttons">
        <button
            class="btn add-to-cart"
            :disabled="product.stock <= 0"
            @click="addToCart"
        >
          <span class="btn-icon">🛒</span> 加入购物车
        </button>
        <button
            class="btn buy-now"
            :disabled="product.stock <= 0"
            @click="buyNow"
        >
          立即购买
        </button>
      </div>

      <!-- 商品描述 -->
      <div class="product-description">
        <h3>商品详情</h3>
        <div class="description-content" v-html="product.description"></div>
      </div>
    </div>

    <!-- 用户评价 -->
    <div class="product-reviews">
      <div class="reviews-header">
        <h3>用户评价 ({{ product.reviewCount }})</h3>
        <div class="overall-rating">
          <div class="rating-score">{{ product.rating.toFixed(1) }}</div>
          <div class="rating-stars">
            <div class="stars">
              <span v-for="n in 5" :key="n" class="star" :class="{ filled: n <= product.rating }">★</span>
            </div>
            <div class="rating-text">{{ product.rating.toFixed(1) }}分</div>
          </div>
        </div>
      </div>

      <!-- 评价筛选 -->
      <div class="review-filters">
        <span
            v-for="filter in reviewFilters"
            :key="filter.label"
            class="review-filter"
            :class="{ active: activeFilter === filter.value }"
            @click="changeFilter(filter.value)"
        >
          {{ filter.label }}({{ filter.count }})
        </span>
      </div>

      <!-- 评价列表 -->
      <div class="review-list">
        <div v-for="review in filteredReviews" :key="review.id" class="review-item">
          <div class="reviewer-info">
            <div class="reviewer-avatar">{{ review.userName.charAt(0) }}</div>
            <div class="reviewer-details">
              <div class="reviewer-name">{{ review.userName }}</div>
              <div class="review-date">{{ formatDate(review.date) }}</div>
            </div>
            <div class="review-rating">
              <span v-for="n in 5" :key="n" class="star" :class="{ filled: n <= review.rating }">★</span>
            </div>
          </div>
          <div class="review-content">{{ review.content }}</div>
          <div v-if="review.images && review.images.length > 0" class="review-images">
            <img
                v-for="(img, index) in review.images"
                :key="index"
                :src="img"
                :alt="'评价图片' + (index+1)"
                class="review-image"
                @click="showReviewImage(img)"
            />
          </div>
        </div>
      </div>

      <!-- 查看更多评价 -->
      <div class="more-reviews" v-if="product.reviewCount > 3">
        <button class="btn more-reviews-btn" @click="showAllReviews">
          查看全部{{ product.reviewCount }}条评价
        </button>
      </div>
    </div>

    <!-- 底部操作栏 -->
    <div class="bottom-bar">
      <div class="bottom-bar-left">
        <router-link to="/" class="bottom-icon">
          <span class="icon">🏠</span>
          <span class="text">首页</span>
        </router-link>
        <router-link to="/cart" class="bottom-icon">
          <span class="icon">🛒</span>
          <span class="text">购物车</span>
          <span v-if="cartCount > 0" class="cart-badge">{{ cartCount }}</span>
        </router-link>
      </div>
      <div class="bottom-bar-right">
        <button class="bottom-btn add-to-cart-btn" @click="addToCart">加入购物车</button>
        <button class="bottom-btn buy-now-btn" @click="buyNow">立即购买</button>
      </div>
    </div>

    <!-- 图片放大模态框 -->
    <div v-if="showImageZoom" class="image-zoom-modal" @click="showImageZoom = false">
      <div class="modal-content" @click.stop>
        <button class="close-modal" @click="showImageZoom = false">×</button>
        <img :src="currentImage" :alt="product.name" class="zoomed-image" />
      </div>
    </div>

    <!-- 评价图片放大模态框 -->
    <div v-if="showReviewImageModal" class="image-zoom-modal" @click="showReviewImageModal = false">
      <div class="modal-content" @click.stop>
        <button class="close-modal" @click="showReviewImageModal = false">×</button>
        <img :src="currentReviewImage" alt="评价图片" class="zoomed-image" />
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ProductDetailView',
  data() {
    return {
      // 商品数据
      product: {
        id: 1,
        name: '男士运动跑步鞋 轻便透气 防滑耐磨',
        sku: 'SP202305001',
        brand: 'DECATHLON',
        currentPrice: 299.00,
        originalPrice: 399.00,
        discount: 7.5,
        rating: 4.5,
        reviewCount: 245,
        stock: 15,
        images: [
          'https://images.unsplash.com/photo-1542291026-7eec264c27ff?ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D&auto=format&fit=crop&w=800&q=80',
          'https://images.unsplash.com/photo-1606107557195-0e29a4b5b4aa?ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D&auto=format&fit=crop&w=800&q=80',
          'https://images.unsplash.com/photo-1600185365483-26d7a4cc7519?ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D&auto=format&fit=crop&w=800&q=80',
          'https://images.unsplash.com/photo-1605348532760-6753d2c43329?ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D&auto=format&fit=crop&w=800&q=80'
        ],
        options: [
          {
            label: '颜色',
            values: ['黑色', '白色', '蓝色', '灰色']
          },
          {
            label: '尺码',
            values: ['39', '40', '41', '42', '43', '44']
          }
        ],
        description: `
          <p>这款运动跑步鞋专为日常跑步训练设计，提供出色的缓震和支撑性能。</p>
          <h4>产品特点：</h4>
          <ul>
            <li>轻质透气鞋面，提供卓越的透气性</li>
            <li>EVA中底，提供良好的缓震效果</li>
            <li>耐磨橡胶外底，抓地力强，适合多种路面</li>
            <li>符合人体工学的鞋垫设计，提升舒适度</li>
            <li>反光细节设计，提升夜间跑步安全性</li>
          </ul>
          <h4>适用场景：</h4>
          <p>日常跑步训练、健身房锻炼、休闲穿着</p>
        `
      },

      // 评价数据
      reviews: [
        {
          id: 1,
          userName: '跑步爱好者',
          date: '2023-10-15',
          rating: 5,
          content: '鞋子非常轻便，透气性很好，跑了10公里脚也不觉得闷。缓震效果也不错，对膝盖友好。',
          images: [
            'https://images.unsplash.com/photo-1542291026-7eec264c27ff?ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D&auto=format&fit=crop&w=400&q=80'
          ]
        },
        {
          id: 2,
          userName: '运动新手',
          date: '2023-10-10',
          rating: 4,
          content: '外观很时尚，穿着舒适。尺码标准，按平时尺码购买即可。唯一不足是白色不太耐脏。',
          images: []
        },
        {
          id: 3,
          userName: '健身达人',
          date: '2023-10-05',
          rating: 5,
          content: '性价比很高的一款跑鞋，无论是跑步还是日常穿着都很舒适。已经推荐给朋友了。',
          images: [
            'https://images.unsplash.com/photo-1606107557195-0e29a4b5b4aa?ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D&auto=format&fit=crop&w=400&q=80',
            'https://images.unsplash.com/photo-1600185365483-26d7a4cc7519?ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D&auto=format&fit=crop&w-400&q=80'
          ]
        },
        {
          id: 4,
          userName: '马拉松选手',
          date: '2023-09-28',
          rating: 4,
          content: '作为训练鞋很合适，轻量化的设计让长距离训练更轻松。不过对于专业比赛来说，缓冲可能稍显不足。',
          images: []
        },
        {
          id: 5,
          userName: '日常通勤者',
          date: '2023-09-20',
          rating: 5,
          content: '不仅适合运动，日常穿搭也很棒。很百搭，舒适度可以穿一整天。',
          images: []
        }
      ],

      // 用户交互数据
      currentImageIndex: 0,
      selectedOptions: {
        '颜色': '黑色',
        '尺码': '41'
      },
      quantity: 1,
      cartCount: 3,
      showImageZoom: false,
      showReviewImageModal: false,
      currentReviewImage: '',

      // 评价筛选
      reviewFilters: [
        { label: '全部', value: 'all', count: 245 },
        { label: '好评', value: 'good', count: 210 },
        { label: '中评', value: 'medium', count: 25 },
        { label: '差评', value: 'bad', count: 10 },
        { label: '有图', value: 'hasImage', count: 89 }
      ],
      activeFilter: 'all'
    }
  },
  computed: {
    currentImage() {
      return this.product.images[this.currentImageIndex]
    },
    maxQuantity() {
      return Math.min(this.product.stock, 99)
    },
    filteredReviews() {
      if (this.activeFilter === 'all') {
        return this.reviews.slice(0, 3)
      } else if (this.activeFilter === 'good') {
        return this.reviews.filter(review => review.rating >= 4).slice(0, 3)
      } else if (this.activeFilter === 'medium') {
        return this.reviews.filter(review => review.rating === 3).slice(0, 3)
      } else if (this.activeFilter === 'bad') {
        return this.reviews.filter(review => review.rating <= 2).slice(0, 3)
      } else if (this.activeFilter === 'hasImage') {
        return this.reviews.filter(review => review.images && review.images.length > 0).slice(0, 3)
      }
      return this.reviews.slice(0, 3)
    }
  },
  methods: {
    changeImage(index) {
      this.currentImageIndex = index
    },
    selectOption(optionLabel, value) {
      // 检查选项是否可用
      if (this.isOptionDisabled(optionLabel, value)) return

      this.selectedOptions[optionLabel] = value
    },
    isOptionDisabled(optionLabel, value) {
      // 这里可以添加逻辑来检查某些选项组合是否不可用
      // 例如：某些颜色可能没有某些尺码
      if (optionLabel === '尺码' && this.selectedOptions['颜色'] === '白色' && value === '44') {
        return true // 白色没有44码
      }
      return false
    },
    increaseQuantity() {
      if (this.quantity < this.maxQuantity) {
        this.quantity++
      }
    },
    decreaseQuantity() {
      if (this.quantity > 1) {
        this.quantity--
      }
    },
    validateQuantity() {
      if (this.quantity < 1) {
        this.quantity = 1
      } else if (this.quantity > this.maxQuantity) {
        this.quantity = this.maxQuantity
      }
    },
    addToCart() {
      if (this.product.stock <= 0) {
        alert('商品已缺货')
        return
      }

      // 构建商品信息
      const productToAdd = {
        id: this.product.id,
        name: this.product.name,
        price: this.product.currentPrice,
        image: this.currentImage,
        options: { ...this.selectedOptions },
        quantity: this.quantity
      }

      // 这里应该调用API添加到购物车
      console.log('添加到购物车:', productToAdd)

      // 更新购物车数量
      this.cartCount += this.quantity

      // 显示成功消息
      alert(`已添加${this.quantity}件商品到购物车`)
    },
    buyNow() {
      if (this.product.stock <= 0) {
        alert('商品已缺货')
        return
      }

      // 这里应该跳转到订单确认页
      console.log('立即购买:', {
        product: this.product,
        options: this.selectedOptions,
        quantity: this.quantity
      })

      // 模拟跳转到订单页
      alert(`立即购买${this.quantity}件商品，跳转到订单页面`)
    },
    formatDate(dateString) {
      const date = new Date(dateString)
      return `${date.getFullYear()}-${(date.getMonth() + 1).toString().padStart(2, '0')}-${date.getDate().toString().padStart(2, '0')}`
    },
    changeFilter(filter) {
      this.activeFilter = filter
    },
    showReviewImage(img) {
      this.currentReviewImage = img
      this.showReviewImageModal = true
    },
    showAllReviews() {
      alert('显示全部评价')
      // 这里应该跳转到完整评价页面或加载更多评价
    }
  }
}
</script>

<style scoped>
.product-detail {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, sans-serif;
  max-width: 1200px;
  margin: 0 auto;
  padding-bottom: 80px; /* 为底部操作栏留出空间 */
  background-color: #f8f9fa;
}

/* 顶部导航 */
.top-nav {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background-color: white;
  border-bottom: 1px solid #eaeaea;
  position: sticky;
  top: 0;
  z-index: 100;
}

.nav-center {
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.back-link, .cart-link {
  display: flex;
  align-items: center;
  text-decoration: none;
  color: #333;
  font-size: 16px;
}

.back-icon {
  margin-right: 4px;
  font-size: 20px;
}

.cart-link {
  position: relative;
}

.cart-count {
  position: absolute;
  top: -8px;
  right: -8px;
  background-color: #ff4444;
  color: white;
  border-radius: 50%;
  width: 18px;
  height: 18px;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 商品图片区域 */
.product-gallery {
  background-color: white;
  padding: 16px;
  margin-bottom: 12px;
}

.main-image {
  width: 100%;
  height: 350px;
  overflow: hidden;
  border-radius: 8px;
  margin-bottom: 12px;
  cursor: zoom-in;
}

.main-image img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.thumbnail-list {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding-bottom: 8px;
}

.thumbnail-item {
  flex: 0 0 80px;
  height: 80px;
  border-radius: 4px;
  overflow: hidden;
  border: 2px solid transparent;
  cursor: pointer;
  opacity: 0.7;
  transition: all 0.2s;
}

.thumbnail-item.active {
  border-color: #007bff;
  opacity: 1;
}

.thumbnail-item:hover {
  opacity: 1;
}

.thumbnail-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* 商品信息 */
.product-info {
  background-color: white;
  padding: 16px;
  margin-bottom: 12px;
  border-radius: 8px;
}

.product-header {
  margin-bottom: 12px;
}

.product-name {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin-bottom: 8px;
  line-height: 1.4;
}

.product-rating {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #666;
}

.stars {
  display: flex;
  gap: 2px;
}

.star {
  color: #ddd;
  font-size: 16px;
}

.star.filled {
  color: #ffc107;
}

.product-price {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #eee;
}

.current-price {
  font-size: 24px;
  font-weight: 700;
  color: #ff4444;
}

.original-price {
  font-size: 16px;
  color: #999;
  text-decoration: line-through;
}

.discount-tag {
  background-color: #ff4444;
  color: white;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 14px;
  font-weight: 600;
}

.product-specs {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid #eee;
  font-size: 14px;
  color: #666;
}

.spec-item {
  display: flex;
}

.spec-label {
  width: 80px;
  color: #999;
}

.spec-value.low {
  color: #ff4444;
  font-weight: 600;
}

/* 商品规格选择 */
.product-options {
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid #eee;
}

.option-item {
  margin-bottom: 16px;
}

.option-item:last-child {
  margin-bottom: 0;
}

.option-label {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 8px;
}

.option-values {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.option-value {
  padding: 8px 16px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  color: #333;
  cursor: pointer;
  transition: all 0.2s;
  min-width: 60px;
  text-align: center;
}

.option-value:hover {
  border-color: #007bff;
  color: #007bff;
}

.option-value.selected {
  border-color: #007bff;
  background-color: #e6f2ff;
  color: #007bff;
  font-weight: 600;
}

.option-value.disabled {
  background-color: #f5f5f5;
  color: #ccc;
  cursor: not-allowed;
  border-color: #eee;
  text-decoration: line-through;
}

.option-value.disabled:hover {
  border-color: #eee;
  color: #ccc;
}

/* 购买数量 */
.quantity-selector {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid #eee;
}

.quantity-label {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.quantity-control {
  display: flex;
  align-items: center;
  gap: 0;
}

.quantity-btn {
  width: 40px;
  height: 40px;
  background-color: #f8f9fa;
  border: 1px solid #ddd;
  font-size: 18px;
  color: #333;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.quantity-btn:hover:not(:disabled) {
  background-color: #e9ecef;
}

.quantity-btn:disabled {
  color: #ccc;
  cursor: not-allowed;
  background-color: #f8f9fa;
}

.quantity-input {
  width: 60px;
  height: 40px;
  border: 1px solid #ddd;
  border-left: none;
  border-right: none;
  text-align: center;
  font-size: 16px;
  color: #333;
  -moz-appearance: textfield;
}

.quantity-input::-webkit-outer-spin-button,
.quantity-input::-webkit-inner-spin-button {
  -webkit-appearance: none;
  margin: 0;
}

.stock-info {
  font-size: 14px;
  color: #666;
}

/* 操作按钮 */
.action-buttons {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.btn {
  flex: 1;
  padding: 16px;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.btn:disabled {
  background-color: #ccc;
  color: #666;
  cursor: not-allowed;
}

.add-to-cart {
  background-color: #fff;
  color: #007bff;
  border: 1px solid #007bff;
}

.add-to-cart:hover:not(:disabled) {
  background-color: #f0f7ff;
}

.buy-now {
  background-color: #007bff;
  color: white;
}

.buy-now:hover:not(:disabled) {
  background-color: #0056b3;
}

/* 商品描述 */
.product-description {
  background-color: white;
  padding: 20px 16px;
  margin-bottom: 12px;
  border-radius: 8px;
}

.product-description h3 {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #eee;
}

.description-content {
  font-size: 15px;
  line-height: 1.6;
  color: #444;
}

.description-content h4 {
  font-size: 16px;
  margin: 16px 0 8px 0;
  color: #333;
}

.description-content ul {
  margin-left: 20px;
  margin-bottom: 16px;
}

.description-content li {
  margin-bottom: 6px;
}

/* 用户评价 */
.product-reviews {
  background-color: white;
  padding: 20px 16px;
  border-radius: 8px;
}

.reviews-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #eee;
}

.reviews-header h3 {
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.overall-rating {
  display: flex;
  align-items: center;
  gap: 12px;
}

.rating-score {
  font-size: 36px;
  font-weight: 700;
  color: #ff6b00;
}

.rating-stars {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.review-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid #eee;
}

.review-filter {
  padding: 8px 16px;
  border: 1px solid #ddd;
  border-radius: 20px;
  font-size: 14px;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
}

.review-filter:hover {
  border-color: #007bff;
  color: #007bff;
}

.review-filter.active {
  border-color: #007bff;
  background-color: #e6f2ff;
  color: #007bff;
  font-weight: 600;
}

.review-list {
  margin-bottom: 20px;
}

.review-item {
  padding: 16px 0;
  border-bottom: 1px solid #f0f0f0;
}

.review-item:last-child {
  border-bottom: none;
}

.reviewer-info {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.reviewer-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: #007bff;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 18px;
}

.reviewer-details {
  flex: 1;
}

.reviewer-name {
  font-size: 15px;
  font-weight: 600;
  color: #333;
  margin-bottom: 4px;
}

.review-date {
  font-size: 12px;
  color: #999;
}

.review-rating {
  display: flex;
  gap: 2px;
}

.review-content {
  font-size: 15px;
  line-height: 1.5;
  color: #444;
  margin-bottom: 12px;
}

.review-images {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.review-image {
  width: 80px;
  height: 80px;
  border-radius: 4px;
  object-fit: cover;
  cursor: pointer;
  transition: transform 0.2s;
}

.review-image:hover {
  transform: scale(1.05);
}

.more-reviews {
  text-align: center;
  padding-top: 20px;
  border-top: 1px solid #eee;
}

.more-reviews-btn {
  width: auto;
  padding: 12px 32px;
  background-color: #f8f9fa;
  color: #333;
  border: 1px solid #ddd;
  border-radius: 20px;
  font-size: 15px;
}

.more-reviews-btn:hover {
  background-color: #e9ecef;
}

/* 底部操作栏 */
.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background-color: white;
  border-top: 1px solid #eaeaea;
  padding: 12px 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  z-index: 1000;
  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.1);
}

.bottom-bar-left {
  display: flex;
  gap: 24px;
}

.bottom-icon {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-decoration: none;
  color: #666;
  position: relative;
}

.bottom-icon .icon {
  font-size: 20px;
  margin-bottom: 4px;
}

.bottom-icon .text {
  font-size: 12px;
}

.cart-badge {
  position: absolute;
  top: -5px;
  right: -5px;
  background-color: #ff4444;
  color: white;
  border-radius: 50%;
  width: 18px;
  height: 18px;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.bottom-bar-right {
  display: flex;
  gap: 12px;
  flex: 1;
  max-width: 300px;
}

.bottom-btn {
  flex: 1;
  padding: 12px;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.add-to-cart-btn {
  background-color: #fff;
  color: #007bff;
  border: 1px solid #007bff;
}

.add-to-cart-btn:hover {
  background-color: #f0f7ff;
}

.buy-now-btn {
  background-color: #007bff;
  color: white;
}

.buy-now-btn:hover {
  background-color: #0056b3;
}

/* 图片放大模态框 */
.image-zoom-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.9);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  padding: 20px;
}

.modal-content {
  position: relative;
  max-width: 90%;
  max-height: 90%;
}

.close-modal {
  position: absolute;
  top: -40px;
  right: 0;
  background: none;
  border: none;
  color: white;
  font-size: 40px;
  cursor: pointer;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
}

.zoomed-image {
  max-width: 100%;
  max-height: 80vh;
  object-fit: contain;
}

/* 响应式设计 */
@media (min-width: 768px) {
  .product-detail {
    padding-bottom: 0;
  }

  .top-nav {
    padding: 16px 24px;
  }

  .product-gallery {
    display: flex;
    gap: 20px;
    padding: 24px;
  }

  .main-image {
    flex: 1;
    height: 400px;
  }

  .thumbnail-list {
    flex-direction: column;
    width: 100px;
    overflow-y: auto;
    overflow-x: hidden;
  }

  .thumbnail-item {
    flex: 0 0 80px;
    width: 80px;
  }

  .product-info, .product-description, .product-reviews {
    padding: 24px;
    margin-bottom: 16px;
  }

  .action-buttons {
    max-width: 400px;
    margin-left: auto;
    margin-right: auto;
  }

  .bottom-bar {
    display: none; /* 在桌面端隐藏底部操作栏 */
  }
}
</style>