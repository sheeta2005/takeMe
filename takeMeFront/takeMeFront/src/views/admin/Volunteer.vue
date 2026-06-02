<template>
  <div class="page-container">
    <div class="header-row">
      <h2 class="page-title">志愿者管理</h2>
      <el-button type="primary" @click="openAddDialog">新增志愿者</el-button>
    </div>

    <!-- 筛选栏 -->
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
        <label class="filter-label">工作状态</label>
        <el-select v-model="filterWorkStatus" placeholder="请选择" @change="fetchVolunteers">
          <el-option label="全部" value="" />
          <el-option label="休息中" :value="0" />
          <el-option label="待命中" :value="1" />
          <el-option label="服务中" :value="2" />
        </el-select>
      </div>

      <el-button type="primary" @click="fetchVolunteers">查询</el-button>
      <el-button @click="resetFilter">重置</el-button>
    </div>

    <!-- 表格 -->
    <div class="table-card">
      <el-table :data="volunteerList" border stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column label="用户信息" min-width="160" align="center">
          <template #default="{ row }">
            <div class="user-info">
              <el-avatar :size="40" :src="row.avatar || defaultAvatar" />
              <div class="info-content">
                <div class="info-name">{{ row.realName || '-' }}</div>
                <div class="info-username">@{{ row.username }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="130" align="center" />
        <el-table-column prop="gender" label="性别" width="70" align="center">
          <template #default="{ row }">
            <el-tag :type="row.gender === 0 ? 'primary' : 'danger'" size="small">
              {{ row.gender === 0 ? '男' : '女' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="age" label="年龄" width="70" align="center" />
        <el-table-column prop="serviceType" label="服务类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="getServiceTypeTagType(row.serviceType)">
              {{ getServiceTypeText(row.serviceType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="workStatus" label="工作状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="getWorkStatusTagType(row.workStatus)">
              {{ getWorkStatusText(row.workStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="totalServiceHours" label="服务时长" width="100" align="center">
          <template #default="{ row }">
            <span style="color: #00a88d; font-weight: 600">{{ row.totalServiceHours || 0 }}小时</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="注册时间" width="170" align="center" />
        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openEditDialog(row)">编辑</el-button>
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
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px">
      <el-form :model="formData" label-width="100px">
        <el-form-item label="姓名" required>
          <el-input v-model="formData.realName" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="账号" required>
          <el-input v-model="formData.username" placeholder="请输入登录账号" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="formData.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="年龄">
          <el-input-number v-model="formData.age" :min="0" :max="150" style="width: 100%" />
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="formData.gender">
            <el-radio :value="0">男</el-radio>
            <el-radio :value="1">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="formData.address" type="textarea" :rows="2" placeholder="请输入地址" />
        </el-form-item>
        <el-form-item label="服务类型">
          <el-select v-model="formData.serviceType" placeholder="请选择" style="width: 100%">
            <el-option label="代购服务" :value="0" />
            <el-option label="助洁服务" :value="1" />
            <el-option label="助餐服务" :value="2" />
            <el-option label="助医服务" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="工作状态">
          <el-select v-model="formData.workStatus" placeholder="请选择" style="width: 100%">
            <el-option label="休息中" :value="0" />
            <el-option label="待命中" :value="1" />
            <el-option label="服务中" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="服务时长">
          <el-input-number v-model="formData.totalServiceHours" :min="0" style="width: 100%" />
        </el-form-item>
        <el-divider>紧急联系人信息</el-divider>
        <el-form-item label="紧急联系人">
          <el-input v-model="formData.emergencyName" placeholder="请输入紧急联系人姓名" />
        </el-form-item>
        <el-form-item label="紧急电话">
          <el-input v-model="formData.emergencyPhone" placeholder="请输入紧急联系电话" />
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { searchVolunteer, addVolunteer, updateVolunteer, deleteVolunteer } from '@/api/admin'

const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

const filterKeyword = ref('')
const filterServiceType = ref<number | ''>('')
const filterWorkStatus = ref<number | ''>('')

const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const volunteerList = ref<any[]>([])

const dialogVisible = ref(false)
const dialogTitle = ref('新增志愿者')
const isEdit = ref(false)
const formData = ref<any>({
  realName: '',
  username: '',
  phone: '',
  gender: 0,
  age: 30,
  address: '',
  serviceType: 0,
  workStatus: 1,
  totalServiceHours: 0,
  emergencyName: '',
  emergencyPhone: ''
})

onMounted(() => {
  fetchVolunteers()
})

const fetchVolunteers = async () => {
  try {
    let availableRange = undefined
    if (filterServiceType.value !== '') {
      availableRange = String(filterServiceType.value)
    }

    const res = await searchVolunteer(
      currentPage.value,
      pageSize.value,
      filterKeyword.value || undefined,
      undefined,
      availableRange
    )
    volunteerList.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (err) {
    console.error('获取志愿者列表失败', err)
    ElMessage.error('获取志愿者列表失败')
  }
}

const resetFilter = () => {
  filterKeyword.value = ''
  filterServiceType.value = ''
  filterWorkStatus.value = ''
  currentPage.value = 1
  fetchVolunteers()
}

const getServiceTypeText = (type: number) => {
  const map: Record<number, string> = { 0: '代购', 1: '助洁', 2: '助餐', 3: '助医', 4: '陪伴' }
  return map[type] ?? '未知'
}

const getServiceTypeTagType = (type: number) => {
  const map: Record<number, string> = { 0: 'primary', 1: 'success', 2: 'warning', 3: 'danger', 4: 'info' }
  return map[type] ?? 'info'
}

const getWorkStatusText = (status: number) => {
  const map: Record<number, string> = { 0: '休息中', 1: '待命中', 2: '服务中' }
  return map[status] ?? '未知'
}

const getWorkStatusTagType = (status: number) => {
  const map: Record<number, string> = { 0: 'info', 1: 'success', 2: 'warning' }
  return map[status] ?? 'info'
}

const openAddDialog = () => {
  dialogTitle.value = '新增志愿者'
  isEdit.value = false
  formData.value = {
    realName: '',
    username: '',
    phone: '',
    gender: 0,
    age: 30,
    address: '',
    serviceType: 0,
    workStatus: 1,
    totalServiceHours: 0,
    emergencyName: '',
    emergencyPhone: ''
  }
  dialogVisible.value = true
}

const openEditDialog = (row: any) => {
  dialogTitle.value = '编辑志愿者'
  isEdit.value = true
  formData.value = { ...row }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formData.value.realName || !formData.value.username) {
    ElMessage.warning('请填写必填字段')
    return
  }

  try {
    if (isEdit.value) {
      await updateVolunteer(formData.value)
      ElMessage.success('编辑成功')
    } else {
      await addVolunteer(formData.value)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    fetchVolunteers()
  } catch (err) {
    ElMessage.error('操作失败')
  }
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm('确定要删除该志愿者吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteVolunteer(row.id)
      ElMessage.success('删除成功')
      fetchVolunteers()
    } catch (err) {
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
}
</script>

<style scoped>
.page-container {
  padding: 20px;
}

.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 20px;
  padding: 16px;
  background: #f9fafb;
  border-radius: 8px;
}

.filter-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.filter-label {
  font-size: 12px;
  color: #666;
  font-weight: 500;
}

.table-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.info-content {
  text-align: left;
}

.info-name {
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.info-username {
  font-size: 12px;
  color: #999;
}

.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
