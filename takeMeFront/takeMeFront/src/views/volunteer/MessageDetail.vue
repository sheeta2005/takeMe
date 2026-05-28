<template>
  <div class="page-container">
    <div class="header-row">
      <h2 class="page-title">消息详情</h2>
    </div>

    <div class="detail-card">
      <div class="detail-header">
        <div class="detail-title">{{ message.title }}</div>
        <el-tag :type="getTypeTagType(message.type)" size="small">
          {{ getTypeText(message.type) }}
        </el-tag>
      </div>

      <div class="detail-content">
        <p class="content-text">{{ message.content }}</p>

        <!-- 关联订单号（可跳转） -->
        <div v-if="message.relatedId" class="related-order">
          <span>关联订单号：</span>
          <el-button
            type="primary"
            link
            @click="goToOrder(message.relatedUrl)"
          >
            {{ message.relatedId }}
          </el-button>
        </div>
      </div>

      <div class="detail-footer">
        <span class="create-time">发送时间：{{ message.createTime }}</span>
      </div>
    </div>

    <div class="action-buttons">
      <el-button @click="$router.back()">返回列表</el-button>
      <el-button
        v-if="!message.isRead"
        type="primary"
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
// import { getVolunteerMessageDetail } from '@/api/volunteer'
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
    // --- 接口调用已注释，对接后端直接取消注释即可 ---
    /*
    const res = await getVolunteerMessageDetail(id)
    message.value = res.data
    */

    // --- 模拟数据 ---
    message.value = {
      id: id,
      userId: 3001,
      type: 1,
      title: '新任务通知',
      content: '您有一个新的助餐服务任务，订单号：ORD20260520001，请及时处理。服务时间为2026-05-21 09:00，请提前准备。',
      createTime: '2026-05-20 10:30:00',
      isRead: false,
      relatedId: 'ORD20260520001',
      relatedUrl: '/volunteer/order/ORD20260520001'
    }
  } catch (err) {
    console.error('获取消息详情失败', err)
    ElMessage.error('获取消息详情失败')
  }
}

// 跳转到关联订单
const goToOrder = (url?: string) => {
  if (url) {
    router.push(url)
  } else {
    ElMessage.info('暂无关联订单')
  }
}

// 标记为已读
const confirmMarkRead = () => {
  message.value.isRead = true
  ElMessage.success('已标记为已读')
}

// 类型映射
const getTypeText = (type: number | string) => {
  const map: Record<string, string> = {
    0: '系统通知',
    1: '任务通知',
    2: '温馨提醒'
  }
  return map[type] || '未知'
}

const getTypeTagType = (type: number | string) => {
  const map: Record<string, string> = {
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
  font-size: 28px;
  font-weight: bold;
  color: #222;
  margin: 0;
}

.detail-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  padding: 32px;
  margin-bottom: 24px;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #eee;
}

.detail-title {
  font-size: 20px;
  font-weight: 600;
  color: #333;
}

.detail-content {
  margin-bottom: 24px;
}

.content-text {
  font-size: 16px;
  line-height: 1.8;
  color: #333;
  margin-bottom: 16px;
}

.related-order {
  font-size: 14px;
  color: #666;
}

.detail-footer {
  padding-top: 16px;
  border-top: 1px solid #eee;
}

.create-time {
  font-size: 14px;
  color: #999;
}

.action-buttons {
  display: flex;
  gap: 12px;
}
</style>
