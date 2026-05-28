/**
 * 地址信息  address
 */
export interface Address {
  id: number                           // 主键ID
  address: string                      // 地址详情
  userId:number // 新增：关联用户ID
  isDefault: boolean                   // 是否默认地址
}
