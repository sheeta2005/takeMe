<template>
  <div class="page-container">
    <h2 class="page-title">发送消息</h2>

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
              :label="`${item.username}（ID：${item.id}）`"
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
            确认发送
          </el-button>
          <el-button
            size="default"
            @click="handleReset"
            style="width: 100px; height: 42px; margin-left: 12px"
          >
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage, ElForm } from 'element-plus'
// import { sendMessage } from '@/api/admin'

const formRef = ref<InstanceType<typeof ElForm>>()
const loading = ref(false)

// 模拟志愿者
const volunteerList = ref([
  { id: 3001, username: '张志愿者' },
  { id: 3002, username: '王志愿者' },
  { id: 3003, username: '李志愿者' }
])

// 模拟老人
const elderList = ref([
  { id: 2001, realName: '王奶奶' },
  { id: 2002, realName: '李爷爷' },
  { id: 2003, realName: '张婆婆' }
])

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

// 切换接收方时清空选择
const handleReceiverChange = () => {
  form.volunteerIds = []
  form.elderIds = []
}

// 发送
const handleSend = async () => {
  await formRef.value?.validate()
  loading.value = true

  try {
    // ===== 后端接口已注释，对接时打开 =====
    // await sendMessage({
    //   userId: xxx,
    //   title: form.title,
    //   content: form.content
    // })

    await new Promise(r => setTimeout(r, 600))
    ElMessage.success('消息发送成功！')
    handleReset()
  } catch (e) {
    ElMessage.error('发送失败')
  } finally {
    loading.value = false
  }
}

// 重置
const handleReset = () => {
  formRef.value?.resetFields()
  form.receiverType = 'all_volunteer'
  form.volunteerIds = []
  form.elderIds = []
  form.type = 0
}
</script>

<style scoped>
.page-container {
  width: 100%;
  padding: 10px 0;
}

.page-title {
  font-size: 28px;
  font-weight: bold;
  color: #222;
  margin: 0 0 24px 0;
}

.form-card {
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
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
