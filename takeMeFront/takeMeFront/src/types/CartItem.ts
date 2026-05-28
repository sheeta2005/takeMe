/**
 * 购物车项（老人选了哪些服务放这里）
 * 作用：临时存放，可删、可改、可清空
 */
export interface CartItem {
  id: number                // 购物车项ID
  cartId: number            // 属于哪个购物车
  productId: number         // 关联哪个服务套餐（ServicePackage.id）
  productName: string       // 服务名称（快照）
  productPrice: number      // 服务价格（快照）
  quantity: number          // 数量（服务默认 1）
  selected: number        // 是否勾选（用于结算） 0未，1勾
  createTime: string
}
