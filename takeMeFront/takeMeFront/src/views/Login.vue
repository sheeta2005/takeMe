<template>
  <div class="login-container">
    <el-card shadow="hover" class="login-card">
      <h2 class="login-title">takeMe 服务平台</h2>

      <el-form ref="loginFormRef" :model="loginForm" label-width="80px">
        <!-- 账号 -->
        <el-form-item label="账号" prop="username">
          <el-input v-model="loginForm.username" placeholder="请输入账号" />
        </el-form-item>

        <!-- 密码 -->
        <el-form-item label="密码" prop="password">
          <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" />
        </el-form-item>

        <!-- ✅ 新增：选择登录身份 -->
        <el-form-item label="登录身份">
          <el-select v-model="loginForm.role" placeholder="请选择登录身份">
            <el-option label="管理员" value="admin" />
            <el-option label="志愿者" value="volunteer" />
            <el-option label="老人用户" value="user" />
          </el-select>
        </el-form-item>

        <!-- 登录按钮 -->
        <el-form-item>
          <el-button type="primary" @click="handleLogin" class="login-btn">登录</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()

const loginFormRef = ref()
const loginForm = ref({
  username: 'test',
  password: '123456',
  role: 'admin' // 默认管理员
})

const handleLogin = () => {
  const { role } = loginForm.value

  // 保存身份（可选）
  userStore.setToken('fake-token', role, loginForm.value.username)

  ElMessage.success('登录成功！即将跳转至 ' + (
    role === 'admin' ? '管理员端' :
    role === 'volunteer' ? '志愿者端' : '老人用户端'
  ))

  // ✅ 根据选择的角色跳转对应页面
  router.push(`/${role}`)
}
</script>

<style scoped>
.login-container {
  width: 100vw;
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: #f5f5f5;
}
.login-card {
  width: 380px;
  padding: 30px;
}
.login-title {
  text-align: center;
  margin-bottom: 30px;
}
.login-btn {
  width: 100%;
}
</style>
