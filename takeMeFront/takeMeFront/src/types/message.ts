
/**
 * 消息信息
 */
export interface message {
  id: number
  userId: number
  type: 0 | 1 | 2 // 0=系统 1=任务 2=提醒
  title: string
  content: string
  createTime: string
  isRead: boolean
  relatedId?: string
  relatedUrl?: string
}
