<template>
  <div class="detail-container">
    <h2 class="title">订单详情</h2>

    <!-- 订单基础信息 -->
    <div class="card">
      <div class="row"><label>订单号：</label><span>{{ order.id }}</span></div>
      <div class="row"><label>服务类型：</label><span>{{ order.serviceType }}</span></div>
      <div class="row"><label>服务时间：</label><span>{{ order.serviceTime }}</span></div>
      <div class="row"><label>服务地址：</label><span>{{ order.address }}</span></div>
    </div>

    <!-- 积分相关信息（你要求的） -->
    <div class="card">
      <h3>积分信息</h3>
      <div class="row"><label>本次积分：</label><span style="color:#00a88d;font-weight:bold">+{{ order.points }} 积分</span></div>
      <div class="row"><label>积分获取时间：</label><span>{{ order.pointTime }}</span></div>
      <div class="row"><label>当前可用积分：</label><span>{{ currentPoints }} 积分</span></div>
    </div>

    <el-button type="primary" @click="back">返回上一页</el-button>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
// 👉 这里以后换成你真实的后端接口
// import { getOrderDetailApi } from '@/api/volunteer'

const route = useRoute()
const router = useRouter()

// 订单详情（含积分）
const order = ref({
  id: '',
  serviceType: '',
  serviceTime: '',
  address: '',
  points: 0,
  pointTime: ''
})

// 全局当前积分（可以从用户信息/接口拿）
const currentPoints = ref(0)

// 模拟：根据路由 id 获取订单详情（以后换真实接口）
const getOrderDetail = async (id) => {
  // 👉 后端联调时注释掉模拟，打开下面真实请求
  /*
  const res = await getOrderDetailApi(id)
  if (res.code === 200) {
    order.value = res.data
    currentPoints.value = res.data.currentPoints
  }
  */

  // 模拟数据
  order.value = {
    id: id,
    serviceType: '助餐服务-营养套餐A',
    serviceTime: '2026-05-21 11:30',
    address: '西安市雁塔区科技二路66号',
    points: 50,
    pointTime: '2026-05-21'
  }
  currentPoints.value = 1250
}

// 页面加载：拿路由 id，查订单
onMounted(() => {
  const id = route.params.id
  if (id) {
    getOrderDetail(id)
  }
})

// 返回：回到积分明细页，不会跳首页
const back = () => {
  router.go(-1)
}
</script>

<style scoped>
.detail-container { max-width: 800px; margin: 0 auto; padding: 20px; }
.title { font-size: 28px; margin-bottom: 20px; }
.card { background: #fff; padding: 30px; border-radius: 16px; margin-bottom: 20px; box-shadow: 0 4px 12px rgba(0,184,153,0.08); }
.row { margin: 12px 0; font-size: 18px; }
label { color: #666; width: 120px; display: inline-block; }
</style>
