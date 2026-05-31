// import request from '@/utils/request'
//
// /**
//  * 1. 创建订单（确认下单 → 提交后端）
//  */
// export function createOrder(data: {
//   serviceName: string
//   serviceDesc: string
//   price: number
//   address: string
//   time: string
//   remark?: string
//   type: '助洁' | '助餐' | '助医' | '代购'
//   hospital?: string
// }) {
//   return request({
//     url: '/api/order/create',
//     method: 'post',
//     data
//   })
// }
//
// /**
//  * 2. 获取【我的订单】列表（个人中心 → 我的订单）
//  */
// export function getMyOrderList() {
//   return request({
//     url: '/api/order/myList',
//     method: 'get'
//   })
// }
//
// /**
//  * 3. 获取订单详情（点击订单查看详情）
//  * @param id 订单ID
//  */
// export function getOrderDetail(id: number | string) {
//   return request({
//     url: `/api/order/detail/${id}`,
//     method: 'get'
//   })
// }
//
// /**
//  * 4. 取消订单
//  * @param id 订单ID
//  */
// export function cancelOrder(id: number | string) {
//   return request({
//     url: `/api/order/cancel/${id}`,
//     method: 'post'
//   })
// }
