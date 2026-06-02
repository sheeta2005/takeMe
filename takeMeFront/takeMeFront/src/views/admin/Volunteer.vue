<template>
  <div class="volunteer-container">
    <el-card class="filter-card">
      <el-form :inline="true" :model="filterForm" class="filter-form">
        <el-form-item label="账号">
          <el-input v-model="filterForm.username" placeholder="志愿者账号" clearable @clear="handleFilter" />
        </el-form-item>
        <el-form-item label="服务类型">
          <el-select v-model="filterForm.serviceType" placeholder="全部" clearable @change="handleFilter" style="width: 140px">
            <el-option label="代购" :value="0" />
            <el-option label="助洁" :value="1" />
            <el-option label="助餐" :value="2" />
            <el-option label="助医" :value="3" />
            <el-option label="陪伴" :value="4" />
          </el-select>
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

    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">志愿者列表</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            添加志愿者
          </el-button>
        </div>
      </template>

      <el-table :data="volunteerList" v-loading="loading" stripe border style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="realName" label="姓名" width="100" />
        <el-table-column prop="username" label="账号" width="120" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column label="性别" width="80">
          <template #default="{ row }">
            <el-tag :type="row.gender === 0 ? 'success' : 'danger'">
              {{ row.gender === 0 ? '男' : '女' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="服务类型" width="100">
          <template #default="{ row }">
            <el-tag :type="getServiceTypeColor(row.serviceType)">
              {{ getServiceTypeName(row.serviceType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="工作状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.workStatus === 1 ? 'success' : 'info'">
              {{ row.workStatus === 1 ? '工作中' : '休息中' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="注册时间" width="180" />
        <el-table-column label="操作" fixed="right" width="200">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleDetail(row)">详情</el-button>
            <el-button type="warning" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
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
        style="margin-top: 20px; justify-content: flex-end"
      />
    </el-card>

    <!-- 添加/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑志愿者' : '添加志愿者'" width="600px">
      <el-form :model="formData" :rules="rules" ref="formRef" label-width="100px">
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
        <el-form-item label="服务类型" prop="serviceType">
          <el-select v-model="formData.serviceType" placeholder="请选择服务类型">
            <el-option label="代购" :value="0" />
            <el-option label="助洁" :value="1" />
            <el-option label="助餐" :value="2" />
            <el-option label="助医" :value="3" />
            <el-option label="陪伴" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="地址" prop="address">
          <el-input v-model="formData.address" type="textarea" :rows="2" placeholder="请输入地址" />
        </el-form-item>
        <el-form-item label="紧急联系人" prop="emergencyName">
          <el-input v-model="formData.emergencyName" placeholder="请输入紧急联系人" />
        </el-form-item>
        <el-form-item label="紧急电话" prop="emergencyPhone">
          <el-input v-model="formData.emergencyPhone" placeholder="请输入紧急电话" />
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
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import { searchVolunteer, addVolunteer, updateVolunteer, deleteVolunteer, updateVolunteerStatus } from '@/api/admin'

const router = useRouter()

const loading = ref(false)
const volunteerList = ref<any[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const filterForm = reactive({
  username: '',
  serviceType: undefined as number | undefined
})

const dialogVisible = ref(false)
const isEdit = ref(false)
const formData = ref({
  id: undefined as number | undefined,
  realName: '',
  username: '',
  phone: '',
  gender: 0,
  age: undefined as number | undefined,
  serviceType: undefined as number | undefined,
  address: '',
  emergencyName: '',
  emergencyPhone: ''
})
const formRef = ref()

const rules = {
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  username: [{ required: true, message: '请输入登录账号', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  serviceType: [{ required: true, message: '请选择服务类型', trigger: 'change' }]
}

const getServiceTypeName = (type: number) => {
  const names = ['代购', '助洁', '助餐', '助医', '陪伴']
  return names[type] || '未知'
}

const getServiceTypeColor = (type: number) => {
  const colors = ['', 'success', 'warning', 'danger', 'info']
  return colors[type] || ''
}

const fetchVolunteers = async () => {
  loading.value = true
  try {
    const res = await searchVolunteer(
      currentPage.value,
      pageSize.value,
      filterForm.username || undefined,
      undefined,
      filterForm.serviceType
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
  filterForm.username = ''
  filterForm.serviceType = undefined
  handleFilter()
}

const handleAdd = () => {
  isEdit.value = false
  formData.value = {
    id: undefined,
    realName: '',
    username: '',
    phone: '',
    gender: 0,
    age: undefined,
    serviceType: undefined,
    address: '',
    emergencyName: '',
    emergencyPhone: ''
  }
  dialogVisible.value = true
}

const handleEdit = (row: any) => {
  isEdit.value = true
  formData.value = {
    id: row.id,
    realName: row.realName,
    username: row.username,
    phone: row.phone,
    gender: row.gender,
    age: row.age,
    serviceType: row.serviceType,
    address: row.address,
    emergencyName: row.emergencyName,
    emergencyPhone: row.emergencyPhone
  }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value?.validate()
  try {
    if (isEdit.value) {
      await updateVolunteer(formData.value)
      ElMessage.success('更新成功')
    } else {
      await addVolunteer(formData.value)
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    fetchVolunteers()
  } catch (err) {
    ElMessage.error(isEdit.value ? '更新失败' : '添加失败')
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

onMounted(() => {
  fetchVolunteers()
})
</script>

<style scoped>
.volunteer-container {
  padding: 20px;
}

.filter-card {
  margin-bottom: 20px;
}

.filter-form {
  margin-bottom: 0;
}

.table-card {
  min-height: calc(100vh - 260px);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}
</style>
