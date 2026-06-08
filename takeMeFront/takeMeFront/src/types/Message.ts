export interface Message {
  id: number
  receiverId: number
  receiverType: number
  type: number
  title: string
  content: string
  isRead: number
  relatedOrderId?: number
  relatedUserId?: number
  relatedVolunteerId?: number
  relatedUrl?: string
  createTime: string
}
