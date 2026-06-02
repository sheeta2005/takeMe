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
 * ✅ 修复：补全缺失的 volunteerLogout 导出，解决当前报错
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
export function uploadVolunteerAvatar(data: any) {
  return request({
    url: '/api/volunteer/uploadAvatar',
    method: 'post',
    data
  })
}

// ========================= 任务管理 =========================
/**
 * 获取志愿者待办任务
 */
export function getVolunteerTodo() {
  return request({
    url: '/api/volunteer/todo',
    method: 'get'
  })
}

/**
 * 确认接单
 */
export function volunteerConfirmOrder(orderId: string) {
  return request({
    url: `/api/volunteer/confirm/${orderId}`,
    method: 'post'
  })
}

/**
 * 完成服务
 */
export function completeOrder(orderId: string) {
  return request({
    url: `/api/volunteer/complete/${orderId}`,
    method: 'post'
  })
}

// ========================= 消息中心（和user.ts完全对齐） =========================
/**
 * 获取当前志愿者消息列表（分页+筛选）
 */
export function getVolunteerMessages(params: {
  page?: number
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
 * 提交请假申请
 */
export function submitLeave(data: any) {
  return request({
    url: '/api/volunteer/leave',
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
