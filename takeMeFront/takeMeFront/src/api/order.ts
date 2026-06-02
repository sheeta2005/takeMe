import request from '@/utils/request'

// ========================= 订单服务 =========================
// 获取我的订单列表
export function getMyOrderList(params: { page: number; pageSize: number; status?: number }) {
  return request({
    url: '/api/user/order/list',
    method: 'get',
    params
  })
}

// 下单预约
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

// 根据类型获取服务列表
export function getServiceList(type: number) {
  return request({
    url: '/api/user/service/list',
    method: 'get',
    params: { type }
  })
}

// 获取订单详情 ✅【修复完成：适配后端 @RequestParam 接口】
export function getOrderDetail(orderId: number) {
  return request({
    url: '/api/user/order/detail',
    method: 'get',
    params: { orderId }
  })
}

// 确认订单完成
export function confirmOrder(orderId: number) {
  return request({
    url: '/api/user/order/confirm',
    method: 'post',
    params: { orderId }
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
  serviceId: number
  serviceName: string
  servicePrice: number
  serviceType: number
  serviceDate?: string
  serviceTime?: string
  address?: string
  remark?: string
  quantity: number
}) {
  return request({
    url: '/api/user/cart/add',
    method: 'post',
    data
  })
}

// 修改购物车商品数量
export function updateCartItem(data: {
  serviceId: number
  quantity: number
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
