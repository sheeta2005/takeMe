<template>
  <div class="page-container">
    <div class="page-header">
      <div style="display: flex; justify-content: space-between; align-items: center;">
        <div>
          <h2 class="page-title">业务审批</h2>
          <p class="page-subtitle">处理各类业务申请审批</p>
        </div>
      </div>
    </div>

    <!-- 筛选栏 -->
    <el-card class="filter-card" shadow="hover">
      <el-form :inline="true" class="filter-form">
        <el-form-item label="申请类型">
          <el-select v-model="filterType" placeholder="请选择" @change="fetchApprovals" style="width: 140px">
            <el-option label="全部" value="" />
            <el-option label="工作日期变更" value="service_days_change" />
            <el-option label="志愿者请假" value="leave" />
            <el-option label="服务变更" value="service_change" />
            <el-option label="积分申诉" value="points_appeal" />
          </el-select>
        </el-form-item>
        <el-form-item label="审批状态">
          <el-select v-model="filterStatus" placeholder="请选择" @change="fetchApprovals" style="width: 120px">
            <el-option label="全部" value="" />
            <el-option label="待审核" value="pending" />
            <el-option label="已通过" value="approved" />
            <el-option label="已驳回" value="rejected" />
          </el-select>
        </el-form-item>
        <el-form-item label="申请人/ID">
          <el-input
            v-model="filterKeyword"
            placeholder="请输入申请人姓名或ID"
            clearable
            style="width: 180px"
            @keyup.enter="fetchApprovals"
          />
        </el-form-item>
        <el-form-item label="申请时间">
          <el-date-picker
            v-model="filterDateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            style="width: 240px"
            @change="fetchApprovals"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchApprovals">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
          <el-button @click="resetFilter">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card class="table-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-icon :size="20" color="#00a88d"><Stamp /></el-icon>
            <span class="card-title">审批列表</span>
          </div>
          <span class="total-count">共 {{ total }} 条</span>
        </div>
      </template>

      <el-table :data="approvalList" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="id" label="申请ID" width="100" />
        <el-table-column label="申请类型" width="140">
          <template #default="{ row }">
            <el-tag size="small" :type="getTypeTagType(row.type)">
              {{ getTypeText(row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="申请人信息" min-width="160">
          <template #default="{ row }">
            <div class="user-info">
              <div class="info-name">{{ row.applicantName }}</div>
              <div class="info-id">ID: {{ row.applicantId }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="申请时间" width="180" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="getStatusTagType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openDetailDialog(row)">
              <el-icon><View /></el-icon>
              查看详情
            </el-button>
            <el-button
              v-if="row.status === 'pending'"
              type="success"
              link
              size="small"
              @click="handleApprove(row)"
            >
              <el-icon><Check /></el-icon>
              通过
            </el-button>
            <el-button
              v-if="row.status === 'pending'"
              type="danger"
              link
              size="small"
              @click="handleReject(row)"
            >
              <el-icon><Close /></el-icon>
              驳回
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @change="fetchApprovals"
        style="margin-top: 24px; justify-content: flex-end"
      />
    </el-card>

    <!-- 审批详情弹窗 -->
    <el-dialog v-model="detailDialogVisible" title="审批详情" width="650">
      <div class="detail-content">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="申请ID">
            <span class="value-text">{{ currentApproval.id }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="申请类型">
            <el-tag :type="getTypeTagType(currentApproval.type)" size="small">
              {{ getTypeText(currentApproval.type) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="申请人">
            <span class="value-text">{{ currentApproval.applicantName }} (ID: {{ currentApproval.applicantId }})</span>
          </el-descriptions-item>
          <el-descriptions-item label="申请时间">
            <span class="time-text">{{ currentApproval.createTime }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="申请内容">
            <div class="content-text">{{ currentApproval.content }}</div>
          </el-descriptions-item>
          <el-descriptions-item label="审批结果" v-if="currentApproval.status !== 'pending'">
            <el-tag :type="getStatusTagType(currentApproval.status)">
              {{ getStatusText(currentApproval.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="审批意见" v-if="currentApproval.remark">
            <span class="value-text">{{ currentApproval.remark }}</span>
          </el-descriptions-item>
        </el-descriptions>

        <!-- 待审核时显示审批意见输入框 -->
        <div v-if="currentApproval.status === 'pending'" style="margin-top: 20px">
          <div class="remark-label">审批意见：</div>
          <el-input
            v-model="approvalRemark"
            type="textarea"
            :rows="3"
            placeholder="请输入审批意见（选填）"
          />
        </div>
      </div>

      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
        <el-button
          v-if="currentApproval.status === 'pending'"
          type="success"
          @click="confirmApprove"
        >
          <el-icon><Check /></el-icon>
          确认通过
        </el-button>
        <el-button
          v-if="currentApproval.status === 'pending'"
          type="danger"
          @click="confirmReject"
        >
          <el-icon><Close /></el-icon>
          确认驳回
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Stamp, View, Check, Close } from '@element-plus/icons-vue'
import { getApprovalPage, approveApplication, rejectApplication } from '@/api/admin'

const loading = ref(false)
const filterType = ref('')
const filterStatus = ref('')
const filterKeyword = ref('')
const filterDateRange = ref<string[]>([])

const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const approvalList = ref<any[]>([])

const detailDialogVisible = ref(false)
const currentApproval = ref<any>({})
const approvalRemark = ref('')

onMounted(() => {
  fetchApprovals()
})

const fetchApprovals = async () => {
  loading.value = true
  try {
    const startDate = filterDateRange.value?.[0] || undefined
    const endDate = filterDateRange.value?.[1] || undefined

    const res = await getApprovalPage(
      currentPage.value,
      pageSize.value,
      filterType.value || undefined,
      filterStatus.value || undefined,
      filterKeyword.value || undefined,
      startDate,
      endDate
    )
    approvalList.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (err) {
    console.error('获取审批列表失败', err)
    ElMessage.error('获取审批列表失败')
  } finally {
    loading.value = false
  }
}

const resetFilter = () => {
  filterType.value = ''
  filterStatus.value = ''
  filterKeyword.value = ''
  filterDateRange.value = []
  currentPage.value = 1
  fetchApprovals()
}

const openDetailDialog = (row: any) => {
  currentApproval.value = { ...row }
  approvalRemark.value = ''
  detailDialogVisible.value = true
}

const handleApprove = (row: any) => {
  currentApproval.value = { ...row }
  approvalRemark.value = ''
  confirmApprove()
}

const handleReject = (row: any) => {
  currentApproval.value = { ...row }
  approvalRemark.value = ''
  confirmReject()
}

const confirmApprove = async () => {
  try {
    await approveApplication(currentApproval.value.id, approvalRemark.value || undefined)
    ElMessage.success('已通过')
    detailDialogVisible.value = false
    fetchApprovals()
  } catch (err) {
    ElMessage.error('操作失败')
  }
}

const confirmReject = async () => {
  if (!approvalRemark.value || !approvalRemark.value.trim()) {
    ElMessage.warning('请填写驳回原因')
    return
  }

  try {
    await rejectApplication(currentApproval.value.id, approvalRemark.value)
    ElMessage.success('已驳回')
    detailDialogVisible.value = false
    fetchApprovals()
  } catch (err) {
    ElMessage.error('操作失败')
  }
}

const getTypeText = (type: string) => {
  const map: Record<string, string> = {
    service_days_change: '工作日期变更',
    leave: '志愿者请假',
    service_change: '服务变更',
    points_appeal: '积分申诉'
  }
  return map[type] || '未知'
}

const getTypeTagType = (type: string) => {
  const map: Record<string, string> = {
    service_days_change: 'primary',
    leave: 'warning',
    service_change: 'success',
    points_appeal: 'danger'
  }
  return map[type] || 'info'
}

const getStatusText = (status: string) => {
  const map: Record<string, string> = {
    pending: '待审核',
    approved: '已通过',
    rejected: '已驳回'
  }
  return map[status] || '未知'
}

const getStatusTagType = (status: string) => {
  const map: Record<string, string> = {
    pending: 'warning',
    approved: 'success',
    rejected: 'danger'
  }
  return map[status] || 'info'
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

.user-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}

.info-id {
  font-size: 12px;
  color: var(--text-tertiary);
}

.detail-content {
  padding: 10px 0;
}

.value-text {
  font-size: 15px;
  color: var(--text-primary);
  font-weight: 500;
}

.time-text {
  color: var(--text-secondary);
  font-size: 14px;
}

.content-text {
  white-space: pre-wrap;
  line-height: 1.8;
  color: var(--text-primary);
  font-size: 14px;
}

.remark-label {
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: 8px;
}
</style>
