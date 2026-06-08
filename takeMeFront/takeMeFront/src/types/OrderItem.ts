/**
 * 订单项（订单里买了哪些服务）
 * 作用：永久记录，不能修改
 */
export interface OrderItem {
  id: number                // 订单项ID
  orderId: number           // 属于哪个订单
  serviceId: number         // 购买的服务套餐ID
  serviceName: string       // 服务名称（永久保存）
  servicePrice: number      // 购买时的价格（快照）
  quantity: number          // 数量
  itemPrice: number         // 该项目小计 = price * quantity
  serviceType?: number      // 服务类型 0=代购 1=助洁 2=助餐 3=助医 4=陪伴
  serviceDate?: string      // 服务日期
  serviceTime?: string      // 服务时间
  address?: string          // 服务地址
  remark?: string           // 备注
  createTime: string
}
