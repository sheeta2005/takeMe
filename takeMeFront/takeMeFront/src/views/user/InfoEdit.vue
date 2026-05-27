<template>
  <div class="info-edit-container">
    <div class="page-title">修改信息</div>

    <el-form
      ref="formRef"
      :model="form"
      label-width="120px"
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

      <!-- 常用地址（支持设为默认） -->
      <el-form-item label="常用地址" prop="addresses">
        <div class="address-list">
          <div v-for="(addr, index) in form.addresses" :key="addr.id" class="address-item">
            <el-input
              v-model="addr.address"
              :placeholder="`地址${index + 1}`"
              class="input-large"
            />
            <el-button
              :type="addr.isDefault ? 'primary' : 'default'"
              @click="setDefaultAddress(index)"
            >
              {{ addr.isDefault ? '默认地址' : '设为默认' }}
            </el-button>
            <el-button
              v-if="form.addresses.length > 1"
              type="danger"
              @click="removeAddress(index)"
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
        <el-input v-model="form.emergencyName" placeholder="紧急联系人姓名" class="input-large" />
      </el-form-item>
      <el-form-item label="紧急联系人电话" prop="emergencyPhone">
        <el-input v-model="form.emergencyPhone" placeholder="紧急联系人手机号" class="input-large" />
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
        <el-button type="primary" size="large" @click="submitForm">保存修改</el-button>
        <el-button size="large" @click="back">返回</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

const userStore = useUserStore()
const router = useRouter()
const formRef = ref(null)

const form = ref({
  username: '',
  account: '',
  password: '',
  phone: '',
  age: null,
  gender: 0,
  addresses: [] as Array<{
    id: number
    address: string
    isDefault: boolean
  }>,
  emergencyName: '',
  emergencyPhone: '',
  avatar: ''
})

// 添加地址
const addAddress = () => {
  if (form.value.addresses.length < 3) {
    form.value.addresses.push({
      id: Date.now(),
      address: '',
      isDefault: form.value.addresses.length === 0
    })
  }
}

// 删除地址
const removeAddress = (index: number) => {
  const isDelDefault = form.value.addresses[index].isDefault
  form.value.addresses.splice(index, 1)
  if (isDelDefault && form.value.addresses.length) {
    form.value.addresses[0].isDefault = true
  }
}

// 设为默认
const setDefaultAddress = (index: number) => {
  form.value.addresses.forEach((item, i) => {
    item.isDefault = i === index
  })
}

onMounted(async () => {
  try {
    await userStore.getUserInfo()
    form.value = {
      username: userStore.username,
      account: userStore.account,
      password: '',
      phone: userStore.phone,
      age: userStore.age,
      gender: userStore.gender,
      addresses: userStore.addresses?.length
        ? userStore.addresses
        : [{ id: Date.now(), address: '', isDefault: true }],
      emergencyName: userStore.emergencyName || '',
      emergencyPhone: userStore.emergencyPhone || '',
      avatar: userStore.avatar
    }
  } catch (e) {
    ElMessage.error('加载用户信息失败')
  }
})

const handleAvatarSuccess = (res: any) => {
  form.value.avatar = res.url
  ElMessage.success('头像上传成功')
}

const submitForm = async () => {
  try {
    await userStore.updateUserInfo(form.value)
    ElMessage.success('修改成功')
    router.push('/user/info')
  } catch (e) {
    ElMessage.error('修改失败')
  }
}

const back = () => {
  router.push('/user/info')
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
