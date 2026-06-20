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
          <div class="service-desc">{{ item.description }}</div>
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
          <!-- ✅ 修复：地址下拉选择框 -->
          <el-select v-model="cartForm.address" placeholder="请选择服务地址" size="large" style="width: 100%">
            <el-option
              v-for="addr in userAddressList"
              :key="addr.id"
              :label="addr.address"
              :value="addr.address"
            />
          </el-select>
          <!-- 无地址提示 -->
          <div v-if="userAddressList.length === 0" class="form-error" style="margin-top:8px">
            请先前往个人中心添加服务地址
          </div>
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
          :disabled="cartForm.serviceDate && availableTimeSlots.length === 0 || userAddressList.length === 0"
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
import { useUserStore } from '@/stores/user'
import { getServiceList } from '@/api'
// ✅ 导入地址接口
import { getUserAddressList } from '@/api/user'
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
const userStore = useUserStore()
const cartFormRef = ref<InstanceType<typeof ElForm>>()
const serviceList = ref<{
  id: number;
  name: string;
  type: number;
  price: number;
  description: string;
  image: string;
  status: number;
}[]>([])

// ✅ 修复：获取地址对象数组
const userAddressList = computed(() => userStore.addresses)

// 弹窗状态
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

// 校验规则
const cartRules = {
  serviceDate: [{ required: true, message: '请选择服务日期', trigger: 'change' }],
  serviceTime: [{ required: true, message: '请选择服务时间', trigger: 'change' }],
  address: [{ required: true, message: '请选择服务地址', trigger: 'change' }],
  quantity: [{ required: true, message: '请输入购买数量', trigger: 'change' }]
}

// 服务配置
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

// 日期时间逻辑
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
  { value: '17:00', label: '下午 5点' },
  { value: '19:00', label: '晚上 7点' },
  { value: '23:00', label: '晚上 11点 (测试用)' }
]

const availableTimeSlots = computed(() => {
  if (!cartForm.value.serviceDate) return []
  if (cartForm.value.serviceDate !== dateList.value[0].value) return allTimeSlots
  const currentHour = now.getHours()
  return allTimeSlots.filter(slot => parseInt(slot.value.split(':')[0]) > currentHour)
})

const selectDate = async (date: string) => {
  cartForm.value.serviceDate = date
  cartForm.value.serviceTime = ''
  await nextTick()
  cartFormRef.value?.clearValidate(['serviceDate', 'serviceTime'])
}

// =============================================
// 接口调用
// =============================================
onMounted(async () => {
  fetchServiceList()
  // ✅ 修复：主动加载地址列表到store
  await loadAddressList()
})

// ✅ 新增：加载地址列表
const loadAddressList = async () => {
  try {
    const res = await getUserAddressList()
    if (res.code === 200) {
      userStore.setAddresses(res.data)
    }
  } catch (e) {
    ElMessage.error('加载地址失败')
  }
}

const fetchServiceList = async () => {
  try {
    const res = await getServiceList(props.type)
    serviceList.value = res.data.filter((item: any) => item.status === 1)
  } catch (err) {
    ElMessage.error('获取服务列表失败')
  }
}

// 打开弹窗
const openAddToCartModal = (item: any) => {
  currentService.value = item
  cartForm.value = { serviceDate: '', serviceTime: '', address: '', remark: '', quantity: 1 }

  // ✅ 修复：自动填充第一个地址
  if (userAddressList.value.length > 0) {
    cartForm.value.address = userAddressList.value[0].address
  }

  addToCartVisible.value = true
  nextTick(() => cartFormRef.value?.clearValidate())
}

const cancelAddToCart = () => {
  addToCartVisible.value = false
  cartFormRef.value?.resetFields()
}

// =============================================
// ✅ 核心修复：适配后端 CartItemDTO，解决报错
// =============================================
const confirmAddToCart = async () => {
  try {
    await cartFormRef.value!.validate()
  } catch {
    ElMessage.warning('请填写完整信息')
    return
  }

  addToCartLoading.value = true
  try {
    const realQuantity = currentConfig.value.title === '助餐服务' ? cartForm.value.quantity : 1

    await cartStore.addItem({
      serviceId: currentService.value.id,
      serviceName: currentService.value.name,
      servicePrice: currentService.value.price,
      quantity: realQuantity,
      serviceType: currentService.value.type,
      serviceDate: cartForm.value.serviceDate,
      serviceTime: cartForm.value.serviceTime,
      address: cartForm.value.address,
      remark: cartForm.value.remark
    })

    addToCartVisible.value = false
    ElMessage.success('加入购物车成功')
  } catch (err) {
    ElMessage.error('加入购物车失败')
    console.error(err)
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
  width: 100%;
}
.date-buttons .el-button,
.time-buttons .el-button {
  width: 100%;
  height: 50px;
  font-size: 18px !important;
  display: flex;
  justify-content: center;
  align-items: center;
  box-sizing: border-box;
  padding: 0 !important;
  margin: 0 !important;
  border: 1px solid #dcdfe6;
}
.date-buttons .el-button.is-active,
.time-buttons .el-button.is-active {
  border: 1px solid #409eff;
}
</style>
