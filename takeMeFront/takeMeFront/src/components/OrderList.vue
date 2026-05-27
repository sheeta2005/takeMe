<template>
  <div class="service-page">
    <div class="page-header">
      <el-icon size="36" color="#006d5c">
        <component :is="iconComponent" />
      </el-icon>
      <h2 class="page-title">{{ serviceTitle }}</h2>
    </div>
    <p class="page-desc">{{ serviceDesc }}</p>

    <div class="service-list">
      <div class="service-card" v-for="item in serviceList" :key="item.id">
        <div class="card-left">
          <div class="service-name">{{ item.name }}</div>
          <div class="service-desc">{{ item.desc }}</div>
        </div>
        <div class="card-right">
          <span class="price">¥{{ item.price }}</span>
          <el-button
            size="large"
            type="primary"
            @click="goToOrder(item)"
            class="order-btn"
          >
            立即下单
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Dish, Brush, FirstAidKit, ShoppingCart, ChatLineRound
} from '@element-plus/icons-vue'

// 接口（已注释保留，以后解开即可）
// import { getServiceList } from '@/api/service'

const props = defineProps({
  type: {
    type: Number,
    required: true
  }
})

const router = useRouter()
const serviceList = ref<any[]>([])

// 服务大类配置
const serviceConfig = {
  0: { title: '代购服务', desc: '帮您购买生活用品、药品等，送货上门', icon: ShoppingCart },
  1: { title: '助洁服务', desc: '上门日常保洁，帮您打扫房间、整理家务', icon: Brush },
  2: { title: '助餐服务', desc: '为您提供营养搭配的一日三餐，可备注饮食需求', icon: Dish },
  3: { title: '助医服务', desc: '陪同就医、取药，为您的健康保驾护航', icon: FirstAidKit },
  4: { title: '陪伴服务', desc: '陪您聊天、散步、玩乐，排解孤独', icon: ChatLineRound }
}

const currentConfig = computed(() => serviceConfig[props.type])
const serviceTitle = computed(() => currentConfig.value.title)
const serviceDesc = computed(() => currentConfig.value.desc)
const iconComponent = computed(() => currentConfig.value.icon)

// =============================================
// 后端接口代码 100% 保留，只是注释掉
// =============================================
onMounted(() => {
  fetchServiceList()
})

const fetchServiceList = async () => {
  try {
    // -------------- 后端接口（已注释保留）--------------
    // const res = await getServiceList(props.type)
    // serviceList.value = res.data

    // -------------- 前端模拟数据 --------------
    const mockData = {
      0: [
        { id: 1, name: '生活用品代购', desc: '帮您购买日常用品', price: 10 },
        { id: 2, name: '药品代购', desc: '帮您购买常用药品', price: 15 }
      ],
      1: [
        { id: 1, name: '日常保洁2小时', desc: '打扫房间、擦桌子、拖地', price: 30 },
        { id: 2, name: '深度保洁3小时', desc: '包含厨房、卫生间清洁', price: 50 }
      ],
      2: [
        { id: 1, name: '营养套餐A', desc: '一荤两素 适合日常', price: 15 },
        { id: 2, name: '软食易消化餐', desc: '软烂易咀嚼 适合牙口不佳', price: 20 }
      ],
      3: [
        { id: 1, name: '陪同就诊服务', desc: '陪您去医院就诊、取药', price: 40 },
        { id: 2, name: '代取药品服务', desc: '帮您取药并送上门', price: 20 }
      ],
      4: [
        { id: 1, name: '聊天陪伴', desc: '陪您聊天、解闷，排解孤独', price: 25 },
        { id: 2, name: '散步陪伴', desc: '陪您出门散步、逛公园', price: 30 }
      ]
    }
    serviceList.value = mockData[props.type] || []

  } catch (err) {
    ElMessage.error('获取服务列表失败')
  }
}

// 去下单
const goToOrder = (item: any) => {
  router.push({
    path: '/user/create',
    query: {
      serviceType: serviceTitle.value,
      serviceName: item.name,
      price: item.price,
      serviceId: item.id
    }
  })
}
</script>

<style scoped>
.service-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}
.page-header {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-bottom: 12px;
}
.page-title {
  font-size: 32px;
  font-weight: bold;
  color: #006d5c;
  margin: 0;
}
.page-desc {
  font-size: 20px;
  color: #666;
  margin-bottom: 40px;
  text-align: center;
}
.service-list {
  display: flex;
  flex-direction: column;
  gap: 24px;
}
.service-card {
  background: #fff;
  border-radius: 20px;
  padding: 32px;
  box-shadow: 0 4px 16px rgba(0, 184, 153, 0.1);
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.card-left .service-name {
  font-size: 26px;
  font-weight: 600;
  color: #333;
  margin-bottom: 12px;
}
.card-left .service-desc {
  font-size: 20px;
  color: #999;
}
.card-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 16px;
}
.price {
  font-size: 32px;
  color: #f5222d;
  font-weight: bold;
}
.order-btn {
  width: 160px;
  height: 56px;
  font-size: 22px !important;
  font-weight: 600 !important;
  border-radius: 12px !important;
}
</style>
