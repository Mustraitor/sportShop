<script setup>
import { ref, computed, onMounted, onActivated, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import NavBar from '@/components/NavBar.vue'
import { useHomeStore } from '@/stores/home'
import { getCategoryList } from '@/api/category'
import { getProductList } from '@/api/product'

// 💡 优化 3：引入你的智能图片路由工具
import { optimizeImage, defaultPlaceholder } from '@/utils/image'

const router = useRouter()
const route = useRoute()
const homeStore = useHomeStore()

const MISMATCH_PRODUCT_IDS = [30001, 30002, 30003, 30004, 30005]

// ==================== state ====================
const tree = ref([])
const categoryProductMap = ref({})
const activeId = ref(null)
const loading = ref(false)
const extraLinks = ref([]) // 确保模板里的 v-for 不会因为未定义报错

// ==================== computed ====================
const mainCategories = computed(() => tree.value)

const activeCategory = computed(() =>
  tree.value.find(c => c.id === activeId.value) || tree.value[0] || null
)

// ==================== utils ====================
const setActive = (id) => {
  activeId.value = id
}

const getCatProduct = (id) => categoryProductMap.value[id] || null

/**
 * 💡 优化 4：判断是否有真实的后端图片字符串，供模板中 v-if 条件使用
 */
const hasRealImage = (id) => {
  const img = getCatProduct(id)?.mainImage
  return !!img // 只有非空字符串才返回 true
}

/**
 * 💡 优化 5：获取分类图片路径，并通过工具函数智能路由切换
 */
const getCatImg = (id) => {
  const img = getCatProduct(id)?.mainImage
  // 丢给组件里的路由工具，如果是 LOCAL_MOCK 会自动切到 `/products/文件名.png`
  return img ? optimizeImage(img, 240) : defaultPlaceholder
}

/**
 * 💡 优化 6：图片破损加载失败的二级兜底防御
 */
const handleImageError = (e) => {
  e.target.onerror = null
  e.target.src = defaultPlaceholder
}

const goCategory = (node) => {
  if (!node?.id) return
  router.push({ path: `/category/${node.id}`, query: { name: node.name } })
}

const goProduct = (categoryId) => {
  const item = getCatProduct(categoryId)
  if (item?.productId) {
    router.push(`/product/${item.productId}`)
  }
}

const handleLink = (link) => {
  if (link.path) router.push(link.path)
}

// ==================== 核心：递归 ====================
const collectDescendantIds = (node) => {
  const ids = [node.id]
  node.children?.forEach(child => {
    ids.push(...collectDescendantIds(child))
  })
  return ids
}

const buildCategoryProductMap = (categories, products) => {
  if (!categories?.length || !products?.length) return {}

  const directMap = {}

  products.forEach(p => {
    // 兼容后端可能返回的图片字段
    const actualImage = p.mainImage || p.picUrl || p.img
    
    if (p.categoryId && actualImage && !directMap[p.categoryId]) {
      directMap[p.categoryId] = {
        mainImage: actualImage,
        productId: p.id
      }
    }
  })

  const map = {}

  const walk = (node) => {
    const ids = collectDescendantIds(node)

    for (const id of ids) {
      if (directMap[id]) {
        map[node.id] = directMap[id]
        break
      }
    }

    node.children?.forEach(walk)
  }

  categories.forEach(walk)
  return map
}

// ==================== ⭐ 核心统一加载函数 ====================
const loadAll = async () => {
  loading.value = true

  try {
    const [categoryRes, productRes] = await Promise.all([
      getCategoryList(),
      getProductList({ page: 1, size: 500, status: 1 })
    ])

    // 分类
    tree.value = Array.isArray(categoryRes) ? categoryRes : []

    // 默认 active
    if (!activeId.value && tree.value.length > 0) {
      activeId.value = tree.value[0].id
    }

    // 商品过滤
    const list = (productRes?.list || []).filter(
      item => !MISMATCH_PRODUCT_IDS.includes(item.id)
    )

    categoryProductMap.value = buildCategoryProductMap(tree.value, list)

  } catch (e) {
    console.error('loadAll error:', e)
    tree.value = []
    categoryProductMap.value = {}
  }

  loading.value = false
}

// ==================== 生命周期三重保障 ====================
onMounted(loadAll)

// 返回页面（keep-alive）
onActivated(loadAll)

// 路由变化（关键修复 back/forward）
watch(
  () => route.fullPath,
  () => {
    loadAll()
  }
)
</script>
<template>
  <NavBar />
  <div class="pc-page">
    <div class="pc-wrapper">
      <aside class="pc-sidebar">
        <div class="pc-sidebar-title">商品分类</div>
        <div class="pc-cat-list">
          <div
            v-for="item in mainCategories"
            :key="item.id"
            class="pc-cat-item"
            :class="{ active: activeId === item.id }"
            @mouseenter="setActive(item.id)"
            @click="goCategory(item)"
          >
            <span class="pc-cat-name">{{ item.name }}</span>
            <span class="pc-arrow">›</span>
          </div>

          <div class="pc-divider"></div>

          <div
            v-for="link in (extraLinks || [])"
            :key="link.key"
            class="pc-cat-item pc-link"
            :class="{ special: link.special }"
            @click="handleLink(link)"
          >
            <span class="pc-cat-name">{{ link.name }}</span>
          </div>
        </div>
      </aside>

      <main class="pc-content" v-loading="loading">
        <template v-if="activeCategory">
          <h3 class="pc-group-title">{{ activeCategory.name }}</h3>
          <div v-if="(activeCategory.children || []).length" class="pc-grid">
            <div
              v-for="child in (activeCategory.children || [])"
              :key="child.id"
              class="pc-card"
            >
              <div
                class="pc-card-img"
                :class="{ clickable: !!getCatProduct(child.id)?.productId }"
                @click.stop="goProduct(child.id)"
              >
                <img
                  v-if="hasRealImage(child.id)"
                  :src="getCatImg(child.id)"
                  :alt="child.name"
                  @error="handleImageError"
                />
                <span v-else class="pc-img-ph">{{ child.name.charAt(0) }}</span>
              </div>
              <span class="pc-card-name" @click="goCategory(child)">{{ child.name }}</span>
            </div>
          </div>
          <div v-else class="pc-empty">该分类下暂无子分类</div>
        </template>
        <div v-else class="pc-empty">暂无分类数据</div>
      </main>
    </div>
  </div>
</template>
<style scoped>
.pc-page {
  width: 100%;
  min-height: 100vh;
  background-color: #f8f8f8;
}

.pc-wrapper {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  display: flex;
  align-items: flex-start;
  gap: 20px;
}

.pc-sidebar {
  width: 240px;
  flex-shrink: 0;
  background-color: #f5f5f5;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  overflow: hidden;
}

.pc-sidebar-title {
  padding: 14px 20px;
  font-size: 16px;
  font-weight: 700;
  color: #333;
  border-bottom: 1px solid #e8e8e8;
  background: #fff;
}

.pc-cat-list {
  padding: 8px 0;
}

.pc-cat-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 13px 20px;
  font-size: 14px;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
  border-bottom: 1px solid #efefef;
}

.pc-cat-item:last-child {
  border-bottom: none;
}

.pc-cat-name {
  white-space: nowrap;
}

.pc-arrow {
  color: #bbb;
  font-size: 16px;
  opacity: 0;
  transition: opacity 0.2s;
}

.pc-cat-item:hover,
.pc-cat-item.active {
  background-color: #fff;
  color: #0056b3;
  font-weight: 600;
}

.pc-cat-item.active .pc-arrow,
.pc-cat-item:hover .pc-arrow {
  opacity: 1;
  color: #0056b3;
}

.pc-cat-item:first-child {
  color: #000;
  font-weight: 600;
}

.pc-divider {
  height: 1px;
  background-color: #ddd;
  margin: 6px 16px;
}

.pc-link {
  color: #888;
}
.pc-link .pc-arrow {
  display: none;
}
.pc-link.special {
  color: #d32f2f;
  font-weight: 700;
}
.pc-link:hover {
  color: #0056b3;
  font-weight: 600;
  background-color: #fff;
}
.pc-link.special:hover {
  color: #b71c1c;
}

.pc-content {
  flex: 1;
  min-height: 500px;
}

.pc-group-title {
  font-size: 15px;
  font-weight: 700;
  color: #333;
  margin: 0 0 14px;
  padding-left: 10px;
  border-left: 3px solid #0056b3;
}

.pc-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
}

.pc-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 16px;
  background-color: #fff;
  border: 1px solid #eee;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.25s;
}

.pc-card:hover {
  border-color: #0056b3;
  box-shadow: 0 4px 12px rgba(0, 86, 179, 0.12);
  transform: translateY(-2px);
}

.pc-card-img {
  width: 68px;
  height: 68px;
  flex-shrink: 0;
  border-radius: 8px;
  background-color: #f3f5f7;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.pc-card-img.clickable {
  cursor: pointer;
}

.pc-card-img.clickable:hover {
  box-shadow: 0 2px 8px rgba(0, 86, 179, 0.2);
}

.pc-card-img img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.pc-img-ph {
  font-size: 20px;
  font-weight: 700;
  color: #b8c2cc;
}

.pc-card-name {
  font-size: 14px;
  color: #333;
  line-height: 1.4;
  cursor: pointer;
}

.pc-card-name:hover {
  color: #0056b3;
}

.pc-empty {
  text-align: center;
  color: #999;
  padding: 120px 0;
  font-size: 15px;
}

@media (max-width: 992px) {
  .pc-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .pc-wrapper {
    flex-direction: column;
  }
  .pc-sidebar {
    width: 100%;
  }
}
</style>
