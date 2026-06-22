<template>
  <div class="page-container">
    <el-card class="filter-card" shadow="hover">
      <el-form :inline="true" :model="filterForm" class="filter-form">
        <el-form-item label="订单状态">
          <el-select v-model="filterForm.status" placeholder="全部" clearable @change="handleFilter" style="width: 140px">
            <el-option label="未支付" :value="6" />
            <el-option label="待接单" :value="0" />
            <el-option label="已接单" :value="1" />
            <el-option label="服务中" :value="2" />
            <el-option label="待确认" :value="3" />
            <el-option label="已完成" :value="4" />
            <el-option label="已取消" :value="5" />
          </el-select>
        </el-form-item>
        <el-form-item label="订单编号">
          <el-input v-model="filterForm.orderNo" placeholder="请输入订单编号" clearable @clear="handleFilter" />
        </el-form-item>
        <el-form-item label="创建时间">
          <el-date-picker
            v-model="filterForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            @change="handleFilter"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleFilter">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-icon :size="20" color="#00a88d"><Document /></el-icon>
            <span class="card-title">订单列表</span>
          </div>
        </div>
      </template>

      <el-table :data="orderList" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="orderNo" label="订单编号" width="180">
          <template #default="{ row }">
            <span class="order-no">{{ row.orderNo }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="userId" label="用户ID" width="100" />
<!--        <el-table-column label="志愿者ID" width="120">-->
<!--          <template #default="{ row }">-->
<!--            <span>{{ parseVolunteerId(row.volunteerIds) }}</span>-->
<!--          </template>-->
<!--        </el-table-column>-->
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="default">
              {{ getStatusName(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="总金额" width="120">
          <template #default="{ row }">
            <span class="amount">¥{{ row.totalPrice?.toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" fixed="right" width="220">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleDetail(row)">
              <el-icon><View /></el-icon>
              详情
            </el-button>
            <el-button v-if="row.status === 0 || row.status === 1 || row.status === 2" type="success" link size="small" @click="handleComplete(row)">
              <el-icon><CircleCheck /></el-icon>
              完成
            </el-button>
            <el-button v-if="row.status !== 4 && row.status !== 5 && row.status !== 6" type="danger" link size="small" @click="handleCancel(row)">
              <el-icon><Close /></el-icon>
              取消
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="fetchOrders"
        @current-change="fetchOrders"
        style="margin-top: 24px; justify-content: flex-end"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Document, View, CircleCheck, Close } from '@element-plus/icons-vue'
import { searchOrder, getOrderDetail, cancelOrder, completeOrder } from '@/api/admin'

const router = useRouter()

const loading = ref(false)
const orderList = ref<any[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const filterForm = reactive({
  status: undefined as number | undefined,
  orderNo: '',
  dateRange: [] as string[]
})

// 解析志愿者ID（volunteerIds是字符串格式）
const parseVolunteerId = (volunteerIds: string) => {
  if (!volunteerIds) return '未分配'
  // 如果包含逗号，取第一个ID
  const ids = volunteerIds.split(',')
  return ids[0] || '未分配'
}

const getStatusName = (status: number) => {
  const names = ['待接单', '已接单', '服务中', '待确认', '已完成', '已取消', '未支付']
  return names[status] || '未知'
}

const getStatusType = (status: number) => {
  const types = ['warning', 'primary', 'warning', 'warning', 'success', 'danger', 'info']
  return types[status] || ''
}

const fetchOrders = async () => {
  loading.value = true
  try {
    const [startDate, endDate] = filterForm.dateRange || []
    const res = await searchOrder(
      currentPage.value,
      pageSize.value,
      filterForm.status,
      filterForm.orderNo || undefined,
      undefined,
      undefined,
      startDate,
      endDate
    )
    orderList.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (err) {
    console.error('获取订单列表失败', err)
    ElMessage.error('获取订单列表失败')
  } finally {
    loading.value = false
  }
}

const handleFilter = () => {
  currentPage.value = 1
  fetchOrders()
}

const handleReset = () => {
  filterForm.status = undefined
  filterForm.orderNo = ''
  filterForm.dateRange = []
  handleFilter()
}

const handleDetail = (row: any) => {
  router.push({ name: 'AdminOrderDetail', params: { id: row.id } })
}

const handleComplete = async (row: any) => {
  try {
    await ElMessageBox.confirm('确定完成该订单吗？', '提示', { type: 'warning' })
    await completeOrder(row.id)
    ElMessage.success('订单已完成')
    fetchOrders()
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

const handleCancel = async (row: any) => {
  try {
    await ElMessageBox.confirm('确定取消该订单吗？', '提示', { type: 'warning' })
    await cancelOrder(row.id)
    ElMessage.success('订单已取消')
    fetchOrders()
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

onMounted(() => {
  fetchOrders()
})
</script>

<style scoped>
.page-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px 0;
}

.filter-card {
  margin-bottom: 24px;
  border: 1px solid #e2e8f0;
}

.filter-form {
  margin-bottom: 0;
}

.table-card {
  border: 1px solid #e2e8f0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
}

.order-no {
  font-family: 'Courier New', monospace;
  font-weight: 600;
  color: #3b82f6;
}

.amount {
  font-weight: 600;
  color: #f5222d;
}
</style>
