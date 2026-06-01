// src/api/user.ts
import request from '@/utils/request'

// ========================= 个人信息 =========================
// 获取当前老人信息
export function getUserInfo() {
  return request({
    url: '/api/user/info',
    method: 'get'
  })
}

// 修改老人信息
export function updateUserInfo(data: any) {
  return request({
    url: '/api/user/update',
    method: 'post',
    data
  })
}

// 头像上传
export function uploadAvatar(data: any) {
  return request({
    url: '/api/user/uploadAvatar',
    method: 'post',
    data
  })
}

// ========================= 登录退出 =========================
export function userLogin(data: { username: string; password: string }) {
  return request({
    url: '/api/user/login',
    method: 'post',
    data
  })
}

// 退出登录
export function logout() {
  return request({
    url: '/api/user/logout',
    method: 'post'
  })
}

// 用户注册
export function userRegister(data: {
  username: string
  password: string
  realName: string
  phone: string
}) {
  return request({
    url: '/api/user/register',
    method: 'post',
    data
  })
}
