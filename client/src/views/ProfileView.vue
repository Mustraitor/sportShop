<script setup>
import { ref, reactive, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'
import NavBar from '@/components/NavBar.vue'

const activeTab = ref('info')
const showAddressDialog = ref(false)
const isEdit = ref(false) // 新增：控制是否为编辑模式

// 个人信息数据
const userInfo = reactive({
  userName: '',
  nickName: '',
  email: '',
  phonenumber: '',
  sex: '0'
})

const addressList = ref([])
const addressForm = reactive({
  id: null,
  receiverName: '',
  receiverPhone: '',
  detailAddress: ''
})

// 获取用户信息
const getUserProfile = async () => {
  try {
    const response = await request.get('/user/list') 
    if (response && response.length > 0) {
      const firstUser = response[0] 
      Object.assign(userInfo, {
        userName: firstUser.userName,
        nickName: firstUser.nickName,
        email: firstUser.email,
        phonenumber: '15888888888',
        sex: '0'
      })
    }
  } catch (error) {
    console.error('获取用户信息失败', error)
  }
}

// 提交个人信息修改
const handleUpdateUser = async () => {
  try {
    // 这里暂时请求之前的接口，如果后端没写更新接口会报404
    await request.put('/system/user/profile', userInfo)
    ElMessage.success('个人信息更新成功')
    isEdit.value = false // 保存成功后，切换回展示模式
  } catch (error) {
    console.error('更新失败', error)
  }
}

const saveAddress = async () => {
  ElMessage.success('地址保存成功（模拟）')
  showAddressDialog.value = false
}

const editAddress = (row) => {
  Object.assign(addressForm, row)
  showAddressDialog.value = true
}

const deleteAddress = (id) => {
  ElMessage.warning('模拟删除地址')
}

onMounted(() => {
  getUserProfile()
})
</script>


<template>
  <NavBar/>
  <div class="profile-container">
    <el-card class="profile-card">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="个人信息" name="info">
          
          <div v-if="!isEdit" class="display-mode">
            <div class="display-header">
              <h3 class="section-title">基本信息</h3>
              <el-button type="primary" plain @click="isEdit = true">编辑资料</el-button>
            </div>
            
            <el-descriptions :column="1" border class="info-table">
              <el-descriptions-item label="用户账号">{{ userInfo.userName }}</el-descriptions-item>
              <el-descriptions-item label="用户昵称">{{ userInfo.nickName }}</el-descriptions-item>
              <el-descriptions-item label="电子邮箱">{{ userInfo.email }}</el-descriptions-item>
              <el-descriptions-item label="手机号码">{{ userInfo.phonenumber }}</el-descriptions-item>
              <el-descriptions-item label="性别">
                <el-tag size="small">{{ userInfo.sex === '1' ? '女' : '男' }}</el-tag>
              </el-descriptions-item>
            </el-descriptions>
          </div>

          <div v-else class="edit-mode">
            <div class="display-header">
              <h3 class="section-title">修改个人资料</h3>
            </div>
            <el-form :model="userInfo" label-width="100px" class="info-form">
              <el-form-item label="用户账号">
                <el-input v-model="userInfo.userName" disabled></el-input>
              </el-form-item>
              <el-form-item label="用户昵称">
                <el-input v-model="userInfo.nickName"></el-input>
              </el-form-item>
              <el-form-item label="邮箱">
                <el-input v-model="userInfo.email"></el-input>
              </el-form-item>
              <el-form-item label="手机号码">
                <el-input v-model="userInfo.phonenumber"></el-input>
              </el-form-item>
              <el-form-item label="性别">
                <el-radio-group v-model="userInfo.sex">
                  <el-radio label="0">男</el-radio>
                  <el-radio label="1">女</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="handleUpdateUser" class="decathlon-btn">保存修改</el-button>
                <el-button @click="isEdit = false">取消</el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-tab-pane>

        <el-tab-pane label="收货地址" name="address">
          <el-button type="primary" @click="showAddressDialog = true" style="margin-bottom: 20px;">新增地址</el-button>
          
          <el-table :data="addressList" border style="width: 100%">
            <el-table-column prop="receiverName" label="收货人" width="120" />
            <el-table-column prop="receiverPhone" label="联系电话" width="150" />
            <el-table-column prop="detailAddress" label="详细地址" />
            <el-table-column label="操作" width="150">
              <template #default="scope">
                <el-button size="small" @click="editAddress(scope.row)">编辑</el-button>
                <el-button size="small" type="danger" @click="deleteAddress(scope.row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="showAddressDialog" title="地址信息">
      <el-form :model="addressForm" label-width="100px">
        <el-form-item label="收货人">
          <el-input v-model="addressForm.receiverName"></el-input>
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="addressForm.receiverPhone"></el-input>
        </el-form-item>
        <el-form-item label="详细地址">
          <el-input type="textarea" v-model="addressForm.detailAddress"></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddressDialog = false">取消</el-button>
        <el-button type="primary" @click="saveAddress">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>



<style scoped>
.profile-container {
  padding: 40px;
  background-color: #f4f7f9;
  min-height: 80vh;
}
.profile-card {
  max-width: 1000px;
  margin: 0 auto;
  border-radius: 0;
}

/* 展示页样式 */
.display-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-top: 10px;
}
.section-title {
  font-size: 20px;
  font-weight: bold;
  color: #333;
  border-left: 5px solid #0082c3; /* 迪卡侬蓝装饰线 */
  padding-left: 15px;
  margin: 0;
}
.info-table {
  margin-bottom: 20px;
}
/* 深度修改 Element Plus 描述列表的标签背景色 */
:deep(.el-descriptions__label) {
  background-color: #f8f9fa !important;
  font-weight: bold;
  width: 150px;
}

/* 按钮样式 */
.decathlon-btn {
  background-color: #0082c3 !important;
  border-color: #0082c3 !important;
  font-weight: bold;
  padding: 10px 30px;
}
.info-form {
  max-width: 500px;
}
</style>