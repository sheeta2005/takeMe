<template>
  <div class="detail-container">
    <div class="header-row">
      <h2 class="page-title">订单详情</h2>
      <el-button @click="$router.back()">返回列表</el-button>
    </div>

    <div class="detail-card">
      <div class="detail-section">
        <h3 class="section-title">基础信息</h3>
        <div class="detail-row">
          <span class="label">订单号：</span>
          <span class="value">{{ orderDetail.id }}</span>
        </div>
        <div class="detail-row">
          <span class="label">服务类型：</span>
          <el-tag :type="getTypeTagType(orderDetail.type)">{{ getTypeText(orderDetail.type) }}</el-tag>
        </div>
        <div class="detail-row">
          <span class="label">订单状态：</span>
          <el-tag :type="getStatusTagType(orderDetail.status)" size="large">
            {{ getStatusText(orderDetail.status) }}
          </el-tag>
        </div>
        <div class="detail-row">
          <span class="label">创建时间：</span>
          <span class="value">{{ orderDetail.createTime }}</span>
        </div>
      </div>

      <div class="detail-section">
        <h3 class="section-title">用户信息</h3>
        <div class="detail-row">
          <span class="label">用户ID：</span>
          <el-button type="primary" link @click="goToUserDetail(orderDetail.userId)">
            {{ orderDetail.userId }}
          </el-button>
        </div>
        <div class="detail-row">
          <span class="label">用户姓名：</span>
          <span class="value">{{ orderDetail.userName }}</span>
        </div>
      </div>

      <div class="detail-section">
        <h3 class="section-title">志愿者信息</h3>
        <div class="detail-row">
          <span class="label">志愿者ID：</span>
          <el-button type="primary" link @click="goToVolunteerDetail(orderDetail.volunteerId)">
            {{ orderDetail.volunteerId }}
          </el-button>
        </div>
        <div class="detail-row">
          <span class="label">志愿者姓名：</span>
          <span class="value">{{ orderDetail.volunteerName }}</span>
        </div>
      </div>

      <div class="detail-section">
        <h3 class="section-title">服务信息</h3>
        <div class="detail-row">
          <span class="label">服务地址：</span>
          <span class="value">{{ orderDetail.address }}</span>
        </div>
        <div class="detail-row">
          <span class="label">备注：</span>
          <span class="value">{{ orderDetail.remark || '无' }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getOrderDetail } from '@/api/admin'

const route = useRoute()
const router = useRouter()

const orderDetail = ref<any>({})

onMounted(() => {
  const orderId = route.query.id as number
  if (orderId) {
    fetchOrderDetail(orderId)
  }
})

const fetchOrderDetail = async (id: number) => {
  try {
    // --- 接口调用已注释 ---
    /*
    const res = await getOrderDetail(id)
    orderDetail.value = res.data
    */

    // --- 模拟数据 ---
    orderDetail.value = {
      id: id,
      type: 'meal',
      status: 'active',
      createTime: '2026-05-21 10:30:00',
      userId: 2001,
      userName: '王奶奶',
      volunteerId: 3001,
      volunteerName: '小张',
      address: '幸福小区1栋',
      remark: '请提前10分钟到达'
    }
  } catch (err) {
    console.error('获取订单详情失败', err)
    ElMessage.error('获取订单详情失败')
  }
}

const goToUserDetail = (userId: number) => {
  router.push({ path: '/admin/user/detail', query: { id: userId } })
}

const goToVolunteerDetail = (volunteerId: number) => {
  router.push({ path: '/admin/volunteer/detail', query: { id: volunteerId } })
}

// 状态/类型映射
const getStatusText = (status: string) => {
  const map: Record<string, string> = {
    active: '服务中',
    completed: '已完成',
    cancelled: '已取消'
  }
  return map[status] || '未知'
}

const getStatusTagType = (status: string) => {
  const map: Record<string, string> = {
    active: 'primary',
    completed: 'success',
    cancelled: 'danger'
  }
  return map[status] || 'info'
}

const getTypeText = (type: string) => {
  const map: Record<string, string> = {
    meal: '助餐服务',
    clean: '助洁服务',
    medical: '助医服务',
    buy: '代购服务'
  }
  return map[type] || '其他'
}

const getTypeTagType = (type: string) => {
  const map: Record<string, string> = {
    meal: 'warning',
    clean: 'info',
    medical: 'danger',
    buy: 'success'
  }
  return map[type] || ''
}
</script>

<style scoped>
.detail-container {
  width: 100%;
  padding: 10px 0;
}

.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-title {
  font-size: 28px;
  font-weight: bold;
  color: #222;
  margin: 0;
}

.detail-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  padding: 30px;
}

.detail-section {
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 1px solid #eee;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin-bottom: 20px;
}

.detail-row {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
}

.label {
  width: 100px;
  font-size: 14px;
  color: #666;
}

.value {
  font-size: 14px;
  color: #333;
}
</style>
