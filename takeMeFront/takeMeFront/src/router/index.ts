import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useVolunteerStore } from '@/stores/volunteer'
import { useAdminStore } from '@/stores/admin'

const routes = [
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
    meta: { role: 0 },
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
    meta: { role: 1 },
    children: [
      { path: '', component: () => import('@/views/volunteer/Index.vue') },
      { path: 'getTodo', component: () => import('@/views/volunteer/GetTodo.vue') },
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
    meta: { role: 2 },
    children: [
      { path: '', component: () => import('@/views/user/Index.vue') },
      { path: 'order', component: () => import('@/views/user/Order.vue') },
      { path: 'order/detail/:id', component: () => import('@/views/user/OrderDetail.vue') },
      { path: 'order/review/:id', component: () => import('@/views/user/OrderReview.vue') },
      { path: 'info', component: () => import('@/views/user/Info.vue') },
      { path: 'info/edit', component: () => import('@/views/user/InfoEdit.vue') },
      { path: 'password-change', component: () => import('@/views/user/PasswordChange.vue') },
      { path: 'setting', component: () => import('@/views/user/Setting.vue') },
      { path: 'create', component: () => import('@/views/user/CreateOrder.vue') },
      { path: 'cart', component: () => import('@/views/user/Cart.vue') },
      { path: 'checkout', component: () => import('@/views/user/Checkout.vue') },
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

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  const volunteerStore = useVolunteerStore()
  const adminStore = useAdminStore()

  if (to.path === '/login') {
    if (adminStore.token) {
      return next('/admin')
    }
    if (userStore.token) {
      return userStore.role === 0 ? next('/admin') : next('/user')
    }
    if (volunteerStore.token) {
      return next('/volunteer')
    }
    return next()
  }

  const requiredRole = to.meta.role as number | undefined

  let hasToken = false
  let currentRole: number | null = null

  if (requiredRole === 0) {
    hasToken = !!adminStore.token
    currentRole = 0
  } else if (requiredRole === 1) {
    hasToken = !!volunteerStore.token
    currentRole = volunteerStore.role
  } else if (requiredRole === 2) {
    hasToken = !!userStore.token
    currentRole = userStore.role
  } else {
    return next()
  }

  if (!hasToken) {
    return next('/login')
  }

  if (currentRole !== requiredRole) {
    alert('无权限访问')
    return next(false)
  }

  next()
})

export default router
