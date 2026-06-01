<template>
  <div class="page-container">
    <div class="header-row">
      <h2 class="page-title">消息详情</h2>
    </div>

    <div class="detail-card" v-loading="loading">
      <div class="detail-header">
        <div class="detail-title">{{ message?.title || '暂无标题' }}</div>
        <el-tag :type="getTypeTagType(message?.type)" size="large">
          {{ getTypeText(message?.type) }}
        </el-tag>
      </div>

      <div class="detail-content">
        <p class="content-text">{{ message?.content || '暂无内容' }}</p>

        <!-- 关联订单跳转 -->
        <div v-if="message?.relatedId" class="related-order">
          <span>关联订单：</span>
          <el-button
            type="primary"
            link
            size="default"
            @click="goToOrder(message.relatedUrl)"
          >
            查看订单 {{ message.relatedId }}
          </el-button>
        </div>
      </div>

      <div class="detail-footer">
        <span class="create-time">发送时间：{{ message?.createTime || '未知时间' }}</span>
      </div>
    </div>

    <div class="action-buttons">
      <el-button size="default" @click="$router.back()">返回列表</el-button>
      <el-button
        v-if="!message?.isRead"
        type="primary"
        size="default"
        @click="confirmMarkRead"
      >
        标记为已读
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { Message } from '@/types/Message.ts'

const route = useRoute()
const router = useRouter()
const message = ref<Message>({} as Message)
const loading = ref(false)

onMounted(() => {
  const id = Number(route.params.id)
  if (id) {
    fetchMessageDetail(id)
  } else {
    ElMessage.error('参数异常')
    router.back()
  }
})

// 获取消息详情
const fetchMessageDetail = async (id: number) => {
  loading.value = true
  try {
    // 模拟数据
    message.value = {
      id: id,
      userId: 2001,
      type: 1,
      title: '服务提醒：助餐服务',
      content: '您的助餐服务订单ORD20260520001将于明天09:00送达，服务内容为营养套餐A。请您保持电话畅通，准备接收。',
      createTime: '2026-05-19 10:30:00',
      isRead: false,
      relatedId: 'ORD20260520001',
      relatedUrl: `/user/order/detail/ORD20260520001`
    }
  } catch (err) {
    console.error('获取消息详情失败', err)
    ElMessage.error('获取消息详情失败')
  } finally {
    loading.value = false
  }
}

// 跳转到关联订单
const goToOrder = (url?: string) => {
  if (!url) {
    ElMessage.info('暂无关联订单')
    return
  }
  router.push(url)
}

// 标记已读
const confirmMarkRead = () => {
  message.value.isRead = true
  ElMessage.success('标记已读成功')
}

// 类型映射
const getTypeText = (type?: number): string => {
  if (type === undefined) return '未知'
  const map = { 0: '系统通知', 1: '服务通知', 2: '温馨提醒' }
  return map[type] || '未知'
}

const getTypeTagType = (type?: number): string => {
  if (type === undefined) return ''
  const map = { 0: 'primary', 1: 'warning', 2: 'success' }
  return map[type] || ''
}
</script>

<style scoped>
.page-container {
  width: 100%;
  padding: 12px 0;
  max-width: 800px;
  margin: 0 auto;
}

.header-row {
  margin-bottom: 24px;
}

.page-title {
  font-size: 28px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
}

.detail-card {
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(0, 184, 153, 0.1);
  padding: 36px 40px;
  margin-bottom: 24px;
  min-height: 300px;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e5e7eb;
}

.detail-title {
  font-size: 22px;
  font-weight: 600;
  color: #1f2937;
}

.detail-content {
  margin-bottom: 24px;
}

.content-text {
  font-size: 16px;
  line-height: 1.8;
  color: #4b5563;
  margin-bottom: 20px;
  white-space: pre-wrap;
}

.related-order {
  font-size: 16px;
  color: #6b7280;
  padding: 12px 16px;
  background: #f9fafb;
  border-radius: 8px;
  display: inline-block;
}

.detail-footer {
  padding-top: 16px;
  border-top: 1px solid #e5e7eb;
}

.create-time {
  font-size: 14px;
  color: #9ca3af;
}

.action-buttons {
  display: flex;
  gap: 16px;
  justify-content: center;
  margin-top: 12px;
}
</style>
