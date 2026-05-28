/**
 * 订单项（订单里买了哪些服务）
 * 作用：永久记录，不能修改
 */
export interface OrderItem {
  id: number                // 订单项ID
  orderId: number           // 属于哪个订单
  productId: number         // 购买的服务套餐ID
  productName: string       // 服务名称（永久保存）
  productPrice: number      // 购买时的价格（快照）
  quantity: number          // 数量
  itemPrice: number         // 该项目小计 = price * quantity
  createTime: string
}
