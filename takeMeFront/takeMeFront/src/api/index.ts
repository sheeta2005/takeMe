import request from '@/utils/request'

// ====================== 工作台 ======================
// 获取工作台所有数据
export function getWorkbenchData() {
  return request({
    url: '/admin/workbench',
    method: 'get',
  })
}

// 导出统计数据 Excel（Apache POI）
export function exportStatisticsExcel() {
  return request({
    url: '/admin/statistics/export',
    method: 'get',
    responseType: 'blob',
  })
}

// ====================== 订单 ======================
// 获取订单列表（只保留 1 个，删除重复）
export function getOrderList(params: any) {
  return request({
    url: '/api/order/list',
    method: 'get',
    params,
  })
}

// 创建订单
export function createOrder(data: any) {
  return request({
    url: '/api/order/create',
    method: 'post',
    data,
  })
}

// ====================== 登录 ======================
// 管理员登录
export function adminLogin(data: { username: string; password: string }) {
  return request({
    url: '/api/admin/login',
    method: 'post',
    data,
  })
}

// 志愿者登录
export function volunteerLogin(data: { username: string; password: string }) {
  return request({
    url: '/api/volunteer/login',
    method: 'post',
    data,
  })
}

// 用户（老人）登录
export function userLogin(data: { username: string; password: string }) {
  return request({
    url: '/api/user/login',
    method: 'post',
    data,
  })
}

// ====================== 管理员 - 志愿者管理 ======================
export function getVolunteerPage(page: number, pageSize: number) {
  return request({
    url: '/api/admin/volunteer/page',
    method: 'get',
    params: { page, pageSize },
  })
}
