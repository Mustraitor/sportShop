<script setup>
import { ref, onMounted } from 'vue'
import { getUserList } from '@/api/user.js'

const userList = ref([])

// 请求后端接口
const getUsers = async () => {
  try {
    const data = await getUserList()
    userList.value = data
    console.log('后端返回数据：', data)
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
