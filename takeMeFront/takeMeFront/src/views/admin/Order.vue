<template>
  <div class="order-container">
    <div class="header-row">
      <h2 class="page-title">订单管理</h2>
    </div>

    <!-- 筛选栏（用户姓名/ID、志愿者ID支持） -->
    <div class="filter-bar">
      <div class="filter-item">
        <label class="filter-label">订单状态</label>
        <el-select v-model="filterStatus" placeholder="请选择状态" @change="fetchOrders">
          <el-option label="全部订单" value="" />
          <el-option label="服务中" value="active" />
          <el-option label="已完成" value="completed" />
          <el-option label="已取消" value="cancelled" />
        </el-select>
      </div>

      <div class="filter-item">
        <label class="filter-label">服务类型</label>
        <el-select v-model="filterType" placeholder="请选择类型" @change="fetchOrders">
          <el-option label="全部类型" value="" />
          <el-option label="助餐服务" value="meal" />
          <el-option label="助洁服务" value="clean" />
          <el-option label="助医服务" value="medical" />
          <el-option label="代购服务" value="buy" />
        </el-select>
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

    <!-- 订单表格（用户ID/志愿者ID列 + 跳转链接） -->
    <div class="table-card">
      <el-table :data="orderList" border stripe>
        <el-table-column prop="id" label="订单号" width="100" align="center" />
        <el-table-column prop="type" label="服务类型" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getTypeTagType(row.type)">
              {{ getTypeText(row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="用户信息" width="150" align="center">
          <template #default="{ row }">
            <div class="user-info">
              <div class="info-name">{{ row.userName }}</div>
              <el-button type="primary" link @click="goToUserDetail(row.userId)">
                ID: {{ row.userId }}
              </el-button>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="志愿者信息" width="150" align="center">
          <template #default="{ row }">
            <div class="volunteer-info">
              <div class="info-name">{{ row.volunteerName }}</div>
              <el-button type="primary" link @click="goToVolunteerDetail(row.volunteerId)">
                ID: {{ row.volunteerId }}
              </el-button>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="address" label="服务地址" align="center" />
        <el-table-column prop="createTime" label="创建时间" width="180" align="center" />
        <el-table-column prop="status" label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)" size="large">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" align="center">
          <template #default="{ row }">
            <el-button type="primary" link @click="goToOrderDetail(row.id)">查看详情</el-button>
            <el-button
              v-if="row.status === 'active'"
              type="success"
              link
              @click="handleComplete(row)"
            >
              标记完成
            </el-button>
            <el-button
              v-if="row.status === 'active'"
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
import { getMyOrderList, getOrderDetail, confirmOrder, cancelOrder } from '@/api'

const route = useRoute()
const router = useRouter()

// 筛选状态
const filterStatus = ref('')
const filterType = ref('')
const filterUserName = ref('')
const filterVolunteerName = ref('')
const filterDateRange = ref<string[]>([])

// 分页状态
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 订单列表数据
const orderList = ref<any[]>([])

// 页面加载
onMounted(() => {
  if (route.query.status) {
    filterStatus.value = route.query.status as string
  }
  fetchOrders()
})

// 获取订单列表
const fetchOrders = async () => {
  try {
    const params = {
      page: currentPage.value,
      pageSize: pageSize.value,
      status: filterStatus.value || undefined
    }
    const res = await getMyOrderList(params)
    orderList.value = res.data.records || res.data.list || []
    total.value = res.data.total || 0
    console.log('订单列表数据:', orderList.value)
  } catch (err) {
    console.error('获取订单失败', err)
    ElMessage.error('获取订单列表失败')
  }
}

// 重置筛选
const resetFilter = () => {
  filterStatus.value = ''
  filterType.value = ''
  filterUserName.value = ''
  filterVolunteerName.value = ''
  filterDateRange.value = []
  currentPage.value = 1
  fetchOrders()
}

// 跳转详情页
const goToOrderDetail = (orderId: number) => {
  router.push({ path: '/admin/order/detail', query: { id: orderId } })
}

const goToUserDetail = (userId: number) => {
  router.push({ path: '/admin/user/detail', query: { id: userId } })
}

const goToVolunteerDetail = (volunteerId: number) => {
  router.push({ path: '/admin/volunteer/detail', query: { id: volunteerId } })
}

// 操作按钮事件
const handleComplete = async (row: any) => {
  try {
    await ElMessageBox.confirm('确定要标记为已完成吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await confirmOrder(row.id)
    ElMessage.success(`订单 ${row.id} 已标记为已完成`)
    fetchOrders()
  } catch (err) {
    if (err !== 'cancel') {
      console.error('操作失败', err)
      ElMessage.error('操作失败')
    }
  }
}

const handleCancel = async (row: any) => {
  try {
    await ElMessageBox.confirm('确定要取消订单吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await cancelOrder(row.id)
    ElMessage.success(`订单 ${row.id} 已取消`)
    fetchOrders()
  } catch (err) {
    if (err !== 'cancel') {
      console.error('操作失败', err)
      ElMessage.error('操作失败')
    }
  }
}

// 状态映射
const getStatusText = (status: number) => {
  const map: Record<number, string> = {
    0: '待接单',
    1: '已接单',
    2: '服务中',
    3: '待确认',
    4: '已完成',
    5: '已取消'
  }
  return map[status] || '未知'
}

const getStatusTagType = (status: number) => {
  const map: Record<number, string> = {
    0: 'warning',   // 待接单
    1: 'primary',   // 已接单
    2: 'primary',   // 服务中
    3: 'success',   // 待确认
    4: 'success',   // 已完成
    5: 'danger'     // 已取消
  }
  return map[status] || 'info'
}

// 服务类型映射
const getTypeText = (type: number) => {
  const map: Record<number, string> = {
    0: '代购服务',
    1: '助洁服务',
    2: '助餐服务',
    3: '助医服务',
    4: '陪伴服务'
  }
  return map[type] || '其他'
}

const getTypeTagType = (type: number) => {
  const map: Record<number, string> = {
    0: 'success',   // 代购
    1: 'info',      // 助洁
    2: 'warning',   // 助餐
    3: 'danger',    // 助医
    4: 'primary'    // 陪伴
  }
  return map[type] || ''
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
