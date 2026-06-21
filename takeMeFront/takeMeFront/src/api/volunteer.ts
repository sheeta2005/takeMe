import request from '@/utils/request'

// ========================= 登录退出 =========================
/**
 * 志愿者登录
 */
export function volunteerLogin(data: { username: string; password: string; userType?: number }) {
  return request({
    url: '/api/volunteer/login',
    method: 'post',
    data
  })
}

/**
 * 志愿者注册
 */
export function volunteerRegister(data: {
  username: string
  password: string
  realName: string
  phone: string
}) {
  return request({
    url: '/api/volunteer/register',
    method: 'post',
    data
  })
}

/**
 * 志愿者退出登录
 */
export function volunteerLogout() {
  return request({
    url: '/api/volunteer/logout',
    method: 'post'
  })
}

// ========================= 个人信息 =========================
/**
 * 获取当前志愿者信息
 */
export function getVolunteerInfo() {
  return request({
    url: '/api/volunteer/info',
    method: 'get'
  })
}

/**
 * 修改志愿者信息
 */
export function updateVolunteerInfo(data: any) {
  return request({
    url: '/api/volunteer/update',
    method: 'post',
    data
  })
}

/**
 * 志愿者头像上传
 */
export const uploadAvatar = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/api/volunteer/avatar/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 删除志愿者头像
 */
export const deleteAvatar = () => {
  return request({
    url: '/api/volunteer/avatar',
    method: 'delete'
  })
}

export const updateServiceDays = (serviceDays: string) => {
  return request({
    url: '/api/volunteer/updateServiceDays',
    method: 'post',
    data: { serviceDays }
  })
}

// ========================= 消息中心（和user.ts完全对齐） =========================
/**
 * 获取当前志愿者消息列表（分页+筛选）
 */
export function getVolunteerMessages(params: {
  pageNum?: number
  pageSize?: number
  type?: number
  isRead?: number
}) {
  return request({
    url: '/api/volunteer/message/list',
    method: 'get',
    params
  })
}

/**
 * 获取单条消息详情
 */
export function getVolunteerMessageDetail(id: number) {
  return request({
    url: `/api/volunteer/message/${id}`,
    method: 'get'
  })
}

/**
 * 标记消息为已读
 */
export function markVolunteerMessageRead(id: number) {
  return request({
    url: `/api/volunteer/message/read/${id}`,
    method: 'post'
  })
}

// ========================= 服务记录与积分 =========================
/**
 * 获取服务记录
 */
export function getServiceRecord() {
  return request({
    url: '/api/volunteer/record',
    method: 'get'
  })
}

/**
 * 获取积分与薪酬
 */
export function getPoints() {
  return request({
    url: '/api/volunteer/points',
    method: 'get'
  })
}

// ========================= 考勤请假 =========================
/**
 * 获取请假记录列表
 */
export function getLeaveList() {
  return request({
    url: '/api/volunteer/leave/list',
    method: 'get'
  })
}

/**
 * 提交请假申请
 */
export function submitLeave(data: {
  type: number
  startTime: string
  endTime: string
  reason: string
}) {
  return request({
    url: '/api/volunteer/leave/submit',
    method: 'post',
    data
  })
}

// ========================= 学习规范 =========================
/**
 * 获取学习规范列表
 */
export function getStudyList() {
  return request({
    url: '/api/volunteer/study',
    method: 'get'
  })
}

/**
 * 获取学习规范详情
 */
export function getStudyDetail(id: number) {
  return request({
    url: `/api/volunteer/study/${id}`,
    method: 'get'
  })
}

export function getVolunteerOrderList(params: { pageNum: number; pageSize: number; status?: number }) {
  return request({
    url: '/api/volunteer/order/list',
    method: 'get',
    params
  })
}

export function getAvailableOrderList(params: { pageNum: number; pageSize: number }) {
  return request({
    url: '/api/volunteer/order/available',
    method: 'get',
    params
  })
}

export function getVolunteerOrderDetail(orderId: number) {
  return request({
    url: '/api/volunteer/order/detail',
    method: 'get',
    params: { orderId }
  })
}

export function confirmOrder(orderItemId: number) {
  return request({
    url: '/api/volunteer/order/confirm',
    method: 'post',
    params: { orderItemId }
  })
}

export function startService(orderItemId: number) {
  return request({
    url: '/api/volunteer/order/start',
    method: 'post',
    params: { orderItemId }
  })
}

export function completeOrder(orderItemId: number) {
  return request({
    url: '/api/volunteer/order/complete',
    method: 'post',
    params: { orderItemId }
  })
}

export function abandonOrder(orderItemId: number) {
  return request({
    url: '/api/volunteer/order/abandon',
    method: 'post',
    params: { orderItemId }
  })
}

export function getPointsList() {
  return request({
    url: '/api/volunteer/points/list',
    method: 'get'
  })
}

export function getPointsSummary() {
  return request({
    url: '/api/volunteer/points/summary',
    method: 'get'
  })
}

export function addPoints(points?: number) {
  return request({
    url: '/api/volunteer/points/add',
    method: 'post',
    params: { points: points || 100 }
  })
}
