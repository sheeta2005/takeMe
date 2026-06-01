import { defineStore } from 'pinia'
import { getCartList, addToCart, updateCartItem, deleteCartItem, clearCart } from '@/api'
import { ElMessage } from 'element-plus'

export const useCartStore = defineStore('cart', {
  state: () => ({
    // 购物车商品列表（完全匹配后端 OrderItemVO 返回字段）
    items: [] as {
      id: number;
      orderId: number;
      serviceId: number;
      serviceName: string;
      servicePrice: number;
      quantity: number;
      itemPrice: number;
      createTime: string;
    }[]
  }),

  getters: {
    totalCount: (state) => {
      return state.items.reduce((sum, item) => sum + item.quantity, 0)
    },
    totalPrice: (state) => {
      return state.items.reduce((sum, item) => sum + item.itemPrice, 0)
    }
  },

  actions: {
    // 从后端获取购物车列表
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

    async addItem(item: {
      serviceId: number;
      serviceName: string;
      servicePrice: number;
      quantity: number;
    }) {
      // 非助餐服务数量强制为1
      if (item.serviceId !== 2) {
        item.quantity = 1
      }

      try {
        await addToCart(item)
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

      // 只有助餐服务(serviceId=2)可修改数量
      if (item.serviceId !== 2) {
        ElMessage.info('这项服务只能预约1次哦，如需多个时间请重新下单')
        return
      }

      try {
        await updateCartItem({
          id: item.id,
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
