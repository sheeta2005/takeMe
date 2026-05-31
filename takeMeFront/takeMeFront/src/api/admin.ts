import request from '@/utils/request'

export function adminLogin(data: { username: string; password: string }) {
  return request({
    url: '/api/admin/login',
    method: 'post',
    data
  })
}

// ========================= 工作台 =========================
// 获取工作台统计数据
export const getDashboardData = () => {
  return request({
    url: '/api/admin/dashboard',
    method: 'get'
  })
}

// 获取近7天订单趋势
export const getOrderTrend7d = () => {
  return request({
    url: '/api/admin/order/trend7d',
    method: 'get'
  })
}

// 获取服务类型分布
export const getServiceTypeDist = () => {
  return request({
    url: '/api/admin/service/type/dist',
    method: 'get'
  })
}

// ========================= 订单管理 =========================
// 分页查询订单列表（支持状态筛选）
export const getOrderPage = (params: {
  page: number
  pageSize: number
  status?: string
}) => {
  return request({
    url: '/api/admin/order/page',
    method: 'get',
    params
  })
}

// 获取订单详情
export const getOrderDetail = (id: number) => {
  return request({
    url: '/api/admin/order/detail',
    method: 'get',
    params: { id }
  })
}

// ========================= 志愿者管理 =========================
// 分页查询志愿者列表
export const getVolunteerPage = (page: number, pageSize: number) => {
  return request({
    url: '/api/admin/volunteer/page',
    method: 'get',
    params: { page, pageSize }
  })
}

// 搜索志愿者
export const volunteerSearch = (
  page: number,
  pageSize: number,
  username: string,
  id: number | null,
  availableRange: string
) => {
  return request({
    url: '/api/admin/volunteer/search',
    method: 'get',
    params: { page, pageSize, username, id, availableRange }
  })
}

// 新增志愿者
export const volunteerAdd = (data: {
  username: string
  phone: string
  age: number
  gender: number
  address: string
  availableRange: string
  freeTime: number
  workDay: number
  status: number
}) => {
  return request({
    url: '/api/admin/volunteer/add',
    method: 'post',
    data
  })
}

// 修改志愿者
export const volunteerUpdate = (data: {
  id: number
  username: string
  phone: string
  age: number
  gender: number
  address: string
  availableRange: string
  freeTime: number
  workDay: number
  status: number
}) => {
  return request({
    url: '/api/admin/volunteer/update',
    method: 'post',
    data
  })
}

// 删除志愿者
export const volunteerDelete = (id: number) => {
  return request({
    url: '/api/admin/volunteer/delete',
    method: 'get',
    params: { id }
  })
}

// ========================= 用户（老人）管理 =========================
// 分页查询用户列表
export const getElderPage = (page: number, pageSize: number) => {
  return request({
    url: '/api/admin/elder/page',
    method: 'get',
    params: { page, pageSize }
  })
}

// 搜索用户
export const elderSearch = (params: {
  page: number
  pageSize: number
  keyword?: string
  gender?: string
  startDate?: string
  endDate?: string
}) => {
  return request({
    url: '/api/admin/elder/search',
    method: 'get',
    params
  })
}

// 获取用户详情
export const getUserDetail = (id: number) => {
  return request({
    url: '/api/admin/elder/detail',
    method: 'get',
    params: { id }
  })
}

// 新增用户
export const userAdd = (data: {
  realName: string
  phone: string
  age: number
  gender: '男' | '女'
  address: string
  emergencyName: string
  emergencyPhone: string
}) => {
  return request({
    url: '/api/admin/elder/add',
    method: 'post',
    data
  })
}

// 修改用户
export const userUpdate = (data: {
  userId: number
  realName: string
  phone: string
  age: number
  gender: '男' | '女'
  address: string
  emergencyName: string
  emergencyPhone: string
}) => {
  return request({
    url: '/api/admin/elder/update',
    method: 'post',
    data
  })
}

// 删除用户
export const userDelete = (id: number) => {
  return request({
    url: '/api/admin/elder/delete',
    method: 'get',
    params: { id }
  })
}

// ========================= 积分（工资）管理 =========================
// 分页查询积分发放记录
export const getPointsPage = (page: number, pageSize: number) => {
  return request({
    url: '/api/admin/points/page',
    method: 'get',
    params: { page, pageSize }
  })
}

// 发放积分
export const grantPoints = (data: {
  volunteerId: number
  points: number
  remark: string
}) => {
  return request({
    url: '/api/admin/points/grant',
    method: 'post',
    data
  })
}

// ========================= 业务审批 =========================
// 分页查询待审批列表
export const getApprovalPage = (page: number, pageSize: number) => {
  return request({
    url: '/api/admin/approval/page',
    method: 'get',
    params: { page, pageSize }
  })
}

// 审批通过/拒绝
export const doApproval = (data: {
  id: number
  status: number // 1通过 2拒绝
  remark?: string
}) => {
  return request({
    url: '/api/admin/approval/do',
    method: 'post',
    data
  })
}

// ========================= 消息/收信箱/差评 =========================
// 收信箱列表
export const getInboxPage = (page: number, pageSize: number) => {
  return request({
    url: '/api/admin/inbox/page',
    method: 'get',
    params: { page, pageSize }
  })
}

// 差评管理列表
export const getFeedbackPage = (page: number, pageSize: number) => {
  return request({
    url: '/api/admin/feedback/page',
    method: 'get',
    params: { page, pageSize }
  })
}

// 发送消息
export const sendMessage = (data: {
  receiverType: 'all_volunteer' | 'all_elder' | 'spec_volunteer' | 'spec_elder'
  receiverIds?: number[]
  type: 0 | 1 | 2 // 0=系统 1=任务 2=提醒
  title: string
  content: string
}) => {
  return request({
    url: '/api/admin/message/send',
    method: 'post',
    data
  })
}

// ========================= 今日待办 =========================
// 获取待办数量
export const getTodoCount = () => {
  return request({
    url: '/api/admin/todo/count',
    method: 'get'
  })
}
