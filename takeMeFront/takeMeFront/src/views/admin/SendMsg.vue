<template>
  <div class="page-container">
    <div class="page-header">
      <div style="display: flex; justify-content: space-between; align-items: center;">
        <div>
          <h2 class="page-title">发送消息</h2>
          <p class="page-subtitle">向用户或志愿者发送系统通知</p>
        </div>
      </div>
    </div>

    <el-card class="form-card" shadow="hover">
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="110px"
        style="max-width: 800px; margin: 0 auto"
      >
        <!-- 接收方类型 -->
        <el-form-item label="接收方类型" prop="receiverType">
          <el-radio-group v-model="form.receiverType" @change="handleReceiverChange">
            <el-radio value="all_volunteer">所有志愿者</el-radio>
            <el-radio value="all_elder">所有老人</el-radio>
            <el-radio value="spec_volunteer">指定志愿者</el-radio>
            <el-radio value="spec_elder">指定老人</el-radio>
          </el-radio-group>
        </el-form-item>

        <!-- 指定志愿者 -->
        <el-form-item
          v-if="form.receiverType === 'spec_volunteer'"
          label="选择志愿者"
          prop="volunteerIds"
        >
          <el-select
            v-model="form.volunteerIds"
            multiple
            filterable
            placeholder="搜索并选择志愿者"
            style="width: 100%"
          >
            <el-option
              v-for="item in volunteerList"
              :key="item.id"
              :label="`${item.realName || item.username}（ID：${item.id}）`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>

        <!-- 指定老人 -->
        <el-form-item
          v-if="form.receiverType === 'spec_elder'"
          label="选择老人"
          prop="elderIds"
        >
          <el-select
            v-model="form.elderIds"
            multiple
            filterable
            placeholder="搜索并选择老人"
            style="width: 100%"
          >
            <el-option
              v-for="item in elderList"
              :key="item.id"
              :label="`${item.realName}（ID：${item.id}）`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>

        <!-- 消息类型 -->
        <el-form-item label="消息类型" prop="type">
          <el-select v-model="form.type" style="width: 220px">
            <el-option label="系统通知" :value="0" />
            <el-option label="任务通知" :value="1" />
            <el-option label="温馨提醒" :value="2" />
          </el-select>
        </el-form-item>

        <!-- 标题 -->
        <el-form-item label="消息标题" prop="title">
          <el-input
            v-model="form.title"
            placeholder="请输入消息标题"
            maxlength="50"
            show-word-limit
          />
        </el-form-item>

        <!-- 内容 -->
        <el-form-item label="消息内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :autosize="{ minRows: 4, maxRows: 8 }"
            placeholder="请输入消息内容"
            maxlength="300"
            show-word-limit
          />
        </el-form-item>

        <!-- 按钮 -->
        <el-form-item style="text-align: center; margin-top: 30px">
          <el-button
            type="primary"
            size="default"
            :loading="loading"
            @click="handleSend"
            style="width: 140px; height: 42px"
          >
            <el-icon><Promotion /></el-icon>
            确认发送
          </el-button>
          <el-button
            size="default"
            @click="handleReset"
            style="width: 100px; height: 42px; margin-left: 12px"
          >
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElForm } from 'element-plus'
import { Promotion, Refresh } from '@element-plus/icons-vue'
import { sendMessage, sendBatchMessage, searchVolunteer, searchUser } from '@/api/admin'

const formRef = ref<InstanceType<typeof ElForm>>()
const loading = ref(false)

const volunteerList = ref<any[]>([])
const elderList = ref<any[]>([])

const form = reactive({
  receiverType: 'all_volunteer',
  volunteerIds: [] as number[],
  elderIds: [] as number[],
  type: 0 as 0 | 1 | 2,
  title: '',
  content: ''
})

const rules = {
  receiverType: [{ required: true, message: '请选择接收方', trigger: 'change' }],
  volunteerIds: [{ required: true, message: '请选择志愿者', trigger: 'change' }],
  elderIds: [{ required: true, message: '请选择老人', trigger: 'change' }],
  type: [{ required: true, message: '请选择消息类型', trigger: 'change' }],
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }]
}

onMounted(async () => {
  await loadVolunteerList()
  await loadElderList()
})

const loadVolunteerList = async () => {
  try {
    const res = await searchVolunteer(1, 100)
    volunteerList.value = res.data.records || []
  } catch (err) {
    console.error('加载志愿者列表失败', err)
  }
}

const loadElderList = async () => {
  try {
    const res = await searchUser(1, 100)
    elderList.value = res.data.records || []
  } catch (err) {
    console.error('加载老人列表失败', err)
  }
}

const handleReceiverChange = () => {
  form.volunteerIds = []
  form.elderIds = []
}

const handleSend = async () => {
  await formRef.value?.validate()
  loading.value = true

  try {
    if (form.receiverType === 'spec_volunteer' && form.volunteerIds.length > 0) {
      // 批量发送给指定志愿者 - 构造MessageDTO数组
      const messages = form.volunteerIds.map(volunteerId => ({
        receiverId: volunteerId,
        receiverType: 1,
        type: form.type,
        title: form.title,
        content: form.content
      }))
      await sendBatchMessage(messages)
    } else if (form.receiverType === 'spec_elder' && form.elderIds.length > 0) {
      // 批量发送给指定老人 - 构造MessageDTO数组
      const messages = form.elderIds.map(elderId => ({
        receiverId: elderId,
        receiverType: 2,
        type: form.type,
        title: form.title,
        content: form.content
      }))
      await sendBatchMessage(messages)
    } else {
      // 群发消息给所有志愿者或所有老人
      let receiverType = 1
      if (form.receiverType === 'all_elder') {
        receiverType = 2
      }
      await sendMessage({
        receiverType,
        type: form.type,
        title: form.title,
        content: form.content
      })
    }

    ElMessage.success('消息发送成功！')
    handleReset()
  } catch (e: any) {
    ElMessage.error(e.message || '发送失败')
  } finally {
    loading.value = false
  }
}

const handleReset = () => {
  formRef.value?.resetFields()
  form.receiverType = 'all_volunteer'
  form.volunteerIds = []
  form.elderIds = []
  form.type = 0
}
</script>

<style scoped>
.form-card {
  border: 1px solid var(--border-light);
  padding: 40px 30px;
}

:deep(.el-form-item) {
  margin-bottom: 26px;
}

:deep(.el-radio-group) {
  display: flex;
  gap: 22px;
  flex-wrap: wrap;
}

:deep(.el-input__inner) {
  height: 40px;
}
</style>
