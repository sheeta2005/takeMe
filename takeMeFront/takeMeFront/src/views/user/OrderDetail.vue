<template>
  <div class="page-container">
    <div class="header-row">
      <h2 class="page-title">订单详情</h2>
    </div>

    <div class="detail-card">
      <!-- 订单信息 -->
      <div class="detail-section">
        <div class="section-title">订单信息</div>
        <div class="info-item">
          <span class="label">订单号：</span>
          <span class="value">{{ orderInfo.orderNo }}</span>
        </div>
        <div class="info-item">
          <span class="label">服务类型：</span>
          <span class="value">{{ orderInfo.serviceType }}</span>
        </div>
        <div class="info-item">
          <span class="label">服务状态：</span>
          <el-tag :type="getStatusTagType(orderInfo.status)" size="large">
            {{ getStatusText(orderInfo.status) }}
          </el-tag>
        </div>
        <div class="info-item">
          <span class="label">下单时间：</span>
          <span class="value">{{ orderInfo.createTime }}</span>
        </div>
        <div class="info-item">
          <span class="label">服务时间：</span>
          <span class="value">{{ orderInfo.serviceTime }}</span>
        </div>
        <div class="info-item">
          <span class="label">服务地址：</span>
          <span class="value">{{ orderInfo.address }}</span>
        </div>
      </div>

      <!-- 志愿者信息 -->
      <div class="detail-section">
        <div class="section-title">服务志愿者</div>
        <div class="volunteer-info">
          <span class="volunteer-name">{{ orderInfo.volunteerName || '待接单' }}</span>
          <span class="volunteer-phone">{{ orderInfo.volunteerPhone || '-' }}</span>
        </div>
      </div>

      <!-- 订单备注 -->
      <div class="detail-section">
        <div class="section-title">订单备注</div>
        <p class="remark-text">{{ orderInfo.remark || '无' }}</p>
      </div>

      <!-- 操作按钮区域（根据状态显示） -->
      <div class="action-section">
        <!-- 0待接单 / 1已接单 可以取消 + 修改 -->
        <el-button
          v-if="[0, 1].includes(orderInfo.status)"
          type="danger"
          size="large"
          @click="handleCancelOrder"
          :loading="cancelLoading"
        >
          取消订单
        </el-button>

        <el-button
          v-if="[0, 1].includes(orderInfo.status)"
          type="primary"
          size="large"
          @click="openEditModal"
        >
          修改订单
        </el-button>

        <!-- 3待确认 → 确认完成 -->
        <el-button
          v-if="orderInfo.status === 3"
          type="primary"
          size="large"
          @click="confirmCompleted"
          :loading="confirmLoading"
        >
          确认服务已完成
        </el-button>

        <!-- 4已完成 → 评价 -->
        <el-button
          v-if="orderInfo.status === 4 && !orderInfo.hasReviewed"
          type="primary"
          size="large"
          @click="goToReview"
        >
          评价服务
        </el-button>
      </div>
    </div>

    <div class="action-buttons">
      <el-button size="large" @click="$router.back()">返回订单列表</el-button>
    </div>

    <!-- 修改订单弹窗 -->
    <el-dialog
      v-model="editVisible"
      title="修改订单信息"
      width="600px"
      @close="cancelEdit"
    >
      <el-form :model="editForm" label-width="100px" size="large">
        <el-form-item label="服务时间">
          <el-input v-model="editForm.serviceTime" placeholder="请输入服务时间" />
        </el-form-item>
        <el-form-item label="服务地址">
          <el-input v-model="editForm.address" type="textarea" :rows="2" placeholder="请输入地址" />
        </el-form-item>
        <el-form-item label="订单备注">
          <el-input v-model="editForm.remark" type="textarea" :rows="2" placeholder="请输入备注" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="cancelEdit">取消</el-button>
        <el-button type="primary" @click="saveEdit" :loading="editLoading">保存修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const orderNo = route.params.orderNo as string

// 订单信息
const orderInfo = ref({
  orderNo: '',
  serviceType: '',
  status: 0,
  createTime: '',
  serviceTime: '',
  address: '',
  volunteerName: '',
  volunteerPhone: '',
  remark: '',
  hasReviewed: false
})

// 加载状态
const cancelLoading = ref(false)
const confirmLoading = ref(false)
const editLoading = ref(false)

// 编辑弹窗
const editVisible = ref(false)
const editForm = ref({
  serviceTime: '',
  address: '',
  remark: ''
})

// 状态映射
const statusMap = {
  0: '待接单',
  1: '已接单',
  2: '服务中',
  3: '待确认',
  4: '已完成',
  5: '已取消'
}
const getStatusText = (status: number) => statusMap[status] || '未知状态'
const getStatusTagType = (status: number) => {
  const map = { 0: 'warning',1: 'primary',2: 'primary',3: 'info',4: 'success',5: 'danger' }
  return map[status] || 'info'
}

// 获取订单详情
const fetchOrderDetail = async () => {
  try {
    // 模拟数据
    orderInfo.value = {
      orderNo: 'ORD20260518001',
      serviceType: '代购服务-生活用品代购',
      status: 0,       // 可测试 0/1/2/3/4
      createTime: '2026-05-18 08:00:00',
      serviceTime: '2026-05-19 09:00:00',
      address: '幸福小区3号楼2单元101室',
      volunteerName: '李志愿者',
      volunteerPhone: '138****5678',
      remark: '请帮忙购买米、油、盐',
      hasReviewed: false
    }
  } catch (err) {
    ElMessage.error('获取订单失败')
  }
}

// 取消订单
const handleCancelOrder = async () => {
  await ElMessageBox.confirm('确定要取消该订单吗？', '提示', {
    confirmButtonText: '确定取消',
    cancelButtonText: '返回',
    type: 'warning'
  }).catch(() => {
    ElMessage.info('已取消')
    return false
  })

  cancelLoading.value = true
  try {
    // await cancelOrderApi(orderNo)
    await new Promise(r => setTimeout(r, 800))
    orderInfo.value.status = 5
    ElMessage.success('订单已取消')
  } catch (err) {
    ElMessage.error('取消失败')
  } finally {
    cancelLoading.value = false
  }
}

// 打开修改弹窗
const openEditModal = () => {
  editForm.value = {
    serviceTime: orderInfo.value.serviceTime,
    address: orderInfo.value.address,
    remark: orderInfo.value.remark
  }
  editVisible.value = true
}
const cancelEdit = () => {
  editVisible.value = false
}

// 保存修改
const saveEdit = async () => {
  editLoading.value = true
  try {
    // await updateOrderApi(orderNo, editForm.value)
    await new Promise(r => setTimeout(r, 800))
    orderInfo.value.serviceTime = editForm.value.serviceTime
    orderInfo.value.address = editForm.value.address
    orderInfo.value.remark = editForm.value.remark
    editVisible.value = false
    ElMessage.success('修改成功')
  } catch (err) {
    ElMessage.error('修改失败')
  } finally {
    editLoading.value = false
  }
}

// 确认服务完成
const confirmCompleted = async () => {
  confirmLoading.value = true
  try {
    await new Promise(r => setTimeout(r, 800))
    orderInfo.value.status = 4
    ElMessage.success('已确认完成，可评价')
  } catch (err) {
    ElMessage.error('操作失败')
  } finally {
    confirmLoading.value = false
  }
}

// 去评价
const goToReview = () => {
  router.push(`/user/order/review/${orderNo}`)
}

onMounted(() => {
  fetchOrderDetail()
})
</script>

<style scoped>
.page-container {
  width: 100%;
  padding: 10px 0;
  max-width: 800px;
  margin: 0 auto;
}
.header-row { margin-bottom: 24px; }
.page-title { font-size: 32px; font-weight: bold; color: #222; margin: 0; }
.detail-card {
  background: #fff; border-radius: 16px;
  box-shadow: 0 4px 16px rgba(0, 184, 153, 0.1);
  padding: 40px; margin-bottom: 24px;
}
.detail-section {
  margin-bottom: 32px; padding-bottom: 32px;
  border-bottom: 2px solid #eee;
}
.detail-section:last-child { border: none; }
.section-title { font-size: 20px; font-weight: 600; margin-bottom: 20px; }
.info-item { display: flex; margin-bottom: 16px; }
.label { width: 120px; font-size: 18px; color: #666; }
.value { font-size: 18px; color: #333; }
.volunteer-info { display: flex; gap: 24px; align-items: center; }
.volunteer-name { font-size: 20px; font-weight: 500; }
.volunteer-phone { font-size: 18px; color: #666; }
.remark-text { font-size: 18px; line-height: 1.6; }

.action-section {
  margin-top: 32px;
  display: flex;
  justify-content: center;
  gap: 20px;
}
.action-buttons {
  display: flex;
  justify-content: center;
  margin-top: 12px;
}
:deep(.el-button) {
  font-size: 18px !important;
  padding: 12px 32px !important;
}
</style>
