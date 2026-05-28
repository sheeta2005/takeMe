/**
 * 订单实体
 */
export interface Order {
  id: number                      // 订单号（主键）
  orderNo: string                 // 业务订单号（字符串格式，展示用）
  userId: number                  // 下单老人ID
  volunteerId?: number            // 接单志愿者ID
  serviceType: 0 | 1 | 2 | 3 | 4  // 服务类型：0助餐 1助洁 2助医 3代购 4陪伴
  serviceContent: string          // 服务内容/套餐名称
  price: number                   // 订单金额
  serviceDate: string             // 服务日期（如2026-05-28）
  serviceTime: string             // 服务时间（如14:00）
  address: string                 // 服务地址
  remark?: string                 // 订单备注
  status: 0 | 1 | 2 | 3 | 4 | 5   // 订单状态：0待接单 1已接单 2服务中 3待确认 4已完成 5已取消
  createTime: string              // 下单时间
  completeTime?: string           // 完成时间
  isReviewed: boolean             // 是否已评价
}
