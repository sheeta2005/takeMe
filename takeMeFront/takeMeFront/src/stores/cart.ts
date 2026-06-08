import { defineStore } from 'pinia'
import { getCartList, addToCart, updateCartItem, deleteCartItem, clearCart } from '@/api/order'
import { ElMessage } from 'element-plus'

export interface CartItem {
  id: number
  cartId: number
  serviceId: number
  serviceName: string
  servicePrice: number
  quantity: number
  selected: number
  createTime: string
  serviceType: number
  serviceDate?: string
  serviceTime?: string
  address?: string
  remark?: string
}

export const useCartStore = defineStore('cart', {
  state: () => ({
    items: [] as CartItem[]
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
        this.items = res.data || []
        return true
      } catch (err) {
        console.error('获取购物车失败:', err)
        ElMessage.error('获取购物车失败')
        return false
      }
    },

    async addItem(item: {
      serviceId: number
      serviceName: string
      servicePrice: number
      quantity: number
      serviceType: number
      serviceDate?: string
      serviceTime?: string
      address?: string
      remark?: string
    }) {
      if (item.serviceType !== 2) {
        item.quantity = 1
      }

      try {
        await addToCart({
          serviceId: item.serviceId,
          serviceName: item.serviceName,
          servicePrice: item.servicePrice,
          quantity: item.quantity,
          serviceType: item.serviceType,
          serviceDate: item.serviceDate || '',
          serviceTime: item.serviceTime || '',
          address: item.address || '',
          remark: item.remark || ''
        })

        await this.fetchCartList()
        ElMessage.success('已加入购物车')
        return true
      } catch (err: any) {
        console.error('加入购物车失败:', err)
        ElMessage.error(err.response?.data?.msg || '加入购物车失败')
        return false
      }
    },

    async updateQuantity(cartItemId: number, quantity: number) {
      const item = this.items.find(i => i.id === cartItemId)
      if (!item) return

      if (item.serviceType !== 2) {
        ElMessage.info('这项服务只能预约1次哦，如需多个时间请重新下单')
        return
      }

      try {
        await updateCartItem(cartItemId, Math.max(1, quantity))
        await this.fetchCartList()
      } catch (err) {
        console.error('修改数量失败:', err)
        ElMessage.error('修改数量失败')
      }
    },

    async removeItem(cartItemId: number) {
      try {
        await deleteCartItem(cartItemId)
        await this.fetchCartList()
        return true
      } catch (err) {
        console.error('删除失败:', err)
        ElMessage.error('删除失败')
        return false
      }
    },

    async clearCart() {
      try {
        await clearCart()
        this.items = []
        ElMessage.success('购物车已清空')
      } catch (err) {
        console.error('清空失败:', err)
        ElMessage.error('清空失败')
      }
    },

    resetCart() {
      this.items = []
    },

    loadFromLocalStorage() {
      this.fetchCartList()
    }
  }
})
