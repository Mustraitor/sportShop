import { createRouter, createWebHistory } from 'vue-router'

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
      path: '/profile',
      component: () => import("../views/ProfileView.vue"),
      meta: { requiresAuth: true }
    },
    {
      path: '/login',
      component: () => import("../views/LoginView.vue")
    },
    {
      path: '/cart',
      component: () => import("../views/CartView.vue"),
      meta: { requiresAuth: true }
    },
    {
      path: '/order',
      component: () => import("../views/OrderListView.vue"),
      meta: { requiresAuth: true }
    },
  ],
})
// 全局路由守卫
// router.beforeEach((to, from, next) => {
//   const token = localStorage.getItem('token')

//   if (to.meta.requiresAuth && !token) {
//     return next('/login')
//   }
//   next()
// })
export default router
