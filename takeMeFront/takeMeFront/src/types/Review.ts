/**
 * 服务评价实体
 */
export interface Review {
  id: number
  orderId: number
  userId: number
  volunteerId: number
  rating: number         // 评分（1-5星）
  comment?: string       // 评价内容
  createTime: string
}
