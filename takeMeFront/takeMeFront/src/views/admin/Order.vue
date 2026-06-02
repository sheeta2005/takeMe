<template>
  <div class="order-container">
    <div class="header-row">
      <h2 class="page-title">订单管理</h2>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <div class="filter-item">
        <label class="filter-label">订单状态</label>
        <el-select v-model="filterStatus" placeholder="请选择状态" @change="fetchOrders">
          <el-option label="全部订单" value="" />
          <el-option label="待分配" :value="0" />
          <el-option label="服务中" :value="1" />
          <el-option label="已完成" :value="2" />
          <el-option label="已评价" :value="3" />
          <el-option label="已取消" :value="4" />
          <el-option label="已拒绝" :value="5" />
        </el-select>
      </div>

      <div class="filter-item">
        <label class="filter-label">订单编号</label>
        <el-input
          v-model="filterOrderNo"
          placeholder="请输入订单编号"
          clearable
          @keyup.enter="fetchOrders"
        />
      </div>

      <div class="filter-item">
        <label class="filter-label">用户姓名/ID</label>
        <el-input
          v-model="filterUserName"
          placeholder="请输入用户姓名或ID"
          clearable
          @keyup.enter="fetchOrders"
        />
      </div>

      <div class="filter-item">
        <label class="filter-label">志愿者姓名/ID</label>
        <el-input
          v-model="filterVolunteerName"
          placeholder="请输入志愿者姓名或ID"
          clearable
          @keyup.enter="fetchOrders"
        />
      </div>

      <div class="filter-item">
        <label class="filter-label">创建时间</label>
        <el-date-picker
          v-model="filterDateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          @change="fetchOrders"
        />
      </div>

      <el-button type="primary" @click="fetchOrders">查询</el-button>
      <el-button @click="resetFilter">重置</el-button>
    </div>

    <!-- 订单表格 -->
    <div class="table-card">
      <el-table :data="orderList" border stripe>
        <el-table-column prop="orderNo" label="订单编号" width="150" align="center" />
        <el-table-column label="服务类型" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getTypeTagType(getOrderType(row))">
              {{ getTypeText(getOrderType(row)) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="用户信息" width="150" align="center">
          <template #default="{ row }">
            <div class="user-info">
              <div class="info-name">用户ID: {{ row.userId }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="志愿者信息" width="150" align="center">
          <template #default="{ row }">
            <div class="volunteer-info">
              <div class="info-name">志愿者ID: {{ row.volunteerId }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="address" label="服务地址" align="center" show-overflow-tooltip />
        <el-table-column prop="serviceDate" label="服务日期" width="120" align="center" />
        <el-table-column prop="createTime" label="创建时间" width="180" align="center" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)" size="large">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="goToOrderDetail(row.id)">查看详情</el-button>
            <el-button
              v-if="row.status === 1"
              type="success"
              link
              @click="handleComplete(row)"
            >
              标记完成
            </el-button>
            <el-button
              v-if="row.status === 1"
              type="danger"
              link
              @click="handleCancel(row)"
            >
              取消订单
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 分页组件 -->
    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @change="fetchOrders"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { searchOrder, cancelOrder, completeOrder } from '@/api/admin'

const route = useRoute()
const router = useRouter()

const filterStatus = ref<number | ''>('')
const filterOrderNo = ref('')
const filterUserName = ref('')
const filterVolunteerName = ref('')
const filterDateRange = ref<string[]>([])

const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const orderList = ref<any[]>([])

onMounted(() => {
  if (route.query.status) {
    filterStatus.value = Number(route.query.status)
  }
  fetchOrders()
})

const fetchOrders = async () => {
  try {
    const startDate = filterDateRange.value?.[0] || undefined
    const endDate = filterDateRange.value?.[1] || undefined

    const res = await searchOrder(
      currentPage.value,
      pageSize.value,
      filterStatus.value !== '' ? filterStatus.value : undefined,
      filterOrderNo.value || undefined,
      undefined,
      filterUserName.value || undefined,
      undefined,
      filterVolunteerName.value || undefined,
      undefined,
      startDate,
      endDate
    )
    orderList.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (err) {
    console.error('获取订单失败', err)
    ElMessage.error('获取订单列表失败')
  }
}

const resetFilter = () => {
  filterStatus.value = ''
  filterOrderNo.value = ''
  filterUserName.value = ''
  filterVolunteerName.value = ''
  filterDateRange.value = []
  currentPage.value = 1
  fetchOrders()
}

const goToOrderDetail = (id: number) => {
  router.push(`/admin/order/detail/${id}`)
}

const handleComplete = async (row: any) => {
  ElMessageBox.confirm('确定标记该订单为完成吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await completeOrder(row.id)
      ElMessage.success('操作成功')
      fetchOrders()
    } catch (err) {
      ElMessage.error('操作失败')
    }
  })
}

const handleCancel = async (row: any) => {
  ElMessageBox.confirm('确定要取消该订单吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await cancelOrder(row.id)
      ElMessage.success('订单已取消')
      fetchOrders()
    } catch (err) {
      ElMessage.error('取消失败')
    }
  })
}

const getOrderType = (row: any) => {
  return row.type || 0
}

const getTypeText = (type: number) => {
  const map = ['代购服务', '助洁服务', '助餐服务', '助医服务', '陪伴服务']
  return map[type] || '未知'
}

const getTypeTagType = (type: number) => {
  const map = ['info', 'success', 'primary', 'danger', 'warning']
  return map[type] || ''
}

const getStatusText = (status: number) => {
  const map = ['待分配', '服务中', '已完成', '已评价', '已取消', '已拒绝']
  return map[status] || '未知'
}

const getStatusTagType = (status: number) => {
  const map = ['info', 'primary', 'success', 'success', 'danger', 'info']
  return map[status] || 'info'
}
</script>

<style scoped>
.order-container {
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

.filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  align-items: center;
  background: #fff;
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  margin-bottom: 20px;
}

.filter-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-label {
  font-size: 14px;
  color: #666;
  white-space: nowrap;
}

.table-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  padding: 20px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
  background: #fff;
  padding: 16px 20px;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.user-info, .volunteer-info {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.info-name {
  font-size: 14px;
  font-weight: 500;
}
</style>
