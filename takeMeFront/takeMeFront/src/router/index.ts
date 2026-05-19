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
      {path: 'order/detail', component: () => import('@/views/user/OrderDetail.vue')},
      { path: 'info/edit', component: () => import('@/views/user/InfoEdit.vue') },
      { path: 'setting', component: () => import('@/views/user/Setting.vue') },
      { path: 'create', component: () => import('@/views/user/CreateOrder.vue') },
      { path: '', component: () => import('@/views/user/Index.vue') },
      // 新增：个人信息展示页
      { path: 'info', component: () => import('@/views/user/Info.vue') },
      // 保留：个人信息修改页
      { path: 'info/edit', component: () => import('@/views/user/InfoEdit.vue') },

      // 四个服务页面 ✅
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

// -------------- 路由守卫 --------------
router.beforeEach((to, _, next) => {
  const userStore = useUserStore()
  const token = userStore.token

  if (!token) {
    if (to.path === '/login') {
      next()
    } else {
      next('/login')
    }
    return
  }

  if (token && to.path === '/login') {
    const role = userStore.role
    if (role === 0) next('/admin')
    else if (role === 1) next('/volunteer')
    else next('/user')
    return
  }

  if (to.meta.role !== undefined && to.meta.role !== userStore.role) {
    alert('无权限访问')
    next(false)
    return
  }

  next()
})

export default router
