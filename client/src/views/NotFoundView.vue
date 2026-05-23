<script setup>
import { ref, onMounted, onBeforeUnmount } from "vue";
import { useRouter } from "vue-router";

const router = useRouter();

// 倒计时
const countdown = ref(5);
let timer = null;
let jumpTimer = null;
let isCancelled = ref(false);

// 返回首页
const goHome = () => {
  isCancelled.value = true;
  clearTimers();
  router.push("/");
};

// 返回上一页
const goBack = () => {
  isCancelled.value = true;
  clearTimers();
  router.back();
};


const clearTimers = () => {
  if (timer) clearInterval(timer);
  if (jumpTimer) clearTimeout(jumpTimer);
};

onMounted(() => {
  // 倒计时显示
  timer = setInterval(() => {
    if (countdown.value > 0) {
      countdown.value--;
    }
  }, 1000);

  // 自动跳转
  jumpTimer = setTimeout(() => {
    if (!isCancelled.value) {
      router.push("/");
    }
  }, countdown.value * 1000);
});

onBeforeUnmount(() => {
  clearTimers();
});
</script>

<template>
  <div class="not-found">
    <div class="content">
      <div class="code">404</div>
      <h1 class="title">页面正在训练中</h1>
      <p class="desc">
        页面未找到，将在 <span class="count">{{ countdown }}</span> 秒后返回首页
      </p>
      <div class="line"></div>
      <div class="btn-group">
        <button class="btn" @click="goHome">返回首页</button>
        <button class="btn ghost" @click="goBack">返回上一页</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.not-found {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #f5f9ff, #eaf2ff);
}

.content {
  text-align: center;
  padding: 40px 30px;
}

.code {
  font-size: 120px;
  font-weight: 800;
  color: #0082c3;
  animation: float 3s ease-in-out infinite;
}

.title {
  font-size: 28px;
  margin-top: 10px;
  color: #1f2d3d;
}

.desc {
  margin-top: 10px;
  color: #6b7c93;
}

.count {
  color: #0082c3;
  font-weight: bold;
}

.line {
  width: 120px;
  height: 4px;
  background: #0082c3;
  margin: 25px auto;
  border-radius: 10px;
  animation: slide 2s infinite alternate;
}

.btn-group {
  display: flex;
  gap: 10px;
  justify-content: center;
  flex-wrap: wrap;
}

.btn {
  padding: 10px 18px;
  border: none;
  border-radius: 25px;
  background: #0082c3;
  color: #fff;
  cursor: pointer;
  transition: 0.3s;
}

.btn:hover {
  background: #006fa8;
  transform: translateY(-2px);
}

.ghost {
  background: transparent;
  border: 2px solid #0082c3;
  color: #0082c3;
}

.ghost:hover {
  background: #0082c3;
  color: #fff;
}

.cancel {
  background: #ff6b6b;
}

.cancel:hover {
  background: #e85a5a;
}

@keyframes float {
  0% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
  100% { transform: translateY(0); }
}

@keyframes slide {
  from { width: 80px; }
  to { width: 160px; }
}
</style>