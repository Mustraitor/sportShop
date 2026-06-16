<template>
  <div class="profile-wrapper">
    <aside class="sidebar">
      <h2 class="sidebar-title">个人中心</h2>
      <nav class="sidebar-nav">
        <a
          v-for="item in navItems"
          :key="item.key"
          class="nav-item"
          :class="{ active: activeNav === item.key }"
          @click="handleNavClick(item.key)"
        >
          <el-icon class="nav-icon"><component :is="item.icon" /></el-icon>
          {{ item.label }}
        </a>
      </nav>
      <div class="sidebar-footer">
        <router-link to="/" class="back-home-btn">
          <el-icon><House /></el-icon>
          返回网站首页
        </router-link>
      </div>
    </aside>

    <main class="main-content">
      <div class="member-banner" v-if="activeNav === 'home'">
        <div class="banner-info">
          <div class="member-name">迪卡侬会员{{ userInfo.userName || '73086784' }}</div>
          <div class="member-id">会员号 {{ userInfo.memberId || '2093620852256' }}</div>
        </div>
        <!-- <div class="banner-points">
          <span class="points-label">我的燃值</span>
          <el-icon><RefreshRight /></el-icon>
          <span class="points-value">0</span>
        </div> -->
      </div>

      <div v-if="activeNav === 'home'" class="home-cards">
        <div class="home-card" @click="handleNavClick('orders')">
          <div class="card-title">近期订单</div>
          <div class="card-body empty-state">
            <img src="https://www.decathlon.com.cn/static/media/order-empty.svg" class="card-img" onerror="this.style.display='none'" />
            <div class="empty-icon order-icon">📋</div>
            <p class="empty-text">暂无订单<br /><span>期待您的第一单哦</span></p>
          </div>
          <el-button class="card-btn" plain>去首页逛逛</el-button>
        </div>
        <div class="home-card">
          <div class="card-title">会员福利</div>
          <div class="card-body empty-state">
            <div class="qr-placeholder benefit-qr">
              <el-icon style="font-size:48px;color:#0082c3"><Grid /></el-icon>
            </div>
            <p class="empty-text">扫描二维码体验迪卡侬官方小程序<br /><span>获取更多 优惠福利</span></p>
          </div>
          <el-button class="card-btn" plain>查看会员权益</el-button>
        </div>
        <div class="home-card">
          <div class="card-title">我的社群</div>
          <div class="card-body empty-state">
            <div class="qr-placeholder community-qr">
              <el-icon style="font-size:48px;color:#0082c3"><Grid /></el-icon>
            </div>
            <p class="empty-text">扫描二维码加入迪卡侬官方社群<br /><span>获取更多 优惠福利</span></p>
          </div>
          <el-button class="card-btn" plain>加入社群</el-button>
        </div>
      </div>

      <div v-if="activeNav === 'info'" class="content-card">
        <div v-if="!isEdit" class="display-mode">
          <div class="display-header">
            <h3 class="section-title">基本信息</h3>
            <el-button type="primary" plain @click="isEdit = true" class="edit-btn">编辑资料</el-button>
          </div>
          <el-descriptions :column="1" border class="info-table">
            <el-descriptions-item label="用户账号">{{ userInfo.userName }}</el-descriptions-item>
            <el-descriptions-item label="用户昵称">{{ userInfo.nickName }}</el-descriptions-item>
            <el-descriptions-item label="电子邮箱">{{ userInfo.email }}</el-descriptions-item>
            <el-descriptions-item label="手机号码">{{ userInfo.phonenumber }}</el-descriptions-item>
            <el-descriptions-item label="性别">
              <el-tag size="small" :type="userInfo.sex === '1' ? 'danger' : (userInfo.sex === '0' ? 'primary' : 'info')">
                {{ userInfo.sex === '1' ? '女' : (userInfo.sex === '0' ? '男' : '不便透露') }}
              </el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <div v-else class="edit-mode">
          <div class="display-header">
            <h3 class="section-title">修改个人资料</h3>
          </div>
          <el-form 
            ref="userInfoFormRef"
            :model="userInfo" 
            :rules="formRules"
            label-width="100px" 
            class="info-form"
            status-icon
          >
            <el-form-item label="用户账号">
              <el-input v-model="userInfo.userName" disabled />
            </el-form-item>
            <el-form-item label="用户昵称" prop="nickName">
              <el-input v-model="userInfo.nickName" placeholder="请输入用户昵称" />
            </el-form-item>
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="userInfo.email" placeholder="example@domain.com" />
            </el-form-item>
            <el-form-item label="手机号码" prop="phonenumber">
              <el-input v-model="userInfo.phonenumber"  />
            </el-form-item>
            <el-form-item label="性别">
              <el-radio-group v-model="userInfo.sex">
                <el-radio label="0">男</el-radio>
                <el-radio label="1">女</el-radio>
                <el-radio label="2">不便透露</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item class="form-actions">
              <el-button type="primary" @click="handleUpdateUser(userInfoFormRef)" class="decathlon-btn">保存修改</el-button>
              <el-button @click="isEdit = false">取消</el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>

  <div v-if="activeNav === 'wallet'" class="content-card wallet-container">
    <div v-if="!isRecharging" class="wallet-card">
      <div class="wallet-label">账户余额</div>
      <div class="wallet-balance">¥ {{ Number(walletBalance).toFixed(2) }}</div>
      <el-button type="danger" class="recharge-btn" @click="isRecharging = true">充 值</el-button>
    </div>

    <div v-else class="recharge-panel">
      <div class="recharge-header">
        <h3 class="section-title">安全充值中心</h3>
        <el-button size="small" @click="cancelRecharge">返回钱包</el-button>
      </div>
      
      <div class="amount-grid">
        <div 
          v-for="amount in presetAmounts" 
          :key="amount" 
          class="amount-item"
          :class="{ active: selectedAmount === amount && !isCustom }"
          @click="selectPresetAmount(amount)"
        >
          {{ amount }}元
        </div>
        
        <div 
          class="amount-item custom-item"
          :class="{ active: isCustom }"
          @click="handleCustomAmount"
        >
          {{ isCustom ? `${customAmount}元` : '自定义数额' }}
        </div>
      </div>

      <div class="recharge-action">
        <p class="pay-tip">应付金额：<span>¥ {{ finalAmount }}</span></p>
        <el-button 
          type="danger" 
          class="confirm-pay-btn" 
          :loading="isSubmittingRecharge"
          @click="submitRecharge"
        >
          确认支付
        </el-button>
      </div>
    </div>
  </div>

      <div v-if="activeNav === 'address'" class="content-card">
        <div class="display-header">
          <h3 class="section-title">地址管理</h3>
          <el-button type="primary" @click="openAddressDialog(null)" class="decathlon-btn">+ 新增地址</el-button>
        </div>
        <el-table :data="addressList" border style="width: 100%" class="address-table">
          <el-table-column prop="receiverName" label="收货人" width="120" />
          <el-table-column prop="receiverPhone" label="联系电话" width="150" />
          <el-table-column label="收货地址">
            <template #default="scope">
              <span>{{ scope.row.province }} {{ scope.row.city }} {{ scope.row.region }} {{ scope.row.detailAddress }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="160">
            <template #default="scope">
              <el-button size="small" @click="openAddressDialog(scope.row)">编辑</el-button>
              <el-button size="small" type="danger" @click="deleteAddress(scope.row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div v-if="addressList.length === 0" class="empty-address">
          <p>暂无收货地址，请添加</p>
        </div>
      </div>

      <div v-if="activeNav === 'orders'" class="content-card">
        <div class="display-header"><h3 class="section-title">购买记录</h3></div>
        <div class="empty-panel">
          <div class="empty-icon-lg">📋</div>
          <p>暂无购买记录</p>
          <el-button type="primary" class="decathlon-btn" @click="handleNavClick('home')">去首页逛逛</el-button>
        </div>
      </div>
      <div v-if="activeNav === 'favorites'" class="content-card">
        <div class="display-header"><h3 class="section-title">收藏夹</h3></div>
        <div class="empty-panel"><div class="empty-icon-lg">❤️</div><p>收藏夹是空的</p></div>
      </div>
      <div v-if="activeNav === 'history'" class="content-card">
        <div class="display-header"><h3 class="section-title">历史浏览</h3></div>
        <div class="empty-panel"><el-icon class="empty-warn-icon"><WarningFilled /></el-icon><p>暂无浏览记录</p></div>
      </div>
    </main>

    <el-dialog 
      v-model="showAddressDialog" 
      :title="addressForm.id ? '编辑地址' : '新增地址'" 
      width="550px"
      @close="resetAddressForm(addressFormRef)"
    >
      <el-form 
        ref="addressFormRef"
        :model="addressForm" 
        :rules="addressRules"
        label-width="100px"
        status-icon
      >
        <el-form-item label="收货人" prop="receiverName">
          <el-input v-model="addressForm.receiverName" placeholder="请输入收货人姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="receiverPhone">
          <el-input v-model="addressForm.receiverPhone" placeholder="请输入11位收货人手机号" maxlength="11" show-word-limit />
        </el-form-item>
        
        <el-form-item label="所在地区" prop="areaCascader">
          <el-cascader
            v-model="addressForm.areaCascader"
            :options="pcaOptions"
            :props="{
              value: 'label',
              label: 'label',
              emitPath: true
            }"
            @change="handleAreaChange"
          />
        </el-form-item>

        <el-form-item label="详细地址" prop="detailAddress">
          <el-input 
            type="textarea" 
            v-model="addressForm.detailAddress" 
            placeholder="请填写详细地址，如街道名称，楼层门牌号等" 
            :rows="3" 
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddressDialog = false">取消</el-button>
        <el-button type="primary" @click="saveAddress(addressFormRef)" class="decathlon-btn">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import {
  User, Location, ShoppingCart, Collection,
  Timer, RefreshRight, Grid, WarningFilled, House, Wallet
} from '@element-plus/icons-vue'
import { regionData } from 'element-china-area-data'
import { useUserStore } from '@/stores/user' 

// 🎯 修改：引入余额与充值接口
import { getAuthInfoApi, updateUserInfoApi, getUserBalanceApi, chargeBalanceApi } from '@/api/user'
import { addressApi } from '@/api/address'
import { ElMessage, ElMessageBox } from 'element-plus'

const pcaOptions = regionData
const userStore = useUserStore() 

const navItems = [
  { key: 'home',      label: '我的首页',   icon: 'User' },
  { key: 'info',      label: '个人信息',   icon: 'User' },
  { key: 'wallet',    label: '我的钱包',   icon: 'Wallet' },
  { key: 'address',   label: '地址管理',   icon: 'Location' },
]

const activeNav = ref('home')
const isEdit = ref(false)
const showAddressDialog = ref(false)
const userInfoFormRef = ref(null)
const addressFormRef = ref(null)

// 钱包变量
const walletBalance = ref(0.00)
const isRecharging = ref(false)
const isSubmittingRecharge = ref(false) // 🎯 新增：控制支付按钮的 loading 状态
const presetAmounts = [50, 100, 200, 300, 500]
const selectedAmount = ref(50)
const isCustom = ref(false)
const customAmount = ref(0)
const finalAmount = computed(() => isCustom.value ? Number(customAmount.value).toFixed(2) : Number(selectedAmount.value).toFixed(2))

const userInfo = reactive({ userName: '', nickName: '', email: '', phonenumber: '', sex: '0', memberId: '' })

// 电话通用校验正则
const phoneReg = /^1[3-9]\d{9}$/
const validatePhone = (rule, value, callback) => {
  if (!value) return callback(new Error('手机号码不能为空'))
  if (!phoneReg.test(value)) return callback(new Error('请输入11位有效的有效手机号'))
  callback()
}

// 个人资料验证规则
const formRules = reactive({
  nickName: [{ required: true, message: '用户昵称不能为空', trigger: 'blur' }],
  email: [{ required: true, message: '电子邮箱不能为空', trigger: 'blur' }, { type: 'email', message: '邮箱格式有误', trigger: ['blur', 'change'] }],
  phonenumber: [{ required: true, validator: validatePhone, trigger: ['blur', 'change'] }]
})

/* ================= 地址簿四栏与格式规范核心定义 ================= */
const addressList = ref([])

const addressForm = reactive({
  id: null,
  receiverName: '',
  receiverPhone: '',
  areaCascader: [], 
  province: '',     
  city: '',         
  region: '',       
  detailAddress: '' 
})

const addressRules = reactive({
  receiverName: [{ required: true, message: '请输入收货人姓名', trigger: 'blur' }],
  receiverPhone: [{ required: true, validator: validatePhone, trigger: ['blur', 'change'] }],
  areaCascader: [{ required: true, type: 'array', message: '请完整选择省、市、区地区栏', trigger: 'change' }],
  detailAddress: [{ required: true, message: '请输入详细地址栏', trigger: 'blur' }]
})

const handleAreaChange = (value) => {
  if (value && value.length === 3) {
    addressForm.province = value[0]
    addressForm.city = value[1]
    addressForm.region = value[2]
  } else {
    addressForm.province = ''
    addressForm.city = ''
    addressForm.region = ''
  }
}

const handleNavClick = (key) => {
  activeNav.value = key
  if (key === 'info') isEdit.value = false
  if (key !== 'wallet') isRecharging.value = false
}

/* ================= 钱包充值与余额查询核心逻辑 ================= */

// 🎯 新增：调用后端 API 获取真实钱包余额
const loadWalletBalance = async () => {
  try {
    const res = await getUserBalanceApi()
    // 假设你的 axios 拦截器已经直接返回了 data 里的值
    walletBalance.value = res || 0.00
  } catch (error) {
    ElMessage.error('获取钱包余额失败')
  }
}

const selectPresetAmount = (amount) => { isCustom.value = false; selectedAmount.value = amount }

const handleCustomAmount = () => {
  ElMessageBox.prompt('请输入您想要充值的自定义金额 (元)', '自定义金额', {
    confirmButtonText: '确定', cancelButtonText: '取消', inputPattern: /^[1-9]\d*$/, inputErrorMessage: '请输入大于0的正整数金额',
  }).then(({ value }) => { isCustom.value = true; customAmount.value = parseInt(value, 10) })
    .catch(() => { if (customAmount.value === 0) isCustom.value = false })
}

const cancelRecharge = () => { isRecharging.value = false; isCustom.value = false; selectedAmount.value = 50 }

// 🎯 修改：重构支付提交逻辑，对接后端 API
const submitRecharge = async () => {
  const amountToPay = parseFloat(finalAmount.value)
  if (amountToPay <= 0) return ElMessage.warning('请选择有效的充值金额')

  // 开启 loading 防重复点击
  isSubmittingRecharge.value = true
  
  try {
    // 调用后端充值 API
    await chargeBalanceApi({ amount: amountToPay })
    
    ElMessage.success(`成功充值 ¥ ${amountToPay.toFixed(2)}！`)
    isRecharging.value = false
    
    // 充值成功后，重新获取最新余额
    await loadWalletBalance()
  } catch (error) {
    ElMessage.error('充值失败，请稍后重试')
  } finally {
    // 无论成功失败，关闭 loading
    isSubmittingRecharge.value = false
  }
}

/* ================= 用户信息逻辑 ================= */
const getUserProfile = async () => {
  try {
    const res = await getAuthInfoApi()
    Object.assign(userInfo, {
      userName: res.userName,
      nickName: res.nickName,
      email: res.email,
      phonenumber: res.phonenumber,
      sex: res.sex ?? '0'
    })
  } catch (e) {
    ElMessage.error('获取用户信息失败')
  }
}

const handleUpdateUser = async (formEl) => {
  if (!formEl) return
  await formEl.validate(async (valid) => {
    if (valid) {
      try {
        await updateUserInfoApi({ ...userInfo })
        ElMessage.success('个人信息更新成功')
        isEdit.value = false
        userStore.userInfo = {
          userId: userInfo.userId,
          userName: userInfo.userName,
          nickName: userInfo.nickName
        }
      } catch (error) {
        console.error('更新失败', error)
        ElMessage.error('更新失败，请重试')
      }
    } else {
      ElMessage.error('表单输入有误，请修正！')
    }
  })
}

/* ================= 地址簿保存与校验逻辑 ================= */
const openAddressDialog = (row) => {
  if (row) {
    Object.assign(addressForm, row)
  } else {
    Object.assign(addressForm, { 
      id: null, receiverName: '', receiverPhone: '', areaCascader: [], 
      province: '', city: '', region: '', detailAddress: '' 
    })
  }
  showAddressDialog.value = true
}

const saveAddress = async (formEl) => {
  if (!formEl) return
  await formEl.validate(async (valid) => {
    if (!valid) return ElMessage.error('请修正表单信息')
    
    try {
      const apiData = mapFormToApi(addressForm)
      if (addressForm.id) {
        await addressApi.updateAddress(addressForm.id, apiData)
        ElMessage.success('地址修改成功')
      } else {
        await addressApi.addAddress(apiData)
        ElMessage.success('地址添加成功')
      }
      showAddressDialog.value = false
      loadAddressList() 
    } catch (e) {
      ElMessage.error('保存失败')
    }
  })
}

const mapApiToForm = (item) => ({
  id: item.id,
  receiverName: item.name,
  receiverPhone: item.phone,
  province: item.province,
  city: item.city,
  region: item.district, 
  detailAddress: item.detail,
  isDefault: item.isDefault,
  areaCascader: [item.province, item.city, item.district]
})

const mapFormToApi = (form) => ({
  name: form.receiverName,
  phone: form.receiverPhone,
  province: form.province,
  city: form.city,
  district: form.region, 
  detail: form.detailAddress,
  isDefault: form.isDefault || 0
})

const resetAddressForm = (formEl) => {
  if (formEl) formEl.resetFields()
}

const loadAddressList = async () => {
  try {
    const res = await addressApi.getAddressList()
    addressList.value = (res || []).map(item => mapApiToForm(item))
  } catch (e) {
    ElMessage.error('获取地址列表失败')
  }
}

const deleteAddress = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除该地址吗？', '提示', { type: 'warning' })
    await addressApi.deleteAddress(id)
    ElMessage.success('地址已删除')
    loadAddressList()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

// 🎯 修改：在挂载时同步触发钱包余额的获取
onMounted(() => { 
  getUserProfile() 
  loadAddressList()
  loadWalletBalance() // 挂载即请求余额数据
})
</script>

<style scoped>
/* ===== 通用排版与导航（保持原样） ===== */
.profile-wrapper { display: flex; min-height: 80vh; background-color: #f4f7f9; font-family: sans-serif; }
.sidebar { width: 200px; flex-shrink: 0; padding: 30px 0 16px; background: #fff; border-right: 1px solid #eee; display: flex; flex-direction: column; }
.sidebar-title { font-size: 18px; font-weight: bold; color: #222; padding: 0 24px 20px; border-bottom: 1px solid #eee; margin: 0 0 10px 0; }
.sidebar-nav { display: flex; flex-direction: column; flex: 1; }
.nav-item { display: flex; align-items: center; gap: 8px; padding: 12px 24px; font-size: 14px; color: #444; cursor: pointer; text-decoration: none; border-left: 3px solid transparent; }
.nav-item:hover, .nav-item.active { color: #0082c3; background: #f0f8ff; }
.nav-item.active { font-weight: bold; border-left-color: #0082c3; }
.sidebar-footer { padding: 16px 16px 8px; border-top: 1px solid #eee; margin-top: auto; }
.back-home-btn { display: flex; align-items: center; gap: 8px; padding: 10px 12px; font-size: 14px; color: #555; text-decoration: none; border-radius: 4px; border: 1px solid #ddd; justify-content: center; }
.back-home-btn:hover { color: #0082c3; border-color: #0082c3; background: #f0f8ff; }
.main-content { flex: 1; padding: 30px; min-width: 0; }

.member-banner { display: flex; justify-content: space-between; align-items: center; background: linear-gradient(135deg, #1a6fa0 0%, #0082c3 50%, #00a8e8 100%); border-radius: 4px; padding: 30px 40px; color: #fff; margin-bottom: 24px; }
.member-name { font-size: 24px; font-weight: bold; margin-bottom: 8px; }
.member-id { font-size: 14px; opacity: 0.85; }
.banner-points { display: flex; align-items: center; gap: 6px; font-size: 15px; }
.points-value { font-size: 20px; font-weight: bold; }
.home-cards { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; }
.home-card { background: #fff; border-radius: 4px; padding: 24px 20px; display: flex; flex-direction: column; align-items: center; gap: 12px; border: 1px solid #eee; }
.card-title { font-size: 16px; font-weight: bold; color: #333; align-self: flex-start; width: 100%; padding-bottom: 12px; border-bottom: 1px solid #f0f0f0; }
.card-body { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 20px 0; width: 100%; }
.empty-icon { font-size: 56px; }
.empty-text { text-align: center; font-size: 14px; color: #333; margin: 0; }
.empty-text span { color: #888; font-size: 13px; }
.qr-placeholder { width: 100px; height: 100px; display: flex; align-items: center; justify-content: center; background: #f5f5f5; border-radius: 4px; }
.card-btn { width: 100%; border-color: #333 !important; color: #333 !important; }

/* ===== 内容主体通栏 ===== */
.content-card { background: #fff; border-radius: 4px; padding: 30px; border: 1px solid #eee; }
.display-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; padding-bottom: 16px; border-bottom: 1px solid #f0f0f0; }
.section-title { font-size: 18px; font-weight: bold; color: #222; border-left: 4px solid #0082c3; padding-left: 12px; margin: 0; }

.info-table { max-width: 600px; }
.info-form { max-width: 650px; }

/* 统一右侧实时错误红字提示结构 */
:deep(.el-form-item) {
  margin-bottom: 26px;
  display: flex;
  align-items: center;
}
:deep(.el-form-item__content) {
  display: flex;
  align-items: center;
  position: relative;
  margin-left: 0 !important;
  flex: 1; /* 让输入控制自适应占满 */
}
:deep(.el-form-item__error) {
  position: relative;
  top: auto;
  left: auto;
  padding-left: 15px;
  white-space: nowrap;
  color: #f56c6c;
  font-size: 13px;
  display: inline-block;
}
.form-actions { margin-top: 35px; }
:deep(.form-actions .el-form-item__content) { display: block; }

/* ===== 钱包资产视图 ===== */
.wallet-container { display: flex; justify-content: center; align-items: center; min-height: 420px; }
.wallet-card { text-align: center; display: flex; flex-direction: column; align-items: center; gap: 25px; }
.wallet-label { font-size: 20px; color: #222; font-weight: 500; }
.wallet-balance { font-size: 54px; font-weight: bold; color: #111; }
.recharge-btn, .confirm-pay-btn { background-color: #e2231a !important; border-color: #e2231a !important; color: #fff !important; font-weight: bold; }
.recharge-panel { width: 100%; max-width: 560px; }
.recharge-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.amount-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; margin-bottom: 30px; }
.amount-item { background: #f8f9fa; border: 1px solid #e9ecef; border-radius: 12px; padding: 24px 10px; text-align: center; font-size: 20px; color: #333; cursor: pointer; }
.amount-item.active { background: #0082c3 !important; border-color: #0082c3 !important; color: #fff !important; }
.custom-item { font-size: 16px; color: #666; display: flex; align-items: center; justify-content: center; }
.recharge-action { border-top: 1px dashed #e9ecef; padding-top: 20px; display: flex; justify-content: space-between; align-items: center; }
.pay-tip span { font-size: 26px; font-weight: bold; color: #e2231a; }

/* ===== 地址簿视图增强 ===== */
.address-table { margin-top: 8px; }
:deep(.el-descriptions__label) { background-color: #f8f9fa !important; font-weight: bold; width: 130px; }
.decathlon-btn { background-color: #0082c3 !important; border-color: #0082c3 !important; color: #fff !important; font-weight: bold; }
.decathlon-btn-outline { border-color: #333 !important; color: #333 !important; }
.edit-btn { border-color: #0082c3 !important; color: #0082c3 !important; }
.empty-address { text-align: center; padding: 40px; color: #999; }
.empty-panel { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 60px 20px; gap: 16px; }
.empty-icon-lg { font-size: 64px; }
.empty-warn-icon { font-size: 32px; color: #999; }
</style>