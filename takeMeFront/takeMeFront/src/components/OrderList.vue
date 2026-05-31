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
            @click="openAddToCartModal(item)"
            class="order-btn"
          >
            加入购物车
          </el-button>
        </div>
      </div>
    </div>

    <!-- 加入购物车确认弹窗 -->
    <el-dialog
      v-model="addToCartVisible"
      title="确认服务信息"
      width="700px"
      @close="cancelAddToCart"
      destroy-on-close
    >
      <el-form :model="cartForm" label-width="100px" size="large" ref="cartFormRef" :rules="cartRules">
        <el-form-item label="服务名称">
          <div class="form-value">{{ currentService.name }}</div>
        </el-form-item>
        <el-form-item label="服务价格">
          <div class="form-value price">¥{{ currentService.price }} / 次</div>
        </el-form-item>
        <el-form-item label="服务日期" prop="serviceDate" required>
          <div class="date-buttons">
            <el-button
              v-for="day in dateList"
              :key="day.value"
              :type="cartForm.serviceDate === day.value ? 'primary' : 'default'"
              size="large"
              @click="selectDate(day.value)"
            >
              {{ day.label }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="服务时间" prop="serviceTime" required>
          <div v-if="cartForm.serviceDate && availableTimeSlots.length === 0" class="form-error">
            今天的服务时间已全部过期，请选择明天或后天
          </div>
          <div v-else-if="!cartForm.serviceDate" class="form-tip">
            请先选择服务日期
          </div>
          <div v-else class="time-buttons">
            <el-button
              v-for="slot in availableTimeSlots"
              :key="slot.value"
              :type="cartForm.serviceTime === slot.value ? 'primary' : 'default'"
              size="large"
              @click="cartForm.serviceTime = slot.value"
            >
              {{ slot.label }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="服务地址" prop="address" required>
          <el-input v-model="cartForm.address" type="textarea" :rows="2" placeholder="请输入服务地址" />
        </el-form-item>
        <el-form-item label="订单备注" prop="remark">
          <el-input v-model="cartForm.remark" type="textarea" :rows="2" placeholder="请输入特殊要求" />
        </el-form-item>

        <!-- 只有助餐服务才显示数量选择器 -->
        <el-form-item
          label="购买数量"
          prop="quantity"
          required
          v-if="currentConfig.title === '助餐服务'"
        >
          <el-input-number
            v-model="cartForm.quantity"
            :min="1"
            :max="10"
            size="large"
            style="width: 120px"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="cancelAddToCart">取消</el-button>
        <el-button
          type="primary"
          @click="confirmAddToCart"
          :loading="addToCartLoading"
          :disabled="cartForm.serviceDate && availableTimeSlots.length === 0"
        >
          确认加入购物车
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { ElMessage, ElForm } from 'element-plus'
import { useCartStore } from '@/stores/cart'
import {
  Dish, Brush, FirstAidKit, ShoppingCart, ChatLineRound
} from '@element-plus/icons-vue'

const props = defineProps({
  type: {
    type: Number,
    required: true
  }
})

const cartStore = useCartStore()
const cartFormRef = ref<InstanceType<typeof ElForm>>()
const serviceList = ref<any[]>([])

// 加入购物车弹窗
const addToCartVisible = ref(false)
const addToCartLoading = ref(false)
const currentService = ref<any>({})
const cartForm = ref({
  serviceDate: '',
  serviceTime: '',
  address: '',
  remark: '',
  quantity: 1
})

// 表单验证规则
const cartRules = {
  serviceDate: [
    { required: true, message: '请选择服务日期', trigger: 'change' }
  ],
  serviceTime: [
    { required: true, message: '请选择服务时间', trigger: 'change' }
  ],
  address: [
    { required: true, message: '请输入服务地址', trigger: 'blur' }
  ],
  quantity: [
    { required: true, message: '请输入购买数量', trigger: 'change' }
  ]
}

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

// ======================
// 日期时间联动逻辑（完全修复）
// ======================
const now = new Date()
const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())

const dateList = computed(() => {
  const days = []
  for (let i = 0; i < 3; i++) {
    const d = new Date(today.getTime() + i * 24 * 60 * 60 * 1000)
    const weekDays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
    const weekDay = weekDays[d.getDay()]
    const label = i === 0 ? `今天 ${weekDay}` : i === 1 ? `明天 ${weekDay}` : `后天 ${weekDay}`
    const value = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
    days.push({ label, value })
  }
  return days
})

const allTimeSlots = [
  { value: '09:00', label: '上午 9点' },
  { value: '10:00', label: '上午 10点' },
  { value: '11:00', label: '上午 11点' },
  { value: '14:00', label: '下午 2点' },
  { value: '15:00', label: '下午 3点' },
  { value: '16:00', label: '下午 4点' },
  { value: '17:00', label: '下午 5点' }
]

const availableTimeSlots = computed(() => {
  // 没有选择日期时，返回空数组
  if (!cartForm.value.serviceDate) return []

  // 如果不是今天，返回所有时间段
  if (cartForm.value.serviceDate !== dateList.value[0].value) return allTimeSlots

  // 如果是今天，过滤掉已经过去的时间
  const currentHour = now.getHours()
  return allTimeSlots.filter(slot => {
    const h = parseInt(slot.value.split(':')[0])
    return h > currentHour
  })
})

// 选择日期时自动清空时间并清除验证错误
const selectDate = async (date: string) => {
  cartForm.value.serviceDate = date
  cartForm.value.serviceTime = '' // 清空已选时间

  // 清除之前的验证错误
  await nextTick()
  if (cartFormRef.value) {
    cartFormRef.value.clearValidate(['serviceDate', 'serviceTime'])
  }
}

// =============================================
// 后端接口代码 100% 保留
// =============================================
onMounted(() => {
  fetchServiceList()
})

const fetchServiceList = async () => {
  try {
    // 模拟数据
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

// 打开加入购物车弹窗
const openAddToCartModal = (item: any) => {
  currentService.value = item
  // 重置表单
  cartForm.value = {
    serviceDate: '',
    serviceTime: '',
    address: '',
    remark: '',
    quantity: 1
  }
  addToCartVisible.value = true

  // 清除所有验证错误
  nextTick(() => {
    if (cartFormRef.value) {
      cartFormRef.value.clearValidate()
    }
  })
}

// 取消加入购物车
const cancelAddToCart = () => {
  addToCartVisible.value = false
  cartFormRef.value?.resetFields()
}

// 确认加入购物车
const confirmAddToCart = async () => {
  // 先进行表单校验
  if (!cartFormRef.value) return

  try {
    await cartFormRef.value.validate()
  } catch (err) {
    ElMessage.warning('请填写完整的服务信息')
    return
  }

  // 额外检查：如果今天没有可用时间，禁止提交
  if (cartForm.value.serviceDate && availableTimeSlots.length === 0) {
    ElMessage.error('今天的服务时间已全部过期，请选择明天或后天')
    return
  }

  addToCartLoading.value = true
  try {
    // 加入购物车
    cartStore.addItem({
      productId: currentService.value.id,
      productName: currentService.value.name,
      productPrice: currentService.value.price,
      serviceType: serviceTitle.value,
      serviceDate: cartForm.value.serviceDate,
      serviceTime: cartForm.value.serviceTime,
      address: cartForm.value.address,
      remark: cartForm.value.remark,
      quantity: cartForm.value.quantity
    })

    addToCartVisible.value = false
    ElMessage.success('已加入购物车')
  } catch (err) {
    ElMessage.error('加入购物车失败')
  } finally {
    addToCartLoading.value = false
  }
}
</script>

<style scoped>
/* 原有样式完全不变 */
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

/* 弹窗表单样式 */
.form-value {
  font-size: 18px;
  color: #333;
}
.form-value.price {
  font-size: 22px;
  font-weight: bold;
  color: #f5222d;
}
.form-tip {
  font-size: 14px;
  color: #999;
  margin-top: 8px;
}
.form-error {
  font-size: 14px;
  color: #f56c6c;
  margin-top: 8px;
}
.date-buttons, .time-buttons {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}
.date-buttons .el-button, .time-buttons .el-button {
  height: 50px;
  font-size: 18px !important;
}
</style>
