import { defineStore } from 'pinia'
import { getCartList, addToCart, updateCartItem, deleteCartItem, clearCart } from '@/api'
import { ElMessage } from 'element-plus'

// 购物车项类型定义（完全匹配后端 CartItemVO）
export interface CartItem {
  id: number;
  cartId: number;
  serviceId: number;
  serviceName: string;
  servicePrice: number;
  quantity: number;
  selected: number;
  createTime: string;
  serviceType: number;
  remark?: string;
}

export const useCartStore = defineStore('cart', {
  state: () => ({
    // 购物车商品列表
    items: [] as CartItem[]
  }),

  getters: {
    // 商品总数量
    totalCount: (state) => {
      return state.items.reduce((sum, item) => sum + item.quantity, 0)
    },
    // 商品总价格（实时计算）
    totalPrice: (state) => {
      return state.items.reduce((sum, item) => sum + item.servicePrice * item.quantity, 0)
    }
  },

  actions: {
    /**
     * 从后端获取购物车列表
     */
    async fetchCartList() {
      try {
        const res = await getCartList()
        this.items = res.data
        return true
      } catch (err) {
        ElMessage.error('获取购物车失败')
        return false
      }
    },

    /**
     * 添加商品到购物车
     * 完全适配后端 CartItemDTO 字段
     */
    async addItem(item: {
      serviceId: number;
      serviceName: string;
      servicePrice: number;
      quantity: number;
      serviceType: number;
      remark?: string;
    }) {
      // 非助餐服务（serviceType=2）强制数量为1
      if (item.serviceType !== 2) {
        item.quantity = 1
      }

      try {
        // 调用后端接口，参数严格匹配 CartItemDTO
        await addToCart({
          serviceId: item.serviceId,
          serviceName: item.serviceName,
          servicePrice: item.servicePrice,
          quantity: item.quantity,
          serviceType: item.serviceType,
          // selected: 1,
          remark: item.remark || ''
        })

        // 重新拉取购物车数据
        await this.fetchCartList()
        ElMessage.success('已加入购物车')
        return true
      } catch (err) {
        ElMessage.error('加入购物车失败')
        console.error(err)
        return false
      }
    },

    /**
     * 修改购物车商品数量
     * 仅助餐服务可修改
     */
    async updateQuantity(itemId: number, quantity: number) {
      const item = this.items.find(i => i.id === itemId)
      if (!item) return

      // 非助餐服务禁止修改数量
      if (item.serviceType !== 2) {
        ElMessage.info('这项服务只能预约1次哦，如需多个时间请重新下单')
        return
      }

      try {
        await updateCartItem({
          serviceId: item.serviceId,
          quantity: Math.max(1, quantity)
        })
        await this.fetchCartList()
      } catch (err) {
        ElMessage.error('修改数量失败')
        console.error(err)
      }
    },

    /**
     * 删除购物车商品
     */
    async removeItem(itemId: number) {
      try {
        await deleteCartItem(itemId)
        await this.fetchCartList()
        ElMessage.success('已删除')
      } catch (err) {
        ElMessage.error('删除失败')
        console.error(err)
      }
    },

    /**
     * 清空购物车
     */
    async clearCart() {
      try {
        await clearCart()
        this.items = []
        ElMessage.success('购物车已清空')
      } catch (err) {
        ElMessage.error('清空失败')
        console.error(err)
      }
    },

    /**
     * 初始化加载购物车
     */
    loadFromLocalStorage() {
      this.fetchCartList()
    }
  }
})
