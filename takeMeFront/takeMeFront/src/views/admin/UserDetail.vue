<template>
  <div class="detail-container">
    <div class="header-row">
      <h2 class="page-title">用户详情</h2>
      <el-button @click="$router.back()">返回</el-button>
    </div>

    <div class="detail-card">
      <div class="detail-section">
        <h3 class="section-title">基础信息</h3>
        <div class="detail-grid">
          <div class="detail-item">
            <span class="label">用户ID：</span>
            <span class="value">{{ userDetail.userId }}</span>
          </div>
          <div class="detail-item">
            <span class="label">姓名：</span>
            <span class="value">{{ userDetail.realName }}</span>
          </div>
          <div class="detail-item">
            <span class="label">年龄：</span>
            <span class="value">{{ userDetail.age }}岁</span>
          </div>
          <div class="detail-item">
            <span class="label">性别：</span>
            <span class="value">{{ userDetail.gender }}</span>
          </div>
          <div class="detail-item">
            <span class="label">手机号：</span>
            <span class="value">{{ userDetail.phone }}</span>
          </div>
          <div class="detail-item">
            <span class="label">居住地址：</span>
            <span class="value">{{ userDetail.address }}</span>
          </div>
          <div class="detail-item">
            <span class="label">注册时间：</span>
            <span class="value">{{ userDetail.createTime }}</span>
          </div>
          <div class="detail-item">
            <span class="label">最后登录：</span>
            <span class="value">{{ userDetail.lastLoginTime }}</span>
          </div>
        </div>
      </div>

      <div class="detail-section">
        <h3 class="section-title">紧急联系人</h3>
        <div class="detail-grid">
          <div class="detail-item">
            <span class="label">姓名：</span>
            <span class="value">{{ userDetail.emergencyName }}</span>
          </div>
          <div class="detail-item">
            <span class="label">联系电话：</span>
            <span class="value">{{ userDetail.emergencyPhone }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getUserDetail } from '@/api/admin'
import type { user } from '@/types/user'

const route = useRoute()
const userDetail = ref<user>({
  userId: 0,
  realName: '',
  username: '',
  phone: '',
  avatar: '',
  gender: '男',
  age: 60,
  address: '',
  emergencyName: '',
  emergencyPhone: '',
  createTime: '',
  lastLoginTime: ''
})

onMounted(() => {
  const userId = route.query.id as number
  if (userId) {
    fetchUserDetail(userId)
  }
})

const fetchUserDetail = async (id: number) => {
  try {
    // --- 接口已注释 ---
    /*
    const res = await getUserDetail(id)
    userDetail.value = res.data
    */

    // 模拟数据
    userDetail.value = {
      userId: id,
      realName: '王奶奶',
      username: 'wang123',
      phone: '138****1111',
      avatar: '',
      gender: '女',
      age: 72,
      address: '幸福小区1栋3单元501',
      emergencyName: '王女士',
      emergencyPhone: '135****2222',
      createTime: '2026-01-01',
      lastLoginTime: '2026-05-26'
    }
  } catch (err) {
    console.error('获取用户详情失败', err)
    ElMessage.error('获取用户详情失败')
  }
}
</script>

<style scoped>
.detail-container {
  width: 100%;
  padding: 10px 0;
}

.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-title {
  font-size: 28px;
  font-weight: bold;
  color: #222;
  margin: 0;
}

.detail-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  padding: 30px;
}

.detail-section {
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 1px solid #eee;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin-bottom: 20px;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.detail-item {
  display: flex;
  align-items: center;
}

.label {
  width: 120px;
  font-size: 14px;
  color: #666;
  flex-shrink: 0;
}

.value {
  font-size: 14px;
  color: #333;
}
</style>
