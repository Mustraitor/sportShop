<template>
  <el-dialog
    v-model="visible"
    :title="title"
    :width="width"
    :before-close="handleBeforeClose"
    append-to-body
    class="custom-modern-dialog"
  >
    <template #header v-if="$slots.header">
      <slot name="header"></slot>
    </template>

    <div class="dialog-body-content">
      <slot></slot>
    </div>

    <template #footer>
      <div class="dialog-footer-actions">
        <slot name="footer">
          <el-button class="btn-cancel" @click="handleCancel">
            {{ cancelText }}
          </el-button>
          <el-button type="primary" class="btn-confirm" :loading="loading" @click="handleConfirm">
            {{ confirmText }}
          </el-button>
        </slot>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed } from 'vue'

// 定义属性
const props = defineProps({
  modelValue: { type: Boolean, default: false }, // 控制弹窗显示隐藏
  title: { type: String, default: '提示' },        // 弹窗标题
  width: { type: String, default: '480px' },      // 弹窗宽度
  confirmText: { type: String, default: '确定' },  // 确定按钮文字
  cancelText: { type: String, default: '取消' },    // 取消按钮文字
  loading: { type: Boolean, default: false }      // 确定按钮的 Loading 状态
})

// 定义事件
const emit = defineEmits(['update:modelValue', 'confirm', 'cancel'])

// 计算属性实现 v-model 双向绑定
const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const handleConfirm = () => emit('confirm')
const handleCancel = () => {
  visible.value = false
  emit('cancel')
}
const handleBeforeClose = (done) => {
  emit('cancel')
  done()
}
</script>

<style lang="scss">
/* 🎯 统一重写 Element Plus 样式，注入现代蓝白科技感生态 */
.custom-modern-dialog {
  border-radius: 16px !important;
  overflow: hidden;
  box-shadow: 0 12px 32px 4px rgba(0, 127, 195, 0.08) !important;
  padding: 0 !important;

  .el-dialog__header {
    margin-right: 0 !important;
    padding: 24px 24px 16px;
    background: #ffffff;
    border-bottom: 1px solid #f0f4f8;
    
    .el-dialog__title {
      font-size: 18px;
      font-weight: 600;
      color: #1f2f3a;
    }
    .el-dialog__headerbtn {
      top: 24px;
      &:hover .el-dialog__close {
        color: #007fc3;
      }
    }
  }

  .el-dialog__body {
    padding: 24px !important;
    color: #4a5568;
    font-size: 15px;
    line-height: 1.6;
    background: #ffffff;
  }

  .el-dialog__footer {
    padding: 16px 24px 24px !important;
    background: #fdfefe;
    border-top: 1px solid #f0f4f8;
  }
}

/* 局部弹性布局 */
.dialog-body-content {
  min-height: 40px;
}

.dialog-footer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;

  /* 现代冷灰取消按钮 */
  .btn-cancel {
    border-radius: 8px;
    padding: 10px 22px;
    border: 1px solid #dcdfe6;
    color: #5f6b7a;
    transition: all 0.2s;
    &:hover {
      background-color: #f5f7fa;
      border-color: #c0c4cc;
      color: #1f2f3a;
    }
  }

  /* 现代高级深蓝支付级按钮 */
  .btn-confirm {
    background-color: #007fc3 !important;
    border-color: #007fc3 !important;
    border-radius: 8px;
    padding: 10px 22px;
    font-weight: 500;
    transition: all 0.2s;
    &:hover {
      background-color: #00669e !important;
      border-color: #00669e !important;
    }
  }
}
</style>