import { defineStore } from 'pinia'
import { getCartList, addToCart, updateCartItem, deleteCartItem, clearCart } from '@/api/user'
import { ElMessage } from 'element-plus'

export const useCartStore = defineStore('cart', {
  state: () => ({
    // 购物车商品列表（完全匹配后端返回字段）
    items: [] as {
      id: number; // 后端返回的购物车条目ID
      productId: number;
      productName: string;
      productPrice: number;
      serviceType: string;
      serviceDate: string;
      serviceTime: string;
      address: string;
      remark: string;
      quantity: number;
    }[]
  }),

  getters: {
    // 购物车商品总数
    totalCount: (state) => {
      return state.items.reduce((sum, item) => sum + item.quantity, 0)
    },
    // 购物车总金额
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

    // ✅ 最终版逻辑：只有助餐能有数量>1，其他全部强制为1
    async addItem(item: {
      productId: number;
      productName: string;
      productPrice: number;
      serviceType: string;
      serviceDate: string;
      serviceTime: string;
      address: string;
      remark: string;
      quantity: number;
    }) {
      // 强制非助餐服务的数量为1，不管前端传什么
      if (item.serviceType !== '助餐服务') {
        item.quantity = 1
      }

      try {
        await addToCart(item)
        // 加入成功后刷新购物车列表
        await this.fetchCartList()
        ElMessage.success('已加入购物车')
        return true
      } catch (err) {
        ElMessage.error('加入购物车失败')
        return false
      }
    },

    // 修改商品数量时也做限制
    async updateQuantity(itemId: number, quantity: number) {
      const item = this.items.find(i => i.id === itemId)
      if (!item) return

      // 非助餐服务数量不能修改，永远是1
      if (item.serviceType !== '助餐服务') {
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

    // 删除单个商品
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

    // 清空购物车
    async clearCart() {
      try {
        await clearCart()
        this.items = []
        ElMessage.success('购物车已清空')
      } catch (err) {
        ElMessage.error('清空失败')
      }
    }
  }
})
