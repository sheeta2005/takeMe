import { defineStore } from 'pinia'
import { getCartList, addToCart, updateCartItem, deleteCartItem, clearCart } from '@/api/user'
import { ElMessage } from 'element-plus'

export const useCartStore = defineStore('cart', {
  state: () => ({
    // 购物车商品列表（完全匹配后端返回字段）
    items: [] as {
      id: number;
      productId: number;
      productName: string;
      productPrice: number;
      serviceType: number; // ✅ 改为数字
      serviceDate: string;
      serviceTime: string;
      address: string;
      remark: string;
      quantity: number;
    }[]
  }),

  getters: {
    totalCount: (state) => {
      return state.items.reduce((sum, item) => sum + item.quantity, 0)
    },
    totalPrice: (state) => {
      return state.items.reduce((sum, item) => sum + item.productPrice * item.quantity, 0)
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

    // ✅ 修复：serviceType 使用数字判断
    async addItem(item: {
      productId: number;
      productName: string;
      productPrice: number;
      serviceType: number;
      serviceDate: string;
      serviceTime: string;
      address: string;
      remark: string;
      quantity: number;
    }) {
      // 只有 2=助餐 可以数量>1，其他全部强制1
      if (item.serviceType !== 2) {
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

      // 只有 2=助餐 可修改数量
      if (item.serviceType !== 2) {
        ElMessage.info('这项服务只能预约1次哦，如需多个时间请重新下单')
        return
      }

      try {
        await updateCartItem({
          productId: item.productId,
          quantity: Math.max(1, quantity)
        })
        await this.fetchCartList()
      } catch (err) {
        ElMessage.error('修改数量失败')
      }
    },

    async removeItem(itemId: number) {
      const item = this.items.find(i => i.id === itemId)
      if (!item) return

      try {
        await deleteCartItem(item.productId)
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

    // ✅ 修复报错：补上这个方法
    loadFromLocalStorage() {
      this.fetchCartList()
    }
  }
})
