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
        <div class="days-tip">
          <el-icon><InfoFilled /></el-icon>
          <span>修改可服务时间需要管理员审批，审批通过后方可生效</span>
        </div>
      </el-form-item>

      <el-form-item label="紧急联系人" prop="emergencyName">
        <el-input v-model="form.emergencyName" placeholder="请输入紧急联系人姓名" class="input-large" />
      </el-form-item>

      <el-form-item label="紧急联系电话" prop="emergencyPhone">
        <el-input v-model="form.emergencyPhone" placeholder="请输入紧急联系电话" class="input-large" />
      </el-form-item>

      <el-form-item label="头像">
        <div class="avatar-upload-wrapper">
          <Avatar :src="form.avatar" :size="120" />
          <el-upload
            class="avatar-uploader"
            :show-file-list="false"
            :before-upload="handleBeforeUpload"
            :http-request="handleAvatarUpload"
          >
            <el-button type="primary" size="small">更换头像</el-button>
          </el-upload>
          <div class="upload-tip">支持 jpg/png/webp，最大 5MB</div>
        </div>
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
import { Plus, InfoFilled } from '@element-plus/icons-vue'
import { useVolunteerStore } from '@/stores/volunteer'
import { uploadAvatar } from '@/api/volunteer'
import { compressImage } from '@/utils/imageCompress'
import Avatar from '@/components/Avatar.vue'

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
    gender: volunteerStore.gender || 0,
    age: volunteerStore.age || 20,
    address: volunteerStore.address || '',
    emergencyName: volunteerStore.emergencyName || '',
    emergencyPhone: volunteerStore.emergencyPhone || ''
  }
})

// 头像上传前校验
const handleBeforeUpload = (file: File) => {
  const isValidType = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp'].includes(file.type)
  const isValidSize = file.size / 1024 / 1024 < 5

  if (!isValidType) {
    ElMessage.error('只支持 JPG/PNG/WEBP 格式')
    return false
  }
  if (!isValidSize) {
    ElMessage.error('图片大小不能超过 5MB')
    return false
  }
  return true
}

const handleAvatarUpload = async (options: any) => {
  const { file } = options

  try {
    ElMessage.info('正在压缩图片...')
    const compressedFile = await compressImage(file, 800, 0.8)

    ElMessage.info('正在上传...')
    const res = await uploadAvatar(compressedFile)

    if (res.code === 200) {
      form.value.avatar = res.data
      volunteerStore.avatar = res.data
      ElMessage.success('上传成功')
    } else {
      ElMessage.error(res.msg || '上传失败')
    }
  } catch (err: any) {
    console.error('上传失败:', err)
    ElMessage.error(err.message || '上传失败')
  }
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
      const newServiceDays = form.value.serviceDays.sort((a, b) => a - b).join(',')

      const submitData = {
        realName: form.value.realName,
        phone: form.value.phone,
        avatar: form.value.avatar,
        gender: form.value.gender,
        age: form.value.age,
        address: form.value.address,
        serviceDays: newServiceDays,
        emergencyName: form.value.emergencyName,
        emergencyPhone: form.value.emergencyPhone
      }

      if (form.value.password) {
        Object.assign(submitData, { password: form.value.password })
      }

      const oldServiceDays = volunteerStore.serviceDays || ''
      const serviceDaysChanged = oldServiceDays !== newServiceDays

      await volunteerStore.updateVolunteerInfo(submitData)

      if (serviceDaysChanged) {
        ElMessage.success('信息已更新，工作日修改申请已提交，等待管理员审批')
      } else {
        ElMessage.success('修改成功')
      }
      router.push('/volunteer/info')
    } catch (error: any) {
      ElMessage.error(error.message || '修改失败，请重试')
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

.avatar-upload-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}
.upload-tip {
  font-size: 12px;
  color: #999;
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

.days-tip {
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 6px;
  color: #e6a23c;
  font-size: 13px;
}

.days-tip .el-icon {
  font-size: 16px;
}
</style>
