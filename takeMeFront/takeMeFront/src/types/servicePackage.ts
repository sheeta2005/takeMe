/**
 * 服务套餐配置（助餐/助洁/助医等）
 */
export interface servicePackage {
  id: number
  type: 0 | 1 | 2 | 3 | 4 // 服务类型，和Order.serviceType一致
  name: string            // 套餐名称（如“营养套餐A”）
  price: number           // 套餐价格
  description: string     // 套餐说明
  status: 0 | 1           // 0=下架 1=上架
}
