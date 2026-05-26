<template>
  <div class="page-container">
    <div class="header-row">
      <h2 class="page-title">志愿者管理</h2>
      <el-button type="primary" @click="openAddDialog">新增志愿者</el-button>
    </div>

    <!-- 筛选栏（和订单管理风格统一） -->
    <div class="filter-bar">
      <div class="filter-item">
        <label class="filter-label">姓名/ID</label>
        <el-input
          v-model="filterKeyword"
          placeholder="请输入姓名或ID"
          clearable
          @keyup.enter="fetchVolunteers"
        />
      </div>

      <div class="filter-item">
        <label class="filter-label">服务类型</label>
        <el-select v-model="filterServiceType" placeholder="请选择" @change="fetchVolunteers">
          <el-option label="全部" value="" />
          <el-option label="代购服务" :value="0" />
          <el-option label="助洁服务" :value="1" />
          <el-option label="助餐服务" :value="2" />
          <el-option label="助医服务" :value="3" />
        </el-select>
      </div>

      <div class="filter-item">
        <label class="filter-label">状态</label>
        <el-select v-model="filterWorkStatus" placeholder="请选择" @change="fetchVolunteers">
          <el-option label="全部" value="" />
          <el-option label="休息中" :value="0" />
          <el-option label="待命中" :value="1" />
          <el-option label="服务中" :value="2" />
        </el-select>
      </div>

      <div class="filter-item">
        <label class="filter-label">性别</label>
        <el-select v-model="filterGender" placeholder="请选择" @change="fetchVolunteers">
          <el-option label="全部" value="" />
          <el-option label="男" value="男" />
          <el-option label="女" value="女" />
        </el-select>
      </div>

      <div class="filter-item">
        <label class="filter-label">注册时间</label>
        <el-date-picker
          v-model="filterDateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          @change="fetchVolunteers"
        />
      </div>

      <el-button type="primary" @click="fetchVolunteers">查询</el-button>
      <el-button @click="resetFilter">重置</el-button>
    </div>

    <!-- 表格（自适应列宽，填满整个宽度） -->
    <div class="table-card">
      <el-table :data="volunteerList" border stripe style="width: 100%">
        <el-table-column prop="volunteerId" label="ID" min-width="70" align="center" />
        <el-table-column label="用户信息" min-width="140" align="center">
          <template #default="{ row }">
            <div class="user-info">
              <el-avatar :size="32" :src="row.avatar" :icon="UserFilled" />
              <div class="info-content">
                <div class="info-name">{{ row.realName }}</div>
                <el-button type="primary" link class="info-id" @click="goToDetail(row.volunteerId)">
                  ID: {{ row.volunteerId }}
                </el-button>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" min-width="120" align="center" />
        <el-table-column prop="age" label="年龄" min-width="70" align="center" />
        <el-table-column prop="gender" label="性别" min-width="70" align="center" />
        <el-table-column label="服务类型" min-width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="getServiceTypeTagType(row.serviceType)">
              {{ getServiceTypeText(row.serviceType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" min-width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="getWorkStatusTagType(row.workStatus)">
              {{ getWorkStatusText(row.workStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="totalServiceHours" label="服务时长" min-width="100" align="center">
          <template #default="{ row }">
            {{ row.totalServiceHours }}小时
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="注册时间" min-width="150" align="center" />
        <el-table-column label="操作" min-width="200" align="center">
          <template #default="{ row }">
            <el-button type="primary" link @click="goToDetail(row.volunteerId)">查看详情</el-button>
            <el-button type="success" link @click="openEditDialog(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 分页 -->
    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @change="fetchVolunteers"
      />
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="650">
      <el-form :model="formData" label-width="100px">
        <el-form-item label="姓名">
          <el-input v-model="formData.realName" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="formData.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="年龄">
          <el-input v-model.number="formData.age" placeholder="请输入年龄" />
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="formData.gender">
            <el-radio label="男" />
            <el-radio label="女" />
          </el-radio-group>
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="formData.address" placeholder="请输入地址" />
        </el-form-item>
        <el-form-item label="服务类型">
          <el-select v-model="formData.serviceType" placeholder="请选择">
            <el-option label="代购服务" :value="0" />
            <el-option label="助洁服务" :value="1" />
            <el-option label="助餐服务" :value="2" />
            <el-option label="助医服务" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="formData.workStatus" placeholder="请选择">
            <el-option label="休息中" :value="0" />
            <el-option label="待命中" :value="1" />
            <el-option label="服务中" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="累计服务时长">
          <el-input v-model.number="formData.totalServiceHours" placeholder="请输入服务时长" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UserFilled } from '@element-plus/icons-vue'
import { getVolunteerPage, volunteerAdd, volunteerUpdate, volunteerDelete } from '@/api/admin'
import type { volunteer } from '@/types/volunteer'

const router = useRouter()

// 筛选
const filterKeyword = ref('')
const filterServiceType = ref<number | ''>('')
const filterWorkStatus = ref<number | ''>('')
const filterGender = ref<string | ''>('')
const filterDateRange = ref<string[]>([])

// 分页
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 列表数据
const volunteerList = ref<volunteer[]>([])

// 弹窗
const dialogVisible = ref(false)
const dialogTitle = ref('新增志愿者')
const isEdit = ref(false)
const formData = ref<volunteer>({
  realName: '',
  username: '',
  phone: '',
  avatar: '',
  serviceDays: '',
  serviceType: 0,
  workStatus: 0,
  gender: '男',
  age: 20,
  address: '',
  emergencyName: '',
  emergencyPhone: '',
  totalServiceHours: 0
})

onMounted(() => {
  fetchVolunteers()
})

const fetchVolunteers = async () => {
  try {
    // --- 接口已注释 ---
    /*
    const params = {
      page: currentPage.value,
      pageSize: pageSize.value,
      keyword: filterKeyword.value,
      serviceType: filterServiceType.value || undefined,
      workStatus: filterWorkStatus.value || undefined,
      gender: filterGender.value || undefined,
      startDate: filterDateRange.value?.[0] || undefined,
      endDate: filterDateRange.value?.[1] || undefined
    }
    const res = await getVolunteerPage(params)
    volunteerList.value = res.data.list
    total.value = res.data.total
    */

    // 模拟数据（填满所有字段）
    volunteerList.value = generateMockData()
    total.value = 128
  } catch (err) {
    console.error('获取志愿者列表失败', err)
    ElMessage.error('获取志愿者列表失败')
  }
}

const resetFilter = () => {
  filterKeyword.value = ''
  filterServiceType.value = ''
  filterWorkStatus.value = ''
  filterGender.value = ''
  filterDateRange.value = []
  currentPage.value = 1
  fetchVolunteers()
}

const goToDetail = (id: number | undefined) => {
  if (id) {
    router.push({ path: '/admin/volunteer/detail', query: { id } })
  }
}

const openAddDialog = () => {
  dialogTitle.value = '新增志愿者'
  isEdit.value = false
  formData.value = {
    realName: '',
    username: '',
    phone: '',
    avatar: '',
    serviceDays: '',
    serviceType: 0,
    workStatus: 0,
    gender: '男',
    age: 20,
    address: '',
    emergencyName: '',
    emergencyPhone: '',
    totalServiceHours: 0
  }
  dialogVisible.value = true
}

const openEditDialog = (row: volunteer) => {
  dialogTitle.value = '编辑志愿者'
  isEdit.value = true
  formData.value = { ...row }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  try {
    if (isEdit.value) {
      /* await volunteerUpdate(formData.value) */
      ElMessage.success('编辑成功')
    } else {
      /* await volunteerAdd(formData.value) */
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    fetchVolunteers()
  } catch (err) {
    ElMessage.error('操作失败')
  }
}

const handleDelete = (row: volunteer) => {
  ElMessageBox.confirm('确定要删除该志愿者吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      /* await volunteerDelete(row.volunteerId!) */
      ElMessage.success('删除成功')
      fetchVolunteers()
    } catch (err) {
      ElMessage.error('删除失败')
    }
  })
}

// 状态/类型映射
const getServiceTypeText = (type: number) => {
  const map = ['代购服务', '助洁服务', '助餐服务', '助医服务']
  return map[type] || '未知'
}

const getServiceTypeTagType = (type: number) => {
  const map = ['info', 'success', 'primary', 'danger']
  return map[type] || ''
}

const getWorkStatusText = (status: number) => {
  const map = ['休息中', '待命中', '服务中']
  return map[status] || '未知'
}

const getWorkStatusTagType = (status: number) => {
  const map = ['info', 'primary', 'success']
  return map[status] || 'info'
}

// 生成完整模拟数据
const generateMockData = (): volunteer[] => {
  return [
    {
      volunteerId: 3001,
      realName: '小张',
      username: 'zhang123',
      phone: '135****9012',
      avatar: '',
      serviceDays: '周一至周五',
      serviceType: 2,
      workStatus: 1,
      gender: '男',
      age: 24,
      address: '幸福小区',
      emergencyName: '张父',
      emergencyPhone: '138****1111',
      totalServiceHours: 120,
      createTime: '2026-01-01',
      lastLoginTime: '2026-05-26'
    },
    {
      volunteerId: 3002,
      realName: '小王',
      username: 'wang123',
      phone: '136****3456',
      avatar: '',
      serviceDays: '周末',
      serviceType: 1,
      workStatus: 2,
      gender: '女',
      age: 23,
      address: '阳光花园',
      emergencyName: '王母',
      emergencyPhone: '139****2222',
      totalServiceHours: 80,
      createTime: '2026-02-01',
      lastLoginTime: '2026-05-26'
    },
    {
      volunteerId: 3003,
      realName: '小李',
      username: 'li123',
      phone: '137****7890',
      avatar: '',
      serviceDays: '周一至周日',
      serviceType: 0,
      workStatus: 0,
      gender: '男',
      age: 22,
      address: '和平社区',
      emergencyName: '李母',
      emergencyPhone: '139****3333',
      totalServiceHours: 60,
      createTime: '2026-03-01',
      lastLoginTime: '2026-05-25'
    },
    {
      volunteerId: 3004,
      realName: '小赵',
      username: 'zhao123',
      phone: '138****4567',
      avatar: '',
      serviceDays: '周二至周四',
      serviceType: 3,
      workStatus: 1,
      gender: '女',
      age: 25,
      address: '温馨家园',
      emergencyName: '赵父',
      emergencyPhone: '137****4444',
      totalServiceHours: 150,
      createTime: '2026-01-15',
      lastLoginTime: '2026-05-26'
    },
    {
      volunteerId: 3005,
      realName: '小孙',
      username: 'sun123',
      phone: '139****5678',
      avatar: '',
      serviceDays: '周六周日',
      serviceType: 2,
      workStatus: 0,
      gender: '男',
      age: 21,
      address: '幸福小区',
      emergencyName: '孙母',
      emergencyPhone: '136****5555',
      totalServiceHours: 40,
      createTime: '2026-04-01',
      lastLoginTime: '2026-05-24'
    }
  ]
}
</script>

<style scoped>
.page-container {
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
  margin-bottom: 20px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  justify-content: center;
}

.info-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.info-name {
  font-size: 14px;
  font-weight: 500;
}

.info-id {
  font-size: 12px;
  padding: 0;
  margin: 0;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  background: #fff;
  padding: 16px 20px;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}
</style>
