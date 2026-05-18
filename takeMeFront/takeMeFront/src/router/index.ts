import {createRouter, createWebHistory} from 'vue-router'

const routes = [
  // 登录页（根路径直接跳登录）
  {
    path: '/login',
    component: () => import('@/views/Login.vue')
  },
  // 管理员路由
  {
    path: '/admin',
    component: () => import('@/layout/admin/Layout.vue'),
    children: [
      {path: '', component: () => import('@/views/admin/Index.vue')},
      {path: 'order', component: () => import('@/views/admin/Order.vue')},
      {path: 'user', component: () => import('@/views/admin/User.vue')},
      {path: 'volunteer', component: () => import('@/views/admin/Volunteer.vue')}
    ]
  },
  // 志愿者路由
  {
    path: '/volunteer',
    component: () => import('@/layout/volunteer/Layout.vue'),
    children: [
      {path: '', component: () => import('@/views/volunteer/Index.vue')},
      {path: 'order', component: () => import('@/views/volunteer/Order.vue')},
      {path: 'profile', component: () => import('@/views/volunteer/Profile.vue')}
    ]
  },
  // 老人路由
  {
    path: '/user',
    component: () => import('@/layout/user/Layout.vue'),
    children: [
      {path: '', component: () => import('@/views/user/Index.vue')},
      {path: 'create', component: () => import('@/views/user/CreateOrder.vue')}
    ]
  },

  {
    path: '/user/info/edit',
    name: 'InfoEdit',
    component: () => import('@/views/user/InfoEdit.vue')
  },
  {
    path: '/user/setting',
    name: 'UserSetting',
    component: () => import('@/views/user/Setting.vue')
  },

  {
    path: '/user/index',
    name: 'UserIndex',
    component: () => import('@/views/user/Index.vue')
  },

  // 根路径强制跳登录页
  {path: '/', redirect: '/login'}
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
