<template>
  <div class="page-container">
    <div class="page-header">
      <div style="display: flex; justify-content: space-between; align-items: center;">
        <div>
          <h2 class="page-title">服务套餐管理</h2>
          <p class="page-subtitle">管理系统中的服务套餐信息</p>
        </div>
        <el-button type="primary" size="large" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          添加套餐
        </el-button>
      </div>
    </div>

    <el-card class="filter-card" shadow="hover">
      <el-form :inline="true" class="filter-form">
        <el-form-item label="套餐名称">
          <el-input
            v-model="filterKeyword"
            placeholder="请输入套餐名称"
            clearable
            style="width: 200px"
            @keyup.enter="fetchServicePackages"
          />
        </el-form-item>
        <el-form-item label="服务类型">
          <el-select v-model="filterType" placeholder="全部" style="width: 140px" @change="fetchServicePackages">
            <el-option label="全部" :value="undefined" />
            <el-option label="代购服务" :value="0" />
            <el-option label="助洁服务" :value="1" />
            <el-option label="助餐服务" :value="2" />
            <el-option label="助医服务" :value="3" />
            <el-option label="陪伴服务" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filterStatus" placeholder="全部" style="width: 120px" @change="fetchServicePackages">
            <el-option label="全部" :value="undefined" />
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchServicePackages">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="resetFilter">
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
            <el-icon :size="20" color="#00a88d"><Goods /></el-icon>
            <span class="card-title">套餐列表</span>
          </div>
          <span class="total-count">共 {{ total }} 个</span>
        </div>
      </template>

      <el-table :data="packageList" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="图片" width="100">
          <template #default="{ row }">
            <el-image
              :src="row.image || defaultImage"
              :preview-src-list="[row.image || defaultImage]"
              style="width: 60px; height: 60px; border-radius: 8px; object-fit: cover;"
              fit="cover"
            />
          </template>
        </el-table-column>
        <el-table-column prop="name" label="套餐名称" min-width="150" />
        <el-table-column label="服务类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getServiceTypeTagType(row.type)" size="small">
              {{ getServiceTypeText(row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="price" label="价格" width="120" align="right">
          <template #default="{ row }">
            <span class="price-text">¥{{ row.price }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">
              <el-icon><Edit /></el-icon>
              编辑
            </el-button>
            <el-button
              :type="row.status === 1 ? 'warning' : 'success'"
              link
              size="small"
              @click="handleToggleStatus(row)"
            >
              <el-icon><Switch /></el-icon>
              {{ row.status === 1 ? '禁用' : '启用' }}
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
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @change="fetchServicePackages"
        style="margin-top: 24px; justify-content: flex-end"
      />
    </el-card>

    <el-dialog v-model="editDialogVisible" :title="isEdit ? '编辑套餐' : '添加套餐'" width="600px">
      <el-form :model="editForm" label-width="100px">
        <el-form-item label="套餐名称" required>
          <el-input v-model="editForm.name" placeholder="请输入套餐名称" />
        </el-form-item>
        <el-form-item label="服务类型" required>
          <el-select v-model="editForm.type" placeholder="请选择服务类型" style="width: 100%">
            <el-option label="代购服务" :value="0" />
            <el-option label="助洁服务" :value="1" />
            <el-option label="助餐服务" :value="2" />
            <el-option label="助医服务" :value="3" />
            <el-option label="陪伴服务" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="价格" required>
          <el-input-number v-model="editForm.price" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="图片地址">
          <el-input v-model="editForm.image" placeholder="请输入图片URL" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="editForm.description" type="textarea" :rows="3" placeholder="请输入套餐描述" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="editForm.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmEdit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Refresh, Goods, Edit, Switch, Delete } from '@element-plus/icons-vue'
import {
  getServicePackagePage,
  addServicePackage,
  updateServicePackage,
  deleteServicePackage,
  updateServicePackageStatus
} from '@/api/admin'

const loading = ref(false)
const filterKeyword = ref('')
const filterType = ref<number | undefined>(undefined)
const filterStatus = ref<number | undefined>(undefined)

const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const packageList = ref<any[]>([])
const editDialogVisible = ref(false)
const isEdit = ref(false)
const editForm = ref({
  id: undefined as number | undefined,
  name: '',
  type: 0,
  price: 0,
  image: '',
  description: '',
  status: 1
})

const defaultImage = ref('/src/assets/default-package.png')

onMounted(() => {
  fetchServicePackages()
})

const fetchServicePackages = async () => {
  loading.value = true
  try {
    const res = await getServicePackagePage(
      currentPage.value,
      pageSize.value,
      filterType.value,
      filterStatus.value,
      filterKeyword.value || undefined
    )
    packageList.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (err) {
    console.error('获取服务套餐列表失败', err)
    ElMessage.error('获取服务套餐列表失败')
  } finally {
    loading.value = false
  }
}

const resetFilter = () => {
  filterKeyword.value = ''
  filterType.value = undefined
  filterStatus.value = undefined
  currentPage.value = 1
  fetchServicePackages()
}

const getServiceTypeText = (type: number) => {
  const map = ['代购服务', '助洁服务', '助餐服务', '助医服务', '陪伴服务']
  return map[type] || '未知'
}

const getServiceTypeTagType = (type: number) => {
  const map = ['primary', 'success', 'warning', 'danger', 'info']
  return map[type] || 'info'
}

const handleAdd = () => {
  isEdit.value = false
  editForm.value = {
    id: undefined,
    name: '',
    type: 0,
    price: 0,
    image: '',
    description: '',
    status: 1
  }
  editDialogVisible.value = true
}

const handleEdit = (row: any) => {
  isEdit.value = true
  editForm.value = {
    id: row.id,
    name: row.name,
    type: row.type,
    price: row.price,
    image: row.image || '',
    description: row.description || '',
    status: row.status
  }
  editDialogVisible.value = true
}

const confirmEdit = async () => {
  if (!editForm.value.name || !editForm.value.type) {
    ElMessage.warning('请填写完整信息')
    return
  }

  try {
    if (isEdit.value) {
      await updateServicePackage(editForm.value)
      ElMessage.success('编辑成功')
    } else {
      await addServicePackage(editForm.value)
      ElMessage.success('添加成功')
    }
    editDialogVisible.value = false
    fetchServicePackages()
  } catch (err) {
    ElMessage.error(isEdit.value ? '编辑失败' : '添加失败')
  }
}

const handleToggleStatus = async (row: any) => {
  const newStatus = row.status === 1 ? 0 : 1
  const action = newStatus === 1 ? '启用' : '禁用'

  try {
    await ElMessageBox.confirm(`确定要${action}该套餐吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await updateServicePackageStatus(row.id, newStatus)
    ElMessage.success(`${action}成功`)
    fetchServicePackages()
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error(`${action}失败`)
    }
  }
}

const handleDelete = async (row: any) => {
  try {
    await ElMessageBox.confirm('确定要删除该套餐吗？此操作不可恢复！', '删除确认', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await deleteServicePackage(row.id)
    ElMessage.success('删除成功')
    fetchServicePackages()
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}
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

.price-text {
  color: #f5222d;
  font-weight: bold;
  font-size: 16px;
}
</style>
