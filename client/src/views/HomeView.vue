<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'

const userList = ref([])

// 请求后端接口
const getUsers = async () => {
  try {
    const res = await axios.get('/api/user/list')
    userList.value = res.data.data
    console.log('后端返回数据：', res.data.data)
  } catch (error) {
    console.error(error)
  }
}

onMounted(() => {
  getUsers()
})
</script>

<template>
  <div>
    <h2>用户列表测试</h2>

    <div v-for="item in userList" :key="item.userId">
      {{ item.userName }} 
    </div>
  </div>
</template>
