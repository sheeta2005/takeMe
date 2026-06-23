<template>
  <div class="payment-container">
    <div class="page-title">订单支付</div>

    <div class="payment-card" v-loading="loading">
      <div class="order-info-section">
        <div class="section-title">订单信息</div>
        <div class="info-grid">
          <div class="info-row">
            <span class="label">订单编号：</span>
            <span class="value">{{ orderInfo.orderNo }}</span>
          </div>
          <div class="info-row">
            <span class="label">订单金额：</span>
            <span class="value price">¥{{ orderInfo.totalPrice }}</span>
          </div>
          <div class="info-row">
            <span class="label">服务项目：</span>
            <span class="value">{{ itemsCount }} 项</span>
          </div>
          <div class="info-row full-width">
            <span class="label">服务时间：</span>
            <span class="value">{{ orderInfo.serviceDate }} {{ orderInfo.serviceTime }}</span>
          </div>
          <div class="info-row full-width" v-if="orderInfo.address">
            <span class="label">服务地址：</span>
            <span class="value">{{ orderInfo.address }}</span>
          </div>
        </div>
      </div>

      <div class="payment-method-section">
        <div class="section-title">支付方式</div>
        <div class="payment-methods">
          <div class="method-item selected">
            <el-icon class="method-icon"><Wallet /></el-icon>
            <span class="method-name">模拟支付</span>
            <el-icon class="check-icon"><Check /></el-icon>
          </div>
        </div>
      </div>

      <div class="payment-tips">
        <el-alert
          title="提示：这是模拟支付环境，点击支付按钮将直接完成支付"
          type="info"
          :closable="false"
          show-icon
        />
      </div>

      <div class="action-buttons">
        <el-button size="large" @click="goBack">返回订单详情</el-button>
        <el-button
          type="success"
          size="large"
          @click="handlePayment"
          :loading="paying"
        >
          确认支付
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Wallet, Check } from '@element-plus/icons-vue'
import { mockPayment } from '@/api/order'
import { getUserOrderDetail } from '@/api/order'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const paying = ref(false)

const orderInfo = ref<any>({
  orderNo: '',
  totalPrice: 0,
  serviceDate: '',
  serviceTime: '',
  address: ''
})

const itemsCount = computed(() => {
  return orderInfo.value.items?.length || 0
})

const loadOrderDetail = async () => {
  const orderId = route.query.orderId

  if (!orderId) {
    ElMessage.error('订单ID不存在')
    router.push('/user/order')
    return
  }

  loading.value = true
  try {
    const res = await getUserOrderDetail(Number(orderId))

    if (res.code === 200 && res.data) {
      orderInfo.value = res.data

      if (res.data.status !== 6) {
        ElMessage.warning('订单状态异常，无需支付')
        router.push('/user/order')
      }
    } else {
      ElMessage.error(res.msg || '加载订单失败')
      router.push('/user/order')
    }
  } catch (e) {
    console.error('加载订单失败:', e)
    ElMessage.error('加载订单失败')
    router.push('/user/order')
  } finally {
    loading.value = false
  }
}

const handlePayment = async () => {
  const orderId = route.query.orderId

  if (!orderId) {
    ElMessage.error('订单ID不存在')
    return
  }

  paying.value = true
  try {
    const res = await mockPayment({ orderId: Number(orderId) })

    if (res.code === 200) {
      ElMessage.success('支付成功！')

      setTimeout(() => {
        router.replace('/user/order')
      }, 1000)
    } else {
      ElMessage.error(res.msg || '支付失败')
    }
  } catch (err: any) {
    console.error('支付失败:', err)
    ElMessage.error(err.response?.data?.msg || '支付失败，请重试')
  } finally {
    paying.value = false
  }
}

const goBack = () => {
  const orderId = route.query.orderId
  if (orderId) {
    router.replace(`/user/order/detail/${orderId}`)
  } else {
    router.replace('/user/order')
  }
}

onMounted(() => {
  loadOrderDetail()
})
</script>

<style scoped>
.payment-container {
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

.payment-card {
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
  padding-left: 12px;
  border-left: 4px solid #00b899;
}

.order-info-section {
  margin-bottom: 32px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  padding: 20px;
  background: #f8faf9;
  border-radius: 8px;
}

.info-row {
  display: flex;
  align-items: center;
  font-size: 16px;
}

.info-row.full-width {
  grid-column: 1 / -1;
}

.label {
  color: #666;
  margin-right: 8px;
}

.value {
  color: #333;
  font-weight: 500;
}

.value.price {
  color: #f5222d;
  font-size: 24px;
  font-weight: bold;
}

.payment-method-section {
  margin-bottom: 32px;
}

.payment-methods {
  display: flex;
  gap: 16px;
}

.method-item {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 20px;
  border: 2px solid #e8e8e8;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.method-item.selected {
  border-color: #00b899;
  background: #f0f9f4;
}

.method-icon {
  font-size: 32px;
  color: #00b899;
}

.method-name {
  font-size: 18px;
  font-weight: 500;
  color: #333;
}

.check-icon {
  margin-left: auto;
  font-size: 24px;
  color: #00b899;
}

.payment-tips {
  margin-bottom: 32px;
}

.action-buttons {
  display: flex;
  justify-content: center;
  gap: 20px;
}

.action-buttons .el-button {
  min-width: 160px;
}
</style>
