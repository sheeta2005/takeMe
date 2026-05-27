<template>
  <div class="page-container">
    <!-- 页面加载骨架屏 -->
    <div v-if="loading" class="skeleton-container">
      <el-skeleton :rows="8" animated />
    </div>

    <!-- 所有非加载状态的内容都放在同一个 v-else 里 -->
    <div v-else>
      <div class="header-row">
        <h2 class="page-title">订单详情</h2>
      </div>

      <div class="detail-card">
        <!-- 订单信息 -->
        <div class="detail-section">
          <div class="section-title">订单信息</div>
          <div class="info-grid">
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
              <span class="label">订单金额：</span>
              <span class="value price">¥{{ orderInfo.price }}</span>
            </div>
            <div class="info-item">
              <span class="label">下单时间：</span>
              <span class="value">{{ orderInfo.createTime }}</span>
            </div>
            <div class="info-item">
              <span class="label">服务时间：</span>
              <span class="value">{{ orderInfo.serviceTime }}</span>
            </div>
            <div class="info-item full-width">
              <span class="label">服务地址：</span>
              <span class="value">{{ orderInfo.address }}</span>
            </div>
          </div>
        </div>

        <!-- 志愿者信息（美化版） -->
        <div class="detail-section">
          <div class="section-title">服务志愿者</div>
          <div class="volunteer-card" v-if="orderInfo.volunteerName">
            <img class="volunteer-avatar" :src="orderInfo.volunteerAvatar || defaultAvatar" alt="志愿者头像" />
            <div class="volunteer-info">
              <div class="volunteer-name">{{ orderInfo.volunteerName }}</div>
              <div class="volunteer-phone">{{ orderInfo.volunteerPhone }}</div>
            </div>
            <el-button
              v-if="[1, 2].includes(orderInfo.status)"
              type="success"
              size="large"
              @click="callVolunteer(orderInfo.volunteerPhone)"
            >
              一键拨打
            </el-button>
          </div>
          <div v-else class="empty-volunteer">
            <el-icon size="40" color="#999"><User /></el-icon>
            <span>等待志愿者接单中...</span>
          </div>
        </div>

        <!-- 订单备注 -->
        <div class="detail-section">
          <div class="section-title">订单备注</div>
          <p class="remark-text">{{ orderInfo.remark || '无特殊要求' }}</p>
        </div>

        <!-- 操作按钮区域（逻辑修正版） -->
        <div class="action-section">
          <!-- 0待接单 / 1已接单：取消 + 修改 -->
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

          <!-- 2服务中：仅取消 -->
          <el-button
            v-if="orderInfo.status === 2"
            type="danger"
            size="large"
            @click="handleCancelOrder"
            :loading="cancelLoading"
          >
            取消订单
          </el-button>

          <!-- 3待确认：确认完成 -->
          <el-button
            v-if="orderInfo.status === 3"
            type="success"
            size="large"
            @click="confirmCompleted"
            :loading="confirmLoading"
          >
            确认服务已完成
          </el-button>

          <!-- 4已完成：评价 -->
          <el-button
            v-if="orderInfo.status === 4 && !orderInfo.hasReviewed"
            type="warning"
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
    </div>

    <!-- 修改订单弹窗（优化版：用和下单页一致的时间选择） -->
    <el-dialog
      v-model="editVisible"
      title="修改订单信息"
      width="700px"
      @close="cancelEdit"
      destroy-on-close
    >
      <el-form :model="editForm" label-width="100px" size="large" ref="editFormRef">
        <el-form-item label="服务日期" prop="serviceDate" required>
          <div class="date-buttons">
            <el-button
              v-for="day in dateList"
              :key="day.value"
              :type="editForm.serviceDate === day.value ? 'primary' : 'default'"
              size="large"
              @click="editForm.serviceDate = day.value"
            >
              {{ day.label }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="服务时间" prop="serviceTimeSlot" required>
          <div class="time-buttons">
            <el-button
              v-for="slot in availableTimeSlots"
              :key="slot.value"
              :type="editForm.serviceTimeSlot === slot.value ? 'primary' : 'default'"
              size="large"
              @click="editForm.serviceTimeSlot = slot.value"
            >
              {{ slot.label }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="服务地址" prop="address" required>
          <el-input v-model="editForm.address" type="textarea" :rows="2" placeholder="请输入服务地址" />
        </el-form-item>
        <el-form-item label="订单备注" prop="remark">
          <el-input v-model="editForm.remark" type="textarea" :rows="2" placeholder="请输入特殊要求" />
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
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, ElForm } from 'element-plus'
import { User } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const orderNo = route.params.orderNo as string
const editFormRef = ref<InstanceType<typeof ElForm>>()

// 默认志愿者头像
const defaultAvatar = 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'

// 页面加载状态
const loading = ref(true)

// 订单信息（新增price、volunteerAvatar字段）
const orderInfo = ref({
  orderNo: '',
  serviceType: '',
  status: 0,
  price: 0,
  createTime: '',
  serviceTime: '',
  address: '',
  volunteerName: '',
  volunteerPhone: '',
  volunteerAvatar: '',
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
  serviceDate: '',
  serviceTimeSlot: '',
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
  const map = {
    0: 'warning',
    1: 'primary',
    2: 'primary',
    3: 'info',
    4: 'success',
    5: 'danger'
  }
  return map[status] || 'info'
}

// ======================
// 修改订单用的时间逻辑（和下单页完全一致）
// ======================
const now = new Date()
const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())

const dateList = computed(() => {
  const days = []
  for (let i = 0; i < 3; i++) {
    const d = new Date(today.getTime() + i * 24 * 60 * 60 * 1000)
    const weekDays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
    const weekDay = weekDays[d.getDay()]
    const label = i === 0 ? `今天 ${weekDay}` : i === 1 ? `明天 ${weekDay}` : `后天 ${weekDay}`
    const value = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
    days.push({ label, value })
  }
  return days
})

const allTimeSlots = [
  { value: '09:00', label: '上午 9点' },
  { value: '10:00', label: '上午 10点' },
  { value: '11:00', label: '上午 11点' },
  { value: '14:00', label: '下午 2点' },
  { value: '15:00', label: '下午 3点' },
  { value: '16:00', label: '下午 4点' },
  { value: '17:00', label: '下午 5点' }
]

const availableTimeSlots = computed(() => {
  if (editForm.value.serviceDate !== dateList.value[0].value) return allTimeSlots
  const currentHour = now.getHours()
  return allTimeSlots.filter(slot => {
    const h = parseInt(slot.value.split(':')[0])
    return h > currentHour
  })
})

// ======================
// 核心功能
// ======================
// 一键拨打志愿者电话
const callVolunteer = (phone: string) => {
  // 移动端直接拨号，PC端提示复制
  if (/Android|iPhone|iPad/i.test(navigator.userAgent)) {
    window.location.href = `tel:${phone}`
  } else {
    navigator.clipboard.writeText(phone).then(() => {
      ElMessage.success('志愿者电话已复制到剪贴板')
    }).catch(() => {
      ElMessage.info(`志愿者电话：${phone}`)
    })
  }
}

// 获取订单详情
const fetchOrderDetail = async () => {
  loading.value = true
  try {
    // 模拟接口请求
    await new Promise(r => setTimeout(r, 500))

    // 模拟不同状态的订单数据（可修改status测试不同按钮显示）
    orderInfo.value = {
      orderNo: 'ORD202605270001',
      serviceType: '助洁服务-日常保洁2小时',
      status: 1, // 0待接单/1已接单/2服务中/3待确认/4已完成
      price: 30,
      createTime: '2026-05-27 10:30:00',
      serviceTime: '2026-05-28 14:00:00',
      address: '西安市雁塔区科技二路66号',
      volunteerName: '张志愿者',
      volunteerPhone: '13800138000',
      volunteerAvatar: '',
      remark: '请帮忙打扫厨房和卫生间',
      hasReviewed: false
    }
  } catch (err) {
    ElMessage.error('获取订单详情失败，请刷新重试')
  } finally {
    loading.value = false
  }
}

// 取消订单
const handleCancelOrder = async () => {
  const tips = orderInfo.value.status === 2
    ? '服务正在进行中，取消订单可能会产生费用，确定要取消吗？'
    : '确定要取消该订单吗？'

  await ElMessageBox.confirm(tips, '提示', {
    confirmButtonText: '确定取消',
    cancelButtonText: '返回',
    type: 'warning'
  }).catch(() => {
    ElMessage.info('已取消操作')
    return false
  })

  cancelLoading.value = true
  try {
    // await cancelOrderApi(orderNo)
    await new Promise(r => setTimeout(r, 800))
    orderInfo.value.status = 5
    ElMessage.success('订单已取消')
  } catch (err) {
    ElMessage.error('取消失败，请稍后重试')
  } finally {
    cancelLoading.value = false
  }
}

// 打开修改弹窗
const openEditModal = () => {
  // 解析原服务时间
  const [date, time] = orderInfo.value.serviceTime.split(' ')
  editForm.value = {
    serviceDate: date,
    serviceTimeSlot: time.substring(0, 5),
    address: orderInfo.value.address,
    remark: orderInfo.value.remark
  }
  editVisible.value = true
}
const cancelEdit = () => {
  editVisible.value = false
  editFormRef.value?.resetFields()
}

// 保存修改
const saveEdit = async () => {
  // 表单校验
  if (!editForm.value.serviceDate || !editForm.value.serviceTimeSlot || !editForm.value.address) {
    ElMessage.warning('请填写完整的订单信息')
    return
  }

  editLoading.value = true
  try {
    // 拼接完整服务时间
    const fullTime = `${editForm.value.serviceDate} ${editForm.value.serviceTimeSlot}:00`

    // await updateOrderApi(orderNo, {
    //   serviceTime: fullTime,
    //   address: editForm.value.address,
    //   remark: editForm.value.remark
    // })
    await new Promise(r => setTimeout(r, 800))

    // 更新本地数据
    orderInfo.value.serviceTime = fullTime
    orderInfo.value.address = editForm.value.address
    orderInfo.value.remark = editForm.value.remark

    editVisible.value = false
    ElMessage.success('订单修改成功')
  } catch (err) {
    ElMessage.error('修改失败，请稍后重试')
  } finally {
    editLoading.value = false
  }
}

// 确认服务完成
const confirmCompleted = async () => {
  await ElMessageBox.confirm('确认志愿者已完成服务吗？', '提示', {
    confirmButtonText: '确认完成',
    cancelButtonText: '返回',
    type: 'success'
  }).catch(() => {
    ElMessage.info('已取消操作')
    return false
  })

  confirmLoading.value = true
  try {
    // await confirmOrderApi(orderNo)
    await new Promise(r => setTimeout(r, 800))
    orderInfo.value.status = 4
    ElMessage.success('已确认完成，感谢您的评价')
  } catch (err) {
    ElMessage.error('操作失败，请稍后重试')
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
  padding: 20px 0;
  max-width: 900px;
  margin: 0 auto;
}
.skeleton-container {
  padding: 40px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 4px 16px rgba(0, 184, 153, 0.1);
}
.header-row {
  margin-bottom: 24px;
}
.page-title {
  font-size: 32px;
  font-weight: bold;
  color: #333;
  margin: 0;
}
.detail-card {
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 4px 16px rgba(0, 184, 153, 0.1);
  padding: 40px;
  margin-bottom: 24px;
}
.detail-section {
  margin-bottom: 32px;
  padding-bottom: 32px;
  border-bottom: 1px solid #eee;
}
.detail-section:last-child {
  border: none;
  margin-bottom: 0;
  padding-bottom: 0;
}
.section-title {
  font-size: 22px;
  font-weight: 600;
  margin-bottom: 24px;
  color: #333;
}
.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}
.info-item.full-width {
  grid-column: 1 / -1;
}
.info-item {
  display: flex;
  align-items: center;
}
.label {
  width: 100px;
  font-size: 18px;
  color: #666;
  flex-shrink: 0;
}
.value {
  font-size: 18px;
  color: #333;
}
.value.price {
  font-size: 22px;
  font-weight: bold;
  color: #f5222d;
}

/* 志愿者卡片 */
.volunteer-card {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 20px;
  background: #f8faf9;
  border-radius: 12px;
}
.volunteer-avatar {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #00a88d;
}
.volunteer-info {
  flex: 1;
}
.volunteer-name {
  font-size: 22px;
  font-weight: 600;
  color: #333;
  margin-bottom: 4px;
}
.volunteer-phone {
  font-size: 18px;
  color: #666;
}
.empty-volunteer {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 40px;
  color: #999;
  font-size: 18px;
}

.remark-text {
  font-size: 18px;
  line-height: 1.6;
  color: #333;
  padding: 16px;
  background: #f8faf9;
  border-radius: 8px;
}

.action-section {
  margin-top: 20px;
  display: flex;
  justify-content: center;
  gap: 20px;
  flex-wrap: wrap;
}
.action-buttons {
  display: flex;
  justify-content: center;
  margin-top: 12px;
}

/* 修改订单弹窗样式 */
.date-buttons, .time-buttons {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}
.date-buttons .el-button, .time-buttons .el-button {
  height: 50px;
  font-size: 18px !important;
}

:deep(.el-button) {
  font-size: 18px !important;
  padding: 12px 32px !important;
}
:deep(.el-tag) {
  font-size: 18px !important;
  padding: 6px 16px !important;
}
</style>
