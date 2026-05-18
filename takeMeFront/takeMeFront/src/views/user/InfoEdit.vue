<template>
  <div class="info-edit-container">
    <div class="page-title">修改老人信息</div>

    <el-form
      ref="formRef"
      :model="form"
      label-width="100px"
      class="info-form"
    >
      <el-form-item label="姓名" prop="username">
        <el-input v-model="form.username" placeholder="请输入姓名" class="input-large" />
      </el-form-item>

      <el-form-item label="账号" prop="account">
        <el-input v-model="form.account" disabled class="input-large" />
      </el-form-item>

      <el-form-item label="密码" prop="password">
        <el-input
          v-model="form.password"
          type="password"
          placeholder="请输入密码"
          show-password
          class="input-large"
        />
      </el-form-item>

      <el-form-item label="手机号" prop="phone">
        <el-input v-model="form.phone" placeholder="请输入手机号" class="input-large" />
      </el-form-item>

      <el-form-item label="年龄" prop="age">
        <el-input-number v-model="form.age" :min="0" :max="150" class="input-large" />
      </el-form-item>

      <el-form-item label="性别" prop="gender">
        <el-radio-group v-model="form.gender">
          <el-radio :label="0">男</el-radio>
          <el-radio :label="1">女</el-radio>
        </el-radio-group>
      </el-form-item>

      <el-form-item label="家庭住址" prop="address">
        <el-input v-model="form.address" type="textarea" :rows="3" class="input-large" />
      </el-form-item>

      <el-form-item label="行走范围" prop="walkingRange">
        <el-input v-model="form.walkingRange" placeholder="例如：500米" class="input-large" />
      </el-form-item>

      <el-form-item label="头像">
        <el-upload
          class="avatar-upload"
          action="/api/user/uploadAvatar"
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
          返回首页
        </el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useUserStore } from '@/store/user'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

const userStore = useUserStore()
const router = useRouter()
const formRef = ref(null)
const form = ref({
  id: '',
  username: '',
  account: '',
  password: '',
  phone: '',
  age: null,
  gender: 0,
  address: '',
  walkingRange: '',
  avatar: ''
})

onMounted(() => {
  form.value = {
    id: userStore.userId || '',
    username: userStore.userName || '老人',
    account: userStore.account || '',
    password: '',
    phone: userStore.phone || '',
    age: userStore.age || null,
    gender: userStore.gender || 0,
    address: userStore.address || '',
    walkingRange: userStore.walkingRange || '',
    avatar: userStore.avatar || ''
  }
})

const handleAvatarSuccess = (res) => {
  form.value.avatar = res.url
  ElMessage.success('头像上传成功')
}

const submitForm = async () => {
  try {
    await userStore.updateUserInfo(form.value)
    ElMessage.success('修改成功')
    // 👉 重点：改为正确的首页路径 /user/index
    await router.push('/user/index')
  } catch (e) {
    ElMessage.error('修改失败，请重试')
  }
}

const back = () => {
  // 👉 重点：改为正确的首页路径 /user/index
  router.push('/user/index').catch(err => {
    console.error('返回失败', err)
    ElMessage.error('返回失败')
  })
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
:deep(.el-form-item__label) {
  font-size: 18px;
  font-weight: 500;
}
:deep(.el-radio__label) {
  font-size: 18px;
}
</style>
