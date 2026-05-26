<template>
  <div class="login-wrapper" @mousemove="handleMouseMove">
    <!-- 动态背景层 -->
    <div class="bg-img" :style="{ transform: `translate(${bgX}px, ${bgY}px)` }"></div>

    <div class="login-box">
      <el-card shadow="hover" class="login-card">
        <div class="login-header">
          <h1>takeMe 服务平台</h1>
          <p>请登录以继续使用系统</p>
        </div>

        <el-form
          v-model="loginForm"
          label-position="left"
          label-width="70px"
          class="login-form"
        >
          <el-form-item label="账号">
            <el-input
              v-model="loginForm.username"
              placeholder="请输入账号"
              size="large"
              prefix-icon="User"
            />
          </el-form-item>

          <el-form-item label="密码">
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              size="large"
              prefix-icon="Lock"
              show-password
            />
          </el-form-item>

          <el-form-item label="身份">
            <el-select
              v-model="loginForm.role"
              placeholder="请选择登录身份"
              size="large"
            >
              <el-option label="管理员" :value="0" />
              <el-option label="志愿者" :value="1" />
              <el-option label="普通用户" :value="2" />
            </el-select>
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              size="large"
              class="login-btn"
              @click="handleLogin"
            >
              登 录
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { login } from '@/api/user'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const loginForm = ref<{
  username: string;
  password: string;
  role: 0 | 1 | 2;
}>({
  username: '',
  password: '',
  role: 2
})

const bgX = ref(0)
const bgY = ref(0)
const speed = 15

const handleMouseMove = (e: MouseEvent) => {
  const x = (window.innerWidth / 2 - e.clientX) / speed
  const y = (window.innerHeight / 2 - e.clientY) / speed
  bgX.value = x
  bgY.value = y
}

// ✅ 修复后的模拟登录
const handleLogin = async () => {
  if (!loginForm.value.username || !loginForm.value.password) {
    ElMessage.warning('请输入账号和密码')
    return
  }

  try {
    ElMessage.success('登录成功（模拟模式）')

    // ✅ 这里参数顺序完全正确
    userStore.setUserInfo(
      'this-is-a-fake-token-123456',
      '1',
      loginForm.value.username,
      loginForm.value.role
    )

    if (loginForm.value.role === 0) router.push('/admin')
    else if (loginForm.value.role === 1) router.push('/volunteer')
    else router.push('/user')

  } catch (err) {
    ElMessage.error('登录失败')
  }
}

// 正式登录
// const handleLogin = async () => {
//   if (!loginForm.value.username || !loginForm.value.password) {
//     ElMessage.warning('请输入账号和密码')
//     return
//   }

//   try {
//     const res = await login(loginForm.value)
//     const token = res.data.token
//     userStore.setUserInfo(token, loginForm.value.username, loginForm.value.role)

//     ElMessage.success('登录成功')

//     if (loginForm.value.role === 0) {
//       router.push('/admin')
//     } else if (loginForm.value.role === 1) {
//       router.push('/volunteer')
//     } else {
//       router.push('/user')
//     }
//   } catch (err) {
//     ElMessage.error('登录失败，请检查账号密码')
//     console.error(err)
//   }
// }
</script>

<style scoped>
.login-wrapper {
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
}

.bg-img {
  position: absolute;
  top: -50px;
  left: -50px;
  right: -50px;
  bottom: -50px;
  z-index: 1;
  background: url('@/assets/oldman.jpg') no-repeat center center;
  background-size: cover;
  transition: transform 0.1s ease-out;
  filter: brightness(0.65);
}

.login-box {
  width: 460px;
  z-index: 9;
  position: relative;
}

.login-card {
  border-radius: 16px;
  padding: 40px 30px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.15);
  border: none;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(6px);
}

.login-header {
  text-align: center;
  margin-bottom: 35px;
}

.login-header h1 {
  font-size: 26px;
  font-weight: 600;
  color: #333;
  margin-bottom: 8px;
}

.login-header p {
  font-size: 14px;
  color: #999;
}

.login-form {
  margin-top: 10px;
}

:deep(.login-btn) {
  width: 100%;
  height: 46px;
  font-size: 16px;
  font-weight: 500;
  background: #00b899 !important;
  border-color: #00b899 !important;
  border-radius: 8px;
}

:deep(.login-btn:hover) {
  background: #00a085 !important;
  border-color: #00a085 !important;
}

:deep(.el-input__wrapper) {
  border-radius: 8px;
  box-shadow: none;
  border: 1px solid #e5e6eb;
}

:deep(.el-input__wrapper:hover) {
  border-color: #00b899;
}

:deep(.el-input__wrapper.is-focus) {
  border-color: #00b899;
  box-shadow: 0 0 0 2px rgba(0, 184, 153, 0.15);
}

:deep(.el-select__wrapper) {
  border-radius: 8px;
}
</style>
