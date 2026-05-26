<template>
  <div class="study-detail-container">
    <div class="header">
      <el-button type="text" @click="back">← 返回</el-button>
      <h2 class="page-title">{{ guide?.title }}</h2>
    </div>

    <div class="detail-card" v-if="guide">
      <div class="detail-content">{{ guide.fullContent }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

// 完整的规范详情数据（前端写死）
const guideList = ref([
  {
    id: 1,
    title: '服务基本规范',
    fullContent: `
服务基本规范是所有助老服务的基础要求，请志愿者务必遵守：

1.  保持耐心与礼貌：与老人沟通时语速放缓、态度温和，尊重老人的表达节奏和生活习惯。
2.  尊重个人意愿：所有服务操作前需征得老人同意，不强迫、不诱导老人接受服务。
3.  避免争执冲突：遇到老人情绪波动或意见不合时，保持冷静，不与老人发生言语或肢体冲突。
4.  保护隐私安全：服务过程中不随意打听老人隐私，不泄露老人的个人信息与生活细节。
    `
  },
  {
    id: 2,
    title: '助餐服务规范',
    fullContent: `
助餐服务规范旨在保障老人用餐安全，请志愿者严格执行以下流程：

1.  送餐前核对信息：确认餐品与订单信息一致，检查餐品温度、包装是否完好，无洒漏、变质情况。
2.  送达时提醒老人：告知餐品的注意事项（如温度、食用建议），提醒老人注意防烫、防噎。
3.  用餐环境确认：观察老人用餐环境是否安全，必要时协助整理，避免餐品污染或意外发生。
4.  餐后简单回访：询问老人用餐感受，收集反馈意见，及时记录并上报问题。
    `
  },
  {
    id: 3,
    title: '助洁服务规范',
    fullContent: `
助洁服务规范要求志愿者在服务中兼顾效率与老人的居住习惯：

1.  提前沟通预约：服务前1天与老人确认时间、服务范围，避免临时上门打扰老人休息。
2.  保护个人物品：清洁过程中不随意翻动老人的私人物品，如需移动需提前告知并征得同意。
3.  重点区域清洁：优先清洁老人高频使用的区域（如厨房、卫生间、卧室），确保环境干净无异味。
4.  安全隐患排查：清洁时注意检查老人家中的安全隐患（如松动的家具、湿滑的地面），及时提醒老人。
    `
  }
])

// 根据路由id获取当前规范详情
const currentId = computed(() => Number(route.params.id))
const guide = computed(() => guideList.value.find(item => item.id === currentId.value))

// 返回列表页
const back = () => {
  router.go(-1)
}
</script>

<style scoped>
.study-detail-container {
  max-width: 900px;
  margin: 0 auto;
}
.header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}
.page-title {
  font-size: 28px;
  font-weight: bold;
  color: #333;
  margin: 0;
}
.detail-card {
  background: #fff;
  border-radius: 16px;
  padding: 32px;
  box-shadow: 0 4px 12px rgba(0, 184, 153, 0.08);
}
.detail-content {
  font-size: 16px;
  color: #333;
  line-height: 2;
  white-space: pre-line; /* 保留文本换行格式 */
}
</style>
