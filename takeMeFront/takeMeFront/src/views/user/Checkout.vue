<template>
  <div class="checkout-container">
    <div class="page-title">确认订单</div>

    <div class="checkout-card" v-loading="loading">
      <div class="section-title">服务项目</div>
      <div class="cart-items">
        <div v-for="item in cartList" :key="item.id" class="cart-item">
          <div class="item-info">
            <span class="item-name">{{ item.serviceName }}</span>
            <span class="item-detail" v-if="item.serviceDate">
              {{ item.serviceDate }} {{ item.serviceTime }}
            </span>
            <span class="item-quantity">× {{ item.quantity }}</span>
          </div>
          <span class="item-price">¥{{ item.servicePrice * item.quantity }}</span>
        </div>
      </div>

      <div class="amount-section">
        <div class="amount-row">
          <span class="label">订单总额：</span>
          <span class="value total">¥{{ totalPrice }}</span>
        </div>
      </div>

      <div class="action-buttons">
        <el-button size="large" @click="goBack">返回购物车</el-button>
        <el-button
          type="primary"
          size="large"
          @click="submitOrder"
          :loading="submitting"
        >
          提交订单
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCartList, checkoutCart } from '@/api/order'
import { useCartStore } from '@/stores/cart'

const router = useRouter()
const cartStore = useCartStore()
const loading = ref(false)
const submitting = ref(false)

const cartList = ref<any[]>([])

const totalPrice = computed(() => {
  return cartList.value.reduce((sum, item) => {
    return sum + item.servicePrice * item.quantity
  }, 0)
})

const loadCart = async () => {
  loading.value = true
  try {
    const res = await getCartList()
    cartList.value = res.data || []
    if (cartList.value.length === 0) {
      ElMessage.warning('购物车为空')
      router.push('/user/cart')
    }
  } catch (e) {
    console.error('加载购物车失败:', e)
    ElMessage.error('加载购物车失败')
  } finally {
    loading.value = false
  }
}

const submitOrder = async () => {
  if (cartList.value.length === 0) {
    ElMessage.warning('购物车为空')
    return
  }

  submitting.value = true
  try {
    const res = await checkoutCart()

    if (res.code === 200) {
      ElMessage.success('订单提交成功')

      cartStore.items = []

      router.replace('/user/order')
    }
  } catch (err: any) {
    console.error('提交订单失败:', err)
    ElMessage.error(err.response?.data?.msg || '提交失败')
  } finally {
    submitting.value = false
  }
}

const goBack = () => router.push('/user/cart')

onMounted(() => {
  loadCart()
})
</script>

<style scoped>
.checkout-container {
  max-width: 800px;
  margin: 40px auto;
  padding: 0 20px;
}

.page-title {
  font-size: 32px;
  font-weight: bold;
  text-align: center;
  margin-bottom: 40px;
  color: #333;
}

.checkout-card {
  background: #fff;
  border-radius: 16px;
  padding: 40px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.section-title {
  font-size: 22px;
  font-weight: 600;
  margin-bottom: 20px;
  color: #333;
}

.cart-items {
  margin-bottom: 30px;
  padding: 20px;
  background: #f8faf9;
  border-radius: 8px;
}

.cart-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #eee;
}

.cart-item:last-child {
  border-bottom: none;
}

.item-info {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.item-name {
  font-size: 18px;
  font-weight: 500;
  color: #333;
}

.item-detail {
  font-size: 14px;
  color: #666;
}

.item-quantity {
  font-size: 16px;
  color: #666;
  margin-left: 10px;
}

.item-price {
  font-size: 18px;
  color: #f56c6c;
  font-weight: bold;
}

.amount-section {
  margin-top: 30px;
  padding: 20px;
  background: #fff8f8;
  border-radius: 8px;
}

.amount-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.label {
  font-size: 18px;
  color: #666;
}

.value.total {
  font-size: 24px;
  color: #f56c6c;
  font-weight: bold;
}

.action-buttons {
  margin-top: 30px;
  display: flex;
  justify-content: center;
  gap: 20px;
}
</style>
