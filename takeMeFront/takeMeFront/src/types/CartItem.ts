export interface CartItem {
  id: number
  cartId: number
  serviceId: number
  serviceName: string
  servicePrice: number
  quantity: number
  selected: number
  createTime: string

  serviceDate?: string
  serviceTime?: string
  address?: string
  remark?: string
}
