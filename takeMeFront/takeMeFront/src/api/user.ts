import request from '@/utils/request'

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

/**
 * 通用登录（统一接口，后端根据 role 0/1/2 判断身份）
 * @param data 账号+密码+角色（数字）
 */
export function login(data: { username: string; password: string; role: 0 | 1 | 2 }) {
  return request.post('/api/user/login', data)
}

/**
 * 退出登录
 */
export function logout() {
  return request.post('/api/user/logout')
}
