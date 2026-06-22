<template>
  <div class="cart-page">
    <div class="page-header">
      <h1>我的购物车</h1>
      <p>查看和管理您已选择的服务</p>
    </div>

    <div v-if="cartStore.items.length > 0" class="cart-content">
      <div class="cart-list">
        <div class="cart-item" v-for="item in currentPageItems" :key="item.id">
          <div class="item-info" @click="viewItemDetail(item)" style="cursor: pointer;">
            <div class="item-name">{{ item.serviceName }}</div>
            <div class="item-price">¥{{ item.servicePrice }}</div>
            <div class="item-detail">
              <el-tag :type="getServiceTypeTag(item.serviceType)" size="small">
                {{ getServiceTypeName(item.serviceType) }}
              </el-tag>
            </div>
          </div>

          <div class="item-quantity">
            <el-button
              :icon="Minus"
              circle
              size="large"
              @click="decreaseQuantity(item)"
            />
            <span class="quantity">{{ item.quantity }}</span>
            <el-button
              :icon="Plus"
              circle
              size="large"
              @click="increaseQuantity(item)"
              :disabled="item.serviceType !== 2"
            />
          </div>

          <div class="item-subtotal">
            ¥{{ (item.servicePrice * item.quantity).toFixed(2) }}
          </div>

          <div class="item-actions">
            <el-button
              type="danger"
              size="large"
              @click="removeItem(item.id)"
            >
              删除
            </el-button>
          </div>
        </div>
      </div>

      <div class="pagination-wrapper" v-if="cartStore.items.length > pageSize">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="cartStore.items.length"
          layout="prev, pager, next"
          @current-change="handlePageChange"
        />
      </div>

      <div class="cart-footer">
        <div class="total-section">
          <span class="total-text">合计：</span>
          <span class="total-price">¥{{ cartStore.totalPrice }}</span>
        </div>
        <div class="action-buttons">
          <el-button
            size="large"
            @click="clearCart"
            :disabled="submitting"
          >
            清空购物车
          </el-button>
          <el-button
            type="primary"
            size="large"
            @click="goToCheckout"
            :disabled="submitting || cartStore.items.length === 0"
          >
            去结算
          </el-button>
        </div>
      </div>
    </div>

    <div v-else class="cart-empty">
      <el-icon :size="120" color="#d9d9d9">
        <ShoppingCart />
      </el-icon>
      <h2>购物车空空如也</h2>
      <p>快去选择您需要的服务吧</p>
      <el-button
        type="primary"
        size="large"
        @click="router.push('/user')"
      >
        去首页看看
      </el-button>
    </div>

    <!-- 服务详情弹窗 -->
    <el-dialog
      v-model="detailVisible"
      title="服务详情"
      width="800px"
      class="service-detail-dialog"
    >
      <div class="detail-content" v-if="currentDetailItem">
        <div class="detail-section">
          <div class="section-title">
            <el-icon><Document /></el-icon>
            基本信息
          </div>
          <el-descriptions :column="2" border class="detail-descriptions">
            <el-descriptions-item label="服务名称">
              <span class="service-name-text">{{ currentDetailItem.serviceName }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="服务类型">
              <el-tag :type="getServiceTypeTag(currentDetailItem.serviceType)">
                {{ getServiceTypeName(currentDetailItem.serviceType) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="单价">
              <span class="price-text">¥{{ currentDetailItem.price }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="数量">
              {{ currentDetailItem.quantity }}
            </el-descriptions-item>
            <el-descriptions-item label="小计" :span="2">
              <span class="total-price-text">¥{{ (currentDetailItem.price * currentDetailItem.quantity).toFixed(2) }}</span>
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <div class="detail-section" v-if="currentDetailItem.address">
          <div class="section-title">
            <el-icon><Location /></el-icon>
            服务地址
          </div>
          <div class="address-box">
            <el-icon class="address-icon"><LocationFilled /></el-icon>
            <span class="address-text">{{ currentDetailItem.address }}</span>
          </div>
        </div>

        <div class="detail-section" v-if="currentDetailItem.remark">
          <div class="section-title">
            <el-icon><ChatDotRound /></el-icon>
            备注信息
          </div>
          <div class="remark-box">
            {{ currentDetailItem.remark }}
          </div>
        </div>
      </div>
    </el-dialog>

  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { useCartStore } from '@/stores/cart'
import {
  ShoppingCart, Minus, Plus, Document, Location,
  LocationFilled, ChatDotRound
} from '@element-plus/icons-vue'

const router = useRouter()
const cartStore = useCartStore()
const loading = ref(false)
const submitting = ref(false)

const currentPage = ref(1)
const pageSize = 5

const detailVisible = ref(false)
const currentDetailItem = ref<any>(null)

const currentPageItems = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  const end = start + pageSize
  return cartStore.items.slice(start, end)
})

const getServiceTypeName = (type: number) => {
  const typeMap: Record<number, string> = {
    0: '代购服务',
    1: '助洁服务',
    2: '助餐服务',
    3: '助医服务',
    4: '陪伴服务'
  }
  return typeMap[type] || '未知服务'
}

const getServiceTypeTag = (type: number) => {
  const tagMap: Record<number, string> = {
    0: 'primary',
    1: 'success',
    2: 'warning',
    3: 'danger',
    4: 'info'
  }
  return tagMap[type] || 'info'
}

const handlePageChange = (page: number) => {
  currentPage.value = page
}

const decreaseQuantity = async (item: any) => {
  if (item.quantity === 1) {
    try {
      await ElMessageBox.confirm('确定删除此服务？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
      const success = await cartStore.removeItem(item.id)
      if (success) {
        ElMessage.success('已删除')
        if (currentPageItems.value.length === 0 && currentPage.value > 1) {
          currentPage.value--
        }
      }
    } catch {
    }
    return
  }

  if (item.serviceType === 2) {
    await cartStore.updateQuantity(item.id, item.quantity - 1)
  } else {
    ElMessage.info('这项服务只能预约1次')
  }
}

const increaseQuantity = async (item: any) => {
  if (item.serviceType === 2) {
    await cartStore.updateQuantity(item.id, item.quantity + 1)
  } else {
    ElMessage.info('这项服务只能预约1次')
  }
}

const removeItem = async (cartItemId: number) => {
  try {
    await ElMessageBox.confirm('确定删除？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const success = await cartStore.removeItem(cartItemId)
    if (success) {
      ElMessage.success('已删除')
      if (currentPageItems.value.length === 0 && currentPage.value > 1) {
        currentPage.value--
      }
    }
  } catch {
  }
}

const clearCart = async () => {
  try {
    await ElMessageBox.confirm('确定清空？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await cartStore.clearCart()
    currentPage.value = 1
  } catch {
  }
}

const viewItemDetail = (item: any) => {
  currentDetailItem.value = item
  detailVisible.value = true
}

const goToCheckout = () => {
  if (cartStore.items.length === 0) {
    ElMessage.warning('购物车为空')
    return
  }
  router.push('/user/checkout')
}

onMounted(async () => {
  loading.value = true
  try {
    await cartStore.fetchCartList()
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.cart-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}
.page-header {
  margin-bottom: 30px;
}
.page-header h1 {
  font-size: 36px;
  font-weight: 600;
  color: #333;
  margin-bottom: 8px;
}
.page-header p {
  font-size: 20px;
  color: #666;
}
.cart-list {
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.08);
}
.cart-item {
  display: flex;
  align-items: center;
  padding: 24px 0;
  border-bottom: 1px solid #f5f5f5;
}
.cart-item:last-child {
  border-bottom: none;
}

/* 服务详情弹窗样式 */
.service-detail-dialog :deep(.el-dialog__header) {
  border-bottom: 2px solid #00b899;
  padding-bottom: 16px;
}

.service-detail-dialog :deep(.el-dialog__title) {
  font-size: 24px;
  font-weight: 600;
  color: #333;
}

.detail-content {
  padding: 8px 0;
}

.detail-section {
  margin-bottom: 24px;
}

.detail-section:last-child {
  margin-bottom: 0;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin-bottom: 16px;
  padding-left: 8px;
  border-left: 4px solid #00b899;
}

.section-title .el-icon {
  color: #00b899;
  font-size: 20px;
}

.detail-descriptions {
  border-radius: 8px;
  overflow: hidden;
}

.detail-descriptions :deep(.el-descriptions__label) {
  background: #f8faf9;
  font-weight: 600;
  color: #666;
  width: 120px;
}

.detail-descriptions :deep(.el-descriptions__content) {
  padding: 12px 16px;
}

.service-name-text {
  font-size: 16px;
  font-weight: 500;
  color: #333;
}

.price-text {
  font-size: 20px;
  font-weight: bold;
  color: #f5222d;
}

.total-price-text {
  font-size: 24px;
  font-weight: bold;
  color: #f5222d;
}

.address-box {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  background: #f0f9f4;
  border-radius: 8px;
  border: 1px solid #b7eb8f;
}

.address-icon {
  color: #00b899;
  font-size: 24px;
}

.address-text {
  font-size: 16px;
  color: #333;
  line-height: 1.6;
}

.remark-box {
  padding: 16px 20px;
  background: #fff7e6;
  border-radius: 8px;
  border: 1px solid #ffd591;
  font-size: 16px;
  color: #333;
  line-height: 1.8;
  white-space: pre-wrap;
}

:deep(.el-tag) {
  padding: 8px 16px;
  font-size: 15px;
}

:deep(.el-tag .el-icon) {
  margin-right: 6px;
}

.item-info {
  flex: 4;
}
.item-name {
  font-size: 24px;
  font-weight: 600;
  margin-bottom: 12px;
}
.item-price {
  font-size: 18px;
  color: #666;
}
.item-detail {
  font-size: 14px;
  color: #999;
  margin-top: 8px;
}
.item-quantity {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
}
.quantity {
  font-size: 24px;
  font-weight: 600;
  min-width: 40px;
  text-align: center;
}
.item-subtotal {
  flex: 1;
  text-align: right;
  font-size: 26px;
  font-weight: 600;
  color: #f5222d;
}
.item-actions {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
  align-items: flex-end;
}
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-bottom: 20px;
}

.cart-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.08);
}
.total-section {
  display: flex;
  align-items: center;
  gap: 16px;
}
.total-text {
  font-size: 22px;
}
.total-price {
  font-size: 32px;
  font-weight: 600;
  color: #f5222d;
}
.action-buttons {
  display: flex;
  gap: 12px;
}
.cart-empty {
  text-align: center;
  padding: 100px 0;
  color: #999;
}
.cart-empty h2 {
  font-size: 28px;
  margin: 20px 0 12px;
}
.cart-empty p {
  font-size: 18px;
  margin-bottom: 30px;
}

.info-item {
  display: flex;
  align-items: center;
}
.info-item.full-width {
  grid-column: 1 / -1;
}
.label {
  width: 100px;
  font-size: 16px;
  color: #666;
  flex-shrink: 0;
}
.value {
  font-size: 16px;
  color: #333;
}
.value.price {
  font-size: 20px;
  font-weight: bold;
  color: #f5222d;
}
</style>
