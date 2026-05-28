/**
 * 志愿者积分/薪酬记录
 */
export interface VolunteerPointsRecord {
  id: number
  volunteerId: number
  orderId: number
  points: number         // 本次获得积分
  type: 0 | 1            // 0=服务奖励 1=惩罚扣除
  description: string    // 记录说明（如“完成助洁服务+10积分”）
  createTime: string
}
