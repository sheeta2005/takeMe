<template>
  <div class="page-container">
    <div class="header-row">
      <h2 class="page-title">消息详情</h2>
    </div>

    <div class="detail-card">
      <div class="detail-header">
        <div class="detail-title">{{ message.title }}</div>
        <el-tag :type="getTypeTagType(message.type)" size="large">
          {{ getTypeText(message.type) }}
        </el-tag>
      </div>

      <div class="detail-content">
        <p class="content-text">{{ message.content }}</p>

        <!-- 关联订单跳转（修复版） -->
        <div v-if="message.relatedId" class="related-order">
          <span>关联订单：</span>
          <el-button
            type="primary"
            link
            size="large"
            @click="goToOrder(message.relatedUrl)"
          >
            查看订单 {{ message.relatedId }}
          </el-button>
        </div>
      </div>

      <div class="detail-footer">
        <span class="create-time">发送时间：{{ message.createTime }}</span>
      </div>
    </div>

    <div class="action-buttons">
      <el-button size="large" @click="$router.back()">返回列表</el-button>
      <el-button
        v-if="!message.isRead"
        type="primary"
        size="large"
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
// import { getUserMessageDetail } from '@/api/user'
import type { message } from '@/types/Message.ts'

const route = useRoute()
const router = useRouter()
const message = ref<message>({} as message)

onMounted(() => {
  const id = Number(route.params.id)
  if (id) {
    fetchMessageDetail(id)
  }
})

// 获取消息详情（接口已注释）
const fetchMessageDetail = async (id: number) => {
  try {
    // --- 后端接口调用（已注释） ---
    /*
    const res = await getUserMessageDetail(id)
    message.value = res.data
    */

    // --- 模拟数据 ---
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
  }
}

// 修复：跳转到关联订单
const goToOrder = (url?: string) => {
  if (url) {
    router.push(url)
  } else {
    ElMessage.info('暂无关联订单')
  }
}

// 标记已读
const confirmMarkRead = () => {
  message.value.isRead = true
  ElMessage.success('已标记为已读')
}

// 类型映射
const getTypeText = (type: number) => {
  const map: Record<number, string> = {
    0: '系统通知',
    1: '服务通知',
    2: '温馨提醒'
  }
  return map[type] || '未知'
}

const getTypeTagType = (type: number) => {
  const map: Record<number, string> = {
    0: 'primary',
    1: 'warning',
    2: 'success'
  }
  return map[type] || ''
}
</script>

<style scoped>
.page-container {
  width: 100%;
  padding: 10px 0;
  max-width: 800px;
  margin: 0 auto;
}

.header-row {
  margin-bottom: 24px;
}

.page-title {
  font-size: 32px;
  font-weight: bold;
  color: #222;
  margin: 0;
}

.detail-card {
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 4px 16px rgba(0, 184, 153, 0.1);
  padding: 40px;
  margin-bottom: 24px;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 2px solid #eee;
}

.detail-title {
  font-size: 24px;
  font-weight: 600;
  color: #333;
}

.detail-content {
  margin-bottom: 24px;
}

.content-text {
  font-size: 18px;
  line-height: 1.8;
  color: #333;
  margin-bottom: 16px;
}

.related-order {
  font-size: 18px;
  color: #666;
}

.detail-footer {
  padding-top: 16px;
  border-top: 2px solid #eee;
}

.create-time {
  font-size: 16px;
  color: #999;
}

.action-buttons {
  display: flex;
  gap: 20px;
  justify-content: center;
}

:deep(.el-button) {
  font-size: 18px;
  padding: 12px 32px;
}
</style>
