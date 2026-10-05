/**
 * 服务套餐（平台卖的服务本身）
 */
export interface ServicePackage {
  id: number                  // 服务套餐ID
  name: string                // 套餐名：如“助洁2小时”
  type: 0 | 1 | 2 | 3 | 4     // 服务类型：0代购 1助洁 2助餐 3助医 4陪伴
  price: number               // 价格
  description: string         // 服务介绍
  image?: string              // 服务图片
  status: 0 | 1               // 0下架 1上架
  createTime: string          // 创建时间
}
