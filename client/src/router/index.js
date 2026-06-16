import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      component: () => import("../views/IndexView.vue"),
      meta: { requiresAuth: false }
    },
    {
      path: '/product/:id',
      component: () => import("../views/ProductDetailView.vue"),
      props: true, // 允许将 id 作为 props 传给组件
      meta: { requiresAuth: false }
    },
    {
      path: '/product-category',
      name: 'ProductCategory',
      component: () => import("../views/ProductCategoryView.vue"),
      meta: { requiresAuth: false }
    },
    {
      path: '/category/:id',
      name: 'CategoryProducts',
      component: () => import("../views/ProductListView.vue"),
      props: true, // 将分类 id 作为 props 传给组件
      meta: { requiresAuth: false }
    },
    {
      path: '/profile',
      component: () => import("../views/ProfileView.vue"),
      meta: { requiresAuth: true }
    },
    // {
    //   path: '/login',
    //   component: () => import("../views/LoginView.vue")
    // },
    {
      path: '/cart',
      component: () => import("../views/CartView.vue"),
      meta: { requiresAuth: false }
    },
    {
      path: '/order',
      component: () => import("../views/OrderListView.vue"),
      meta: { requiresAuth: true }
    },
    {
      path: '/order-detail/:id',
      name: 'OrderDetail',
      component: () => import('@/views/OrderDetailView.vue')
    },
    {
      path: '/checkout',
      name: 'Checkout',
      component: () => import('@/views/CheckoutView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/AIChat',
      component: () => import("../views/AIChatView.vue"),
      meta: { requiresAuth: true }
    },
    {
      path: "/:pathMatch(.*)*",
      name: "NotFound",
      component: () => import("@/views/NotFoundView.vue")
    }
  ],
  scrollBehavior(to, from, savedPosition) {
    // 如果有保存的位置（比如点击浏览器的“前进/后退”按钮），则恢复到之前的位置
    if (savedPosition) {
      return savedPosition
    } else {
      // 否则，所有导航一律滚动到顶部
      return { top: 0, behavior: 'smooth' } // 'smooth' 代表平滑滚动，如果不想要动画可以改成 'auto'
    }
  }
})
// 全局路由守卫
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  if (to.meta.requiresAuth && !userStore.token) {
    userStore.showLogin() 
    return next(false)    
  }
  next() 
})

export default router
