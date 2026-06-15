<template>
  <el-drawer
    v-model="visible"
    direction="rtl"
    :size="drawerSize"
    :before-close="handleClose"
    custom-class="sku-drawer-container"
    :with-header="false"
  >
    <div class="sku-drawer-content" v-if="product" v-loading="isDetailLoading">
      
      <div class="sku-header">
        <img :src="currentSkuImage" class="sku-avatar" />
        <div class="sku-header-info">
          <p class="sku-price">¥{{ (currentPrice || 0).toFixed(2) }}</p>
          <p class="sku-stock">库存: {{ currentStock }} 件</p>
          <p class="sku-selected">
            {{ selectedSkuName ? `已选: ${selectedSkuName}` : '请选择商品规格' }}
          </p>
        </div>
      </div>

      <el-divider class="sku-divider" />

      <div class="sku-body">
        
        <div class="spec-group" v-if="skuList && skuList.length > 0">
          <h4 class="spec-title">规格选择</h4>
          <div class="spec-options">
            <button
              v-for="sku in skuList"
              :key="sku.id"
              class="spec-opt-btn"
              :class="{ 
                'is-active': selectedSkuId === sku.id,
                'is-disabled': isSkuDisabled(sku) 
              }"
              :disabled="isSkuDisabled(sku)"
              @click="selectSku(sku)"
            >
              {{ sku.skuName }}
            </button>
          </div>
        </div>

        <div class="quantity-section">
          <span class="qty-label">购买数量</span>
          <el-input-number 
            v-model="quantity" 
            :min="1" 
            :max="currentStock > 0 ? currentStock : 1" 
            size="small"
            @change="validateQuantity"
          />
        </div>
      </div>

      <div class="sku-footer">
        <el-button 
          type="primary" 
          class="sku-submit-btn" 
          :loading="submitting"
          :disabled="currentStock <= 0 || (skuList.length > 0 && !selectedSkuId)"
          @click="handleSubmit"
        >
          确定加入购物车
        </el-button>
      </div>
    </div>
  </el-drawer>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { optimizeImage } from '@/utils/image'
import { getProductDetail } from '@/api/product' 

const props = defineProps({
  show: { type: Boolean, default: false },
  product: { type: Object, default: null },
  submitting: { type: Boolean, default: false }
})

const emit = defineEmits(['update:show', 'confirm'])

const OSS_BASE_URL = 'https://sportshop-pictures.oss-cn-beijing.aliyuncs.com/'

const visible = ref(false)
const quantity = ref(1)

// 💡 替换原先复杂的 selectedSpecs 对象，现在只需要记住选中的 skuId
const selectedSkuId = ref(null) 

const fullProductDetail = ref(null)
const isDetailLoading = ref(false)

watch(() => props.show, async (newVal) => {
  visible.value = newVal
  if (newVal && props.product) {
    quantity.value = 1
    selectedSkuId.value = null // 清空选中状态
    fullProductDetail.value = null
    await fetchCompleteProductDetail()
  }
})

const fetchCompleteProductDetail = async () => {
  if (props.product.skus || props.product.skuList) {
    fullProductDetail.value = props.product
    initDefaultSku()
    return
  }

  isDetailLoading.value = true
  try {
    const res = await getProductDetail(props.product.id)
    fullProductDetail.value = res.data || res
    initDefaultSku() 
  } catch (error) {
    console.error('动态补充商品规格数据失败:', error)
    ElMessage.error('获取商品详情失败，请稍后重试')
    fullProductDetail.value = props.product
  } finally {
    isDetailLoading.value = false
  }
}

// 提取 SKU 数组
const skuList = computed(() => {
  return fullProductDetail.value?.skus || fullProductDetail.value?.skuList || []
})

// 💡 自动选中默认规格（逻辑同详情页）
const initDefaultSku = () => {
  if (skuList.value.length > 0) {
    // 优先选中第一个有库存的，如果没有，就选中第一个
    const firstAvailable = skuList.value.find(s => s.stock > 0) || skuList.value[0]
    selectedSkuId.value = firstAvailable?.id ?? null
  }
}

// 获取当前选中的完整 SKU 对象
const selectedSku = computed(() => {
  if (!selectedSkuId.value || !skuList.value.length) return null
  return skuList.value.find(s => s.id === selectedSkuId.value) || null
})

// === 核心数据计算 ===
const currentPrice = computed(() => {
  return selectedSku.value?.price ?? fullProductDetail.value?.price ?? props.product?.price ?? 0
})

const currentStock = computed(() => {
  if (selectedSku.value) return selectedSku.value.stock
  return fullProductDetail.value?.stock ?? props.product?.stock ?? 0
})

const selectedSkuName = computed(() => {
  return selectedSku.value?.skuName || ''
})

const currentSkuImage = computed(() => {
  const rawPath = selectedSku.value?.skuImage || selectedSku.value?.mainImage || fullProductDetail.value?.mainImage || props.product?.mainImage
  if (!rawPath) return `${OSS_BASE_URL}upload/default.png`
  
  const fullUrl = rawPath.startsWith('http') 
    ? rawPath 
    : `${OSS_BASE_URL.replace(/\/$/, '')}/${rawPath.replace(/^\/+/, '')}`
    
  return optimizeImage(fullUrl, 160)
})

// === 交互逻辑 ===

const isSkuDisabled = (sku) => !sku || sku.stock <= 0

// 点击选择规格
const selectSku = (sku) => {
  if (isSkuDisabled(sku)) return
  selectedSkuId.value = sku.id
  
  // 如果当前选的数量大于新规格的库存，自动降为库存最大值
  if (quantity.value > sku.stock) {
    quantity.value = Math.max(1, sku.stock)
  }
}

// 校验数量（防止手动输入非法值）
const validateQuantity = (val) => {
  if (val > currentStock.value) {
    quantity.value = currentStock.value
  }
}

const handleClose = () => {
  emit('update:show', false)
}

const handleSubmit = () => {
  if (skuList.value.length > 0 && !selectedSkuId.value) {
    ElMessage.warning('请选择商品规格')
    return
  }
  
  if (currentStock.value <= 0) {
    ElMessage.warning('该规格商品库存不足')
    return
  }

  // 🛡️ 保持上一次的 productId 防丢机制
  const finalProductId = fullProductDetail.value?.id || 
                         fullProductDetail.value?.productId || 
                         props.product?.id || 
                         props.product?.productId

  emit('confirm', {
    productId: finalProductId, 
    skuId: selectedSkuId.value || (fullProductDetail.value?.defaultSkuId || finalProductId),
    quantity: quantity.value,
    skuName: selectedSkuName.value,
    price: currentPrice.value
  })
}

const drawerSize = computed(() => {
  return window.innerWidth >= 768 ? '500px' : '70%'
})
</script>

<style lang="scss">
/* 使用全局覆写或确保能渗透组件样式 */
.sku-drawer-container {
  border-top-left-radius: 16px !important;
  border-top-right-radius: 16px !important;
  overflow: hidden;
  
  .el-drawer__body {
    padding: 0 !important;
  }
}
</style>

<style lang="scss" scoped>
.sku-drawer-content {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: white;
  padding: 20px 16px;
  box-sizing: border-box;
}

.sku-header {
  display: flex;
  gap: 14px;
  align-items: flex-end;
  
  .sku-avatar {
    width: 88px;
    height: 88px;
    border-radius: 8px;
    object-fit: cover;
    border: 1px solid #f0f0f0;
    box-shadow: 0 2px 6px rgba(0,0,0,0.05);
  }
  
  .sku-header-info {
    flex: 1;
    .sku-price {
      color: #ff4d4f; /* 现代高亮红 */
      font-size: 22px;
      font-weight: 600;
      margin: 0 0 4px 0;
    }
    .sku-stock {
      font-size: 12px;
      color: #8c8c8c;
      margin: 0 0 4px 0;
    }
    .sku-selected {
      font-size: 13px;
      color: #262626;
      margin: 0;
    }
  }
}

.sku-divider {
  margin: 16px 0;
}

.sku-body {
  flex: 1;
  overflow-y: auto;
  padding-bottom: 20px;
}

.spec-group {
  margin-bottom: 18px;
  
  .spec-title {
    font-size: 14px;
    color: #262626;
    margin: 0 0 8px 0;
    font-weight: 500;
  }
  
  .spec-options {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
  }
  
  .spec-opt-btn {
    padding: 6px 14px;
    border-radius: 6px;
    border: 1px solid #d9d9d9;
    background: #f5f5f5;
    color: #595959;
    font-size: 13px;
    cursor: pointer;
    transition: all 0.2s ease;
    
    &:hover {
      border-color: #40a9ff;
    }
    
    &.is-active {
      background: #e6f7ff;
      border-color: #1890ff;
      color: #1890ff;
      font-weight: 500;
    }
  }
}

.quantity-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 24px;
  padding-top: 14px;
  border-top: 1px dashed #f0f0f0;
  
  .qty-label {
    font-size: 14px;
    color: #262626;
    font-weight: 500;
  }
}

.sku-footer {
  padding-top: 10px;
  padding-bottom: calc(4px + env(safe-area-inset-bottom));
  
  .sku-submit-btn {
    width: 100%;
    height: 42px;
    border-radius: 21px;
    font-size: 15px;
    font-weight: 500;
    background: #1890ff; /* 对应你系统的主色调 */
    border-color: #1890ff;
  }
}
</style>