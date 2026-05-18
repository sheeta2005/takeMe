import request from '@/utils/request'

// 获取当前老人信息
export function getUserInfo() {
  return request({
    url: '/api/user/info',
    method: 'get'
  })
}

// 修改老人信息
export function updateUserInfo(data) {
  return request({
    url: '/api/user/update',
    method: 'post',
    data
  })
}

// 头像上传
export function uploadAvatar(data) {
  return request({
    url: '/api/user/uploadAvatar',
    method: 'post',
    data
  })
}
