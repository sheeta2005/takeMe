<template>
  <div class="page-container">
    <div class="header-row">
      <h2 class="page-title">用户管理</h2>
      <el-button type="primary" @click="openAddDialog">新增用户</el-button>
    </div>

    <!-- 筛选栏（和志愿者管理完全统一） -->
    <div class="filter-bar">
      <div class="filter-item">
        <label class="filter-label">姓名/ID</label>
        <el-input
          v-model="filterKeyword"
          placeholder="请输入姓名或ID"
          clearable
          @keyup.enter="fetchUsers"
        />
      </div>

      <div class="filter-item">
        <label class="filter-label">性别</label>
        <el-select v-model="filterGender" placeholder="请选择" @change="fetchUsers">
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
          @change="fetchUsers"
        />
      </div>

      <el-button type="primary" @click="fetchUsers">查询</el-button>
      <el-button @click="resetFilter">重置</el-button>
    </div>

    <!-- 表格（自适应填满，无大空格） -->
    <div class="table-card">
      <el-table :data="userList" border stripe style="width: 100%">
        <el-table-column prop="userId" label="ID" min-width="70" align="center" />
        <el-table-column label="用户信息" min-width="140" align="center">
          <template #default="{ row }">
            <div class="user-info">
              <el-avatar :size="32" :src="row.avatar" :icon="UserFilled" />
              <div class="info-content">
                <div class="info-name">{{ row.realName }}</div>
                <el-button type="primary" link class="info-id" @click="goToDetail(row.userId)">
                  ID: {{ row.userId }}
                </el-button>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" min-width="120" align="center" />
        <el-table-column prop="age" label="年龄" min-width="70" align="center" />
        <el-table-column prop="gender" label="性别" min-width="70" align="center" />
        <el-table-column prop="address" label="居住地址" min-width="180" align="center" />
        <el-table-column prop="createTime" label="注册时间" min-width="150" align="center" />
        <el-table-column label="操作" min-width="200" align="center">
          <template #default="{ row }">
            <el-button type="primary" link @click="goToDetail(row.userId)">查看详情</el-button>
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
        @change="fetchUsers"
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
        <el-form-item label="居住地址">
          <el-input v-model="formData.address" placeholder="请输入居住地址" />
        </el-form-item>
        <el-form-item label="紧急联系人">
          <el-input v-model="formData.emergencyName" placeholder="请输入紧急联系人姓名" />
        </el-form-item>
        <el-form-item label="紧急联系电话">
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
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UserFilled } from '@element-plus/icons-vue'
import { getElderPage, userAdd, userUpdate, userDelete } from '@/api/admin'
import type { user } from '@/types/user'

const router = useRouter()

// 筛选
const filterKeyword = ref('')
const filterGender = ref<string | ''>('')
const filterDateRange = ref<string[]>([])

// 分页
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 列表数据
const userList = ref<user[]>([])

// 弹窗
const dialogVisible = ref(false)
const dialogTitle = ref('新增用户')
const isEdit = ref(false)
const formData = ref<user>({
  realName: '',
  username: '',
  phone: '',
  avatar: '',
  gender: '男',
  age: 60,
  address: '',
  emergencyName: '',
  emergencyPhone: ''
})

onMounted(() => {
  fetchUsers()
})

const fetchUsers = async () => {
  try {
    // --- 接口已注释 ---
    /*
    const params = {
      page: currentPage.value,
      pageSize: pageSize.value,
      keyword: filterKeyword.value,
      gender: filterGender.value || undefined,
      startDate: filterDateRange.value?.[0] || undefined,
      endDate: filterDateRange.value?.[1] || undefined
    }
    const res = await getElderPage(params)
    userList.value = res.data.list
    total.value = res.data.total
    */

    // 模拟数据（填满所有字段）
    userList.value = generateMockData()
    total.value = 210
  } catch (err) {
    console.error('获取用户列表失败', err)
    ElMessage.error('获取用户列表失败')
  }
}

const resetFilter = () => {
  filterKeyword.value = ''
  filterGender.value = ''
  filterDateRange.value = []
  currentPage.value = 1
  fetchUsers()
}

const goToDetail = (id: number | undefined) => {
  if (id) {
    router.push({ path: '/admin/user/detail', query: { id } })
  }
}

const openAddDialog = () => {
  dialogTitle.value = '新增用户'
  isEdit.value = false
  formData.value = {
    realName: '',
    username: '',
    phone: '',
    avatar: '',
    gender: '男',
    age: 60,
    address: '',
    emergencyName: '',
    emergencyPhone: ''
  }
  dialogVisible.value = true
}

const openEditDialog = (row: user) => {
  dialogTitle.value = '编辑用户'
  isEdit.value = true
  formData.value = { ...row }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  try {
    if (isEdit.value) {
      /* await userUpdate(formData.value) */
      ElMessage.success('编辑成功')
    } else {
      /* await userAdd(formData.value) */
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    fetchUsers()
  } catch (err) {
    ElMessage.error('操作失败')
  }
}

const handleDelete = (row: user) => {
  ElMessageBox.confirm('确定要删除该用户吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      /* await userDelete(row.userId!) */
      ElMessage.success('删除成功')
      fetchUsers()
    } catch (err) {
      ElMessage.error('删除失败')
    }
  })
}

// 生成完整模拟数据
const generateMockData = (): user[] => {
  return [
    {
      userId: 2001,
      realName: '王奶奶',
      username: 'wang123',
      phone: '138****1111',
      avatar: '',
      gender: '女',
      age: 72,
      address: '幸福小区1栋3单元501',
      emergencyName: '王女士',
      emergencyPhone: '135****2222',
      createTime: '2026-01-01',
      lastLoginTime: '2026-05-26'
    },
    {
      userId: 2002,
      realName: '李爷爷',
      username: 'li123',
      phone: '139****3333',
      avatar: '',
      gender: '男',
      age: 75,
      address: '阳光花园3栋2单元102',
      emergencyName: '李先生',
      emergencyPhone: '136****4444',
      createTime: '2026-01-05',
      lastLoginTime: '2026-05-25'
    },
    {
      userId: 2003,
      realName: '张婆婆',
      username: 'zhang123',
      phone: '137****5555',
      avatar: '',
      gender: '女',
      age: 68,
      address: '和平社区5栋1单元303',
      emergencyName: '张女士',
      emergencyPhone: '134****6666',
      createTime: '2026-02-01',
      lastLoginTime: '2026-05-26'
    },
    {
      userId: 2004,
      realName: '刘大爷',
      username: 'liu123',
      phone: '136****7777',
      avatar: '',
      gender: '男',
      age: 70,
      address: '温馨家园2栋4单元404',
      emergencyName: '刘先生',
      emergencyPhone: '133****8888',
      createTime: '2026-02-10',
      lastLoginTime: '2026-05-24'
    },
    {
      userId: 2005,
      realName: '赵奶奶',
      username: 'zhao123',
      phone: '135****9999',
      avatar: '',
      gender: '女',
      age: 65,
      address: '幸福小区2栋1单元201',
      emergencyName: '赵女士',
      emergencyPhone: '132****0000',
      createTime: '2026-03-01',
      lastLoginTime: '2026-05-26'
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
