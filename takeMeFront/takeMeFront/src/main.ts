import { createApp } from 'vue'
import App from './App.vue'
import { createPinia } from 'pinia'
import piniaPersist from 'pinia-plugin-persist'
import router from './router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

const app = createApp(App)

// 1. 初始化 Pinia + 挂载持久化插件
const pinia = createPinia()
pinia.use(piniaPersist)
app.use(pinia)

// 2. 挂载路由
app.use(router)

// 3. 挂载 Element Plus + 中文语言包
app.use(ElementPlus, { locale: zhCn })

// 4. 全局注册所有 Element Plus 图标（必须！）
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 5. 挂载应用
app.mount('#app')
