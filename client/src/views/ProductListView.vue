<template>
  <div class="page-container">
    <NavBar />

    <div class="list-wrapper">
      <div class="breadcrumb">
        <router-link to="/" class="crumb">首页</router-link>
        <template v-for="(node, idx) in breadcrumb" :key="node.id">
          <span class="sep">/</span>
          <span
            class="crumb"
            :class="{ current: idx === breadcrumb.length - 1 }"
            @click="goCategory(node)"
          >{{ node.name }}</span>
        </template>
      </div>

      <div class="content-grid">
        <aside class="sidebar">
          <h3 class="sidebar-title">商品分类</h3>
          <ul class="cat-tree">
            <li v-for="root in tree" :key="root.id" class="root-item">
              <div
                class="cat-l1"
                :class="{ active: expandedId === root.id }"
                @click="toggleExpand(root.id)"
              >
                {{ root.name }}
                <span class="arrow-icon">{{ expandedId === root.id ? '▼' : '›' }}</span>
              </div>
              
              <transition name="fade">
                <ul v-if="expandedId === root.id && root.children" class="cat-children">
                  <li v-for="child in root.children" :key="child.id" class="cat-l2-wrapper">
                    <div class="cat-l2" @click="goCategory(child)">{{ child.name }}</div>
                    
                    <div class="cat-l3-list" v-if="child.children && child.children.length">
                      <span
                        v-for="leaf in child.children"
                        :key="leaf.id"
                        class="cat-l3"
                        @click.stop="goCategory(leaf)"
                      >{{ leaf.name }}</span>
                    </div>
                  </li>
                </ul>
              </transition>
            </li>
          </ul>
        </aside>

        <main class="main"> 
          <div class="main-header">
            <div class="title-area">
              <h2 class="cat-name">{{ currentName }}</h2>
              <span class="total-count" v-if="!loading && !hasSubCategories">共 {{ total }} 件商品</span>
            </div>
            
            <div class="sort-bar" v-if="!hasSubCategories">
              <span
                v-for="opt in sortOptions"
                :key="opt.value"
                class="sort-item"
                :class="{ active: sort === opt.value }"
                @click="changeSort(opt.value)"
              >{{ opt.label }}</span>
            </div>
          </div>

          <template v-if="hasSubCategories">
            <div class="pc-grid" style="display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 20px; padding: 20px 0;">
              <div
                v-for="child in quickChildren"
                :key="child.id"
                class="pc-card"
                style="display: flex; flex-direction: column; align-items: center; cursor: pointer; background: #fff; padding: 15px; border-radius: 8px; transition: transform 0.2s;"
                @click="goCategory(child)" 
              >
                <div class="pc-card-img" style="width: 120px; height: 120px; display: flex; align-items: center; justify-content: center; background: #f8f9fa; border-radius: 6px; overflow: hidden; margin-bottom: 10px;">
                  <img
                    v-if="child.mainImage || child.picUrl || child.img || categoryProductMap[child.id]"
                    :src="getCatImg(child)"
                    :alt="child.name"
                    style="width: 100%; height: 100%; object-fit: cover;"
                    @error="handleImageError"
                  />
                  <span v-else class="pc-img-ph" style="font-size: 24px; color: #999; font-weight: bold;">
                    {{ child.name.charAt(0) }}
                  </span>
                </div>
                <span class="pc-card-name" style="font-size: 14px; color: #333; font-weight: 500;">{{ child.name }}</span>
              </div>
            </div>
          </template>

          <template v-else>
            <div class="quick-children" v-if="quickChildren.length">
              <span
                v-for="c in quickChildren"
                :key="c.id"
                class="quick-chip"
                @click="goCategory(c)"
              >{{ c.name }}</span>
            </div>

            <div v-if="loading" class="state-box">加载中...</div>

            <div v-else-if="!products.length" class="state-box">
              该分类下暂无商品
            </div>

            <div v-else class="product-grid">
              <div
                v-for="item in products"
                :key="item.id"
                class="product-card"
                @click="goDetail(item.id)"
              >
                <div class="img-box">
                  <img 
                    :src="optimizeImage(item.mainImage || item.picUrl || item.img, 400)" 
                    :alt="item.name" 
                    @error="onImgError" 
                  />
                  <span v-if="item.status === 0" class="off-shelf">已下架</span>
                </div>
                <div class="info">
                  <div class="name" :title="item.name">{{ item.name }}</div>
                  <div class="price">¥{{ formatPrice(item.price) }}</div>
                </div>
              </div>
            </div>

            <div v-if="!loading && pages > 1" class="pagination">
              <button class="page-btn" :disabled="page <= 1" @click="changePage(page - 1)">上一页</button>
              <span class="page-info">{{ page }} / {{ pages }}</span>
              <button class="page-btn" :disabled="page >= pages" @click="changePage(page + 1)">下一页</button>
            </div>
          </template>
        </main>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import NavBar from '@/components/NavBar.vue'
import { getCategoryList } from '@/api/category'
import { getProductList } from '@/api/product'

import { optimizeImage, defaultPlaceholder } from '@/utils/image'

const route = useRoute()
const router = useRouter()

const tree = ref([])
const products = ref([])
const total = ref(0)
const pages = ref(0)
const page = ref(1)
const size = ref(12)
const sort = ref('default')
const loading = ref(false)

// 🌟 修改点 2：定义存储借用图片的字典
const categoryProductMap = ref({})

const sortOptions = [
  { label: '综合', value: 'default' },
  { label: '价格从低到高', value: 'price_asc' },
  { label: '价格从高到低', value: 'price_desc' }
]

const activeId = computed(() => Number(route.params.id))

const findNode = (nodes, id) => {
  for (const n of nodes) {
    if (n.id === id) return n
    if (n.children && n.children.length) {
      const found = findNode(n.children, id)
      if (found) return found
    }
  }
  return null
}

const findPath = (nodes, id, path = []) => {
  for (const n of nodes) {
    const next = [...path, n]
    if (n.id === id) return next
    if (n.children && n.children.length) {
      const found = findPath(n.children, id, next)
      if (found) return found
    }
  }
  return null
}

const currentNode = computed(() => findNode(tree.value, activeId.value))
const currentName = computed(() => currentNode.value?.name || route.query.name || '全部商品')
const breadcrumb = computed(() => findPath(tree.value, activeId.value) || [])
const quickChildren = computed(() => currentNode.value?.children || [])

const hasSubCategories = computed(() => {
  const children = currentNode.value?.children
  return Array.isArray(children) && children.length > 0
})

const formatPrice = (p) => {
  const num = Number(p)
  return isNaN(num) ? p : num.toFixed(2)
}

const onImgError = (e) => {
  e.target.onerror = null
  e.target.src = defaultPlaceholder
}

// 🌟 修改点 3：加上抓取商品图片赋给分类的逻辑
const collectDescendantIds = (node) => {
  const ids = [node.id]
  node.children?.forEach(child => {
    ids.push(...collectDescendantIds(child))
  })
  return ids
}

const buildCategoryProductMap = (categories, productsData) => {
  if (!categories?.length || !productsData?.length) return {}
  const directMap = {}
  
  // 遍历所有商品，提取它的主图并绑定给它对应的分类ID
  productsData.forEach(p => {
    const actualImage = p.mainImage || p.picUrl || p.img
    if (p.categoryId && actualImage && !directMap[p.categoryId]) {
      directMap[p.categoryId] = actualImage
    }
  })
  
  const map = {}
  // 递归处理：如果一个分类本身没有图片，就从它的子孙分类（包含的商品）里借一张
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

const loadProducts = async () => {
  if (!activeId.value) return
  loading.value = true
  try {
    const data = await getProductList({
      page: page.value,
      size: size.value,
      categoryId: activeId.value,
      status: 1,
      sort: sort.value === 'default' ? undefined : sort.value
    })
    products.value = data?.list || []
    total.value = data?.total || 0
    pages.value = data?.pages || 0
  } catch (e) {
    products.value = []
    total.value = 0
    pages.value = 0
  } finally {
    loading.value = false
  }
}

const goCategory = (node) => {
  if (!node || node.id === activeId.value) return
  router.push({ path: `/category/${node.id}`, query: { name: node.name } })
}

const goDetail = (id) => {
  router.push(`/product/${id}`)
}

const changeSort = (value) => {
  if (sort.value === value) return
  sort.value = value
  page.value = 1
  loadProducts()
}

const changePage = (p) => {
  if (p < 1 || p > pages.value) return
  page.value = p
  loadProducts()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

onMounted(async () => {
  try {
    // 🌟 修改点 4：初始化时顺带查前500个商品来收集图片
    const [catRes, prodRes] = await Promise.all([
      getCategoryList(),
      getProductList({ page: 1, size: 500, status: 1 }) 
    ])
    tree.value = catRes || []
    categoryProductMap.value = buildCategoryProductMap(tree.value, prodRes?.list || [])
  } catch (e) {
    tree.value = []
  }
  loadProducts()
})

const expandedId = ref(null)

const toggleExpand = (id) => {
  expandedId.value = (expandedId.value === id) ? null : id
}

// 从 categoryProductMap 字典里取值
const getCatImg = (item) => {
  const img = item.mainImage || item.picUrl || item.img || categoryProductMap.value[item.id]
  return img ? optimizeImage(img, 240) : defaultPlaceholder
}

const handleImageError = (e) => {
  e.target.onerror = null
  e.target.src = defaultPlaceholder
}

watch(() => route.path, () => {
}, { immediate: true })

watch(activeId, () => {
  page.value = 1
  sort.value = 'default'
  loadProducts()
})
</script>



<style scoped>
.page-container {
  width: 100%;
  background-color: #f8f8f8;
  min-height: 100vh;
}

.list-wrapper {
  max-width: 1500px;
  margin: 0 auto;
  padding: 20px;
}

/* 面包屑 */
.breadcrumb {
  font-size: 13px;
  color: #888;
  margin-bottom: 16px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
}
.breadcrumb .sep { color: #ccc; }
.breadcrumb .crumb {
  color: #666;
  cursor: pointer;
  text-decoration: none;
  transition: color 0.2s;
}
.breadcrumb .crumb:hover { color: #0056b3; }
.breadcrumb .crumb.current { color: #333; font-weight: 600; cursor: default; }

.content-grid {
  display: grid;
  grid-template-columns: 220px 1fr;
  gap: 20px;
  align-items: start;
}

/* 左侧分类栏 */
.sidebar {
  background: #fff;
  border-radius: 8px;
  border: 1px solid #eee;
  padding: 14px 0;
  position: sticky;
  top: 100px;
}
.sidebar-title {
  font-size: 15px;
  font-weight: 700;
  color: #333;
  padding: 0 18px 12px;
  margin: 0;
  border-bottom: 1px solid #f0f0f0;
}
.cat-tree { list-style: none; margin: 0; padding: 6px 0 0; }
.cat-l1 {
  font-size: 14px;
  font-weight: 600;
  color: #222;
  padding: 10px 18px;
  cursor: pointer;
  transition: all 0.2s;
}
.cat-l1:hover, .cat-l1.active { color: #0056b3; background: #f2f7fb; }
.cat-children { list-style: none; margin: 0; padding: 0 0 6px; }
.cat-l2 {
  font-size: 13px;
  color: #666;
  padding: 7px 18px 7px 30px;
  cursor: pointer;
  transition: all 0.2s;
}
.cat-l2:hover, .cat-l2.active { color: #0056b3; background: #f7fafd; }
.cat-l3 {
  display: inline-block;
  font-size: 12px;
  color: #999;
  margin: 4px 8px 0 0;
  padding: 2px 8px;
  border-radius: 10px;
  background: #f3f3f3;
}
.cat-l3:hover, .cat-l3.active { color: #fff; background: #0056b3; }

/* 右侧主体 */
.main {
  background: #fff;
  border-radius: 8px;
  border: 1px solid #eee;
  padding: 20px;
  min-height: 400px;
}
.main-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  flex-wrap: wrap;
  gap: 12px;
  border-bottom: 1px solid #f0f0f0;
  padding-bottom: 14px;
}
.title-area { display: flex; align-items: baseline; gap: 12px; }
.cat-name { font-size: 20px; font-weight: 700; color: #333; margin: 0; }
.total-count { font-size: 13px; color: #999; }

.sort-bar { display: flex; gap: 6px; }
.sort-item {
  font-size: 13px;
  color: #666;
  padding: 6px 12px;
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.2s;
}
.sort-item:hover { background: #f2f7fb; color: #0056b3; }
.sort-item.active { background: #0056b3; color: #fff; }

.quick-children {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 16px 0 4px;
}
.quick-chip {
  font-size: 13px;
  color: #555;
  padding: 5px 14px;
  border: 1px solid #e3e3e3;
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.2s;
}
.quick-chip:hover { border-color: #0056b3; color: #0056b3; }

.state-box {
  text-align: center;
  color: #999;
  font-size: 15px;
  padding: 80px 0;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 18px;
  margin-top: 18px;
}
.product-card {
  border: 1px solid #eee;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.25s;
  background: #fff;
}
.product-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.1);
  border-color: #0056b3;
}
.img-box {
  position: relative;
  width: 100%;
  aspect-ratio: 1 / 1;
  background: #f7f7f7;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}
.img-box img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}
.off-shelf {
  position: absolute;
  top: 8px;
  left: 8px;
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 4px;
}
.info { padding: 12px; }
.name {
  font-size: 14px;
  color: #333;
  line-height: 1.4;
  height: 2.8em;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
.price {
  margin-top: 8px;
  color: #e60012;
  font-size: 17px;
  font-weight: 700;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 18px;
  margin-top: 30px;
}
.page-btn {
  padding: 8px 18px;
  border: 1px solid #ddd;
  background: #fff;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  color: #333;
  transition: all 0.2s;
}
.page-btn:hover:not(:disabled) { border-color: #0056b3; color: #0056b3; }
.page-btn:disabled { color: #ccc; cursor: not-allowed; }
.page-info { font-size: 14px; color: #666; }

@media (max-width: 768px) {
  .content-grid { grid-template-columns: 1fr; }
  .sidebar { position: static; display:none; }
}
.cat-tree { list-style: none; padding: 0; margin: 0; }
.root-item { border-bottom: 1px solid #eee; }

.cat-l1 {
  padding: 15px 20px;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  font-weight: bold;
  background: #fff;
  transition: 0.3s;
}

.cat-l1:hover { background: #f9f9f9; }

.cat-l1.active { color: #0056b3; }

.cat-children {
  background: #fdfdfd;
  padding: 10px 0;
}

.cat-l2 {
  padding: 8px 20px 8px 30px;
  font-size: 14px;
  color: #555;
  cursor: pointer;
}

.cat-l3-list {
  padding: 0 20px 10px 40px;
  display: flex;
  flex-wrap: wrap; /* 关键：自动换行 */
  gap: 8px;
}

.cat-l3 {
  font-size: 12px;
  padding: 3px 10px;
  background: #eee;
  border-radius: 12px;
  cursor: pointer;
  color: #666;
}

.cat-l3:hover { background: #0056b3; color: #fff; }

/* 展开动画 */
.fade-enter-active, .fade-leave-active { transition: all 0.3s; }
.fade-enter-from, .fade-leave-to { opacity: 0; height: 0; overflow: hidden; }
</style>