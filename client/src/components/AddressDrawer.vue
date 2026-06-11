<template>
  <el-drawer
    v-model="visible"
    title="管理收货地址"
    direction="rtl"
    size="460px"
    class="modern-address-drawer"
    @close="handleClose"
  >
    <div class="drawer-content">
      <div class="action-header">
        <el-button type="primary" class="add-btn-modern" @click="openAddDialog">
          <el-icon><Plus /></el-icon>添加新地址
        </el-button>
      </div>

      <div class="drawer-address-list">
        <div 
          v-for="addr in addressList" 
          :key="addr.id" 
          class="address-item"
          :class="{ active: modelValue === addr.id }"
          @click="selectAddress(addr.id)"
        >
          <div class="addr-info">
            <div class="addr-name">
              {{ addr.name }} <span class="phone-text">{{ addr.phone }}</span>
            </div>
            <div class="addr-detail">
              {{ addr.province }}{{ addr.city }}{{ addr.district }}{{ addr.detail }}
            </div>
          </div>
          
          <div class="addr-actions" @click.stop>
            <el-button 
              v-if="addr.isDefault !== 1 && addr.isDefault !== true" 
              type="primary" 
              link 
              size="small" 
              @click="setAsDefault(addr)"
            >设为默认</el-button>
            <el-button type="primary" link size="small" @click="openEditDialog(addr)">编辑</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(addr.id)">删除</el-button>
            
            <div v-if="addr.isDefault === 1 || addr.isDefault === true" class="addr-tag">默认</div>
          </div>
        </div>
      </div>
    </div>

    <el-dialog 
      v-model="showFormDialog" 
      :title="isEdit ? '修改收货地址' : '新增收货地址'" 
      width="500px" 
      append-to-body
    >
      <el-form :model="addressForm" label-width="90px" label-position="right">
        <el-form-item label="收货人" required>
          <el-input v-model="addressForm.receiverName" placeholder="请填写收货人姓名" />
        </el-form-item>
        
        <el-form-item label="手机号" required>
          <el-input v-model="addressForm.receiverPhone" maxlength="11" placeholder="请填写11位手机号码" show-word-limit />
        </el-form-item>
        
        <el-form-item label="所在省份" required>
          <el-select v-model="addressForm.province" placeholder="请选择省份" class="w-full" @change="handleProvinceChange">
            <el-option v-for="prov in regionData" :key="prov.value" :label="prov.label" :value="prov.label" />
          </el-select>
        </el-form-item>

        <el-form-item label="所在城市" required>
          <el-select v-model="addressForm.city" placeholder="请选择城市" class="w-full" :disabled="!addressForm.province" @change="handleCityChange">
            <el-option v-for="city in cityOptions" :key="city.value" :label="city.label" :value="city.label" />
          </el-select>
        </el-form-item>

        <el-form-item label="所在区县" required>
          <el-select v-model="addressForm.district" placeholder="请选择区/县" class="w-full" :disabled="!addressForm.city">
            <el-option v-for="dist in districtOptions" :key="dist.value" :label="dist.label" :value="dist.label" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="详细地址" required>
          <el-input type="textarea" :rows="3" v-model="addressForm.detailAddress" placeholder="详细地址、街道、门牌号等" />
        </el-form-item>
        
        <el-form-item label="设为默认">
          <el-switch v-model="addressForm.isDefault" :active-value="1" :inactive-value="0" active-color="#007fc3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showFormDialog = false">取消</el-button>
          <el-button type="primary" class="bg-blue-modern" @click="handleSave">确定完成</el-button>
        </div>
      </template>
    </el-dialog>
  </el-drawer>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { addressApi } from '@/api/address'
// 💡 只导入 regionData，彻底抛弃会报错的 CodeToText
import { regionData } from 'element-china-area-data'

const props = defineProps({
  show: { type: Boolean, default: false },
  modelValue: { type: [Number, String], default: null }
})

const emit = defineEmits(['update:show', 'update:modelValue', 'refresh'])

const visible = ref(false)
const addressList = ref([])
const showFormDialog = ref(false)
const isEdit = ref(false)
const currentAddressId = ref(null)

const addressForm = ref({
  receiverName: '',
  receiverPhone: '',
  province: '',
  city: '',
  district: '',
  detailAddress: '',
  isDefault: 0
})

// 💡 联动计算属性：动态筛选当前省份下的城市
const cityOptions = computed(() => {
  if (!addressForm.value.province) return []
  const targetProv = regionData.find(item => item.label === addressForm.value.province)
  return targetProv ? targetProv.children : []
})

// 💡 联动计算属性：动态筛选当前城市下的区县
const districtOptions = computed(() => {
  if (!addressForm.value.city) return []
  const targetCity = cityOptions.value.find(item => item.label === addressForm.value.city)
  return targetCity ? targetCity.children : []
})

const handleProvinceChange = () => {
  addressForm.value.city = ''
  addressForm.value.district = ''
}

const handleCityChange = () => {
  addressForm.value.district = ''
}

watch(() => props.show, (newVal) => {
  visible.value = newVal
  if (newVal) {
    loadAddresses()
  }
})

const loadAddresses = async () => {
  try {
    const res = await addressApi.getAddressList()
    addressList.value = res.data || res || []
  } catch (error) {
    console.error(error)
  }
}

const selectAddress = (id) => {
  emit('update:modelValue', id)
  ElMessage.success('收货地址已切换')
  handleClose()
}

const handleClose = () => {
  emit('update:show', false)
}

const openAddDialog = () => {
  isEdit.value = false
  currentAddressId.value = null
  addressForm.value = { receiverName: '', receiverPhone: '', province: '', city: '', district: '', detailAddress: '', isDefault: 0 }
  showFormDialog.value = true
}

const openEditDialog = (addr) => {
  isEdit.value = true
  currentAddressId.value = addr.id
  addressForm.value = {
    receiverName: addr.name,
    receiverPhone: addr.phone,
    province: addr.province,
    city: addr.city,
    district: addr.district,
    detailAddress: addr.detail,
    isDefault: addr.isDefault ? 1 : 0
  }
  showFormDialog.value = true
}

const handleSave = async () => {
  const f = addressForm.value
  if (!f.receiverName || !f.receiverPhone || !f.province || !f.city || !f.district || !f.detailAddress) {
    ElMessage.warning('请填写完整的收货信息和三级省市区')
    return
  }
  
  const payload = {
    name: f.receiverName,
    phone: f.receiverPhone,
    province: f.province,
    city: f.city,
    district: f.district,
    detail: f.detailAddress,
    isDefault: f.isDefault
  }

  try {
    if (isEdit.value) {
      await addressApi.updateAddress(currentAddressId.value, payload)
      ElMessage.success('地址修改成功')
    } else {
      await addressApi.addAddress(payload)
      ElMessage.success('地址添加成功')
    }
    showFormDialog.value = false
    await loadAddresses()
    emit('refresh')
  } catch (error) {
    console.error(error)
  }
}

const setAsDefault = async (addr) => {
  try {
    await addressApi.updateAddress(addr.id, {
      name: addr.name,
      phone: addr.phone,
      province: addr.province,
      city: addr.city,
      district: addr.district,
      detail: addr.detail,
      isDefault: 1
    })
    ElMessage.success('默认地址修改成功')
    await loadAddresses()
    emit('refresh')
  } catch (error) {
    console.error(error)
  }
}

const handleDelete = (id) => {
  ElMessageBox.confirm('确定要彻底移除该收货地址吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await addressApi.deleteAddress(id)
    ElMessage.success('删除成功')
    await loadAddresses()
    emit('refresh')
  }).catch(() => {})
}
</script>

<style scoped lang="scss">
.w-full { width: 100%; }

.add-btn-modern {
  width: 100%;
  padding: 22px 0;
  font-size: 15px;
  font-weight: 500;
  background-color: #007fc3;
  border-color: #007fc3;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  transition: all 0.2s;
  &:hover {
    background-color: #00669e;
    border-color: #00669e;
  }
}

.drawer-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.drawer-address-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  max-height: calc(100vh - 160px);
  overflow-y: auto;
  padding-right: 4px;
}

/* 🎯 100% 融合并重组你的高质感蓝白卡片类名 */
.address-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px; 
  border: 1px solid #edf0f2;
  border-radius: 12px;
  cursor: pointer;
  position: relative; 
  transition: all 0.2s;
  background: #ffffff;

  &:hover { 
    border-color: #007fc3; 
    background: #fcfdfe; 
  }

  &.active {
    border-color: #007fc3;
    background: #f0f9ff;
  }

  .addr-info {
    flex: 1;
    padding-right: 145px; /* 💡 关键防御：防止过长的地址文字与右侧绝对定位区重叠 */

    .addr-name { 
      font-size: 15px;
      font-weight: 600; 
      color: #1f2f3a;
      margin-bottom: 6px; 
      display: flex;
      align-items: center;
      gap: 10px; 

      .phone-text {
        font-weight: normal;
        color: #5f6b7a;
      }
    }

    .addr-detail { 
      font-size: 13px; 
      color: #6c7a8e; 
      line-height: 1.5;
    }
  }

  /* 💡 绝对定位操作控制台：精准间距 */
  .addr-actions {
    position: absolute;
    right: 18px;
    top: 50%;
    transform: translateY(-50%); 
    display: flex;
    align-items: center;
    gap: 12px; 

    :deep(.el-button) {
      padding: 0;
      margin: 0;
      font-size: 13px;
      font-weight: 500;
    }
  }

  .addr-tag {
    font-size: 12px;
    color: #007fc3;
    background: #e6f2fa;
    padding: 2px 10px;
    border-radius: 12px;
    font-weight: 500;
  }
}

.bg-blue-modern {
  background-color: #007fc3;
  border-color: #007fc3;
  &:hover {
    background-color: #00669e;
  }
}
</style>