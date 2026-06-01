import { defineStore } from 'pinia'
import { getCartList, addToCart, updateCartItem, deleteCartItem, clearCart } from '@/api'
import { ElMessage } from 'element-plus'

export const useCartStore = defineStore('cart', {
  state: () => ({
    // 匹配后端 CartItemVO 字段
    items: [] as {
      id: number;
      cartId: number;
      serviceId: number;
      serviceName: string;
      servicePrice: number;
      quantity: number;
      selected: number;
      createTime: string;
    }[]
  }),

  getters: {
    totalCount: (state) => {
      return state.items.reduce((sum, item) => sum + item.quantity, 0)
    },
    totalPrice: (state) => {
      return state.items.reduce((sum, item) => sum + item.servicePrice * item.quantity, 0)
    }
  },

  actions: {
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

    // ✅ 核心修复：完全匹配后端 CartItemDTO 字段
    async addItem(item: {
      serviceId: number;
      serviceName: string;
      servicePrice: number;
      quantity: number;
      serviceType: number; // 后端必填字段
    }) {
      // 非助餐服务数量强制为1
      if (item.serviceType !== 2) {
        item.quantity = 1
      }

      try {
        // 传入后端需要的完整参数，selected 默认 1（Integer类型，杜绝布尔值）
        await addToCart({
          serviceId: item.serviceId,
          serviceName: item.serviceName,
          servicePrice: item.servicePrice,
          quantity: item.quantity,
          serviceType: item.serviceType,
          selected: 1
        })
        await this.fetchCartList()
        ElMessage.success('已加入购物车')
        return true
      } catch (err) {
        ElMessage.error('加入购物车失败')
        return false
      }
    },

    async updateQuantity(itemId: number, quantity: number) {
      const item = this.items.find(i => i.id === itemId)
      if (!item) return

      if (item.serviceType !== 2) {
        ElMessage.info('这项服务只能预约1次哦，如需多个时间请重新下单')
        return
      }

      try {
        await updateCartItem({
          productId: item.serviceId,
          quantity: Math.max(1, quantity)
        })
        await this.fetchCartList()
      } catch (err) {
        ElMessage.error('修改数量失败')
      }
    },

    async removeItem(itemId: number) {
      try {
        await deleteCartItem(itemId)
        await this.fetchCartList()
        ElMessage.success('已删除')
      } catch (err) {
        ElMessage.error('删除失败')
      }
    },

    async clearCart() {
      try {
        await clearCart()
        this.items = []
        ElMessage.success('购物车已清空')
      } catch (err) {
        ElMessage.error('清空失败')
      }
    },

    loadFromLocalStorage() {
      this.fetchCartList()
    }
  }
})
