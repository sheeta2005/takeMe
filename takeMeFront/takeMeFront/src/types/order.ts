/**
 * 订单信息
 */
export interface Order {
  id: string
  elderId: number
  volunteerId?: number

  // 0=代购 1=助洁 2=助餐 3=助医 4=陪伴
  type: 0 | 1 | 2 | 3 | 4

  // 0=待接单 1=已接单 2=服务中 3=已完成待确认 4=已完成 5=已取消
  status: 0 | 1 | 2 | 3 | 4 | 5

  createTime: string
  serviceTime: string
  address: string
  description?: string
  price?: number
  completionTime?: string
  rating?: number
}

/**
 * 订单类型文字映射
 */
export const OrderTypeMap: Record<Order['type'], string> = {
  0: '代购',
  1: '助洁',
  2: '助餐',
  3: '助医',
  4: '陪伴'
}

/**
 * 订单状态文字映射
 */
export const OrderStatusMap: Record<Order['status'], string> = {
  0: '待接单',
  1: '已接单',
  2: '服务中',
  3: '已完成待确认',
  4: '已完成',
  5: '已取消'
}
