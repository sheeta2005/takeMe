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

        <div v-if="message?.relatedOrderId" class="related-order">
          <span>关联订单：</span>
          <el-button
            type="primary"
            link
            size="default"
            @click="goToOrder"
          >
            查看订单 {{ message.relatedOrderId }}
          </el-button>
        </div>
      </div>

      <div class="detail-footer">
        <span class="create-time">发送时间：{{ formatTime(message?.createTime) }}</span>
      </div>
    </div>

    <div class="action-buttons">
      <el-button size="default" @click="$router.back()">返回列表</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getUserMessageById, markMessageRead } from '@/api/user'

const route = useRoute()
const router = useRouter()
const message = ref<any>(null)
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

const fetchMessageDetail = async (id: number) => {
  loading.value = true
  try {
    const res = await getUserMessageById(id)
    if (res.code === 200 && res.data) {
      message.value = res.data

      if (message.value.isRead === 0) {
        await markMessageRead(id)
        message.value.isRead = 1
      }
    } else {
      ElMessage.error('消息不存在')
      router.back()
    }
  } catch (err) {
    console.error('获取消息详情失败', err)
    ElMessage.error('获取消息详情失败')
    router.back()
  } finally {
    loading.value = false
  }
}

const goToOrder = () => {
  if (!message.value?.relatedOrderId) {
    ElMessage.info('暂无关联订单')
    return
  }
  router.push(`/user/order/detail/${message.value.relatedOrderId}`)
}

const getTypeText = (type?: number): string => {
  if (type === undefined) return '未知'
  const map = { 0: '系统通知', 1: '订单通知', 2: '服务通知' }
  return map[type] || '未知'
}

const getTypeTagType = (type?: number): string => {
  if (type === undefined) return ''
  const map = { 0: 'primary', 1: 'warning', 2: 'success' }
  return map[type] || ''
}

const formatTime = (time: string) => {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
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
