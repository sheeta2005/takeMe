<template>
  <div class="order-manage-page">
    <div class="page-header">
      <h2>服务项目管理</h2>
      <el-button type="primary" size="large" @click="openAddModal">
        <el-icon><Plus /></el-icon>
        添加服务项目
      </el-button>
    </div>

    <!-- 服务列表 -->
    <div class="table-box">
      <el-table :data="serviceList" border stripe>
        <el-table-column label="ID" prop="id" align="center" />
        <el-table-column label="服务大类" align="center">
          <template #default="scope">
            {{ getServiceTypeName(scope.row.type) }}
          </template>
        </el-table-column>
        <el-table-column label="服务名称" prop="name" />
        <el-table-column label="服务描述" prop="desc" min-width="260" />
        <el-table-column label="价格（元）" prop="price" align="center" />
        <el-table-column label="操作" align="center" width="200">
          <template #default="scope">
            <el-button type="primary" link size="small" @click="openEditModal(scope.row)">
              编辑
            </el-button>
            <el-button type="danger" link size="small" @click="handleDelete(scope.row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 添加 / 编辑 弹窗 -->
    <el-dialog v-model="dialogVisible" title="服务信息" width="600px" append-to-body>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="服务大类" prop="type">
          <el-select v-model="form.type" placeholder="请选择服务类型" size="large">
            <el-option label="助餐服务" value="meal" />
            <el-option label="助洁服务" value="clean" />
            <el-option label="助医服务" value="medical" />
            <el-option label="代购服务" value="shop" />
            <el-option label="陪伴服务" value="companion" />
          </el-select>
        </el-form-item>

        <el-form-item label="服务名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入服务名称" size="large" />
        </el-form-item>

        <el-form-item label="服务描述" prop="desc">
          <el-input
            v-model="form.desc"
            type="textarea"
            :rows="3"
            placeholder="请输入服务描述"
            size="large"
          />
        </el-form-item>

        <el-form-item label="服务价格" prop="price">
          <el-input-number
            v-model="form.price"
            :min="0"
            placeholder="请输入价格"
            size="large"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm" :loading="loading">
            确认保存
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

const formRef = ref(null)
const dialogVisible = ref(false)
const loading = ref(false)

// 表单数据
const form = ref({
  id: '',
  type: '',
  name: '',
  desc: '',
  price: 0
})

// 校验规则
const rules = {
  type: [{ required: true, message: '请选择服务大类', trigger: 'change' }],
  name: [{ required: true, message: '请输入服务名称', trigger: 'blur' }],
  desc: [{ required: true, message: '请输入服务描述', trigger: 'blur' }],
  price: [{ required: true, message: '请输入服务价格', trigger: 'blur' }]
}

// 服务列表
const serviceList = ref([])

// 服务类型名称映射
const serviceTypeMap = {
  meal: '助餐服务',
  clean: '助洁服务',
  medical: '助医服务',
  shop: '代购服务',
  companion: '陪伴服务'
}

const getServiceTypeName = (type) => {
  return serviceTypeMap[type] || '未知'
}

// 获取服务列表
const getServiceList = async () => {
  try {
    // 这里以后替换为后端接口请求
    // const res = await getAdminServiceList()
    // serviceList.value = res.data

    // 模拟数据
    serviceList.value = [
      { id: 1, type: 'meal', name: '营养套餐A', desc: '一荤两素 日常用餐', price: 15 },
      { id: 2, type: 'clean', name: '日常保洁2小时', desc: '打扫房间、整理家务', price: 30 },
      { id: 3, type: 'companion', name: '聊天陪伴', desc: '陪老人聊天解闷', price: 25 }
    ]
  } catch (err) {
    ElMessage.error('获取服务列表失败')
  }
}

onMounted(() => {
  getServiceList()
})

// 打开添加弹窗
const openAddModal = () => {
  form.value = { id: '', type: '', name: '', desc: '', price: 0 }
  dialogVisible.value = true
}

// 打开编辑弹窗
const openEditModal = (row) => {
  form.value = { ...row }
  dialogVisible.value = true
}

// 提交保存
const submitForm = async () => {
  await formRef.value.validate()
  loading.value = true

  try {
    if (form.value.id) {
      // 编辑：调用后端更新接口
      ElMessage.success('编辑成功')
    } else {
      // 添加：调用后端新增接口
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    getServiceList()
  } catch (err) {
    ElMessage.error('操作失败')
  } finally {
    loading.value = false
  }
}

// 删除
const handleDelete = async (row) => {
  await ElMessageBox.confirm('确定要删除该服务吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })

  try {
    // 调用后端删除接口
    ElMessage.success('删除成功')
    getServiceList()
  } catch (err) {
    ElMessage.error('删除失败')
  }
}
</script>

<style scoped>
.order-manage-page {
  padding: 20px 30px;
  background: #f5f7fa;
  min-height: calc(100vh - 80px);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-header h2 {
  font-size: 24px;
  font-weight: 600;
  margin: 0;
}

.table-box {
  background: #fff;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
}

.dialog-footer {
  text-align: right;
}
</style>
