<template>
  <div class="page-container">
    <div class="header-row">
      <h2 class="page-title">消息中心</h2>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <div class="filter-item">
        <label class="filter-label">消息类型</label>
        <el-select v-model="filterType" placeholder="请选择类型" @change="fetchMsgs">
          <el-option label="全部" value="" />
          <el-option label="系统通知" :value="0" />
          <el-option label="任务通知" :value="1" />
          <el-option label="温馨提醒" :value="2" />
          <el-option label="用户建议" value="suggestion" />
        </el-select>
      </div>

      <div class="filter-item">
        <label class="filter-label">状态</label>
        <el-select v-model="filterStatus" placeholder="请选择状态" @change="fetchMsgs">
          <el-option label="全部" value="" />
          <el-option label="未读" :value="false" />
          <el-option label="已读" :value="true" />
        </el-select>
      </div>

      <div class="filter-item">
        <label class="filter-label">时间范围</label>
        <el-date-picker
          v-model="filterDateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          @change="fetchMsgs"
        />
      </div>

      <el-button type="primary" @click="fetchMsgs">查询</el-button>
      <el-button @click="resetFilter">重置</el-button>
    </div>

    <!-- 消息列表卡片 -->
    <div class="list-card">
      <el-list :data="msgList" class="msg-list">
        <el-list-item v-for="msg in msgList" :key="msg.id" class="msg-item">
          <template #default>
            <div class="msg-content">
              <div class="msg-header">
                <div class="msg-title-wrapper">
                  <el-tag
                    v-if="msg.type !== undefined"
                    :type="getTypeTagType(msg.type)"
                    size="small"
                    class="type-tag"
                  >
                    {{ getTypeText(msg.type) }}
                  </el-tag>
                  <span class="msg-title" :class="{ unread: !msg.isRead }">{{ msg.title }}</span>
                  <el-icon v-if="!msg.isRead" class="unread-icon"><Bell /></el-icon>
                </div>
                <div class="msg-time">{{ msg.createTime }}</div>
              </div>
              <div class="msg-preview">{{ msg.content }}</div>
            </div>
            <div class="msg-actions">
              <el-button
                v-if="!msg.isRead"
                type="primary"
                link
                @click="handleMarkRead(msg)"
              >
                标记已读
              </el-button>
              <el-button type="primary" link @click="openDetailDialog(msg)">查看详情</el-button>
            </div>
          </template>
        </el-list-item>
      </el-list>

      <!-- 空状态 -->
      <el-empty v-if="msgList.length === 0" description="暂无消息" />
    </div>

    <!-- 分页 -->
    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @change="fetchMsgs"
      />
    </div>

    <!-- 消息详情弹窗 -->
    <el-dialog v-model="detailDialogVisible" title="消息详情" width="650">
      <div class="detail-content">
        <div class="detail-row">
          <span class="label">消息类型：</span>
          <el-tag :type="getTypeTagType(currentMsg.type)" size="small">
            {{ getTypeText(currentMsg.type) }}
          </el-tag>
        </div>
        <div class="detail-row">
          <span class="label">消息标题：</span>
          <span class="value">{{ currentMsg.title }}</span>
        </div>
        <div class="detail-row">
          <span class="label">发送时间：</span>
          <span class="value">{{ currentMsg.createTime }}</span>
        </div>
        <div class="detail-row">
          <span class="label">发送方：</span>
          <span class="value">{{ currentMsg.sender || '系统消息' }}</span>
        </div>
        <div class="detail-row">
          <span class="label">消息内容：</span>
          <div class="value content">{{ currentMsg.content }}</div>
        </div>
      </div>

      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
        <el-button
          v-if="!currentMsg.isRead"
          type="primary"
          @click="confirmMarkRead"
        >
          标记为已读
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Bell } from '@element-plus/icons-vue'
import { getInboxPage } from '@/api/admin'
import type { message } from '@/types/message'

// 筛选
const filterType = ref('')
const filterStatus = ref('')
const filterDateRange = ref<string[]>([])

// 分页
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 列表数据
const msgList = ref<message[]>([])

// 详情弹窗
const detailDialogVisible = ref(false)
const currentMsg = ref<message>({})

onMounted(() => {
  fetchMsgs()
})

// 获取消息列表（接口已注释，使用模拟数据）
const fetchMsgs = async () => {
  try {
    // --- 接口调用已注释，对接后端直接取消注释即可 ---
    /*
    const params = {
      page: currentPage.value,
      pageSize: pageSize.value,
      type: filterType.value || undefined,
      isRead: filterStatus.value === '' ? undefined : filterStatus.value,
      startDate: filterDateRange.value?.[0] || undefined,
      endDate: filterDateRange.value?.[1] || undefined
    }
    const res = await getInboxPage(params)
    msgList.value = res.data.list
    total.value = res.data.total
    */

    // --- 模拟数据（包含用户建议、系统通知等） ---
    msgList.value = generateMockData()
    total.value = 25
  } catch (err) {
    console.error('获取消息列表失败', err)
    ElMessage.error('获取消息列表失败')
  }
}

// 重置筛选
const resetFilter = () => {
  filterType.value = ''
  filterStatus.value = ''
  filterDateRange.value = []
  currentPage.value = 1
  fetchMsgs()
}

// 打开详情弹窗
const openDetailDialog = (msg: message) => {
  currentMsg.value = { ...msg }
  detailDialogVisible.value = true
}

// 快速标记已读
const handleMarkRead = (msg: message) => {
  ElMessage.success('已标记为已读')
  msg.isRead = true
}

// 详情页确认标记已读
const confirmMarkRead = () => {
  currentMsg.value.isRead = true
  ElMessage.success('已标记为已读')
  detailDialogVisible.value = false
  fetchMsgs()
}

// 类型/状态映射
const getTypeText = (type: number | string) => {
  const map: Record<string, string> = {
    0: '系统通知',
    1: '任务通知',
    2: '温馨提醒',
    suggestion: '用户建议'
  }
  return map[type] || '未知'
}

const getTypeTagType = (type: number | string) => {
  const map: Record<string, string> = {
    0: 'primary',
    1: 'warning',
    2: 'success',
    suggestion: 'info'
  }
  return map[type] || ''
}

// 生成模拟数据（包含用户建议、系统通知等）
const generateMockData = (): message[] => {
  return [
    {
      id: 1,
      userId: 0,
      type: 0,
      title: '平台更新通知',
      content: '平台将于2026-06-01进行系统维护，维护期间部分服务可能暂时无法使用，请提前做好准备。',
      createTime: '2026-05-25 10:30:00',
      isRead: false,
      relatedId: '',
      relatedUrl: ''
    },
    {
      id: 2,
      userId: 2001,
      type: 'suggestion',
      title: '用户建议：增加夜间服务',
      content: '王奶奶建议平台增加夜间助餐服务，方便行动不便的老人在夜间也能获取餐食。',
      createTime: '2026-05-24 15:20:00',
      isRead: false,
      relatedId: '2001',
      relatedUrl: ''
    },
    {
      id: 3,
      userId: 3002,
      type: 1,
      title: '任务提醒：助洁服务',
      content: '您有一个新的助洁服务任务，服务对象为李爷爷，服务时间为2026-05-26 09:00。',
      createTime: '2026-05-23 08:15:00',
      isRead: true,
      relatedId: '10012',
      relatedUrl: ''
    },
    {
      id: 4,
      userId: 2003,
      type: 'suggestion',
      title: '用户建议：优化志愿者匹配',
      content: '张婆婆建议优化志愿者匹配算法，希望能匹配到距离更近的志愿者，减少等待时间。',
      createTime: '2026-05-22 14:40:00',
      isRead: true,
      relatedId: '2003',
      relatedUrl: ''
    },
    {
      id: 5,
      userId: 0,
      type: 2,
      title: '温馨提醒：天气变化',
      content: '未来三天将有降雨，请志愿者和老人注意出行安全，避免在恶劣天气外出。',
      createTime: '2026-05-21 11:00:00',
      isRead: false,
      relatedId: '',
      relatedUrl: ''
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

.list-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  padding: 20px;
  margin-bottom: 20px;
}

.msg-list {
  width: 100%;
}

.msg-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0;
  border-bottom: 1px solid #f5f5f5;
}

.msg-item:last-child {
  border-bottom: none;
}

.msg-content {
  flex: 1;
}

.msg-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.msg-title-wrapper {
  display: flex;
  align-items: center;
  gap: 10px;
}

.type-tag {
  flex-shrink: 0;
}

.msg-title {
  font-size: 16px;
  font-weight: 500;
  color: #333;
}

.msg-title.unread {
  color: #222;
  font-weight: 600;
}

.unread-icon {
  color: #f56c6c;
  font-size: 16px;
}

.msg-time {
  font-size: 14px;
  color: #999;
}

.msg-preview {
  font-size: 14px;
  color: #666;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.msg-actions {
  display: flex;
  gap: 16px;
  flex-shrink: 0;
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
