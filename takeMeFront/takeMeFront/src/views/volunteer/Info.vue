<template>
  <div class="info-container">
    <h2 class="page-title">个人信息</h2>
    <div class="info-card">
      <div class="avatar-box">
        <img class="avatar" :src="volunteerInfo.avatar" alt="头像" />
      </div>
      <div class="info-list">
        <div class="info-item">
          <span class="label">姓名：</span>
          <span class="value">{{ volunteerInfo.realName }}</span>
        </div>
        <div class="info-item">
          <span class="label">账号：</span>
          <span class="value">{{ volunteerInfo.username }}</span>
        </div>
        <div class="info-item">
          <span class="label">手机号：</span>
          <span class="value">{{ volunteerInfo.phone || '未绑定' }}</span>
        </div>
        <div class="info-item">
          <span class="label">性别：</span>
          <span class="value">{{ volunteerInfo.gender }}</span>
        </div>
        <div class="info-item">
          <span class="label">年龄：</span>
          <span class="value">{{ volunteerInfo.age }}</span>
        </div>
        <div class="info-item">
          <span class="label">居住地址：</span>
          <span class="value">{{ volunteerInfo.address || '未填写' }}</span>
        </div>

        <!-- 可服务时间：按钮展示 -->
        <div class="info-item">
          <span class="label">可服务时间：</span>
          <div class="days-display">
            <span
              v-for="day in weekDays"
              :key="day"
              class="day-tag"
              :class="{ active: serviceDaysArr.includes(day) }"
            >
              {{ day }}
            </span>
          </div>
        </div>

        <div class="info-item">
          <span class="label">可服务业务：</span>
          <span class="value">{{ volunteerInfo.serviceType }}</span>
        </div>
        <div class="info-item">
          <span class="label">服务状态：</span>
          <span class="value">{{ volunteerInfo.status }}</span>
        </div>
        <div class="info-item">
          <span class="label">紧急联系人：</span>
          <span class="value">{{ volunteerInfo.emergencyName || '未填写' }}</span>
        </div>
        <div class="info-item">
          <span class="label">紧急联系电话：</span>
          <span class="value">{{ volunteerInfo.emergencyPhone || '未填写' }}</span>
        </div>
      </div>
      <el-button type="primary" size="large" @click="goEditInfo">
        修改信息
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getVolunteerInfo } from '@/api/volunteer'

const router = useRouter()
const volunteerInfo = ref({
  realName: '',
  username: '',
  phone: '',
  avatar: '',
  serviceDays: '', // 后端返回字符串（如"周一,周三"）
  serviceType: '',
  status: '正常服务中',
  gender: '',
  age: 0,
  address: '',
  emergencyName: '',
  emergencyPhone: ''
})

// 星期列表（和编辑页一致）
const weekDays = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']

// 字符串转数组，用于按钮高亮判断
const serviceDaysArr = computed(() => {
  return volunteerInfo.value.serviceDays?.split(',') || []
})

onMounted(async () => {
  try {
    const res = await getVolunteerInfo()
    if (res.code === 200) {
      volunteerInfo.value = {
        ...volunteerInfo.value,
        ...res.data
      }
    }
  } catch {
    ElMessage.error('加载信息失败')
  }
})

const goEditInfo = () => {
  router.push('/volunteer/info/edit')
}
</script>

<style scoped>
.info-container {
  max-width: 700px;
  margin: 0 auto;
}
.page-title {
  font-size: 28px;
  font-weight: bold;
  color: #333;
  margin-bottom: 24px;
}
.info-card {
  background: #fff;
  border-radius: 16px;
  padding: 32px;
  box-shadow: 0 4px 12px rgba(0, 184, 153, 0.08);
  display: flex;
  align-items: center;
  gap: 32px;
}
.avatar-box .avatar {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  object-fit: cover;
}
.info-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  flex: 1;
}
.info-item .label {
  font-size: 18px;
  color: #666;
  width: 130px;
  display: inline-block;
  vertical-align: top;
}
.info-item .value {
  font-size: 18px;
  color: #333;
}

/* 可服务时间按钮样式（和编辑页统一） */
.days-display {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  display: inline-block;
}
.day-tag {
  display: inline-block;
  padding: 4px 12px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  font-size: 14px;
  margin-right: 6px;
}
.day-tag.active {
  background-color: #00b899;
  color: #fff;
  border-color: #00b899;
}
</style>
