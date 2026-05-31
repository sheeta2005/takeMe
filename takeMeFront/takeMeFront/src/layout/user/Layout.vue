<template>
  <el-container class="layout-container">
    <el-header class="layout-header">
      <div class="header-left">takeMe 老人服务平台</div>
      <div class="header-right">
        <!-- 右上角悬浮购物车图标（和管理员风格完全一致） -->
        <el-popover
          placement="bottom"
          width="420"
          trigger="click"
          v-model:visible="cartPopoverVisible"
          popper-class="cart-popover"
        >
          <template #reference>
            <div class="cart-icon-wrapper">
              <el-icon size="28" class="cart-icon"><ShoppingCart /></el-icon>
              <el-badge :value="cartStore.totalCount" class="cart-badge" />
            </div>
          </template>

          <!-- 购物车弹窗内容 -->
          <div class="cart-popover-content">
            <div class="cart-popover-header">
              <h3>我的购物车</h3>
              <span>共 {{ cartStore.totalCount }} 件商品</span>
            </div>

            <div class="cart-popover-list" v-if="cartStore.items.length > 0">
              <div class="cart-item" v-for="item in cartStore.items" :key="item.id">
                <div class="item-info">
                  <div class="item-name">{{ item.productName }}</div>
                  <div class="item-price">¥{{ item.productPrice }} × {{ item.quantity }}</div>
                </div>
                <div class="item-subtotal">¥{{ item.productPrice * item.quantity }}</div>
              </div>
            </div>

            <div class="cart-empty" v-else>
              <p>购物车是空的</p>
            </div>

            <div class="cart-popover-footer" v-if="cartStore.items.length > 0">
              <div class="total-price">
                总计：<span>¥{{ cartStore.totalPrice }}</span>
              </div>
              <el-button type="primary" size="large" @click="goToCart">
                去结算
              </el-button>
            </div>
          </div>
        </el-popover>

        <div class="user-box">
          <img class="user-avatar" :src="userStore.avatar" alt="头像" />
          <span>您好，{{ userStore.username }} 用户</span>
        </div>
      </div>
    </el-header>

    <el-container>
      <el-aside width="240px" class="layout-aside">
        <el-menu
          :default-active="activeMenu"
          class="aside-menu"
          @select="handleMenuSelect"
        >
          <el-menu-item index="/user">
            <el-icon><House /></el-icon>
            <span>首页</span>
          </el-menu-item>
          <el-menu-item index="/user/meal">
            <el-icon><Dish /></el-icon>
            <span>助餐服务</span>
          </el-menu-item>
          <el-menu-item index="/user/clean">
            <el-icon><Brush /></el-icon>
            <span>助洁服务</span>
          </el-menu-item>
          <el-menu-item index="/user/medical">
            <el-icon><FirstAidKit /></el-icon>
            <span>助医服务</span>
          </el-menu-item>
          <el-menu-item index="/user/shop">
            <el-icon><ShoppingBag /></el-icon>
            <span>代购服务</span>
          </el-menu-item>
          <el-menu-item index="/user/companion">
            <el-icon><ChatLineRound /></el-icon>
            <span>陪伴服务</span>
          </el-menu-item>
          <el-menu-item index="/user/order">
            <el-icon><Document /></el-icon>
            <span>我的订单</span>
          </el-menu-item>
          <el-menu-item index="/user/cart">
            <el-icon><ShoppingCart /></el-icon>
            <span>我的购物车</span>
            <el-badge :value="cartStore.totalCount" class="menu-badge" />
          </el-menu-item>
          <el-menu-item index="/user/message">
            <el-icon><Message /></el-icon>
            <span>消息中心</span>
            <el-badge :value="3" class="menu-badge" />
          </el-menu-item>
          <el-menu-item index="/user/info">
            <el-icon><User /></el-icon>
            <span>个人信息</span>
          </el-menu-item>
          <el-menu-item index="/user/setting">
            <el-icon><Setting /></el-icon>
            <span>账号设置</span>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useCartStore } from '@/stores/cart'
import {
  House, Dish, Brush, FirstAidKit, ShoppingBag,
  ChatLineRound, Document, ShoppingCart, Message, User, Setting
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const cartStore = useCartStore()

// 购物车弹窗显示状态
const cartPopoverVisible = ref(false)

// 菜单高亮
const activeMenu = computed(() => route.path)

// 菜单跳转
const handleMenuSelect = (path: string) => {
  router.push(path)
}

// 去购物车页面
const goToCart = () => {
  cartPopoverVisible.value = false
  router.push('/user/cart')
}

// 页面加载时从本地存储加载购物车
onMounted(() => {
  cartStore.loadFromLocalStorage()
})
</script>

<style scoped>
/* 完全复制管理员的基础样式，100% 对齐 */
.layout-container {
  height: 100vh;
}
.layout-header {
  background: linear-gradient(90deg, #00a88d 0%, #00c4a0 100%);
  color: #fff;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 22px 32px;
  font-size: 26px;
  font-weight: 600;
  box-shadow: 0 2px 12px rgba(0, 184, 141, 0.25);
  border-bottom: 1px solid rgba(255,255,255,0.1);
}
.header-right {
  font-size: 19px;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 32px;
}
.user-box {
  display: flex;
  align-items: center;
  gap: 14px;
}
.user-avatar {
  width: 46px;
  height: 46px;
  border-radius: 50%;
  object-fit: cover;
  border: 3px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}
.layout-aside {
  background-color: #ffffff;
  border-right: 1px solid #e5e7eb;
}
.aside-menu {
  border-right: none;
  height: 100%;
  padding-top: 20px;
}
:deep(.el-menu-item) {
  height: 64px;
  line-height: 64px;
  font-size: 19px;
  margin: 6px 16px;
  border-radius: 14px;
  transition: all 0.25s ease;
  position: relative;
}
:deep(.el-menu-item.is-active) {
  background: linear-gradient(90deg, #e6f7f3 0%, #f0fffc 100%);
  color: #008c74;
  font-weight: 600;
  box-shadow: 0 2px 8px rgba(0, 184, 141, 0.1);
}
:deep(.el-menu-item:hover) {
  background-color: #f5fcfb;
  transform: translateX(2px);
}
.layout-main {
  background-color: #f8faf9;
  padding: 32px;
  overflow-y: auto;
}

/* ✅ 修复红点对齐 */
.cart-icon-wrapper {
  position: relative;
  cursor: pointer;
  display: flex;
  align-items: center;
}
.cart-icon {
  color: white;
}
.cart-badge {
  position: absolute;
  top: -8px !important;
  right: -10px !important;
  --el-badge-size: 18px;
  font-size: 12px !important;
  line-height: 18px !important;
}
.menu-badge {
  position: absolute;
  top: 16px !important;
  right: 20px !important;
  --el-badge-size: 18px;
  font-size: 12px !important;
  line-height: 18px !important;
}
</style>

<style>
/* 购物车弹窗全局样式 */
.cart-popover {
  border-radius: 16px !important;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12) !important;
  border: none !important;
}
.cart-popover-content {
  padding: 20px;
}
.cart-popover-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #eee;
}
.cart-popover-header h3 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}
.cart-popover-list {
  max-height: 320px;
  overflow-y: auto;
  margin-bottom: 16px;
}
.cart-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f5f5f5;
}
.item-name {
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 4px;
}
.item-price {
  font-size: 14px;
  color: #666;
}
.item-subtotal {
  font-size: 16px;
  font-weight: 600;
  color: #f5222d;
}
.cart-empty {
  text-align: center;
  padding: 40px 0;
  color: #999;
  font-size: 16px;
}
.cart-popover-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid #eee;
}
.total-price {
  font-size: 18px;
}
.total-price span {
  font-size: 22px;
  font-weight: 600;
  color: #f5222d;
}
</style>
