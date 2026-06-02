import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
// ✅ 新增：导入志愿者store
import { useVolunteerStore } from '@/stores/volunteer'

const routes = [
  // 你的所有路由配置完全不变，一个字都不用改
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue')
  },
  {
    path: '/admin',
    component: () => import('@/layout/admin/Layout.vue'),
    redirect: '/admin/index',
    meta: { role: 0 }, // 管理员
    children: [
      { path: 'index', component: () => import('@/views/admin/Index.vue') },
      { path: 'order', component: () => import('@/views/admin/Order.vue') },
      { path: 'order/detail', component: () => import('@/views/admin/OrderDetail.vue') },
      { path: 'orderManage', component: () => import('@/views/admin/OrderManage.vue') },
      { path: 'volunteer', component: () => import('@/views/admin/Volunteer.vue') },
      { path: 'volunteer/detail', component: () => import('@/views/admin/VolunteerDetail.vue') },
      { path: 'user', component: () => import('@/views/admin/User.vue') },
      { path: 'user/detail', component: () => import('@/views/admin/UserDetail.vue') },
      { path: 'approval', component: () => import('@/views/admin/Approval.vue') },
      { path: 'sendMsg', component: () => import('@/views/admin/SendMsg.vue') },
      { path: 'message', component: () => import('@/views/admin/Message.vue') },
      { path: 'setting', component: () => import('@/views/admin/Setting.vue') }
    ]
  },
  {
    path: '/volunteer',
    component: () => import('@/layout/volunteer/Layout.vue'),
    meta: { role: 1 }, // 志愿者
    children: [
      { path: '', component: () => import('@/views/volunteer/Index.vue') },
      { path: 'todo', component: () => import('@/views/volunteer/Todo.vue') },
      {
        path: 'order/:id',
        name: 'OrderDetail',
        component: () => import('@/views/volunteer/OrderDetail.vue')
      },
      { path: 'message', component: () => import('@/views/volunteer/Message.vue') },
      {
        path: 'message/detail/:id',
        component: () => import('@/views/volunteer/MessageDetail.vue')
      },
      { path: 'record', component: () => import('@/views/volunteer/Record.vue') },
      { path: 'points', component: () => import('@/views/volunteer/Points.vue') },
      { path: 'leave', component: () => import('@/views/volunteer/Leave.vue') },
      { path: 'study', component: () => import('@/views/volunteer/Study.vue') },
      {
        path: 'study/detail/:id',
        component: () => import('@/views/volunteer/StudyDetail.vue')
      },
      { path: 'info', component: () => import('@/views/volunteer/Info.vue') },
      { path: 'info/edit', component: () => import('@/views/volunteer/InfoEdit.vue') },
      { path: 'setting', component: () => import('@/views/volunteer/Setting.vue') }
    ]
  },
  {
    path: '/user',
    component: () => import('@/layout/user/Layout.vue'),
    meta: { role: 2 }, // 普通用户
    children: [
      { path: '', component: () => import('@/views/user/Index.vue') },
      { path: 'order', component: () => import('@/views/user/Order.vue') },
      { path: 'order/detail/:id', component: () => import('@/views/user/OrderDetail.vue') },
      { path: 'order/review/:id', component: () => import('@/views/user/OrderReview.vue') },
      { path: 'info', component: () => import('@/views/user/Info.vue') },
      { path: 'info/edit', component: () => import('@/views/user/InfoEdit.vue') },
      { path: 'setting', component: () => import('@/views/user/Setting.vue') },
      { path: 'create', component: () => import('@/views/user/CreateOrder.vue') },
      { path: 'cart', component: () => import('@/views/user/Cart.vue') },
      { path: 'meal', component: () => import('@/views/user/OrderMeal.vue') },
      { path: 'clean', component: () => import('@/views/user/OrderClean.vue') },
      { path: 'medical', component: () => import('@/views/user/OrderMedical.vue') },
      { path: 'shop', component: () => import('@/views/user/OrderShop.vue') },
      { path: 'companion', component: () => import('@/views/user/OrderCompanion.vue') },
      { path: 'message', component: () => import('@/views/user/Message.vue') },
      { path: 'message/detail/:id', component: () => import('@/views/user/MessageDetail.vue') }
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

// ✅ 完全修复后的路由守卫：根据路由meta.role自动检测对应store
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  const volunteerStore = useVolunteerStore()

  // 1. 登录页直接放行
  if (to.path === '/login') {
    // 有token还访问登录页 → 自动跳转到对应身份的首页
    if (userStore.token) {
      return userStore.role === 0 ? next('/admin') : next('/user')
    }
    if (volunteerStore.token) {
      return next('/volunteer')
    }
    return next()
  }

  // 2. 获取目标路由需要的身份
  const requiredRole = to.meta.role as number | undefined

  // 3. 根据需要的身份，检查对应store的token
  let hasToken = false
  let currentRole: number | null = null

  if (requiredRole === 1) {
    // 访问志愿者页面 → 检查志愿者token
    hasToken = !!volunteerStore.token
    currentRole = volunteerStore.role
  } else if (requiredRole === 2) {
    // 访问用户页面 → 检查用户token
    hasToken = !!userStore.token
    currentRole = userStore.role
  } else if (requiredRole === 0) {
    // 访问管理员页面 → 检查管理员token（你原来的逻辑不变）
    hasToken = !!userStore.token
    currentRole = userStore.role
  } else {
    // 不需要权限的页面直接放行
    return next()
  }

  // 4. 没有token → 跳登录
  if (!hasToken) {
    return next('/login')
  }

  // 5. 有token但身份不匹配 → 无权限
  if (currentRole !== requiredRole) {
    alert('无权限访问')
    return next(false)
  }

  // 6. 全部通过 → 放行
  next()
})

export default router
