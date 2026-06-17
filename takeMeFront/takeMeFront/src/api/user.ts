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
export function uploadAvatar(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/api/user/avatar/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 删除头像
export function deleteAvatar() {
  return request({
    url: '/api/user/avatar',
    method: 'delete'
  })
}

export function updatePassword(data: { oldPassword: string; newPassword: string }) {
  return request({
    url: '/api/user/updatePassword',
    method: 'post',
    data
  })
}

// ========================= 地址管理（新增，匹配Address实体） =========================
/**
 * 获取当前用户的所有地址列表
 */
export function getUserAddressList() {
  return request({
    url: '/api/user/address/list',
    method: 'get'
  })
}

/**
 * 新增用户地址
 * @param data 地址参数 { address: string, isDefault: 0/1 }
 */
export function addUserAddress(data: { address: string; isDefault: number }) {
  return request({
    url: '/api/user/address/add',
    method: 'post',
    data
  })
}

/**
 * 修改用户地址
 * @param data 地址参数 { id: 地址ID, address: string, isDefault: 0/1 }
 */
export function updateUserAddress(data: { id: number; address: string; isDefault: number }) {
  return request({
    url: '/api/user/address/update',
    method: 'post',
    data
  })
}

/**
 * 删除用户地址
 * @param id 地址ID
 */
export function deleteUserAddress(id: number) {
  return request({
    url: '/api/user/address/delete',
    method: 'post',
    params: { id }
  })
}

/**
 * 设置默认地址
 * @param id 地址ID
 */
export function setDefaultAddress(id: number) {
  return request({
    url: '/api/user/address/default',
    method: 'post',
    params: { id }
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

// ========================= 消息中心 =========================
/**
 * 获取当前用户消息列表（分页+筛选）
 * @param params 筛选参数（页码、类型、已读状态）
 */
export function getUserMessages(params: {
  pageNum?: number
  pageSize?: number
  type?: number
  isRead?: number
}) {
  return request({
    url: '/api/user/message/list',
    method: 'get',
    params
  })
}

/**
 * 获取单条消息详情
 * @param id 消息ID
 */
export function getUserMessageDetail(id: number) {
  return request({
    url: `/api/user/message/${id}`,
    method: 'get'
  })
}

/**
 * 标记消息为已读
 * @param id 消息ID
 */
export function markMessageRead(id: number) {
  return request({
    url: `/api/user/message/read/${id}`,
    method: 'post'
  })
}

// ========================= 首页数据 =========================
/**
 * 获取用户首页统计数据（总订单数、已完成、进行中、平均评分）
 */
export function getUserStatistics() {
  return request({
    url: '/api/user/statistics',
    method: 'get'
  })
}

/**
 * 获取未读消息数量
 */
export function getUnreadMessageCount() {
  return request({
    url: '/api/user/message/list',
    method: 'get',
    params: { pageNum: 1, pageSize: 1, isRead: 0 }
  })
}

// ========================= 订单相关 =========================
/**
 * 确认开始服务
 * @param orderItemId 服务项目ID
 */
export function startService(orderItemId: number) {
  return request({
    url: '/api/user/order/startService',
    method: 'post',
    params: { orderItemId }
  })
}

export function getVolunteerDetail(volunteerId: number) {
  return request({
    url: '/api/user/volunteer/detail',
    method: 'get',
    params: { volunteerId }
  })
}
