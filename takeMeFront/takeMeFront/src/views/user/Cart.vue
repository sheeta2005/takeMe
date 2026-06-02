<template>
  <div class="cart-page">
    <div class="page-header">
      <h1>我的购物车</h1>
      <p>共 {{ cartStore.totalCount }} 件商品，总计 ¥{{ cartStore.totalPrice }}</p>
    </div>

    <div class="cart-content" v-if="cartStore.items.length > 0">
      <div class="cart-list">
        <div class="cart-item" v-for="item in currentPageItems" :key="item.id">
          <div class="item-info">
            <div class="item-name">{{ item.serviceName }}</div>
            <div class="item-price">单价：¥{{ item.servicePrice }}</div>
          </div>

          <div class="item-quantity">
            <el-button size="large" @click="decreaseQuantity(item)">-</el-button>
            <span class="quantity">{{ item.quantity }}</span>
            <el-button size="large" @click="increaseQuantity(item)">+</el-button>
          </div>

          <div class="item-subtotal">
            ¥{{ item.servicePrice * item.quantity }}
          </div>

          <div class="item-actions">
            <el-button size="large" type="primary" @click="viewItemDetail(item)">
              查看详情
            </el-button>
            <el-button size="large" type="danger" @click="removeItem(item.id)">
              删除
            </el-button>
          </div>
        </div>
      </div>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="cartStore.items.length"
          layout="prev, pager, next"
          size="large"
          @current-change="handlePageChange"
        />
      </div>

      <div class="cart-footer">
        <el-button size="large" @click="clearCart">清空购物车</el-button>
        <div class="total-section">
          <span class="total-text">总计：</span>
          <span class="total-price">¥{{ cartStore.totalPrice }}</span>
          <el-button type="primary" size="large" @click="goToCheckout">
            去结算
          </el-button>
        </div>
      </div>
    </div>

    <div class="cart-empty" v-else>
      <el-icon size="120" color="#ccc"><ShoppingCart /></el-icon>
      <h2>购物车是空的</h2>
      <p>快去挑选您需要的服务吧</p>
      <el-button type="primary" size="large" @click="goToHome">
        去首页
      </el-button>
    </div>

    <el-dialog
      v-model="detailVisible"
      title="服务详情"
      width="700px"
      @close="detailVisible = false"
    >
      <div class="detail-content" v-if="currentDetailItem">
        <div class="detail-section">
          <div class="section-title">服务信息</div>
          <div class="info-grid">
            <div class="info-item">
              <span class="label">服务类型：</span>
              <span class="value">{{ getServiceTypeName(currentDetailItem.serviceType) }}</span>
            </div>
            <div class="info-item">
              <span class="label">服务名称：</span>
              <span class="value">{{ currentDetailItem.serviceName }}</span>
            </div>
            <div class="info-item">
              <span class="label">服务价格：</span>
              <span class="value price">¥{{ currentDetailItem.servicePrice }}</span>
            </div>
            <div class="info-item">
              <span class="label">购买数量：</span>
              <span class="value">{{ currentDetailItem.quantity }}</span>
            </div>
            <div class="info-item">
              <span class="label">小计金额：</span>
              <span class="value price">¥{{ currentDetailItem.servicePrice * currentDetailItem.quantity }}</span>
            </div>

            <div class="info-item full-width" v-if="currentDetailItem.remark">
              <span class="label">备注：</span>
              <span class="value">{{ currentDetailItem.remark }}</span>
            </div>
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { useCartStore } from '@/stores/cart'
import { ShoppingCart } from '@element-plus/icons-vue'

const router = useRouter()
const cartStore = useCartStore()

const currentPage = ref(1)
const pageSize = 5

const detailVisible = ref(false)
const currentDetailItem = ref<any>(null)

const currentPageItems = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  const end = start + pageSize
  return cartStore.items.slice(start, end)
})

const getServiceTypeName = (type) => {
  const typeMap = {
    0: '代购服务',
    1: '助洁服务',
    2: '助餐服务',
    3: '助医服务',
    4: '陪伴服务'
  }
  return typeMap[type] || '未知服务'
}

const handlePageChange = (page) => {
  currentPage.value = page
}

const decreaseQuantity = (item) => {
  if (item.serviceType !== 2) {
    ElMessage.info('这项服务只能预约1次')
    return
  }
  if (item.quantity > 1) {
    cartStore.updateQuantity(item.id, item.quantity - 1)
  }
}

const increaseQuantity = (item) => {
  if (item.serviceType !== 2) {
    ElMessage.info('这项服务只能预约1次')
    return
  }
  cartStore.updateQuantity(item.id, item.quantity + 1)
}

const removeItem = (itemId) => {
  ElMessageBox.confirm('确定删除？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    cartStore.removeItem(itemId)
    ElMessage.success('已删除')
  }).catch(() => {})
}

const clearCart = () => {
  ElMessageBox.confirm('确定清空？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    cartStore.clearCart()
    currentPage.value = 1
    ElMessage.success('已清空')
  }).catch(() => {})
}

const viewItemDetail = (item) => {
  currentDetailItem.value = item
  detailVisible.value = true
}

const goToCheckout = () => {
  router.push({ path: '/user/create', query: { fromCart: 'true' } })
}

const goToHome = () => {
  router.push('/user')
}
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
  gap: 24px;
}
.total-text {
  font-size: 22px;
}
.total-price {
  font-size: 32px;
  font-weight: 600;
  color: #f5222d;
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

.detail-section {
  margin-bottom: 24px;
  padding-bottom: 24px;
  border-bottom: 1px solid #eee;
}
.detail-section:last-child {
  border: none;
  margin-bottom: 0;
  padding-bottom: 0;
}
.section-title {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 20px;
  color: #333;
}
.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
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
