<template>
  <div class="page-container">
    <div class="header-row">
      <h2 class="page-title">业务审批</h2>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <div class="filter-item">
        <label class="filter-label">申请类型</label>
        <el-select v-model="filterType" placeholder="请选择" @change="fetchApprovals">
          <el-option label="全部" value="" />
          <el-option label="志愿者注册" value="register" />
          <el-option label="志愿者请假" value="leave" />
          <el-option label="服务变更" value="service_change" />
          <el-option label="积分申诉" value="points_appeal" />
        </el-select>
      </div>

      <div class="filter-item">
        <label class="filter-label">审批状态</label>
        <el-select v-model="filterStatus" placeholder="请选择" @change="fetchApprovals">
          <el-option label="全部" value="" />
          <el-option label="待审核" value="pending" />
          <el-option label="已通过" value="approved" />
          <el-option label="已驳回" value="rejected" />
        </el-select>
      </div>

      <div class="filter-item">
        <label class="filter-label">申请人/ID</label>
        <el-input
          v-model="filterKeyword"
          placeholder="请输入申请人姓名或ID"
          clearable
          @keyup.enter="fetchApprovals"
        />
      </div>

      <div class="filter-item">
        <label class="filter-label">申请时间</label>
        <el-date-picker
          v-model="filterDateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          @change="fetchApprovals"
        />
      </div>

      <el-button type="primary" @click="fetchApprovals">查询</el-button>
      <el-button @click="resetFilter">重置</el-button>
    </div>

    <!-- 表格 -->
    <div class="table-card">
      <el-table :data="approvalList" border stripe style="width: 100%">
        <el-table-column prop="id" label="申请ID" width="100" align="center" />
        <el-table-column label="申请类型" width="140" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="getTypeTagType(row.type)">
              {{ getTypeText(row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="申请人信息" min-width="160" align="center">
          <template #default="{ row }">
            <div class="user-info">
              <div class="info-name">{{ row.applicantName }}</div>
              <div class="info-id">ID: {{ row.applicantId }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="申请时间" width="180" align="center" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="getStatusTagType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDetailDialog(row)">查看详情</el-button>
            <el-button
              v-if="row.status === 'pending'"
              type="success"
              link
              @click="handleApprove(row)"
            >
              通过
            </el-button>
            <el-button
              v-if="row.status === 'pending'"
              type="danger"
              link
              @click="handleReject(row)"
            >
              驳回
            </el-button>
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
        @change="fetchApprovals"
      />
    </div>

    <!-- 审批详情弹窗 -->
    <el-dialog v-model="detailDialogVisible" title="审批详情" width="650">
      <div class="detail-content">
        <div class="detail-row">
          <span class="label">申请ID：</span>
          <span class="value">{{ currentApproval.id }}</span>
        </div>
        <div class="detail-row">
          <span class="label">申请类型：</span>
          <el-tag :type="getTypeTagType(currentApproval.type)" size="small">
            {{ getTypeText(currentApproval.type) }}
          </el-tag>
        </div>
        <div class="detail-row">
          <span class="label">申请人：</span>
          <span class="value">{{ currentApproval.applicantName }} (ID: {{ currentApproval.applicantId }})</span>
        </div>
        <div class="detail-row">
          <span class="label">申请时间：</span>
          <span class="value">{{ currentApproval.createTime }}</span>
        </div>
        <div class="detail-row">
          <span class="label">申请内容：</span>
          <div class="value content">{{ currentApproval.content }}</div>
        </div>
        <div class="detail-row" v-if="currentApproval.status !== 'pending'">
          <span class="label">审批结果：</span>
          <el-tag :type="getStatusTagType(currentApproval.status)">
            {{ getStatusText(currentApproval.status) }}
          </el-tag>
        </div>
        <div class="detail-row" v-if="currentApproval.remark">
          <span class="label">审批意见：</span>
          <span class="value">{{ currentApproval.remark }}</span>
        </div>

        <!-- 待审核时显示审批意见输入框 -->
        <div class="detail-row" v-if="currentApproval.status === 'pending'">
          <span class="label">审批意见：</span>
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
          确认通过
        </el-button>
        <el-button
          v-if="currentApproval.status === 'pending'"
          type="danger"
          @click="confirmReject"
        >
          确认驳回
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getApprovalPage, approveApplication, rejectApplication } from '@/api/admin'

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
    register: '志愿者注册',
    leave: '志愿者请假',
    service_change: '服务变更',
    points_appeal: '积分申诉'
  }
  return map[type] || '未知'
}

const getTypeTagType = (type: string) => {
  const map: Record<string, string> = {
    register: 'success',
    leave: 'warning',
    service_change: 'primary',
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
  color: #999;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  background: #fff;
  padding: 16px 20px;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

/* 详情弹窗样式 */
.detail-content {
  padding: 10px 0;
}

.detail-row {
  display: flex;
  margin-bottom: 16px;
  align-items: flex-start;
}

.label {
  width: 100px;
  font-size: 14px;
  color: #666;
  flex-shrink: 0;
}

.value {
  font-size: 14px;
  color: #333;
  flex: 1;
}

.value.content {
  white-space: pre-wrap;
  line-height: 1.6;
}
</style>
