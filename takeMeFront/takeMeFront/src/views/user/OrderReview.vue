<template>
  <div class="review-page">
    <h2 class="page-title">评价订单</h2>

    <div class="review-card" v-loading="loading">
      <div class="order-info">
        <div class="info-item">
          <span class="label">订单号：</span>
          <span class="value">{{ orderInfo.orderNo }}</span>
        </div>
        <div class="info-item">
          <span class="label">订单金额：</span>
          <span class="value price">¥{{ orderInfo.totalPrice }}</span>
        </div>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
        class="review-form"
      >
        <el-form-item label="评分" prop="score">
          <el-rate
            v-model="form.score"
            :max="5"
            show-score
            :colors="['#99A9BF', '#F7BA2A', '#FF9900']"
            score-template="{value} 分"
          />
        </el-form-item>

        <el-form-item label="评价内容">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="5"
            placeholder="请输入您的评价内容（选填）"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" size="large" @click="submitReview" :loading="submitting">
            提交评价
          </el-button>
          <el-button size="large" @click="goBack">返回</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getUserOrderDetail, evaluateOrder } from '@/api/order'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const submitting = ref(false)
const formRef = ref<any>(null)

const orderInfo = ref<any>({
  orderNo: '',
  totalPrice: 0
})

const form = ref({
  score: 5,
  content: ''
})

const rules = {
  score: [
    { required: true, message: '请选择评分', trigger: 'change' }
  ]
}

const fetchOrderDetail = async () => {
  const orderId = Number(route.params.id)
  if (!orderId || isNaN(orderId)) {
    ElMessage.error('订单ID无效')
    router.back()
    return
  }

  loading.value = true
  try {
    const res = await getOrderDetail(orderId)
    if (res.code === 200 && res.data) {
      orderInfo.value = res.data
    }
  } catch (e) {
    console.error('获取订单详情失败:', e)
    ElMessage.error('获取订单详情失败')
  } finally {
    loading.value = false
  }
}

const submitReview = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  const orderId = Number(route.params.id)
  if (!orderId || isNaN(orderId)) {
    ElMessage.error('订单ID无效')
    return
  }

  submitting.value = true
  try {
    await evaluateOrder(orderId)
    ElMessage.success('评价成功')
    router.replace('/user/order')
  } catch (e) {
    console.error('评价失败:', e)
    ElMessage.error('评价失败')
  } finally {
    submitting.value = false
  }
}

const goBack = () => router.back()

onMounted(() => {
  fetchOrderDetail()
})
</script>

<style scoped>
.review-page {
  max-width: 700px;
  margin: 0 auto;
  padding: 20px 0;
}

.page-title {
  font-size: 32px;
  font-weight: bold;
  color: #333;
  margin-bottom: 32px;
  text-align: center;
}

.review-card {
  background: #fff;
  border-radius: 16px;
  padding: 40px;
  box-shadow: 0 4px 16px rgba(0, 184, 153, 0.1);
}

.order-info {
  margin-bottom: 32px;
  padding: 20px;
  background: #f8faf9;
  border-radius: 8px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
}

.info-item:last-child {
  margin-bottom: 0;
}

.label {
  font-size: 18px;
  color: #666;
}

.value {
  font-size: 18px;
  color: #333;
  font-weight: 500;
}

.value.price {
  color: #f56c6c;
}

.review-form {
  margin-top: 20px;
}

:deep(.el-button) {
  font-size: 18px;
  padding: 12px 32px;
}
</style>
