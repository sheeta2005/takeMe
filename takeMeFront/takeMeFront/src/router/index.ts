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
      { path: 'create', component: () => import('@/views/user/CreateOrder.vue') }
    ]
  },
  {
    path: '/user/info/edit',
    name: 'InfoEdit',
    component: () => import('@/views/user/InfoEdit.vue'),
    meta: { role: 2 }
  },
  {
    path: '/user/setting',
    name: 'UserSetting',
    component: () => import('@/views/user/Setting.vue'),
    meta: { role: 2 }
  },
  {
    path: '/user/index',
    name: 'UserIndex',
    component: () => import('@/views/user/Index.vue'),
    meta: { role: 2 }
  },
  { path: '/', redirect: '/login' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// ==============================
// ✅ 修复后：不会自动跳页面
// ==============================
router.beforeEach((to, _, next) => {
  const userStore = useUserStore()
  const token = userStore.token

  // 未登录 → 只允许去登录页
  if (!token) {
    if (to.path === '/login') {
      next()
    } else {
      next('/login')
    }
    return
  }

  // 已登录，允许去任何页面（权限交给角色判断）
  // 不自动跳！不自动跳！不自动跳！
  if (to.meta.role !== undefined && to.meta.role !== userStore.role) {
    alert('无权限访问')
    next(false)
    return
  }

  next()
})

export default router
