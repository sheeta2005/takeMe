import request from '@/utils/request'
// 管理员订单页面
import {
  getMyOrderList,  // 订单列表
  getOrderDetail,  // 订单详情
  confirmOrder,    // 确认完成
  cancelOrder      // 取消订单
} from '@/api/order'

// 导出order相关的函数供管理员使用
export { getMyOrderList, getOrderDetail, confirmOrder, cancelOrder }

// 管理员登录
export function adminLogin(data: { username: string; password: string }) {
  return request({
    url: '/api/admin/login',
    method: 'post',
    data
  })
}

// ========================= 工作台 =========================
export const getDashboardData = () => {
  return request({
    url: '/api/admin/dashboard',
    method: 'get'
  })
}
export const getOrderTrend7d = () => {
  return request({
    url: '/api/admin/order/trend7d',
    method: 'get'
  })
}
export const getServiceTypeDist = () => {
  return request({
    url: '/api/admin/service/type/dist',
    method: 'get'
  })
}

// ========================= 志愿者管理 =========================
export const getVolunteerPage = (page: number, pageSize: number) => {
  return request({
    url: '/api/admin/volunteer/page',
    method: 'get',
    params: { page, pageSize }
  })
}
export const volunteerSearch = (
  page: number, pageSize: number, username: string, id: number | null, availableRange: string
) => {
  return request({
    url: '/api/admin/volunteer/search',
    method: 'get',
    params: { page, pageSize, username, id, availableRange }
  })
}
export const volunteerAdd = (data: {
  username: string; phone: string; age: number; gender: number; address: string;
  availableRange: string; freeTime: number; workDay: number; status: number
}) => {
  return request({ url: '/api/admin/volunteer/add', method: 'post', data })
}
export const volunteerUpdate = (data: {
  id: number; username: string; phone: string; age: number; gender: number; address: string;
  availableRange: string; freeTime: number; workDay: number; status: number
}) => {
  return request({ url: '/api/admin/volunteer/update', method: 'post', data })
}
export const volunteerDelete = (id: number) => {
  return request({ url: '/api/admin/volunteer/delete', method: 'get', params: { id } })
}

// ========================= 用户（老人）管理 =========================
export const getElderPage = (page: number, pageSize: number) => {
  return request({ url: '/api/admin/elder/page', method: 'get', params: { page, pageSize } })
}
export const elderSearch = (params: {
  page: number; pageSize: number; keyword?: string; gender?: string; startDate?: string; endDate?: string
}) => {
  return request({ url: '/api/admin/elder/search', method: 'get', params })
}
export const getUserDetail = (id: number) => {
  return request({ url: '/api/admin/elder/detail', method: 'get', params: { id } })
}
export const userAdd = (data: {
  realName: string; phone: string; age: number; gender: '男' | '女'; address: string;
  emergencyName: string; emergencyPhone: string
}) => {
  return request({ url: '/api/admin/elder/add', method: 'post', data })
}
export const userUpdate = (data: {
  userId: number; realName: string; phone: string; age: number; gender: '男' | '女'; address: string;
  emergencyName: string; emergencyPhone: string
}) => {
  return request({ url: '/api/admin/elder/update', method: 'post', data })
}
export const userDelete = (id: number) => {
  return request({ url: '/api/admin/elder/delete', method: 'get', params: { id } })
}

// ========================= 积分（工资）管理 =========================
export const getPointsPage = (page: number, pageSize: number) => {
  return request({ url: '/api/admin/points/page', method: 'get', params: { page, pageSize } })
}
export const grantPoints = (data: { volunteerId: number; points: number; remark: string }) => {
  return request({ url: '/api/admin/points/grant', method: 'post', data })
}

// ========================= 业务审批 =========================
export const getApprovalPage = (page: number, pageSize: number) => {
  return request({ url: '/api/admin/approval/page', method: 'get', params: { page, pageSize } })
}
export const doApproval = (data: { id: number; status: number; remark?: string }) => {
  return request({ url: '/api/admin/approval/do', method: 'post', data })
}

// ========================= 消息/收信箱/差评 =========================
export const getInboxPage = (page: number, pageSize: number) => {
  return request({ url: '/api/admin/inbox/page', method: 'get', params: { page, pageSize } })
}
export const getFeedbackPage = (page: number, pageSize: number) => {
  return request({ url: '/api/admin/feedback/page', method: 'get', params: { page, pageSize } })
}
export const sendMessage = (data: {
  receiverType: 'all_volunteer' | 'all_elder' | 'spec_volunteer' | 'spec_elder';
  receiverIds?: number[]; type: 0 | 1 | 2; title: string; content: string
}) => {
  return request({ url: '/api/admin/message/send', method: 'post', data })
}

// ========================= 今日待办 =========================
export const getTodoCount = () => {
  return request({ url: '/api/admin/todo/count', method: 'get' })
}
