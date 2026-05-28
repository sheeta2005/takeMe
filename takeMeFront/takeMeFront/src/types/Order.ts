/**
 * 订单（老人结算后生成）
 * 作用：记录一次完整购买
 */
export interface Order {
  id: number                      // 订单主键
  orderNo: string                 // 订单编号（给用户看）
  userId: number                  // 下单老人ID
  volunteerId?: number            // 接单志愿者ID

  totalPrice: number              // 订单总金额

  serviceDate: string             // 服务日期
  serviceTime: string             // 服务时间
  address: string                 // 服务地址
  remark?: string                 // 备注

  status: 0 | 1 | 2 | 3 | 4 | 5   // 0待接单 1已接单 2服务中 3待确认 4已完成 5已取消
  createTime: string              // 下单时间
  completeTime?: string           // 完成时间
  isReviewed: boolean             // 是否已评价
}
