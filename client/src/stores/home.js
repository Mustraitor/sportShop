import { defineStore } from 'pinia'
import { ref } from 'vue'

// 首页顶部选项卡状态：'deals' = 优惠专区（主展示区）, 'category' = 商品分类
export const useHomeStore = defineStore('home', () => {
    const activeTab = ref('deals')

    const setTab = (tab) => {
        activeTab.value = tab
    }

    return { activeTab, setTab }
})
