<template>
  <div class="todo-container">
    <h2 class="page-title">我的待办任务</h2>

    <!-- 有进行中的订单 → 只显示这一个 -->
    <div v-if="currentOrder" class="order-list">
      <div class="order-card">
        <div class="order-header">
          <span class="order-id">订单号：{{ currentOrder.id }}</span>
          <el-tag type="primary" size="large">服务中</el-tag>
        </div>
        <div class="order-body">
          <div class="order-info">
            <div class="service-type">{{ currentOrder.serviceType }}</div>
            <div class="service-time">服务时间：{{ currentOrder.serviceTime }}</div>
            <div class="address">服务地址：{{ currentOrder.address }}</div>
          </div>
        </div>
        <div class="order-footer">
          <el-button type="success" size="large" @click="goToOrderDetail">
            查看订单详情
          </el-button>
          <el-button type="danger" size="large" @click="giveUpOrder">
            放弃订单
          </el-button>
        </div>
      </div>
    </div>

    <!-- 无进行中订单 → 显示3个待接单（三选一） -->
    <div v-else class="order-list">
      <div class="order-card" v-for="order in availableOrders" :key="order.id">
        <div class="order-header">
          <span class="order-id">订单号：{{ order.id }}</span>
          <el-tag type="warning" size="large">待确认</el-tag>
        </div>
        <div class="order-body">
          <div class="order-info">
            <div class="service-type">{{ order.serviceType }}</div>
            <div class="service-time">服务时间：{{ order.serviceTime }}</div>
            <div class="address">服务地址：{{ order.address }}</div>
          </div>
        </div>
        <div class="order-footer">
          <el-button type="primary" size="large" @click="confirmTask(order)">
            确认接单
          </el-button>
        </div>
      </div>
      <div class="empty-tip" v-if="availableOrders.length === 0">暂无待接单任务</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
// import { confirmOrder, giveUpOrder } from '@/api/volunteer' // 后端接口，先注释

const router = useRouter()

// 模拟3个订单（三选一）
const todoList = ref<any[]>([
  { id: 'ORD20260520001', serviceType: '助餐服务-营养套餐A', status: '待确认', serviceTime: '2026-05-21 11:30', address: '西安市雁塔区科技二路66号' },
  { id: 'ORD20260520002', serviceType: '助洁服务-日常保洁', status: '待确认', serviceTime: '2026-05-21 14:00', address: '西安市高新区锦业路1号' },
  { id: 'ORD20260520003', serviceType: '陪医服务-陪同就诊', status: '待确认', serviceTime: '2026-05-22 09:00', address: '西安市未央区凤城八路' }
])

// 当前正在服务的订单（只能有一个）
const currentOrder = ref<any>(null)

// 可选择的订单（未接单时显示）
const availableOrders = computed(() => {
  return todoList.value.filter(o => o.status === '待确认')
})

// 从 localStorage 恢复状态（刷新页面也不丢）
onMounted(() => {
  const saved = localStorage.getItem('currentOrder')
  if (saved) {
    const parsed = JSON.parse(saved)
    currentOrder.value = parsed
  }
})

// 确认接单（三选一，其他隐藏，跳转详情）
const confirmTask = async (order: any) => {
  // ===== 后端联调（上线打开注释）=====
  // try {
  //   await confirmOrder(order.id)
  // } catch (e) {
  //   ElMessage.error('接单失败')
  //   return
  // }

  // 纯前端模拟
  ElMessage.success(`已接单：${order.id}`)

  // 标记当前订单为服务中
  const newOrder = { ...order, status: '服务中' }
  currentOrder.value = newOrder
  localStorage.setItem('currentOrder', JSON.stringify(newOrder))

  // 跳转到订单详情页
  router.push(`/volunteer/order/${order.id}`)
}

// 放弃订单（扣分+恢复状态）
const giveUpOrder = async () => {
  try {
    await ElMessageBox.confirm(
      '放弃订单会扣除信用分，确定放弃吗？',
      '警告',
      {
        confirmButtonText: '确定放弃',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    // ===== 后端联调（上线打开注释）=====
    // try {
    //   await giveUpOrder(currentOrder.value.id)
    // } catch (e) {
    //   ElMessage.error('放弃订单失败')
    //   return
    // }

    // 纯前端模拟：恢复为待确认订单
    const orderId = currentOrder.value.id
    todoList.value = todoList.value.map(o => {
      if (o.id === orderId) {
        return { ...o, status: '待确认' }
      }
      return o
    })

    // 清空当前订单
    currentOrder.value = null
    localStorage.removeItem('currentOrder')

    ElMessage.warning('订单已放弃，已扣除信用分')
  } catch {
    // 用户取消操作，不处理
  }
}

// 去订单详情页
const goToOrderDetail = () => {
  router.push(`/volunteer/order/${currentOrder.value.id}`)
}
</script>

<style scoped>
.todo-container {
  max-width: 900px;
  margin: 0 auto;
}
.page-title {
  font-size: 28px;
  font-weight: bold;
  color: #333;
  margin-bottom: 24px;
}
.order-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.order-card {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 12px rgba(0, 184, 153, 0.08);
}
.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.order-id { font-size: 18px; color: #666; }
.order-info .service-type { font-size: 20px; font-weight: 600; color: #006d5c; margin-bottom: 8px; }
.order-info .service-time, .order-info .address { font-size: 16px; color: #999; margin-bottom: 4px; }
.order-footer { display: flex; gap: 12px; justify-content: flex-end; margin-top: 16px; }
.empty-tip { text-align: center; font-size: 20px; color: #999; padding: 60px 0; }
</style>
