<template>
  <div class="leave-container">
    <h2 class="page-title">出勤请假</h2>
    <div class="form-card">
      <h3 class="card-title">提交请假申请</h3>
      <el-form :model="leaveForm" label-width="100px">
        <el-form-item label="请假类型">
          <el-select v-model="leaveForm.type" placeholder="请选择">
            <el-option label="事假" value="事假" />
            <el-option label="病假" value="病假" />
          </el-select>
        </el-form-item>
        <el-form-item label="请假时间">
          <el-date-picker v-model="leaveForm.time" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" />
        </el-form-item>
        <el-form-item label="请假原因">
          <el-input v-model="leaveForm.reason" type="textarea" :rows="3" placeholder="请输入请假原因" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="submitLeave">提交申请</el-button>
        </el-form-item>
      </el-form>
    </div>
    <div class="record-card">
      <h3 class="card-title">请假记录</h3>
      <div class="leave-list">
        <div class="leave-item" v-for="item in leaveList" :key="item.id">
          <span class="type">{{ item.type }}</span>
          <span class="time">{{ item.time }}</span>
          <el-tag :type="getTagType(item.status)" size="small">{{ item.status }}</el-tag>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'

const leaveForm = ref({ type: '', time: [], reason: '' })
const leaveList = ref([
  { id: 1, type: '事假', time: '2026-05-25 至 2026-05-26', status: '已通过' }
])
const getTagType = (status: string) => status === '已通过' ? 'success' : 'warning'

const submitLeave = () => {
  if (!leaveForm.value.type || !leaveForm.value.time.length) {
    ElMessage.warning('请完善请假信息')
    return
  }
  ElMessage.success('请假申请已提交')
}
</script>

<style scoped>
.leave-container {
  max-width: 700px;
  margin: 0 auto;
}
.page-title {
  font-size: 28px;
  font-weight: bold;
  color: #333;
  margin-bottom: 24px;
}
.form-card, .record-card {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 12px rgba(0, 184, 153, 0.08);
  margin-bottom: 24px;
}
.card-title { font-size: 22px; color: #333; margin-bottom: 16px; }
.leave-list { display: flex; flex-direction: column; gap: 12px; }
.leave-item { display: flex; justify-content: space-between; align-items: center; padding: 12px 0; border-bottom: 1px solid #eee; }
.leave-item:last-child { border-bottom: none; }
.type { font-size: 16px; color: #333; }
.time { font-size: 16px; color: #666; }
</style>
