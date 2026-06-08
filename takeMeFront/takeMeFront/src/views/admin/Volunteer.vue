<template>
  <div class="page-container">
    <div class="page-header">
      <div style="display: flex; justify-content: space-between; align-items: center;">
        <div>
          <h2 class="page-title">志愿者管理</h2>
          <p class="page-subtitle">管理系统中的志愿者信息</p>
        </div>
        <el-button type="primary" size="large" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          添加志愿者
        </el-button>
      </div>
    </div>

    <!-- 筛选栏 -->
    <el-card class="filter-card" shadow="hover">
      <el-form :inline="true" :model="filterForm" class="filter-form">
        <el-form-item label="关键词">
          <el-input v-model="filterForm.keyword" placeholder="姓名/账号" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="工作状态">
          <el-select v-model="filterForm.workStatus" placeholder="全部" clearable style="width: 120px">
            <el-option label="休息中" :value="0" />
            <el-option label="待命中" :value="1" />
            <el-option label="服务中" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchVolunteers">
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

    <!-- 志愿者列表 -->
    <el-card class="table-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-icon :size="20" color="#00a88d"><UserFilled /></el-icon>
            <span class="card-title">志愿者列表</span>
          </div>
          <span class="total-count">共 {{ total }} 人</span>
        </div>
      </template>

      <el-table :data="volunteerList" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="realName" label="姓名" width="120" />
        <el-table-column prop="username" label="账号" width="130" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column label="性别" width="80">
          <template #default="{ row }">
            <el-tag :type="row.gender === 0 ? '' : 'danger'" size="small">
              {{ row.gender === 0 ? '男' : '女' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="age" label="年龄" width="80" />
        <el-table-column label="工作状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getWorkStatusType(row.workStatus)" size="small">
              {{ getWorkStatusText(row.workStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              inline-prompt
              active-text="启用"
              inactive-text="禁用"
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="注册时间" width="180" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleDetail(row)">
              <el-icon><View /></el-icon>
              详情
            </el-button>
            <el-button type="warning" link size="small" @click="handleEdit(row)">
              <el-icon><Edit /></el-icon>
              编辑
            </el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">
              <el-icon><Delete /></el-icon>
              删除
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
        @size-change="fetchVolunteers"
        @current-change="fetchVolunteers"
        style="margin-top: 24px; justify-content: flex-end"
      />
    </el-card>

    <!-- 添加/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑志愿者' : '添加志愿者'"
      width="700px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="100px">
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="formData.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="登录账号" prop="username" v-if="!isEdit">
          <el-input v-model="formData.username" placeholder="请输入登录账号" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="formData.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="性别" prop="gender">
          <el-radio-group v-model="formData.gender">
            <el-radio :value="0">男</el-radio>
            <el-radio :value="1">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="年龄" prop="age">
          <el-input-number v-model="formData.age" :min="18" :max="100" />
        </el-form-item>
        <el-form-item label="居住地址" prop="address">
          <el-input v-model="formData.address" type="textarea" :rows="2" placeholder="请输入居住地址" />
        </el-form-item>
        <el-form-item label="紧急联系人" prop="emergencyName">
          <el-input v-model="formData.emergencyName" placeholder="请输入紧急联系人" />
        </el-form-item>
        <el-form-item label="紧急电话" prop="emergencyPhone">
          <el-input v-model="formData.emergencyPhone" placeholder="请输入紧急联系电话" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, FormInstance } from 'element-plus'
import { Search, Refresh, Plus, UserFilled, View, Edit, Delete } from '@element-plus/icons-vue'
import { searchVolunteer, addVolunteer, updateVolunteer, deleteVolunteer, updateVolunteerStatus } from '@/api/admin'

const router = useRouter()

const loading = ref(false)
const submitLoading = ref(false)
const volunteerList = ref<any[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const filterForm = reactive({
  keyword: '',
  workStatus: undefined as number | undefined
})

const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()

const formData = reactive({
  id: undefined as number | undefined,
  realName: '',
  username: '',
  phone: '',
  gender: 0,
  age: undefined as number | undefined,
  address: '',
  emergencyName: '',
  emergencyPhone: ''
})

const rules = {
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  username: [{ required: true, message: '请输入登录账号', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
  age: [{ required: true, message: '请输入年龄', trigger: 'blur' }],
  address: [{ required: true, message: '请输入居住地址', trigger: 'blur' }],
  emergencyName: [{ required: true, message: '请输入紧急联系人', trigger: 'blur' }],
  emergencyPhone: [
    { required: true, message: '请输入紧急联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ]
}

const fetchVolunteers = async () => {
  loading.value = true
  try {
    const res = await searchVolunteer(
      currentPage.value,
      pageSize.value,
      filterForm.keyword || undefined,
      filterForm.workStatus
    )
    volunteerList.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (err) {
    console.error('获取志愿者列表失败', err)
    ElMessage.error('获取志愿者列表失败')
  } finally {
    loading.value = false
  }
}

const handleFilter = () => {
  currentPage.value = 1
  fetchVolunteers()
}

const handleReset = () => {
  filterForm.keyword = ''
  filterForm.workStatus = undefined
  handleFilter()
}

const handleAdd = () => {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (row: any) => {
  isEdit.value = true
  Object.assign(formData, {
    id: row.id,
    realName: row.realName,
    username: row.username,
    phone: row.phone,
    gender: row.gender,
    age: row.age,
    address: row.address,
    emergencyName: row.emergencyName,
    emergencyPhone: row.emergencyPhone
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value?.validate()
  submitLoading.value = true
  try {
    if (isEdit.value) {
      await updateVolunteer(formData)
      ElMessage.success('更新成功')
    } else {
      await addVolunteer(formData)
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    fetchVolunteers()
  } catch (err) {
    ElMessage.error(isEdit.value ? '更新失败' : '添加失败')
  } finally {
    submitLoading.value = false
  }
}

const handleDetail = (row: any) => {
  router.push({ name: 'VolunteerDetail', params: { id: row.id } })
}

const handleDelete = async (row: any) => {
  try {
    await ElMessageBox.confirm('确定删除该志愿者吗？', '提示', { type: 'warning' })
    await deleteVolunteer(row.id)
    ElMessage.success('删除成功')
    fetchVolunteers()
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const handleStatusChange = async (row: any) => {
  try {
    await updateVolunteerStatus(row.id, row.status)
    ElMessage.success('状态更新成功')
  } catch (err) {
    row.status = row.status === 1 ? 0 : 1
    ElMessage.error('状态更新失败')
  }
}

const resetForm = () => {
  formData.id = undefined
  formData.realName = ''
  formData.username = ''
  formData.phone = ''
  formData.gender = 0
  formData.age = undefined
  formData.address = ''
  formData.emergencyName = ''
  formData.emergencyPhone = ''
  formRef.value?.resetFields()
}

const getWorkStatusText = (status: number) => {
  const map = ['休息中', '待命中', '服务中']
  return map[status] || '未知'
}

const getWorkStatusType = (status: number) => {
  const map = ['info', 'success', 'warning']
  return map[status] || 'info'
}

onMounted(() => {
  fetchVolunteers()
})
</script>

<style scoped>
.filter-card {
  margin-bottom: 24px;
  border: 1px solid var(--border-light);
}

.filter-form {
  margin-bottom: 0;
}

.table-card {
  border: 1px solid var(--border-light);
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
  color: var(--text-primary);
}

.total-count {
  font-size: 14px;
  color: var(--text-secondary);
}
</style>
