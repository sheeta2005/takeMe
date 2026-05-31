import { defineStore } from 'pinia'

export const useCartStore = defineStore('cart', {
  state: () => ({
    // 购物车商品列表（每个商品都是独立条目，包含完整订单信息）
    items: [] as {
      id: number; // 唯一ID
      productId: number;
      productName: string;
      productPrice: number;
      serviceType: string;
      serviceDate: string; // 服务日期
      serviceTime: string; // 服务时间
      address: string; // 服务地址
      remark: string; // 备注
      quantity: number; // 数量
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
    // ✅ 最终版逻辑：只有助餐能有数量>1，其他全部强制为1
    addItem(item: {
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
      // ✅ 强制非助餐服务的数量为1，不管前端传什么
      if (item.serviceType !== '助餐服务') {
        item.quantity = 1
      }

      // 只有助餐服务才检查是否可以叠加
      if (item.serviceType === '助餐服务') {
        // 查找是否有完全相同的助餐服务（同商品、同时间、同地址）
        const existingItem = this.items.find(i =>
          i.productId === item.productId &&
          i.serviceDate === item.serviceDate &&
          i.serviceTime === item.serviceTime &&
          i.address === item.address
        )

        if (existingItem) {
          // 存在相同的，数量叠加
          existingItem.quantity += item.quantity
          this.saveToLocalStorage()
          return
        }
      }

      // 其他所有服务直接新增独立条目
      this.items.push({
        id: Date.now(), // 用时间戳作为唯一ID
        ...item
      })

      // 保存到本地存储
      this.saveToLocalStorage()
    },

    // 修改商品数量时也做限制
    updateQuantity(itemId: number, quantity: number) {
      const item = this.items.find(i => i.id === itemId)
      if (item) {
        // 非助餐服务数量不能修改，永远是1
        if (item.serviceType !== '助餐服务') {
          item.quantity = 1
        } else {
          item.quantity = Math.max(1, quantity)
        }
        this.saveToLocalStorage()
      }
    },

    // 删除单个商品
    removeItem(itemId: number) {
      this.items = this.items.filter(i => i.id !== itemId)
      this.saveToLocalStorage()
    },

    // 清空购物车
    clearCart() {
      this.items = []
      this.saveToLocalStorage()
    },

    // 保存到本地存储
    saveToLocalStorage() {
      localStorage.setItem('cartItems', JSON.stringify(this.items))
    },

    // 从本地存储加载
    loadFromLocalStorage() {
      const savedItems = localStorage.getItem('cartItems')
      if (savedItems) {
        this.items = JSON.parse(savedItems)
      }
    }
  }
})
