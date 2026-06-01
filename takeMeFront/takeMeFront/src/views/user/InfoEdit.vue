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

      <!-- 地址管理 -->
      <el-form-item label="常用地址">
        <div class="address-list">
          <div v-for="(addr, idx) in form.addresses" :key="addr.id || idx" class="address-item">
            <el-input v-model="addr.address" placeholder="请输入地址" class="input-large" />
            <el-checkbox v-model="addr.isDefault">默认</el-checkbox>
            <el-button v-if="form.addresses.length > 1" type="danger" @click="removeAddress(idx, addr)">删除</el-button>
          </div>
          <el-button v-if="form.addresses.length < 3" type="primary" @click="addAddress">添加地址</el-button>
        </div>
      </el-form-item>

      <el-form-item label="紧急联系人" prop="emergencyName">
        <el-input v-model="form.emergencyName" placeholder="姓名" class="input-large" />
      </el-form-item>

      <el-form-item label="紧急联系人电话" prop="emergencyPhone">
        <el-input v-model="form.emergencyPhone" placeholder="电话" class="input-large" />
      </el-form-item>

      <!-- 头像上传 -->
      <el-form-item label="头像">
        <el-upload
          class="avatar-upload"
          action="/api/user/uploadAvatar"
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
        <el-button type="primary" size="large" @click="submitForm" :loading="isSubmitting">保存修改</el-button>
        <el-button size="large" @click="back">返回</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
// 导入API
import {
  getUserAddressList,
  addUserAddress,
  updateUserAddress,
  deleteUserAddress,
  setDefaultAddress
} from '@/api/user'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref<any>(null)
const isSubmitting = ref(false)

// 表单数据
const form = ref({
  realName: '',
  account: '',
  phone: '',
  age: null as number | null,
  gender: 0,
  emergencyName: '',
  emergencyPhone: '',
  avatar: '',
  addresses: [] as any[]
})

// 校验规则
const rules = {
  realName: [
    { required: true, message: '请输入姓名', trigger: 'blur' },
    { min: 2, max: 20, message: '长度2-20位', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '格式错误', trigger: 'blur' }
  ],
  emergencyPhone: [
    { pattern: /^1[3-9]\d{9}$/, message: '格式错误', trigger: 'blur' }
  ]
}

// 地址操作
const addAddress = () => {
  if (form.value.addresses.length < 3) {
    form.value.addresses.push({ id: null, address: '', isDefault: 0 })
  }
}
const removeAddress = async (index: number, addr: any) => {
  if (addr.id) await deleteUserAddress(addr.id)
  form.value.addresses.splice(index, 1)
}

// 初始化数据（Store + API）
const initForm = async () => {
  await userStore.getUserInfo()
  // 从Store赋值基础信息
  form.value = {
    realName: userStore.realName || '',
    account: userStore.account || '',
    phone: userStore.phone || '',
    age: userStore.age,
    gender: userStore.gender ?? 0,
    emergencyName: userStore.emergencyName || '',
    emergencyPhone: userStore.emergencyPhone || '',
    avatar: userStore.avatar || '',
    addresses: []
  }
  // 从API获取地址
  try {
    const res = await getUserAddressList()
    if (res.code === 200) {
      // ✅ 修复：强制转为数字类型，避免布尔值
      form.value.addresses = res.data.map(item => ({
        ...item,
        isDefault: Number(item.isDefault)
      }))
    } else {
      form.value.addresses = [{ id: null, address: '', isDefault: 0 }]
    }
  } catch {
    form.value.addresses = [{ id: null, address: '', isDefault: 0 }]
  }
}

// 头像上传
const handleAvatarSuccess = (res: any) => {
  if (res.code === 200) {
    form.value.avatar = res.data
    userStore.avatar = res.data
    ElMessage.success('上传成功')
  } else ElMessage.error('上传失败')
}
const handleAvatarError = () => ElMessage.error('网络异常')

// 提交保存（Store + API）
const submitForm = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  isSubmitting.value = true
  try {
    // 1. 保存基础信息（Store封装）
    const { addresses, ...userData } = form.value
    await userStore.updateUserInfo(userData)

    // 2. 保存地址（API）✅ 核心修复：布尔值转 0/1
    for (const addr of form.value.addresses) {
      if (!addr.address) continue
      // 强制转换类型
      const isDefaultNum = addr.isDefault ? 1 : 0

      if (addr.id) {
        await updateUserAddress({
          id: addr.id,
          address: addr.address,
          isDefault: isDefaultNum
        })
      } else {
        const res = await addUserAddress({
          address: addr.address,
          isDefault: isDefaultNum
        })
        addr.id = res.data.id
      }
      if (isDefaultNum === 1) {
        await setDefaultAddress(addr.id)
      }
    }

    ElMessage.success('保存成功')
    router.push('/user/info')
  } catch (err) {
    ElMessage.error('保存失败')
  } finally {
    isSubmitting.value = false
  }
}

// 返回
const back = () => router.push('/user/info')

onMounted(() => initForm())
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
