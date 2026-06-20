<template>
  <div class="order-list">
    <el-card v-for="order in orders" :key="order.id" class="order-card">
      <div class="order-header">
        <span>订单号: {{ order.orderNo }}</span>
        <el-tag :type="getStatusType(order.status)">
          {{ getStatusText(order.status) }}
        </el-tag>
      </div>

      <div v-for="item in order.items" :key="item.id" class="order-item">
        <div class="item-info">
          <span>{{ item.serviceName }}</span>
          <span>¥{{ item.itemPrice }}</span>
        </div>

        <div class="item-actions">
          <el-button
            v-if="canCancelItem(item)"
            size="small"
            type="danger"
            @click="handleCancelItem(item.id)"
          >
            取消服务
          </el-button>

          <el-button
            v-if="canEvaluateItem(item)"
            size="small"
            type="primary"
            @click="handleEvaluateItem(item)"
          >
            评价
          </el-button>
        </div>
      </div>

      <div class="order-actions">
        <el-button
          v-if="order.status === 0 || order.status === 1"
          type="danger"
          @click="handleCancelOrder(order.id)"
        >
          取消订单
        </el-button>

        <el-button
          v-if="order.status === 3"
          type="success"
          @click="handleConfirmOrder(order.id)"
        >
          确认完成
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
// ... existing code ...
import { cancelOrderItem, evaluateOrderItem } from '@/api/order'

const canCancelItem = (item: any) => {
  return item.itemStatus === 0 || item.itemStatus === 1
}

const canEvaluateItem = (item: any) => {
  return item.itemStatus === 3 || item.itemStatus === 4
}

const handleCancelItem = async (orderItemId: number) => {
  try {
    await ElMessageBox.confirm('确认取消该服务项？', '提示', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await cancelOrderItem(orderItemId)
    ElMessage.success('取消成功')

    setTimeout(() => {
      loadOrders()
    }, 500)
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '取消失败')
    }
  }
}

const handleEvaluateItem = async (item: any) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入评价内容（可选）', '评分', {
      confirmButtonText: '提交',
      cancelButtonText: '取消',
      inputPattern: /.*/,
      inputErrorMessage: '请输入评价内容'
    })

    const rating = await showRatingDialog()

    await evaluateOrderItem(item.id, rating, value || '')
    ElMessage.success('评价成功')

    setTimeout(() => {
      loadOrders()
    }, 500)
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '评价失败')
    }
  }
}

const showRatingDialog = async (): Promise<number> => {
  return new Promise((resolve) => {
    ElMessageBox({
      title: '评分',
      message: h('div', [
        h('el-rate', {
          modelValue: 5,
          'onUpdate:modelValue': (val: number) => {
            resolve(val)
          }
        })
      ]),
      showCancelButton: false,
      showConfirmButton: false
    })
  })
}

// ... existing code ...
</script>
