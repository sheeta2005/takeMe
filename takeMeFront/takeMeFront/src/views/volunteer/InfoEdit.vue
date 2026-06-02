<template>
  <div class="info-edit-container">
    <div class="page-title">修改信息</div>

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
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
          <el-radio :value="0">男</el-radio>
          <el-radio :value="1">女</el-radio>
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
            v-for="(day, index) in weekDays"
            :key="index"
            class="day-btn"
            :class="{ active: form.serviceDays.includes(index) }"
            @click="toggleDay(index)"
          >
            {{ day }}
          </div>
        </div>
      </el-form-item>

      <el-form-item label="可服务业务" prop="serviceType">
        <el-select v-model="form.serviceType" placeholder="请选择服务业务" class="input-large">
          <el-option label="代购服务" :value="0" />
          <el-option label="助洁服务" :value="1" />
          <el-option label="助餐服务" :value="2" />
          <el-option label="助医服务" :value="3" />
          <el-option label="陪伴服务" :value="4" />
        </el-select>
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
          :show-file-list="false"
          :before-upload="handleBeforeUpload"
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
import { useVolunteerStore } from '@/stores/volunteer'

const router = useRouter()
const volunteerStore = useVolunteerStore()
const formRef = ref()

// 一周七天
const weekDays = ['周天', '周一', '周二', '周三', '周四', '周五', '周六']

const form = ref({
  realName: '',
  username: '',
  phone: '',
  password: '',
  avatar: '',
  serviceDays: [] as number[], // 多选数组，存储0-6的数字
  serviceType: 0,
  gender: 0,
  age: 20,
  address: '',
  emergencyName: '',
  emergencyPhone: ''
})

// 表单验证规则
const rules = {
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  age: [{ required: true, message: '请输入年龄', trigger: 'blur' }],
  address: [{ required: true, message: '请填写居住地址', trigger: 'blur' }],
  emergencyName: [{ required: true, message: '请输入紧急联系人姓名', trigger: 'blur' }],
  emergencyPhone: [
    { required: true, message: '请输入紧急联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '电话格式不正确', trigger: 'blur' }
  ]
}

// 切换选中日期（最少1个，最多3个）
const toggleDay = (dayIndex: number) => {
  const index = form.value.serviceDays.indexOf(dayIndex)
  if (index === -1) {
    if (form.value.serviceDays.length >= 3) {
      ElMessage.warning('最多只能选择3天可服务时间')
      return
    }
    form.value.serviceDays.push(dayIndex)
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
  await volunteerStore.fetchVolunteerInfo()

  const serviceDaysStr = volunteerStore.serviceDays || ''
  const serviceDaysArr = serviceDaysStr ? serviceDaysStr.split(',').map(d => parseInt(d.trim())) : [0]

  form.value = {
    realName: volunteerStore.realName || '',
    username: volunteerStore.username || '',
    phone: volunteerStore.phone || '',
    password: '',
    avatar: volunteerStore.avatar || '',
    serviceDays: serviceDaysArr,
    serviceType: volunteerStore.serviceType || 0,
    gender: volunteerStore.gender || 0,
    age: volunteerStore.age || 20,
    address: volunteerStore.address || '',
    emergencyName: volunteerStore.emergencyName || '',
    emergencyPhone: volunteerStore.emergencyPhone || ''
  }
})

// 头像上传前校验
const handleBeforeUpload = async (file: File) => {
  const isImage = file.type.startsWith('image/')
  const isLt2M = file.size / 1024 / 1024 < 2

  if (!isImage) {
    ElMessage.error('只能上传图片文件!')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过 2MB!')
    return false
  }

  const url = await volunteerStore.uploadAvatar(file)
  if (url) {
    form.value.avatar = url
  }
  return false // 阻止默认上传行为
}

// 提交
const submitForm = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid: boolean) => {
    if (!valid) return

    if (form.value.serviceDays.length === 0) {
      ElMessage.warning('请至少选择一天可服务时间')
      return
    }

    if (form.value.serviceDays.length > 3) {
      ElMessage.warning('最多只能选择3天可服务时间')
      return
    }

    try {
      const submitData = {
        realName: form.value.realName,
        phone: form.value.phone,
        avatar: form.value.avatar,
        gender: form.value.gender,
        age: form.value.age,
        address: form.value.address,
        serviceDays: form.value.serviceDays.sort((a, b) => a - b).join(','),
        serviceType: form.value.serviceType,
        emergencyName: form.value.emergencyName,
        emergencyPhone: form.value.emergencyPhone
      }

      if (form.value.password) {
        Object.assign(submitData, { password: form.value.password })
      }

      await volunteerStore.updateVolunteerInfo(submitData)
      router.push('/volunteer/info')
    } catch {
      ElMessage.error('修改失败，请重试')
    }
  })
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
  width: 100%;
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
  cursor: pointer;
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
  border-color: #00b899;
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
