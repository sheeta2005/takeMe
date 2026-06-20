<template>
  <div class="page-container">
    <div class="page-header">
      <div style="display: flex; justify-content: space-between; align-items: center;">
        <div>
          <h2 class="page-title">用户管理</h2>
          <p class="page-subtitle">管理系统中的用户信息</p>
        </div>
      </div>
    </div>

    <el-card class="filter-card" shadow="hover">
      <el-form :inline="true" class="filter-form">
        <el-form-item label="ID">
          <el-input
            v-model="filterId"
            placeholder="用户ID"
            clearable
            style="width: 120px"
            @keyup.enter="fetchUsers"
          />
        </el-form-item>
        <el-form-item label="关键词">
          <el-input
            v-model="filterKeyword"
            placeholder="姓名/账号/手机号"
            clearable
            style="width: 200px"
            @keyup.enter="fetchUsers"
          />
        </el-form-item>
        <el-form-item label="性别">
          <el-select v-model="filterGender" placeholder="全部" style="width: 120px" @change="fetchUsers">
            <el-option label="全部" :value="undefined" />
            <el-option label="男" :value="0" />
            <el-option label="女" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item label="注册时间">
          <el-date-picker
            v-model="filterDateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            style="width: 240px"
            @change="fetchUsers"
          />
        </el-form-item>
        <el-form-item label="最后登录">
          <el-select v-model="sortBy" placeholder="默认排序" style="width: 140px" @change="fetchUsers">
            <el-option label="默认排序" value="" />
            <el-option label="升序 ↑" value="lastLoginTime-asc" />
            <el-option label="降序 ↓" value="lastLoginTime-desc" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchUsers">
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
            <el-icon :size="20" color="#00a88d"><User /></el-icon>
            <span class="card-title">用户列表</span>
          </div>
          <span class="total-count">共 {{ total }} 人</span>
        </div>
      </template>

      <el-table :data="userList" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="头像" width="100">
          <template #default="{ row }">
            <el-avatar :size="50" :src="row.avatar || defaultAvatar" />
          </template>
        </el-table-column>
        <el-table-column prop="username" label="账号" width="140" />
        <el-table-column prop="realName" label="姓名" width="120" />
        <el-table-column label="性别" width="80">
          <template #default="{ row }">
            <el-tag :type="row.gender === 0 ? '' : 'danger'" size="small">
              {{ row.gender === 0 ? '男' : '女' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column prop="age" label="年龄" width="80" />
        <el-table-column prop="address" label="地址" min-width="200" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="注册时间" width="180" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleView(row)">
              <el-icon><View /></el-icon>
              详情
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
        @change="fetchUsers"
        style="margin-top: 24px; justify-content: flex-end"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Refresh, User, View, Switch, Delete } from '@element-plus/icons-vue'
import { searchUser, deleteUser, updateUserStatus } from '@/api/admin'
import defaultAvatar from '@/assets/default-avatar.png'

const router = useRouter()
const loading = ref(false)
const filterId = ref<number | undefined>(undefined)
const filterKeyword = ref('')
const filterGender = ref<number | undefined>(undefined)
const filterDateRange = ref<string[]>([])
const sortBy = ref('')

const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const userList = ref<any[]>([])

onMounted(() => {
  fetchUsers()
})

const fetchUsers = async () => {
  loading.value = true
  try {
    const startDate = filterDateRange.value?.[0] || undefined
    const endDate = filterDateRange.value?.[1] || undefined

    let sortField = ''
    let sortOrder = ''
    if (sortBy.value) {
      const parts = sortBy.value.split('-')
      sortField = parts[0]
      sortOrder = parts[1]
    }

    const res = await searchUser(
      currentPage.value,
      pageSize.value,
      filterKeyword.value || undefined,
      filterGender.value,
      filterId.value,
      startDate,
      endDate,
      sortField,
      sortOrder
    )
    userList.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (err) {
    console.error('获取用户列表失败', err)
    ElMessage.error('获取用户列表失败')
  } finally {
    loading.value = false
  }
}

const resetFilter = () => {
  filterId.value = undefined
  filterKeyword.value = ''
  filterGender.value = undefined
  filterDateRange.value = []
  sortBy.value = ''
  currentPage.value = 1
  fetchUsers()
}

const handleView = (row: any) => {
  router.push(`/admin/user/detail/${row.id}`)
}

const handleToggleStatus = async (row: any) => {
  const newStatus = row.status === 1 ? 0 : 1
  const action = newStatus === 1 ? '启用' : '禁用'

  try {
    await ElMessageBox.confirm(`确定要${action}该用户吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await updateUserStatus(row.id, newStatus)
    ElMessage.success(`${action}成功`)
    fetchUsers()
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error(`${action}失败`)
    }
  }
}

const handleDelete = async (row: any) => {
  try {
    await ElMessageBox.confirm('确定要删除该用户吗？此操作不可恢复！', '删除确认', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await deleteUser(row.id)
    ElMessage.success('删除成功')
    fetchUsers()
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
</style>
