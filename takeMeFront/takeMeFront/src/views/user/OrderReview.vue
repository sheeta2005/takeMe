<template>
  <div class="page-container">
    <div class="header-row">
      <h2 class="page-title">订单评价</h2>
    </div>

    <div class="review-card">
      <!-- 订单信息 -->
      <div class="order-info">
        <div class="order-title">{{ orderInfo.serviceName }}</div>
        <div class="order-detail">
          <span>订单号：{{ orderInfo.orderNo }}</span>
          <span>服务时间：{{ orderInfo.serviceTime }}</span>
        </div>
        <div class="volunteer-info">
          <span>服务志愿者：{{ orderInfo.volunteerName }}</span>
        </div>
      </div>

      <!-- 评分区域 -->
      <div class="rating-section">
        <h3 class="section-title">服务评分</h3>
        <el-rate v-model="rating" show-text size="large" />
      </div>

      <!-- 评价内容 -->
      <div class="comment-section">
        <h3 class="section-title">评价内容</h3>
        <el-input
          v-model="comment"
          type="textarea"
          :rows="4"
          placeholder="请输入您的评价（可选）"
          maxlength="200"
          show-word-limit
          size="large"
        />
      </div>

      <!-- 按钮区域 -->
      <div class="action-buttons">
        <el-button size="large" @click="$router.back()">返回</el-button>
        <el-button type="primary" size="large" @click="submitReview" :loading="loading">
          提交评价
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
// import { submitOrderReview } from '@/api/user'

const route = useRoute()
const router = useRouter()
const loading = ref(false)

// 订单信息
const orderInfo = ref({
  orderNo: '',
  serviceName: '',
  serviceTime: '',
  volunteerName: ''
})

// 评分和评价
const rating = ref(0)
const comment = ref('')

onMounted(() => {
  const orderNo = route.params.orderNo as string
  if (orderNo) {
    fetchOrderInfo(orderNo)
  }
})

// 获取订单信息（接口已注释）
const fetchOrderInfo = async (orderNo: string) => {
  try {
    // --- 后端接口调用（已注释） ---
    /*
    const res = await getOrderInfo(orderNo)
    orderInfo.value = res.data
    */

    // --- 模拟数据 ---
    orderInfo.value = {
      orderNo: 'ORD20260518001',
      serviceName: '代购服务-生活用品代购',
      serviceTime: '2026-05-18 09:15:00',
      volunteerName: '李志愿者'
    }
  } catch (err) {
    console.error('获取订单信息失败', err)
    ElMessage.error('获取订单信息失败')
  }
}

// 提交评价
const submitReview = async () => {
  if (rating.value === 0) {
    ElMessage.warning('请选择服务评分')
    return
  }

  loading.value = true
  try {
    // --- 后端接口调用（已注释） ---
    /*
    await submitOrderReview({
      orderNo: orderInfo.value.orderNo,
      rating: rating.value,
      comment: comment.value
    })
    */

    // 模拟提交延迟
    await new Promise(resolve => setTimeout(resolve, 800))

    ElMessage.success('评价提交成功！感谢您的反馈')
    router.push('/user/order')
  } catch (err) {
    console.error('提交评价失败', err)
    ElMessage.error('提交评价失败，请重试')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.page-container {
  width: 100%;
  padding: 10px 0;
  max-width: 800px;
  margin: 0 auto;
}

.header-row {
  margin-bottom: 24px;
}

.page-title {
  font-size: 32px;
  font-weight: bold;
  color: #222;
  margin: 0;
}

.review-card {
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 4px 16px rgba(0, 184, 153, 0.1);
  padding: 40px;
}

.order-info {
  margin-bottom: 32px;
  padding-bottom: 24px;
  border-bottom: 2px solid #eee;
}

.order-title {
  font-size: 24px;
  font-weight: 600;
  color: #006d5c;
  margin-bottom: 16px;
}

.order-detail {
  display: flex;
  gap: 32px;
  font-size: 18px;
  color: #666;
  margin-bottom: 12px;
}

.volunteer-info {
  font-size: 18px;
  color: #333;
}

.section-title {
  font-size: 20px;
  font-weight: 600;
  color: #333;
  margin-bottom: 16px;
}

.rating-section {
  margin-bottom: 32px;
}

.comment-section {
  margin-bottom: 40px;
}

.action-buttons {
  display: flex;
  gap: 20px;
  justify-content: center;
}

:deep(.el-button) {
  font-size: 18px;
  padding: 12px 32px;
}
</style>
