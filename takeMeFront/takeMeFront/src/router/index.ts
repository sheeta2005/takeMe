import {createRouter, createWebHistory} from 'vue-router'
import {useUserStore} from '@/stores/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue')
  },
  {
    path: '/admin',
    component: () => import('@/layout/admin/Layout.vue'), // 你的 Layout 文件
    redirect: '/admin/index',
    children: [
      {path: 'index', component: () => import('@/views/admin/Index.vue')},
      {path: 'order', component: () => import('@/views/admin/Order.vue')},
      {path: 'order/detail', component: () => import('@/views/admin/OrderDetail.vue')},
      {path: 'volunteer', component: () => import('@/views/admin/Volunteer.vue')},
      {path: 'volunteer/detail', component: () => import('@/views/admin/VolunteerDetail.vue')},
      {path: 'user', component: () => import('@/views/admin/User.vue')},
      {path: 'user/detail', component: () => import('@/views/admin/UserDetail.vue')},
      {path: 'approval', component: () => import('@/views/admin/Approval.vue')},
      {path: 'sendMsg', component: () => import('@/views/admin/SendMsg.vue')},
      {path: 'inbox', component: () => import('@/views/admin/Inbox.vue')},
      {path: '/admin/setting', component: () => import('@/views/admin/Setting.vue')}
    ]
  },
  {
    path: '/volunteer',
    component: () => import('@/layout/volunteer/Layout.vue'),
    meta: {role: 1},
    children: [
      {path: '', component: () => import('@/views/volunteer/Index.vue')},
      {path: 'todo', component: () => import('@/views/volunteer/Todo.vue')},
      {
        path: 'order/:id',
        name: 'OrderDetail',
        component: () => import('@/views/volunteer/OrderDetail.vue')
      },
      {path: 'message', component: () => import('@/views/volunteer/Message.vue')},
      {
        path: 'message/read',
        component: () => import('@/views/volunteer/MessageRead.vue')
      },
      {
        path: 'message/detail/:id',
        component: () => import('@/views/volunteer/MessageDetail.vue')
      },
      {path: 'record', component: () => import('@/views/volunteer/Record.vue')},
      {path: 'points', component: () => import('@/views/volunteer/Points.vue')},
      {path: 'leave', component: () => import('@/views/volunteer/Leave.vue')},
      {path: 'study', component: () => import('@/views/volunteer/Study.vue')},
      {
        path: 'study/detail/:id',
        component: () => import('@/views/volunteer/StudyDetail.vue')
      },
      {path: 'info', component: () => import('@/views/volunteer/Info.vue')},
      {path: 'info/edit', component: () => import('@/views/volunteer/InfoEdit.vue')},
      {path: 'setting', component: () => import('@/views/volunteer/Setting.vue')}
    ]
  },
  {
    path: '/user',
    component: () => import('@/layout/user/Layout.vue'),
    meta: {role: 2},
    children: [
      {path: '', component: () => import('@/views/user/Index.vue')},
      {path: 'order', component: () => import('@/views/user/Order.vue')},
      {path: 'order/detail', component: () => import('@/views/user/OrderDetail.vue')},
      {path: 'info', component: () => import('@/views/user/Info.vue')},
      {path: 'info/edit', component: () => import('@/views/user/InfoEdit.vue')},
      {path: 'setting', component: () => import('@/views/user/Setting.vue')},
      {path: 'create', component: () => import('@/views/user/CreateOrder.vue')},
      {path: 'meal', component: () => import('@/views/user/OrderMeal.vue')},
      {path: 'clean', component: () => import('@/views/user/OrderClean.vue')},
      {path: 'medical', component: () => import('@/views/user/OrderMedical.vue')},
      {path: 'shop', component: () => import('@/views/user/OrderShop.vue')},
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

let userStore: ReturnType<typeof useUserStore>
router.beforeEach((to, _, next) => {
  if (!userStore) {
    userStore = useUserStore()
  }
  const token = userStore.token

  if (!token) {
    if (to.path === '/login') {
      next()
    } else {
      next('/login')
    }
    return
  }

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

  if (to.meta.role !== undefined && to.meta.role !== userStore.role) {
    alert('无权限访问')
    next(false)
    return
  }

  next()
})

export default router
