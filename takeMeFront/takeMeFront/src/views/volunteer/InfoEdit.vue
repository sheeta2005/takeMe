<template>
  <div class="info-edit-container">
    <div class="page-title">修改信息</div>

    <el-form
      ref="formRef"
      :model="form"
      label-width="130px"
      class="info-form"
    >
      <el-form-item label="姓名" prop="realName">
        <el-input v-model="form.realName" placeholder="请输入姓名" class="input-large" />
      </el-form-item>

      <el-form-item label="账号" prop="username">
        <el-input v-model="form.username" disabled class="input-large" />
      </el-form-item>

      <el-form-item label="手机号" prop="phone">
        <el-input v-model="form.phone" placeholder="请输入手机号" class="input-large" />
      </el-form-item>

      <el-form-item label="修改密码" prop="password">
        <el-input
          v-model="form.password"
          type="password"
          placeholder="留空则不修改"
          show-password
          class="input-large"
        />
      </el-form-item>

      <el-form-item label="性别" prop="gender">
        <el-radio-group v-model="form.gender">
          <el-radio value="男">男</el-radio>
          <el-radio value="女">女</el-radio>
        </el-radio-group>
      </el-form-item>

      <el-form-item label="年龄" prop="age">
        <el-input-number v-model="form.age" :min="16" :max="80" class="input-large" />
      </el-form-item>

      <el-form-item label="居住地址" prop="address">
        <el-input v-model="form.address" placeholder="请填写居住地址" class="input-large" />
      </el-form-item>

      <!-- 可服务时间：7天多选按钮 -->
      <el-form-item label="可服务时间" prop="serviceDays">
        <div class="days-selector">
          <div
            v-for="day in weekDays"
            :key="day"
            class="day-btn"
            :class="{ active: form.serviceDays.includes(day) }"
            @click="toggleDay(day)"
          >
            {{ day }}
          </div>
        </div>
      </el-form-item>

      <el-form-item label="可服务业务" prop="serviceType">
        <el-radio-group v-model="form.serviceType">
          <el-radio value="代购服务" />
          <el-radio value="助洁服务" />
          <el-radio value="送餐服务" />
          <el-radio value="陪医服务" />
        </el-radio-group>
      </el-form-item>

      <el-form-item label="服务状态">
        <el-input v-model="form.status" disabled class="input-large" />
      </el-form-item>

      <el-form-item label="紧急联系人" prop="emergencyName">
        <el-input v-model="form.emergencyName" placeholder="请输入紧急联系人姓名" class="input-large" />
      </el-form-item>

      <el-form-item label="紧急联系电话" prop="emergencyPhone">
        <el-input v-model="form.emergencyPhone" placeholder="请输入紧急联系电话" class="input-large" />
      </el-form-item>

      <el-form-item label="头像">
        <el-upload
          class="avatar-upload"
          action="/api/volunteer/uploadAvatar"
          :show-file-list="false"
          :on-success="handleAvatarSuccess"
        >
          <img v-if="form.avatar" :src="form.avatar" class="avatar" />
          <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
        </el-upload>
      </el-form-item>

      <el-form-item class="btn-group">
        <el-button type="primary" size="large" @click="submitForm">
          保存修改
        </el-button>
        <el-button size="large" @click="back">
          返回
        </el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getVolunteerInfo, updateVolunteerInfo } from '@/api/volunteer'

const router = useRouter()
const formRef = ref(null)

// 一周七天
const weekDays = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']

const form = ref({
  realName: '',
  username: '',
  phone: '',
  password: '',
  avatar: '',
  serviceDays: [] as string[], // 多选数组
  serviceType: '',
  status: '正常服务中',
  gender: '',
  age: 0,
  address: '',
  emergencyName: '',
  emergencyPhone: ''
})

// 切换选中日期（最少保留1个）
const toggleDay = (day: string) => {
  const index = form.value.serviceDays.indexOf(day)
  if (index === -1) {
    form.value.serviceDays.push(day)
  } else {
    if (form.value.serviceDays.length > 1) {
      form.value.serviceDays.splice(index, 1)
    } else {
      ElMessage.warning('至少选择一天可服务时间')
    }
  }
}

// 加载用户信息
onMounted(async () => {
  try {
    const res = await getVolunteerInfo()
    if (res.code === 200) {
      form.value = {
        ...form.value,
        ...res.data,
        serviceDays: res.data.serviceDays ? res.data.serviceDays.split(',') : [],
        password: ''
      }
    }
  } catch {
    ElMessage.error('加载信息失败')
  }
})

// 头像上传
const handleAvatarSuccess = (res: any) => {
  form.value.avatar = res.url
  ElMessage.success('头像上传成功')
}

// 提交
const submitForm = async () => {
  if (form.value.serviceDays.length === 0) {
    ElMessage.warning('请至少选择一天可服务时间')
    return
  }

  try {
    const submitData = {
      ...form.value,
      serviceDays: form.value.serviceDays.join(',')
    }
    await updateVolunteerInfo(submitData)
    ElMessage.success('修改成功')
    router.push('/volunteer/info')
  } catch {
    ElMessage.error('修改失败，请重试')
  }
}

// 返回
const back = () => {
  router.push('/volunteer/info')
}
</script>

<style scoped>
.info-edit-container {
  max-width: 800px;
  margin: 40px auto;
  padding: 0 20px;
}

.page-title {
  font-size: 32px;
  font-weight: bold;
  text-align: center;
  margin-bottom: 40px;
  color: #333;
}

.info-form {
  background: #fff;
  border-radius: 16px;
  padding: 40px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.input-large {
  font-size: 18px !important;
  padding: 10px 16px !important;
}

.btn-group {
  text-align: center;
  margin-top: 30px;
}

.avatar {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  object-fit: cover;
}

.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 120px;
  height: 120px;
  text-align: center;
  line-height: 120px;
  border: 1px dashed #d9d9d9;
  border-radius: 50%;
  cursor: pointer;
  transition: border-color 0.2s;
}

.avatar-uploader-icon:hover {
  border-color: #409eff;
}

:deep(.el-form-item__label) {
  font-size: 18px;
  font-weight: 500;
}

/* 星期按钮样式 */
.days-selector {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}
.day-btn {
  padding: 8px 16px;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 16px;
}
.day-btn:hover {
  border-color: #00b899;
  color: #00b899;
}
.day-btn.active {
  background-color: #00b899;
  color: #fff;
  border-color: #00b899;
}
</style>
