<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

const isSearchShow = ref(false)
const searchQuery = ref('')
const historyList = ref(['鞋子'])
const hotSearch = ref(['rcr105', '泳裤', '头灯', '儿童自行车', '帐篷', 'mh500'])

// 点击外部关闭浮层
const searchRef = ref(null)
const handleClickOutside = (event) => {
  if (searchRef.value && !searchRef.value.contains(event.target)) {
    isSearchShow.value = false
  }
}

onMounted(() => document.addEventListener('click', handleClickOutside))
onUnmounted(() => document.removeEventListener('click', handleClickOutside))

const vFocus = { mounted: (el) => el.focus() }
</script>

<template>
  <div class="search-wrapper" ref="searchRef">
    <!-- 搜索输入框 -->
    <div class="search-input-group" :class="{ 'is-active': isSearchShow }">
      <input 
        v-model="searchQuery"
        type="text" 
        placeholder="搜索商品..." 
        @focus="isSearchShow = true"
      />
      <button class="search-btn">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line></svg>
      </button>
    </div>

    <!-- 下拉面板 (仅显示“一半”内容) -->
    <Transition name="fade">
      <div v-if="isSearchShow" class="search-dropdown">
        <section class="section">
          <div class="header">
            <span>历史搜索</span>
            <span class="action">清除</span>
          </div>
          <div class="tags">
            <span v-for="tag in historyList" :key="tag" class="tag">{{ tag }}</span>
          </div>
        </section>

        <section class="section">
          <div class="header">
            <span>依粉热搜</span>
            <span class="action">换一换</span>
          </div>
          <div class="tags">
            <span v-for="tag in hotSearch" :key="tag" class="tag">{{ tag }}</span>
          </div>
        </section>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.search-wrapper {
  position: relative;
  width: 400px; /* 或者根据你的布局定宽度 */
}

.search-input-group {
  display: flex;
  align-items: center;
  background: #f5f5f5;
  border-radius: 20px;
  padding: 5px 15px;
  border: 1px solid transparent;
  transition: all 0.3s;
}

.search-input-group.is-active {
  background: #fff;
  border-color: #ddd;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.search-input-group input {
  flex: 1;
  border: none;
  background: transparent;
  outline: none;
  height: 30px;
}

.search-btn { background: none; border: none; cursor: pointer; color: #666; }

/* 下拉浮层样式 */
.search-dropdown {
  position: absolute;
  top: 110%; /* 紧贴输入框下方 */
  left: 0;
  right: 0;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.15);
  padding: 15px;
  z-index: 100;
}

.section { margin-bottom: 15px; }
.header {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #999;
  margin-bottom: 10px;
}
.action { cursor: pointer; }
.action:hover { color: #333; }

.tags { display: flex; flex-wrap: wrap; gap: 8px; }
.tag {
  background: #f5f5f5;
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 12px;
  color: #444;
  cursor: pointer;
}
.tag:hover { background: #eee; }

/* 动画 */
.fade-enter-active, .fade-leave-active { transition: all 0.2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; transform: translateY(-10px); }
</style>