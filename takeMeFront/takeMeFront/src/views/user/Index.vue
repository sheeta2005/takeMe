<template>
  <div class="elder-home-container">
    <div class="greeting">您好，{{ userName }} 老人</div>

    <div class="service-grid">
      <div class="service-card" @click="goOrder('助餐')">
        <div class="card-icon">🍚</div>
        <div class="card-text">助餐服务</div>
      </div>
      <div class="service-card" @click="goOrder('助洁')">
        <div class="card-icon">🧹</div>
        <div class="card-text">助洁服务</div>
      </div>
      <div class="service-card" @click="goOrder('助医')">
        <div class="card-icon">🏥</div>
        <div class="card-text">助医服务</div>
      </div>
      <div class="service-card" @click="goOrder('代购')">
        <div class="card-icon">🛒</div>
        <div class="card-text">代购服务</div>
      </div>
    </div>

    <div class="order-section">
      <div class="section-title">我的订单</div>
      <div class="order-table-box">
        <el-table :data="orderList" border empty-text="暂无订单记录">
          <el-table-column prop="id" label="订单ID" />
          <el-table-column prop="serviceType" label="服务类型" />
          <el-table-column prop="status" label="状态">
            <template #default="scope">
              <el-tag :type="getTagType(scope.row.status)" size="large">
                {{ scope.row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="下单时间" />
        </el-table>
      </div>
    </div>

    <div class="ai-sidebar">
      <div class="ai-content">
        <div class="ai-title">AI 智能助手</div>
        <div class="voice-box">
          <div class="voice-btn" @mousedown="startVoice" @mouseup="stopVoice" @mouseleave="stopVoice">
            🎤
          </div>
          <div class="voice-tip">{{ voiceTip }}</div>
        </div>
        <div class="quick-group">
          <div class="quick-item" @click="quickSend('我想订助餐')">我想订助餐</div>
          <div class="quick-item" @click="quickSend('我想查订单')">我想查订单</div>
          <div class="quick-item" @click="quickSend('今天吃什么好')">今天吃什么好</div>
        </div>
        <div class="chat-list" ref="chatListRef">
          <div v-for="(msg, idx) in chatList" :key="idx" :class="['chat-item', msg.role]">
            {{ msg.content }}
          </div>
          <div v-if="loading" class="loading-tip">AI 思考中...</div>
        </div>
        <div class="chat-input">
          <el-input v-model="inputMsg" @keyup.enter="sendMessage" />
          <el-button type="primary" @click="sendMessage">发送</el-button>
        </div>
      </div>
    </div>

    <!-- 左下角：设置按钮 -->
    <el-button
      type="primary"
      size="large"
      class="setting-btn"
      @click="goToSetting"
    >
      设置
    </el-button>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { getOrderList } from '@/api'
import { useUserStore } from '@/store/user'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
const router = useRouter()
const userName = ref('')
const orderList = ref([])

const chatList = ref<any>([])
const inputMsg = ref('')
const loading = ref(false)
const chatListRef = ref(null)
const voiceTip = ref('长按说话')
const isRecording = ref(false)

onMounted(async () => {
  userName.value = userStore.userName || '尊敬的用户'
  const res = await getOrderList({ userId: userStore.userId })
  orderList.value = res.data || []
})

const goOrder = (type: string) => {
  router.push('/user/order/create?type=' + type)
}

// 跳转设置页
const goToSetting = () => {
  router.push('/user/setting')
}

const getTagType = (status: string) => {
  if (!status) return 'info'
  if (status.includes('待')) return 'warning'
  if (status.includes('完成')) return 'success'
  if (status.includes('进行')) return 'primary'
  return 'info'
}

const quickSend = (text: string) => {
  inputMsg.value = text
  sendMessage()
}

const sendMessage = async () => {
  if (!inputMsg.value.trim()) return
  chatList.value.push({ role: 'user', content: inputMsg.value })
  const text = inputMsg.value
  inputMsg.value = ''
  loading.value = true
  nextTick(() => {
    if (chatListRef.value) chatListRef.value.scrollTop = chatListRef.value.scrollHeight
  })
  try {
    const res = await fetch('/api/elder/ai/chat', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        model: 'qwen-max',
        input: {
          messages: [
            { role: 'system', content: '你是老年人智能服务助手，说话简单、亲切、慢语速。' },
            ...chatList.value,
            { role: 'user', content: text }
          ]
        },
        parameters: { result_format: 'message' }
      })
    })
    const data = await res.json()
    const reply = data.output?.choices?.[0]?.message?.content || '我暂时无法回答'
    chatList.value.push({ role: 'ai', content: reply })
  } catch (e) {
    ElMessage.error('AI服务异常')
  } finally {
    loading.value = false
    nextTick(() => {
      if (chatListRef.value) chatListRef.value.scrollTop = chatListRef.value.scrollHeight
    })
  }
}

const startVoice = () => {
  isRecording.value = true
  voiceTip.value = '正在录音...'
}
const stopVoice = () => {
  if (!isRecording.value) return
  isRecording.value = false
  voiceTip.value = '识别中...'
  setTimeout(() => {
    voiceTip.value = '长按说话'
    inputMsg.value = '我想订餐'
  }, 800)
}
</script>

<style scoped>
.elder-home-container {
  padding: 0;
  margin: 0;
  min-height: 100vh;
  background: #f7f8fa;
  padding-right: 320px;
  padding-bottom: 40px;
  box-sizing: border-box;
  position: relative;
}
.greeting {
  text-align: center;
  font-size: 30px;
  font-weight: bold;
  margin: 30px 0;
  color: #222;
}
.service-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  padding: 0 24px;
  margin-bottom: 36px;
}
.service-card {
  background: #fff;
  border-radius: 20px;
  padding: 36px;
  text-align: center;
  cursor: pointer;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
}
.service-card:active {
  transform: scale(0.97);
}
.card-icon {
  font-size: 56px;
  margin-bottom: 20px;
}
.card-text {
  font-size: 24px;
  font-weight: 600;
  color: #333;
}
.order-section {
  padding: 0 24px;
  margin-bottom: 60px;
}
.section-title {
  font-size: 24px;
  font-weight: bold;
  margin-bottom: 20px;
  color: #222;
}
.order-table-box {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
}
:deep(.el-table__header-wrapper th) {
  font-size: 18px;
  padding: 16px 0;
}
:deep(.el-table__body-wrapper td) {
  font-size: 18px;
  padding: 16px 0;
}
.ai-sidebar {
  position: fixed;
  top: 0;
  right: 0;
  width: 280px;
  height: 100vh;
  background: #fff;
  box-shadow: -4px 0 20px rgba(0, 0, 0, 0.12);
  z-index: 9999;
}
.ai-content {
  padding: 24px;
  height: 100vh;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
}
.ai-title {
  font-size: 24px;
  font-weight: bold;
  text-align: center;
  margin-bottom: 20px;
  color: #222;
}
.voice-box {
  text-align: center;
  margin-bottom: 20px;
}
.voice-btn {
  width: 72px;
  height: 72px;
  background: #f5f5f5;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 36px;
  cursor: pointer;
}
.voice-tip {
  margin-top: 8px;
  font-size: 18px;
  color: #666;
}
.quick-group {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 20px;
}
.quick-item {
  background: #f0f2f5;
  padding: 8px 16px;
  border-radius: 24px;
  font-size: 18px;
  cursor: pointer;
}
.chat-list {
  flex: 1;
  overflow-y: auto;
  background: #f9f9f9;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 16px;
}
.chat-item {
  max-width: 85%;
  padding: 14px 18px;
  border-radius: 16px;
  margin-bottom: 14px;
  font-size: 20px;
  line-height: 1.6;
}
.chat-item.user {
  background: #409eff;
  color: #fff;
  margin-left: auto;
}
.chat-item.ai {
  background: #fff;
  color: #333;
  border: 1px solid #eee;
}
.loading-tip {
  text-align: center;
  color: #999;
  padding: 12px;
  font-size: 18px;
}
.chat-input {
  display: flex;
  gap: 10px;
}
:deep(.el-input__inner) {
  font-size: 18px;
  padding: 14px 16px;
}
:deep(.el-button) {
  font-size: 18px;
  padding: 14px 20px;
}
/* 左下角设置按钮 */
.setting-btn {
  position: fixed;
  left: 24px;
  bottom: 24px;
  font-size: 20px;
  padding: 12px 32px;
  border-radius: 12px;
  z-index: 999;
}
</style>
