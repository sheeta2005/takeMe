/**
 * 系统消息实体  message
 */
export interface Message {
  id: number
  receiverId: number          // 接收者ID（老人/志愿者ID）
  receiverType: 0 | 1         // 0=老人用户 1=志愿者
  type: 0 | 1 | 2              // 0=系统消息 1=订单消息 2=服务提醒
  title: string                // 消息标题
  content: string              // 消息内容
  isRead: boolean              // 是否已读
  relatedOrderId?: number      // 关联订单ID
  relatedUserId?: number       // 关联老人ID
  relatedVolunteerId?: number  // 关联志愿者ID
  relatedUrl?: string          // 跳转链接
  createTime: string           // 创建时间
}
