<template>
  <div class="review-page">
    <h2 class="page-title">{{ selectedItem?.reviewed ? '修改服务评价' : '评价服务' }}</h2>

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
        <div class="info-item">
          <span class="label">评价服务：</span>
          <span class="value">{{ selectedItem?.serviceName }}</span>
        </div>
        <div class="info-item">
          <span class="label">预约时间：</span>
          <span class="value">{{ selectedItem?.serviceDate }} {{ selectedItem?.serviceTime }}</span>
        </div>
      </div>

      <el-form
        v-if="selectedItem"
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
import { getUserOrderDetail, evaluateOrderItem } from '@/api/order'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const submitting = ref(false)
const formRef = ref<any>(null)
const selectedItem = ref<any>(null)

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

//加载待评价订单详情
const fetchOrderDetail = async () => {
  const orderId = Number(route.params.id)
  const orderItemId = Number(route.query.orderItemId)
  if (!Number.isSafeInteger(orderId) || orderId <= 0 || !Number.isSafeInteger(orderItemId) || orderItemId <= 0) {
    ElMessage.error('请选择需要评价的服务项')
    router.back()
    return
  }

  loading.value = true
  try {
    const res = await getUserOrderDetail(orderId)
    if (res.code === 200 && res.data) {
      orderInfo.value = res.data
      // 只允许选择当前订单内的服务项，按已有评价回填评分和文字。
      const item = res.data.items?.find((item: any) => item.id === orderItemId)
      if (!item || ![3, 4].includes(item.itemStatus) || res.data.status !== 4) {
        ElMessage.error('该服务项暂不可评价')
        return
      }
      selectedItem.value = item
      form.value.score = item.review?.rating ?? 5
      form.value.content = item.review?.comment ?? ''
    }
  } catch (e) {
    console.error('获取订单详情失败:', e)
    ElMessage.error('获取订单详情失败')
  } finally {
    loading.value = false
  }
}

//提交服务评分与评价
const submitReview = async () => {
  if (submitting.value || !selectedItem.value) return
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid || submitting.value) return

  submitting.value = true
  try {
    await evaluateOrderItem(selectedItem.value.id, form.value.score, form.value.content || '')
    ElMessage.success('评价成功')
    // 返回详情重新读取各服务项评价状态，支持继续评价同一订单内的其他服务。
    router.replace(`/user/order/detail/${orderInfo.value.id}`)
  } catch (e: any) {
    console.error('评价失败:', e)
    ElMessage.error(e.message || '评价失败')
  } finally {
    submitting.value = false
  }
}

//返回上一页
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
