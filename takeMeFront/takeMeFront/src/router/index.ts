import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue')
  },
  {
    path: '/admin',
    component: () => import('@/layout/admin/Layout.vue'),
    meta: { role: 0 },
    children: [
      { path: '', component: () => import('@/views/admin/Index.vue') },
      { path: 'order', component: () => import('@/views/admin/Order.vue') },
      { path: 'user', component: () => import('@/views/admin/User.vue') },
      { path: 'volunteer', component: () => import('@/views/admin/Volunteer.vue') }
    ]
  },
  {
    path: '/volunteer',
    component: () => import('@/layout/volunteer/Layout.vue'),
    meta: { role: 1 },
    children: [
      { path: '', component: () => import('@/views/volunteer/Index.vue') },
      { path: 'order', component: () => import('@/views/volunteer/Order.vue') },
      { path: 'profile', component: () => import('@/views/volunteer/Profile.vue') }
    ]
  },
  {
    path: '/user',
    component: () => import('@/layout/user/Layout.vue'),
    meta: { role: 2 },
    children: [
      { path: '', component: () => import('@/views/user/Index.vue') },
      { path: 'order', component: () => import('@/views/user/Order.vue') },
      { path: 'order/detail', component: () => import('@/views/user/OrderDetail.vue') },
      { path: 'info', component: () => import('@/views/user/Info.vue') },
      { path: 'info/edit', component: () => import('@/views/user/InfoEdit.vue') },
      { path: 'setting', component: () => import('@/views/user/Setting.vue') },
      { path: 'create', component: () => import('@/views/user/CreateOrder.vue') },
      { path: 'meal', component: () => import('@/views/user/OrderMeal.vue') },
      { path: 'clean', component: () => import('@/views/user/OrderClean.vue') },
      { path: 'medical', component: () => import('@/views/user/OrderMedical.vue') },
      { path: 'shop', component: () => import('@/views/user/OrderShop.vue') },
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/login'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})
// -------------- 路由守卫 稳定版 --------------
router.beforeEach((to, _, next) => {
  const userStore = useUserStore()
  const token = userStore.token

  // 1. 未登录状态：
  //    - 访问登录页：放行
  //    - 访问其他页：强制跳登录页
  if (!token) {
    if (to.path === '/login') {
      next()
    } else {
      next('/login')
    }
    return
  }

  // 2. 已登录，但还想访问登录页：自动跳对应角色的主页
  if (to.path === '/login') {
    if (userStore.role === 0) {
      next('/admin')
    } else if (userStore.role === 1) {
      next('/volunteer')
    } else {
      next('/user')
    }
    return
  }

  // 3. 已登录，访问其他页：判断角色权限
  if (to.meta.role !== undefined && to.meta.role !== userStore.role) {
    alert('无权限访问')
    next(false)
    return
  }

  // 4. 都没问题，放行
  next()
})

export default router
