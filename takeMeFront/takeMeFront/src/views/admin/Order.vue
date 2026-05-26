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
import { getOrderPage } from '@/api/admin'

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

// 订单列表数据（模拟）
const orderList = ref<any[]>([])

// 页面加载
onMounted(() => {
  if (route.query.status) {
    filterStatus.value = route.query.status as string
  }
  fetchOrders()
})

// 获取订单列表（接口已注释，用模拟数据）
const fetchOrders = async () => {
  try {
    // --- 接口调用已注释 ---
    /*
    const params = {
      page: currentPage.value,
      pageSize: pageSize.value,
      status: filterStatus.value || undefined,
      type: filterType.value || undefined,
      userName: filterUserName.value || undefined,
      volunteerName: filterVolunteerName.value || undefined,
      startDate: filterDateRange.value?.[0] || undefined,
      endDate: filterDateRange.value?.[1] || undefined
    }
    const res = await getOrderPage(params)
    orderList.value = res.data.list
    total.value = res.data.total
    */

    // --- 模拟数据（当前使用） ---
    const mockData = generateMockData()
    orderList.value = mockData.list
    total.value = mockData.total
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

// 操作按钮事件（模拟）
const handleComplete = (row: any) => {
  ElMessageBox.confirm('确定要标记为已完成吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    ElMessage.success(`订单 ${row.id} 已标记为已完成`)
    fetchOrders()
  })
}

const handleCancel = (row: any) => {
  ElMessageBox.confirm('确定要取消订单吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    ElMessage.success(`订单 ${row.id} 已取消`)
    fetchOrders()
  })
}

// 状态映射
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

// 服务类型映射
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

// 生成模拟数据（包含用户ID/志愿者ID）
const generateMockData = () => {
  const types = ['meal', 'clean', 'medical', 'buy']
  const statuses = ['active', 'completed', 'cancelled']
  const users = ['王奶奶', '李爷爷', '张婆婆', '刘大爷']
  const volunteers = ['小张', '小李', '小王', '小赵']
  const addresses = ['幸福小区1栋', '阳光花园3栋', '和平社区5栋', '温馨家园2栋']

  const list = []
  for (let i = 1; i <= 10; i++) {
    list.push({
      id: 10000 + i,
      type: types[i % 4],
      userId: 2000 + i,
      userName: users[i % 4],
      volunteerId: 3000 + i,
      volunteerName: volunteers[i % 4],
      address: addresses[i % 4],
      createTime: `2026-05-${20 + i} 10:30:00`,
      status: statuses[i % 3]
    })
  }

  return {
    list,
    total: 128
  }
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
