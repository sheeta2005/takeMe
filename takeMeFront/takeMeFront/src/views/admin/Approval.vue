<template>
  <div class="page-container">
    <div class="header-row">
      <h2 class="page-title">业务审批</h2>
    </div>

    <!-- 筛选栏（和其他页面完全统一） -->
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

    <!-- 表格（自适应填满） -->
    <div class="table-card">
      <el-table :data="approvalList" border stripe style="width: 100%">
        <el-table-column prop="id" label="申请ID" min-width="100" align="center" />
        <el-table-column label="申请类型" min-width="120" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="getTypeTagType(row.type)">
              {{ getTypeText(row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="申请人信息" min-width="140" align="center">
          <template #default="{ row }">
            <div class="user-info">
              <div class="info-name">{{ row.applicantName }}</div>
              <div class="info-id">ID: {{ row.applicantId }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="申请时间" min-width="160" align="center" />
        <el-table-column label="状态" min-width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="getStatusTagType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="200" align="center">
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
          <span class="value">{{ getTypeText(currentApproval.type) }}</span>
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
// 完全使用你提供的API
import { getApprovalPage, doApproval } from '@/api/admin'

// 筛选
const filterType = ref('')
const filterStatus = ref('')
const filterKeyword = ref('')
const filterDateRange = ref<string[]>([])

// 分页
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 列表数据
const approvalList = ref<any[]>([])

// 详情弹窗
const detailDialogVisible = ref(false)
const currentApproval = ref<any>({})
const approvalRemark = ref('')

onMounted(() => {
  fetchApprovals()
})

// 获取审批列表（API已注释，使用精简模拟数据）
const fetchApprovals = async () => {
  try {
    // --- 接口调用已注释，对接后端直接取消注释即可 ---
    /*
    const params = {
      page: currentPage.value,
      pageSize: pageSize.value,
      type: filterType.value || undefined,
      status: filterStatus.value || undefined,
      keyword: filterKeyword.value || undefined,
      startDate: filterDateRange.value?.[0] || undefined,
      endDate: filterDateRange.value?.[1] || undefined
    }
    const res = await getApprovalPage(params)
    approvalList.value = res.data.list
    total.value = res.data.total
    */

    // --- 精简模拟数据（仅5条，覆盖4种业务类型） ---
    approvalList.value = generateMockData()
    total.value = 32
  } catch (err) {
    console.error('获取审批列表失败', err)
    ElMessage.error('获取审批列表失败')
  }
}

// 重置筛选
const resetFilter = () => {
  filterType.value = ''
  filterStatus.value = ''
  filterKeyword.value = ''
  filterDateRange.value = []
  currentPage.value = 1
  fetchApprovals()
}

// 打开详情弹窗
const openDetailDialog = (row: any) => {
  currentApproval.value = { ...row }
  approvalRemark.value = ''
  detailDialogVisible.value = true
}

// 快速通过
const handleApprove = (row: any) => {
  ElMessageBox.confirm('确定要通过该申请吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      // --- 接口调用已注释 ---
      /*
      await doApproval({
        id: row.id,
        status: 1,
        remark: ''
      })
      */
      ElMessage.success('审批通过')
      fetchApprovals()
    } catch (err) {
      ElMessage.error('操作失败')
    }
  })
}

// 快速驳回
const handleReject = (row: any) => {
  ElMessageBox.prompt('请输入驳回原因', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  }).then(async ({ value }) => {
    try {
      // --- 接口调用已注释 ---
      /*
      await doApproval({
        id: row.id,
        status: 2,
        remark: value
      })
      */
      ElMessage.success('已驳回')
      fetchApprovals()
    } catch (err) {
      ElMessage.error('操作失败')
    }
  })
}

// 详情页确认通过
const confirmApprove = async () => {
  try {
    // --- 接口调用已注释 ---
    /*
    await doApproval({
      id: currentApproval.value.id,
      status: 1,
      remark: approvalRemark.value
    })
    */
    ElMessage.success('审批通过')
    detailDialogVisible.value = false
    fetchApprovals()
  } catch (err) {
    ElMessage.error('操作失败')
  }
}

// 详情页确认驳回
const confirmReject = async () => {
  if (!approvalRemark.value) {
    ElMessage.warning('请输入驳回原因')
    return
  }
  try {
    // --- 接口调用已注释 ---
    /*
    await doApproval({
      id: currentApproval.value.id,
      status: 2,
      remark: approvalRemark.value
    })
    */
    ElMessage.success('已驳回')
    detailDialogVisible.value = false
    fetchApprovals()
  } catch (err) {
    ElMessage.error('操作失败')
  }
}

// 类型/状态映射（已去掉不需要的类型）
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
    register: 'primary',
    leave: 'warning',
    service_change: 'info',
    points_appeal: 'success'
  }
  return map[type] || ''
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

// 生成精简模拟数据（仅5条）
const generateMockData = () => {
  return [
    {
      id: 'APR0001',
      type: 'register',
      applicantId: 3001,
      applicantName: '张志愿者',
      createTime: '2026-05-26 10:30:00',
      status: 'pending',
      content: '申请成为平台志愿者，已完成实名认证和培训',
      remark: ''
    },
    {
      id: 'APR0002',
      type: 'leave',
      applicantId: 3002,
      applicantName: '王志愿者',
      createTime: '2026-05-25 14:20:00',
      status: 'pending',
      content: '因个人身体原因，申请5月27日-5月29日请假3天',
      remark: ''
    },
    {
      id: 'APR0003',
      type: 'service_change',
      applicantId: 3003,
      applicantName: '李志愿者',
      createTime: '2026-05-24 09:15:00',
      status: 'approved',
      content: '申请将服务范围从幸福小区扩展到阳光花园和和平社区',
      remark: '同意扩展服务范围'
    },
    {
      id: 'APR0004',
      type: 'points_appeal',
      applicantId: 3004,
      applicantName: '赵志愿者',
      createTime: '2026-05-23 16:40:00',
      status: 'rejected',
      content: '申请补加5月22日助餐服务积分15分，系统未自动发放',
      remark: '经核实，该服务已取消，不予补加'
    },
    {
      id: 'APR0005',
      type: 'register',
      applicantId: 3005,
      applicantName: '孙志愿者',
      createTime: '2026-05-22 11:00:00',
      status: 'approved',
      content: '申请成为平台志愿者，有相关护理经验',
      remark: '同意注册'
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
