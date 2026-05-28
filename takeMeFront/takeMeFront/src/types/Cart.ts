/**
 * 购物车（每个老人只有一个）
 */
export interface Cart {
  id: number             // 购物车ID
  userId: number         // 属于哪个老人
  createTime: string     // 创建时间
  updateTime: string     // 更新时间
}
