<template>
  <div class="page-container">
    <div class="card-container">
      <h3 class="page-title">志愿者管理</h3>

      <!-- 搜索栏 -->
      <div class="search-bar">
        <el-input
          v-model="query.username"
          placeholder="姓名"
          style="width:180px"
          clearable
        />
        <el-input
          v-model.number="query.id"
          placeholder="ID"
          style="width:120px"
          type="number"
          clearable
        />
        <el-input
          v-model="query.availableRange"
          placeholder="地区"
          style="width:180px"
          clearable
        />
        <el-button type="primary" @click="getList">搜索</el-button>
        <el-button @click="resetQuery">重置</el-button>
        <el-button type="success" @click="openAdd">新增志愿者</el-button>
      </div>

      <!-- 数据表格 -->
      <el-table
        :data="tableData"
        border
        stripe
        style="width: 100%"
      >
        <!-- 头像 -->
        <el-table-column label="头像" width="70" align="center">
          <template #default="{ row }">
            <el-avatar
              :src="row.avatar"
              size="40"
              fit="cover"
              style="border:1px solid #eee;"
            >
              <template #icon><User /></template>
            </el-avatar>
          </template>
        </el-table-column>

        <el-table-column prop="id" label="ID" width="60" align="center" />
        <el-table-column prop="username" label="姓名" align="center" />

        <!-- 联系电话（强制一排） -->
        <el-table-column label="联系电话" align="center" min-width="120">
          <template #default="{ row }">
            <div style="white-space: nowrap;">
              {{ row.phone || '-' }}
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="age" label="年龄" width="80" align="center" />

        <el-table-column prop="gender" label="性别" width="80" align="center">
          <template #default="{ row }">
            <span v-if="row.gender === 1">男</span>
            <span v-else-if="row.gender === 0">女</span>
            <span>-</span>
          </template>
        </el-table-column>

        <el-table-column prop="address" label="地址" min-width="120" align="center" />
        <el-table-column prop="availableRange" label="服务区域" width="100" align="center" />

        <el-table-column prop="freeTime" label="可服务时间" width="140" align="center">
          <template #default="{ row }">
            <span v-if="row.freeTime === 1">上午 8-12点</span>
            <span v-else-if="row.freeTime === 2">下午 14-18点</span>
            <span>未知</span>
          </template>
        </el-table-column>

        <!-- 周几：修复语法，去掉- -->
        <el-table-column prop="workDay" label="周几" width="100" align="center">
          <template #default="{ row }">
            <span v-if="row.workDay === 1">周一</span>
            <span v-else-if="row.workDay === 2">周二</span>
            <span v-else-if="row.workDay === 3">周三</span>
            <span v-else-if="row.workDay === 4">周四</span>
            <span v-else-if="row.workDay === 5">周五</span>
            <span v-else-if="row.workDay === 6">周六</span>
            <span v-else-if="row.workDay === 7">周日</span>
            <span>-</span>
          </template>
        </el-table-column>

        <el-table-column prop="createTime" label="创建时间" width="180" align="center" />

        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>

        <!-- 操作列 -->
        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button type="primary" size="small" @click="openEdit(row)">修改</el-button>
              <el-button
                :type="row.status === 1 ? 'warning' : 'success'"
                size="small"
                @click="toggleStatus(row)"
              >
                {{ row.status === 1 ? '禁用' : '启用' }}
              </el-button>
              <el-button type="danger" size="small" @click="doDelete(row.id)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.pageSize"
        :page-sizes="[5, 10, 20]"
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        @size-change="getList"
        @current-change="getList"
        style="margin-top: 20px; text-align: right"
      />
    </div>

    <!-- 新增/修改弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '修改志愿者' : '新增志愿者'" width="650px">
      <el-form label-width="100px" :model="form">
        <el-form-item label="头像">
          <el-input v-model="form.avatar" placeholder="输入头像URL" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="form.username" />
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="年龄">
          <el-input v-model.number="form.age" />
        </el-form-item>
        <el-form-item label="性别">
          <el-select v-model="form.gender">
            <el-option label="男" :value="1" />
            <el-option label="女" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="form.address" />
        </el-form-item>
        <el-form-item label="服务区域">
          <el-input v-model="form.availableRange" />
        </el-form-item>
        <el-form-item label="可服务时间">
          <el-select v-model="form.freeTime">
            <el-option label="上午 8-12点" :value="1" />
            <el-option label="下午 14-18点" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="周几">
          <el-select v-model="form.workDay">
            <el-option label="周一" :value="1" />
            <el-option label="周二" :value="2" />
            <el-option label="周三" :value="3" />
            <el-option label="周四" :value="4" />
            <el-option label="周五" :value="5" />
            <el-option label="周六" :value="6" />
            <el-option label="周日" :value="7" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">确认保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { User } from '@element-plus/icons-vue'
import { volunteerSearch, volunteerAdd, volunteerUpdate, volunteerDelete } from '@/api/admin'

// 定义志愿者类型
interface Volunteer {
  id?: number
  username: string
  phone: string
  age: number
  gender: number
  address: string
  availableRange: string
  freeTime: number
  workDay: number
  status: number
  avatar?: string
  createTime?: string
}

// 查询参数
const query = ref({
  page: 1,
  pageSize: 5,
  username: '',
  id: '',
  availableRange: ''
})

const tableData = ref<Volunteer[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const form = ref<Volunteer>({
  username: '',
  phone: '',
  age: 0,
  gender: 1,
  address: '',
  availableRange: '',
  freeTime: 1,
  workDay: 1,
  status: 1,
  avatar: ''
})
const isEdit = ref(false)

const getList = async () => {
  try {
    const res = await volunteerSearch(
      query.value.page,
      query.value.pageSize,
      query.value.username,
      query.value.id ? Number(query.value.id) : null,
      query.value.availableRange
    )
    tableData.value = res.data.list || []
    total.value = res.data.total || 0
  } catch (err) {
    console.error('请求失败：', err)
  }
}

const resetQuery = () => {
  query.value.username = ''
  query.value.id = ''
  query.value.availableRange = ''
  getList()
}

const openAdd = () => {
  isEdit.value = false
  form.value = {
    username: '',
    phone: '',
    age: 0,
    gender: 1,
    address: '',
    availableRange: '',
    freeTime: 1,
    workDay: 1,
    status: 1,
    avatar: ''
  }
  dialogVisible.value = true
}

const openEdit = (row: Volunteer) => {
  isEdit.value = true
  form.value = { ...row }
  dialogVisible.value = true
}

const save = async () => {
  try {
    if (isEdit.value) {
      await volunteerUpdate(form.value)
      ElMessage.success('修改成功')
    } else {
      const { id, ...newVolunteer } = form.value
      await volunteerAdd(newVolunteer as Omit<Volunteer, 'id'>)
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    getList()
  } catch (err) {
    ElMessage.error('操作失败')
  }
}

const toggleStatus = async (row: Volunteer) => {
  const newRow = { ...row, status: row.status === 1 ? 0 : 1 }
  await volunteerUpdate(newRow)
  ElMessage.success('状态已更新')
  getList()
}

const doDelete = async (id: number) => {
  await ElMessageBox.confirm('确定删除该志愿者？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await volunteerDelete(id)
  ElMessage.success('删除成功')
  getList()
}

onMounted(() => getList())
</script>

<style scoped>
.page-container {
  padding: 20px;
  background: #f5f5f5;
  min-height: 100vh;
}

.card-container {
  background: #fff;
  padding: 24px;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}

.page-title {
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 20px;
  color: #303133;
}

.search-bar {
  margin-bottom: 20px;
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.action-buttons {
  display: flex;
  gap: 8px;
  justify-content: center;
}
</style>
