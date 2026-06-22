import request from '@/utils/request'

export function getMyOrderList(params: { pageNum: number; pageSize: number; status?: number; orderNo?: string }) {
  return request({
    url: '/api/user/order/list',
    method: 'get',
    params
  })
}

export function createOrder(data: any) {
  return request({
    url: '/api/user/order/create',
    method: 'post',
    data
  })
}

export function checkoutCart() {
  return request({
    url: '/api/user/cart/checkout',
    method: 'post'
  })
}

export function cancelOrder(orderId: number) {
  return request({
    url: '/api/user/order/cancel',
    method: 'post',
    params: { orderId }
  })
}

export function confirmOrder(orderId: number) {
  return request({
    url: '/api/user/order/confirm',
    method: 'post',
    params: { orderId }
  })
}

export function evaluateOrder(orderId: number) {
  return request({
    url: '/api/user/order/evaluate',
    method: 'post',
    params: { orderId }
  })
}

export function getServiceList(type: number) {
  return request({
    url: '/api/user/service/list',
    method: 'get',
    params: { type }
  })
}

export function getUserOrderDetail(orderId: number) {
  return request({
    url: '/api/user/order/detail',
    method: 'get',
    params: { orderId }
  })
}

export function getCartList() {
  return request({
    url: '/api/user/cart/list',
    method: 'get'
  })
}

export function mockPayment(data: { orderId: number }) {
  return request({
    url: '/api/user/payment/mock',
    method: 'post',
    data
  })
}

export function cancelOrderWithRefund(orderId: number) {
  return request({
    url: '/api/user/payment/cancel',
    method: 'post',
    params: { orderId }
  })
}

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

export function updateCartItem(cartItemId: number, quantity: number) {
  return request({
    url: '/api/user/cart/update',
    method: 'post',
    data: { cartItemId, quantity }
  })
}

export function deleteCartItem(cartItemId: number) {
  return request({
    url: '/api/user/cart/delete',
    method: 'post',
    data: { cartItemId }
  })
}

export function clearCart() {
  return request({
    url: '/api/user/cart/clear',
    method: 'post'
  })
}

export function cancelOrderItem(orderItemId: number) {
  return request({
    url: '/api/user/order/cancelItem',
    method: 'post',
    params: { orderItemId }
  })
}

export function evaluateOrderItem(orderItemId: number, rating: number, comment: string) {
  return request({
    url: '/api/user/order/evaluateItem',
    method: 'post',
    params: { orderItemId, rating, comment }
  })
}
