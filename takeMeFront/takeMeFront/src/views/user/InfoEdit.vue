<template>
  <div class="info-edit-container">
    <div class="page-title">修改信息</div>

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="120px"
      class="info-form"
    >
      <el-form-item label="姓名" prop="realName">
        <el-input v-model="form.realName" placeholder="请输入姓名" class="input-large" />
      </el-form-item>

      <el-form-item label="账号">
        <el-input v-model="form.account" disabled class="input-large" />
      </el-form-item>

      <el-form-item label="手机号" prop="phone">
        <el-input v-model="form.phone" placeholder="请输入手机号" class="input-large" />
      </el-form-item>

      <el-form-item label="年龄" prop="age">
        <el-input-number v-model="form.age" :min="0" :max="150" class="input-large" />
      </el-form-item>

      <el-form-item label="性别">
        <el-radio-group v-model="form.gender">
          <el-radio :value="0">男</el-radio>
          <el-radio :value="1">女</el-radio>
        </el-radio-group>
      </el-form-item>

      <el-form-item label="常用地址">
        <div class="address-list">
          <div v-for="(addr, idx) in form.addresses" :key="idx" class="address-item">
            <el-input v-model="form.addresses[idx]" placeholder="请输入地址" class="input-large" />
            <el-button
              v-if="form.addresses.length > 1"
              type="danger"
              @click="removeAddress(idx)"
            >删除</el-button>
          </div>
          <el-button
            v-if="form.addresses.length < 3"
            type="primary"
            @click="addAddress"
          >添加地址</el-button>
        </div>
      </el-form-item>

      <el-form-item label="紧急联系人" prop="emergencyName">
        <el-input v-model="form.emergencyName" placeholder="姓名" class="input-large" />
      </el-form-item>

      <el-form-item label="紧急联系人电话" prop="emergencyPhone">
        <el-input v-model="form.emergencyPhone" placeholder="电话" class="input-large" />
      </el-form-item>

      <el-form-item label="头像">
        <el-upload
          class="avatar-upload"
          action="http://localhost:8080/api/user/uploadAvatar"
          :headers="{ Authorization: userStore.token }"
          :show-file-list="false"
          :on-success="handleAvatarSuccess"
          :on-error="handleAvatarError"
        >
          <img v-if="form.avatar" :src="form.avatar" class="avatar" />
          <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
        </el-upload>
      </el-form-item>

      <el-form-item class="btn-group">
        <el-button type="primary" size="large" @click="submitForm" :loading="isSubmitting">
          保存修改
        </el-button>
        <el-button size="large" @click="back">返回</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

const userStore = useUserStore()
const router = useRouter()
const formRef = ref()
const isSubmitting = ref(false)

const form = ref({
  realName: '',
  account: '',
  phone: '',
  age: null,
  gender: 0,
  addresses: [] as string[],
  emergencyName: '',
  emergencyPhone: '',
  avatar: ''
})

// ✅ 新增：表单验证规则
const rules = {
  realName: [
    { required: true, message: '请输入姓名', trigger: 'blur' },
    { min: 2, max: 20, message: '姓名长度为2-20位', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号格式', trigger: 'blur' }
  ],
  emergencyPhone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号格式', trigger: 'blur' }
  ]
}

// ✅ 修复：地址添加/删除功能
const addAddress = () => {
  if (form.value.addresses.length < 3) {
    form.value.addresses.push('')
  }
}

const removeAddress = (index: number) => {
  form.value.addresses.splice(index, 1)
}

onMounted(async () => {
  // ✅ 修复：直接访问修改页时，先加载用户信息
  await userStore.getUserInfo()

  form.value = {
    realName: userStore.realName || '',
    account: userStore.account || '',
    phone: userStore.phone || '',
    age: userStore.age,
    gender: userStore.gender ?? 0,
    // ✅ 修复：默认至少有一个地址输入框
    addresses: userStore.addresses?.length ? [...userStore.addresses] : [''],
    emergencyName: userStore.emergencyName || '',
    emergencyPhone: userStore.emergencyPhone || '',
    avatar: userStore.avatar || ''
  }
})

const handleAvatarSuccess = (res: any) => {
  if (res.code === 200) {
    form.value.avatar = res.data.url
    // 同步更新store，返回Info页时直接显示新头像
    userStore.avatar = res.data.url
    ElMessage.success('头像上传成功')
  } else {
    ElMessage.error(res.msg || '头像上传失败')
  }
}

// ✅ 新增：头像上传失败处理
const handleAvatarError = () => {
  ElMessage.error('头像上传失败，请检查网络')
}

const submitForm = async () => {
  // 先验证表单
  const valid = await formRef.value?.validate()
  if (!valid) return

  isSubmitting.value = true

  try {
    const ok = await userStore.updateUserInfo(form.value)
    if (ok) {
      ElMessage.success('保存成功')
      router.push('/user/info')
    }
  } finally {
    isSubmitting.value = false
  }
}

const back = () => {
  // ✅ 新增：表单有修改时提示确认
  if (formRef.value?.isDirty) {
    ElMessageBox.confirm(
      '您有未保存的修改，确定要返回吗？',
      '提示',
      { type: 'warning' }
    ).then(() => {
      router.push('/user/info')
    }).catch(() => {})
  } else {
    router.push('/user/info')
  }
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
}
.address-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.address-item {
  display: flex;
  gap: 10px;
  align-items: center;
}
:deep(.el-form-item__label) {
  font-size: 18px;
  font-weight: 500;
}
:deep(.el-radio__label) {
  font-size: 18px;
}
</style>
