/**
 * 订单信息
 */
export interface order {
  id: string
  elderId: number
  volunteerId?: number
  type: 0 | 1 | 2 | 3 // 0=代购 1=助洁 2=助餐 3=助医
  status: 0 | 1 | 2 | 3 | 4 // 0=待接单 1=已接单 2=服务中 3=已完成 4=已取消
  createTime: string
  serviceTime: string
  address: string
  description?: string
  price?: number
  completionTime?: string
  rating?: number
}
