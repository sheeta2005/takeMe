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

        <!-- 登录/注册切换标签 -->
        <el-tabs v-model="activeTab" class="login-tabs" @tab-change="resetForm">
          <el-tab-pane label="登录" name="login">
            <el-form
              v-model="loginForm"
              label-position="left"
              label-width="70px"
              class="login-form"
              ref="loginFormRef"
            >
              <el-form-item label="账号" prop="username">
                <el-input
                  v-model="loginForm.username"
                  placeholder="请输入账号"
                  size="large"
                  prefix-icon="User"
                  clearable
                />
              </el-form-item>

              <el-form-item label="密码" prop="password">
                <el-input
                  v-model="loginForm.password"
                  type="password"
                  placeholder="请输入密码"
                  size="large"
                  prefix-icon="Lock"
                  show-password
                  clearable
                />
              </el-form-item>

              <el-form-item label="身份" prop="role">
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
                <el-checkbox v-model="rememberPassword" class="remember-checkbox">
                  记住密码
                </el-checkbox>
              </el-form-item>

              <el-form-item>
                <el-button
                  type="primary"
                  size="large"
                  class="login-btn"
                  @click="handleLogin"
                  :loading="loginLoading"
                >
                  登 录
                </el-button>
              </el-form-item>
            </el-form>
          </el-tab-pane>

          <el-tab-pane label="注册" name="register">
            <el-form
              v-model="registerForm"
              label-position="left"
              label-width="70px"
              class="login-form"
              ref="registerFormRef"
              :rules="registerRules"
            >
              <el-form-item label="账号" prop="username">
                <el-input
                  v-model="registerForm.username"
                  placeholder="请输入账号"
                  size="large"
                  prefix-icon="User"
                  clearable
                />
              </el-form-item>

              <el-form-item label="密码" prop="password">
                <el-input
                  v-model="registerForm.password"
                  type="password"
                  placeholder="请输入密码（6-20位）"
                  size="large"
                  prefix-icon="Lock"
                  show-password
                  clearable
                />
              </el-form-item>

              <el-form-item label="确认密码" prop="confirmPassword">
                <el-input
                  v-model="registerForm.confirmPassword"
                  type="password"
                  placeholder="请再次输入密码"
                  size="large"
                  prefix-icon="Lock"
                  show-password
                  clearable
                />
              </el-form-item>

              <el-form-item label="真实姓名" prop="realName">
                <el-input
                  v-model="registerForm.realName"
                  placeholder="请输入真实姓名"
                  size="large"
                  prefix-icon="User"
                  clearable
                />
              </el-form-item>

              <el-form-item label="手机号" prop="phone">
                <el-input
                  v-model="registerForm.phone"
                  placeholder="请输入手机号"
                  size="large"
                  prefix-icon="Phone"
                  clearable
                />
              </el-form-item>

              <el-form-item label="身份" prop="role">
                <el-select
                  v-model="registerForm.role"
                  placeholder="请选择注册身份"
                  size="large"
                >
                  <el-option label="志愿者" :value="1" />
                  <el-option label="普通用户" :value="2" />
                </el-select>
              </el-form-item>

              <el-form-item>
                <el-button
                  type="primary"
                  size="large"
                  class="login-btn"
                  @click="handleRegister"
                  :loading="registerLoading"
                >
                  注 册
                </el-button>
              </el-form-item>
            </el-form>
          </el-tab-pane>
        </el-tabs>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElForm } from 'element-plus'
import { useRouter } from 'vue-router'
import { adminLogin, volunteerLogin, userLogin, userRegister, volunteerRegister } from '@/api'
import { useUserStore } from '@/stores/user'
import { useVolunteerStore } from '@/stores/volunteer'

const router = useRouter()
const userStore = useUserStore()
const volunteerStore = useVolunteerStore()

// 表单引用
const loginFormRef = ref<InstanceType<typeof ElForm>>()
const registerFormRef = ref<InstanceType<typeof ElForm>>()

// 加载状态
const loginLoading = ref(false)
const registerLoading = ref(false)

// 标签页切换
const activeTab = ref('login')

// 动态背景
const bgX = ref(0)
const bgY = ref(0)
const speed = 15

// 记住密码
const rememberPassword = ref(false)

// 登录表单
const loginForm = ref<{
  username: string;
  password: string;
  role: 0 | 1 | 2 | null;
}>({
  username: '',
  password: '',
  role: null
})

// 注册表单
const registerForm = ref<{
  username: string;
  password: string;
  confirmPassword: string;
  realName: string;
  phone: string;
  role: 1 | 2 | null;
}>({
  username: '',
  password: '',
  confirmPassword: '',
  realName: '',
  phone: '',
  role: null
})

// 注册验证规则
const registerRules = {
  username: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 3, max: 20, message: '账号长度为3-20位', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为6-20位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (rule: any, value: string, callback: any) => {
        if (value !== registerForm.value.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  realName: [
    { required: true, message: '请输入真实姓名', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号格式', trigger: 'blur' }
  ],
  role: [
    { required: true, message: '请选择注册身份', trigger: 'change' }
  ]
}

// ===================== 核心修缮：纯本地清空状态，无网络请求 =====================
/** 清空普通用户/管理员状态 */
const clearUserState = () => {
  userStore.$reset()
  localStorage.removeItem('token')
  localStorage.removeItem('userId')
  localStorage.removeItem('role')
}

/** 清空志愿者状态 */
const clearVolunteerState = () => {
  volunteerStore.$reset()
  localStorage.removeItem('volunteerToken')
  localStorage.removeItem('volunteerId')
  localStorage.removeItem('volunteerUsername')
  localStorage.removeItem('volunteerRole')
}
// ============================================================================

// 鼠标移动背景效果
const handleMouseMove = (e: MouseEvent) => {
  const x = (window.innerWidth / 2 - e.clientX) / speed
  const y = (window.innerHeight / 2 - e.clientY) / speed
  bgX.value = x
  bgY.value = y
}

// 页面加载读取本地存储
onMounted(() => {
  const lastUsername = localStorage.getItem('lastUsername')
  if (lastUsername) loginForm.value.username = lastUsername

  const savedPassword = localStorage.getItem('savedPassword')
  if (savedPassword) {
    loginForm.value.password = savedPassword
    rememberPassword.value = true
  }
})

// 重置表单
const resetForm = () => {
  if (activeTab.value === 'login') {
    loginFormRef.value?.resetFields()
    const lastUsername = localStorage.getItem('lastUsername')
    if (lastUsername) loginForm.value.username = lastUsername
  } else {
    registerFormRef.value?.resetFields()
  }
}

// ===================== 核心修缮：登录逻辑（删除所有 store.logout()） =====================
const handleLogin = async () => {
  if (!loginForm.value.username || !loginForm.value.password || loginForm.value.role === null) {
    ElMessage.warning('请填写完整的登录信息')
    return
  }

  loginLoading.value = true

  try {
    let res
    const loginParams = {
      username: loginForm.value.username,
      password: loginForm.value.password,
      userType: loginForm.value.role
    }

    // 根据身份登录 + 仅清空本地状态，不调用任何接口
    switch (loginForm.value.role) {
      // 管理员
      case 0:
        res = await adminLogin(loginParams)
        clearUserState()
        clearVolunteerState()
        userStore.setUserInfo(res.data.token, String(res.data.loginId), loginForm.value.username, 0)
        break
      // 志愿者
      case 1:
        res = await volunteerLogin(loginParams)
        clearUserState()
        volunteerStore.setVolunteerInfo(res.data.token, String(res.data.loginId), loginForm.value.username)
        break
      // 普通用户
      default:
        res = await userLogin(loginParams)
        clearVolunteerState()
        userStore.setUserInfo(res.data.token, String(res.data.loginId), loginForm.value.username, 2)
        break
    }

    // 记住密码逻辑
    localStorage.setItem('lastUsername', loginForm.value.username)
    rememberPassword.value
      ? localStorage.setItem('savedPassword', loginForm.value.password)
      : localStorage.removeItem('savedPassword')

    ElMessage.success('登录成功')

    // 路由跳转
    const pathMap = { 0: '/admin', 1: '/volunteer', 2: '/user' }
    await router.push(pathMap[loginForm.value.role!])

  } catch (err) {
    ElMessage.error('登录失败，请检查账号密码')
    console.error('登录错误：', err)
  } finally {
    loginLoading.value = false
  }
}
// ============================================================================

// 注册逻辑
const handleRegister = async () => {
  if (!await registerFormRef.value?.validate()) return

  registerLoading.value = true
  try {
    const params = {
      username: registerForm.value.username,
      password: registerForm.value.password,
      realName: registerForm.value.realName,
      phone: registerForm.value.phone
    }

    registerForm.value.role === 1
      ? await volunteerRegister(params)
      : await userRegister(params)

    ElMessage.success('注册成功，请登录')
    activeTab.value = 'login'
    loginForm.value.username = registerForm.value.username
    loginForm.value.role = registerForm.value.role

  } catch (err) {
    ElMessage.error('注册失败，账号可能已存在')
    console.error('注册错误：', err)
  } finally {
    registerLoading.value = false
  }
}
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
  margin-bottom: 20px;
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

.login-tabs {
  --el-tabs-header-height: 48px;
  margin-bottom: 20px;
}

.login-form {
  margin-top: 10px;
}

.remember-checkbox {
  font-size: 14px;
  color: #666;
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

:deep(.el-tabs__item) {
  font-size: 16px;
  font-weight: 500;
}

:deep(.el-tabs__active-bar) {
  background-color: #00b899;
}
</style>
