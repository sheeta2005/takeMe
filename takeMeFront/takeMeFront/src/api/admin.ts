import request from '@/utils/request'

// 管理员登录
export function adminLogin(data: { username: string; password: string }) {
  return request({
    url: '/api/admin/login',
    method: 'post',
    data
  })
}

// 管理员退出登录
export function adminLogout() {
  return request({
    url: '/api/admin/logout',
    method: 'post'
  })
}

// 获取管理员信息
export function getAdminInfo() {
  return request({
    url: '/api/admin/info',
    method: 'get'
  })
}

// 更新管理员信息
export function updateAdminInfo(data: any) {
  return request({
    url: '/api/admin/update',
    method: 'post',
    data
  })
}

// 上传管理员头像
export const uploadAvatar = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/api/admin/avatar/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 删除管理员头像
export const deleteAvatar = () => {
  return request({
    url: '/api/admin/avatar',
    method: 'delete'
  })
}

// ========================= 工作台 =========================
export const getDashboardData = () => {
  return request({
    url: '/api/admin/dashboard',
    method: 'get'
  })
}

//查询近七日订单数量
export const getOrderTrend7d = () => {
  return request({
    url: '/api/admin/order/trend7d',
    method: 'get'
  })
}

//查询近七日支付净额
export const getOrderAmountTrend7d = () => {
  return request({
    url: '/api/admin/order/amount7d',
    method: 'get'
  })
}

//查询服务类型分布
export const getServiceTypeDist = () => {
  return request({
    url: '/api/admin/service/type/dist',
    method: 'get'
  })
}

//查询在线人数
export const getOnlineStats = () => {
  return request({
    url: '/api/admin/online',
    method: 'get'
  })
}

// ========================= 订单管理 =========================
export const getOrderPage = (pageNum: number, pageSize: number, status?: number) => {
  return request({
    url: '/api/admin/order/page',
    method: 'get',
    params: { pageNum, pageSize, status }
  })
}

//按条件查询订单
export const searchOrder = (
  pageNum: number,
  pageSize: number,
  status?: number,
  orderNo?: string,
  userId?: number,
  volunteerId?: number,
  startDate?: string,
  endDate?: string
) => {
  return request({
    url: '/api/admin/order/search',
    method: 'get',
    params: { pageNum, pageSize, status, orderNo, userId, volunteerId, startDate, endDate }
  })
}

//查询订单详情
export const getOrderDetail = (id: number) => {
  return request({
    url: `/api/admin/order/detail/${id}`,
    method: 'get'
  })
}

//确认订单完成
export const completeOrder = (id: number) => {
  return request({
    url: `/api/admin/order/complete/${id}`,
    method: 'post'
  })
}

//取消订单
export const cancelOrder = (id: number) => {
  return request({
    url: `/api/admin/order/cancel/${id}`,
    method: 'post'
  })
}

//查询订单统计
export const getOrderStatistics = () => {
  return request({
    url: '/api/admin/order/statistics',
    method: 'get'
  })
}

// ========================= 志愿者管理 =========================
export const searchVolunteer = (
  pageNum: number,
  pageSize: number,
  username?: string,
  id?: number,
  keyword?: string,
  sortBy?: string,
  sortOrder?: string
) => {
  return request({
    url: '/api/admin/volunteer/search',
    method: 'get',
    params: { pageNum, pageSize, username, id, keyword, sortBy, sortOrder }
  })
}

//查询志愿者详情
export const getVolunteerDetail = (id: number) => {
  return request({
    url: `/api/admin/volunteer/detail/${id}`,
    method: 'get'
  })
}

//新增志愿者
export const addVolunteer = (data: any) => {
  return request({
    url: '/api/admin/volunteer/add',
    method: 'post',
    data
  })
}

//修改志愿者资料
export const updateVolunteer = (data: any) => {
  return request({
    url: '/api/admin/volunteer/update',
    method: 'post',
    data
  })
}

//删除志愿者
export const deleteVolunteer = (id: number) => {
  return request({
    url: `/api/admin/volunteer/delete/${id}`,
    method: 'delete'
  })
}

//修改志愿者启用状态
export const updateVolunteerStatus = (id: number, status: number) => {
  return request({
    url: `/api/admin/volunteer/status/${id}`,
    method: 'post',
    params: { status }
  })
}

// ========================= 用户（老人）管理 =========================
export const searchUser = (
  pageNum: number,
  pageSize: number,
  keyword?: string,
  gender?: number,
  id?: number,
  startDate?: string,
  endDate?: string,
  sortBy?: string,
  sortOrder?: string
) => {
  return request({
    url: '/api/admin/user/search',
    method: 'get',
    params: { pageNum, pageSize, keyword, gender, id, startDate, endDate, sortBy, sortOrder }
  })
}

//查询用户详情
export const getUserDetail = (id: number) => {
  return request({
    url: `/api/admin/user/detail/${id}`,
    method: 'get'
  })
}

//新增用户
export const addUser = (data: any) => {
  return request({
    url: '/api/admin/user/add',
    method: 'post',
    data
  })
}

//修改用户资料
export const updateUser = (data: any) => {
  return request({
    url: '/api/admin/user/update',
    method: 'post',
    data
  })
}

//删除用户
export const deleteUser = (id: number) => {
  return request({
    url: `/api/admin/user/delete/${id}`,
    method: 'delete'
  })
}

//修改用户启用状态
export const updateUserStatus = (id: number, status: number) => {
  return request({
    url: `/api/admin/user/status/${id}`,
    method: 'post',
    params: { status }
  })
}

// ========================= 消息管理 =========================
export const getMessagePage = (pageNum: number, pageSize: number, receiverType?: number, type?: number) => {
  return request({
    url: '/api/admin/message/page',
    method: 'get',
    params: { pageNum, pageSize, receiverType, type }
  })
}

//分页查询发送记录
export const getSentMessagePage = (pageNum: number, pageSize: number, receiverType?: number, type?: number) => {
  return request({
    url: '/api/admin/message/sent',
    method: 'get',
    params: { pageNum, pageSize, receiverType, type }
  })
}

//发送消息
export const sendMessage = (data: any) => {
  return request({
    url: '/api/admin/message/send',
    method: 'post',
    data
  })
}

//批量发送消息
export const sendBatchMessage = (data: any) => {
  return request({
    url: '/api/admin/message/sendBatch',
    method: 'post',
    data
  })
}

//删除消息
export const deleteMessage = (id: number) => {
  return request({
    url: `/api/admin/message/delete/${id}`,
    method: 'delete'
  })
}

//查询消息统计
export const getMessageStatistics = () => {
  return request({
    url: '/api/admin/message/statistics',
    method: 'get'
  })
}

// ========================= 评价管理 =========================
export const getReviewPage = (pageNum: number, pageSize: number, rating?: number) => {
  return request({
    url: '/api/admin/review/page',
    method: 'get',
    params: { pageNum, pageSize, rating }
  })
}

//查询评价详情
export const getReviewDetail = (id: number) => {
  return request({
    url: `/api/admin/review/detail/${id}`,
    method: 'get'
  })
}

//删除评价
export const deleteReview = (id: number) => {
  return request({
    url: `/api/admin/review/delete/${id}`,
    method: 'delete'
  })
}

//查询评价统计
export const getReviewStatistics = () => {
  return request({
    url: '/api/admin/review/statistics',
    method: 'get'
  })
}

// ========================= 审批管理 =========================
export const getApprovalPage = (
  pageNum: number,
  pageSize: number,
  type?: string,
  status?: string,
  keyword?: string,
  startDate?: string,
  endDate?: string
) => {
  return request({
    url: '/api/admin/approval/page',
    method: 'get',
    params: { pageNum, pageSize, type, status, keyword, startDate, endDate }
  })
}

//查询审批详情
export const getApprovalDetail = (id: number) => {
  return request({
    url: `/api/admin/approval/detail/${id}`,
    method: 'get'
  })
}

//通过审批申请
export const approveApplication = (id: number, remark?: string) => {
  return request({
    url: `/api/admin/approval/approve/${id}`,
    method: 'post',
    data: { remark }
  })
}

//驳回审批申请
export const rejectApplication = (id: number, remark: string) => {
  return request({
    url: `/api/admin/approval/reject/${id}`,
    method: 'post',
    data: { remark }
  })
}

// ========================= 服务套餐管理 =========================
export const getServicePackagePage = (
  pageNum: number,
  pageSize: number,
  type?: number,
  status?: number,
  keyword?: string
) => {
  return request({
    url: '/api/admin/service-package/page',
    method: 'get',
    params: { pageNum, pageSize, type, status, keyword }
  })
}

//查询服务套餐详情
export const getServicePackageDetail = (id: number) => {
  return request({
    url: `/api/admin/service-package/detail/${id}`,
    method: 'get'
  })
}

//新增服务套餐
export const addServicePackage = (data: any) => {
  return request({
    url: '/api/admin/service-package/add',
    method: 'post',
    data
  })
}

//修改服务套餐
export const updateServicePackage = (data: any) => {
  return request({
    url: '/api/admin/service-package/update',
    method: 'post',
    data
  })
}

//删除服务套餐
export const deleteServicePackage = (id: number) => {
  return request({
    url: `/api/admin/service-package/delete/${id}`,
    method: 'delete'
  })
}

//修改服务套餐启用状态
export const updateServicePackageStatus = (id: number, status: number) => {
  return request({
    url: `/api/admin/service-package/status/${id}`,
    method: 'post',
    params: { status }
  })
}
