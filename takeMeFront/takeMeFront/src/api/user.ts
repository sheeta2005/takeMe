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
//退出登录
export function logout() {
  return request({
    url: '/api/user/logout',
    method: 'post'
  })
}

// 用户注册
export function userRegister(data: {
  username: string;
  password: string;
  realName: string;
  phone: string;
}) {
  return request({
    url: '/api/user/register',
    method: 'post',
    data
  })
}

// ========================= 订单服务 =========================
// 获取我的订单列表
export function getMyOrderList(params: { page: number; pageSize: number; status?: number }) {
  return request({
    url: '/api/user/order/list',
    method: 'get',
    params
  })
}

// 下单预约服务
export function createOrder(data: any) {
  return request({
    url: '/api/user/order/create',
    method: 'post',
    data
  })
}

// 取消订单
export function cancelOrder(orderId: number) {
  return request({
    url: '/api/user/order/cancel',
    method: 'post',
    params: { orderId }
  })
}

// 评价服务
export function evaluateOrder(data: { orderId: number; score: number; content?: string }) {
  return request({
    url: '/api/user/order/evaluate',
    method: 'post',
    data
  })
}

// ========================= 购物车 =========================
// 获取购物车列表
export function getCartList() {
  return request({
    url: '/api/user/cart/list',
    method: 'get'
  })
}

// 加入购物车
export function addToCart(data: {
  productId: number;
  productName: string;
  productPrice: number;
  serviceType: string;
}) {
  return request({
    url: '/api/user/cart/add',
    method: 'post',
    data
  })
}

// 修改购物车商品数量
export function updateCartItem(data: {
  productId: number;
  quantity: number;
}) {
  return request({
    url: '/api/user/cart/update',
    method: 'post',
    data
  })
}

// 删除购物车商品
export function deleteCartItem(productId: number) {
  return request({
    url: '/api/user/cart/delete',
    method: 'post',
    params: { productId }
  })
}

// 清空购物车
export function clearCart() {
  return request({
    url: '/api/user/cart/clear',
    method: 'post'
  })
}
