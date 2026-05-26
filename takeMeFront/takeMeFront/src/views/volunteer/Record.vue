<template>
  <div class="record-container">
    <h2 class="page-title">服务记录</h2>
    <div class="record-list">
      <div class="record-card"
           v-for="record in pageRecords"
           :key="record.id"
           @click="goToDetail(record.id)">
        <div class="record-header">
          <span class="record-id">订单号：{{ record.id }}</span>
          <el-tag type="success" size="large">已完成</el-tag>
        </div>
        <div class="record-body">
          <div class="service-type">{{ record.serviceType }}</div>
          <div class="service-time">服务时间：{{ record.serviceTime }}</div>
          <div class="evaluation">老人评价：{{ record.evaluation }}</div>
        </div>
      </div>
    </div>

    <!-- 分页 -->
    <div class="pagination">
      <el-pagination
        :current-page="currentPage"
        :page-size="pageSize"
        :total="recordList.length"
        layout="prev, pager, next"
        @current-change="handlePageChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
// import { getServiceRecord } from '@/api/volunteer' // 后端接口，先注释

const router = useRouter()

// 分页参数
const currentPage = ref(1)
const pageSize = ref(2) // 每页2条

// 模拟服务记录数据（可以后续用后端接口替换）
const recordList = ref([
  { id: 'ORD20260518001', serviceType: '助餐服务-营养套餐A', serviceTime: '2026-05-18 11:30', evaluation: '服务态度很好，配送准时' },
  { id: 'ORD20260517001', serviceType: '助洁服务-日常保洁', serviceTime: '2026-05-17 14:00', evaluation: '打扫得很干净，很满意' },
  { id: 'ORD20260516001', serviceType: '陪医服务-陪同就诊', serviceTime: '2026-05-16 09:00', evaluation: '陪同很耐心，非常感谢' },
  { id: 'ORD20260515001', serviceType: '代购服务-生活用品', serviceTime: '2026-05-15 10:00', evaluation: '东西买得很全，很贴心' }
])

// 当前页数据（分页计算）
const pageRecords = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return recordList.value.slice(start, end)
})

// 分页切换
const handlePageChange = (page: number) => {
  currentPage.value = page
}

// 跳转到订单详情（通过订单ID调用后端）
const goToDetail = async (orderId: string) => {
  try {
    // 后端接口预留（后期打开注释）
    // const res = await getServiceRecordDetail(orderId)
    // if (res.code !== 200) throw new Error('获取详情失败')

    // 模拟接口调用提示
    ElMessage.info(`正在加载订单 ${orderId} 详情...`)
    // 跳转到订单详情页
    router.push(`/volunteer/order/${orderId}`)
  } catch {
    ElMessage.error('加载订单详情失败')
  }
}

// 页面加载时（模拟后端获取数据）
onMounted(async () => {
  // 后端接口预留（后期打开注释）
  /*
  const res = await getServiceRecord({ page: currentPage.value, pageSize: pageSize.value })
  if (res.code === 200) {
    recordList.value = res.data.list
  }
  */
})
</script>

<style scoped>
.record-container {
  max-width: 900px;
  margin: 0 auto;
}
.page-title {
  font-size: 28px;
  font-weight: bold;
  color: #333;
  margin-bottom: 24px;
}
.record-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 24px;
}
.record-card {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 12px rgba(0, 184, 153, 0.08);
  cursor: pointer;
  transition: all 0.2s;
}
.record-card:hover {
  box-shadow: 0 6px 16px rgba(0, 184, 153, 0.12);
  transform: translateY(-2px);
}
.record-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.record-id { font-size: 18px; color: #666; }
.record-body .service-type { font-size: 20px; font-weight: 600; color: #006d5c; margin-bottom: 8px; }
.record-body .service-time, .record-body .evaluation { font-size: 16px; color: #999; margin-bottom: 4px; }
.pagination {
  display: flex;
  justify-content: center;
}
</style>
