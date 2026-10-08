import request from '@/utils/request'

//查询用户订单列表
export function getMyOrderList(params: { pageNum: number; pageSize: number; status?: number; orderNo?: string }) {
  return request({
    url: '/api/user/order/list',
    method: 'get',
    params
  })
}

//创建订单
export function createOrder(data: any) {
  return request({
    url: '/api/user/order/create',
    method: 'post',
    data
  })
}

//结算购物车
export function checkoutCart(requestId: string) {
  return request({
    url: '/api/user/cart/checkout',
    method: 'post',
    headers: { 'Idempotency-Key': requestId }
  })
}

//取消订单
export function cancelOrder(orderId: number) {
  return request({
    url: '/api/user/order/cancel',
    method: 'post',
    params: { orderId }
  })
}

//确认订单完成
export function confirmOrder(orderId: number) {
  return request({
    url: '/api/user/order/confirm',
    method: 'post',
    params: { orderId }
  })
}

//提交订单评价
export function evaluateOrder(orderId: number) {
  return request({
    url: '/api/user/order/evaluate',
    method: 'post',
    params: { orderId }
  })
}

//按类型查询服务
export function getServiceList(type: number) {
  return request({
    url: '/api/user/service/list',
    method: 'get',
    params: { type }
  })
}

//查询用户订单详情
export function getUserOrderDetail(orderId: number) {
  return request({
    url: '/api/user/order/detail',
    method: 'get',
    params: { orderId }
  })
}

//查询购物车服务
export function getCartList() {
  return request({
    url: '/api/user/cart/list',
    method: 'get'
  })
}

//模拟订单支付
export function mockPayment(data: { orderId: number }) {
  return request({
    url: '/api/user/payment/mock',
    method: 'post',
    data
  })
}

//取消订单并退款
export function cancelOrderWithRefund(orderId: number) {
  return request({
    url: '/api/user/payment/cancel',
    method: 'post',
    params: { orderId }
  })
}

//添加服务到购物车
export function addToCart(data: {
  serviceId: number
  serviceName: string
  servicePrice: number
  serviceType: number
  quantity: number
  serviceDate?: string
  serviceTime?: string
  address?: string
  remark?: string
}) {
  return request({
    url: '/api/user/cart/add',
    method: 'post',
    data
  })
}

//修改购物车数量
export function updateCartItem(cartItemId: number, quantity: number) {
  return request({
    url: '/api/user/cart/update',
    method: 'post',
    data: { cartItemId, quantity }
  })
}

//删除购物车服务
export function deleteCartItem(cartItemId: number) {
  return request({
    url: '/api/user/cart/delete',
    method: 'post',
    data: { cartItemId }
  })
}

//清空购物车
export function clearCart() {
  return request({
    url: '/api/user/cart/clear',
    method: 'post'
  })
}

//取消单项服务
export function cancelOrderItem(orderItemId: number) {
  return request({
    url: '/api/user/order/cancelItem',
    method: 'post',
    params: { orderItemId }
  })
}

//评价单项服务
export function evaluateOrderItem(orderItemId: number, rating: number, comment: string) {
  return request({
    url: '/api/user/order/evaluateItem',
    method: 'post',
    params: { orderItemId, rating, comment }
  })
}
