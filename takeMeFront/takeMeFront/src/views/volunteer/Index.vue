<template>
  <div class="volunteer-home">
    <div class="welcome">欢迎您，志愿者 {{ userName }}</div>
    <div class="task-card">
      <div class="task-title">待处理订单</div>
      <el-table :data="taskList" border>
        <el-table-column prop="id" label="订单ID" />
        <el-table-column prop="userName" label="老人姓名" />
        <el-table-column prop="serviceType" label="服务类型" />
        <el-table-column prop="createTime" label="下单时间" />
        <el-table-column label="操作">
          <template #default="{ row }">
            <el-button type="primary" @click="acceptOrder(row.id)">接单</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <div class="my-order">
      <div class="order-title">我的订单</div>
      <el-table :data="myOrderList" border>
        <el-table-column prop="id" label="订单ID" />
        <el-table-column prop="userName" label="老人姓名" />
        <el-table-column prop="status" label="状态" />
        <el-table-column prop="completeTime" label="完成时间" />
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getOrderList } from '@/api'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const userName = ref('')
const taskList = ref([])
const myOrderList = ref([])

onMounted(async () => {
  userName.value = userStore.userName || '志愿者'
  const res = await getOrderList({ status: 'pending' })
  taskList.value = res.data
})

const acceptOrder = (id: number) => {
  // 对应后端接单接口
  console.log('接单', id)
}
</script>

<style scoped>
.volunteer-home { padding: 20px; }
.welcome { font-size: 20px; margin-bottom: 20px; }
.task-card, .my-order {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
}
</style>
