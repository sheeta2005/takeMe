import { ElMessage } from 'element-plus'

interface WebSocketMessage {
  type: string
  data: any
  timestamp: string
}

interface OrderStatusChangeData {
  orderId: number
  orderNo: string
  oldStatus: number
  newStatus: number
  userId?: number
  volunteerId?: number
  orderItemId?: number
  message: string
  changeTime: string
}

class WebSocketManager {
  private ws: WebSocket | null = null
  private reconnectTimer: number | null = null
  private heartbeatTimer: number | null = null
  private readonly baseUrl: string
  private userType: string = ''
  private userId: string = ''
  private isConnected: boolean = false

  //初始化实时连接地址
  constructor() {
    this.baseUrl = import.meta.env.VITE_WS_URL || 'ws://localhost:8080'
  }

  //建立当前账号实时连接
  connect(userType: string, userId: string) {
    if (this.userType === userType && this.userId === userId && this.isConnected) {
      console.log('WebSocket已连接，无需重复连接')
      return
    }

    this.disconnect()

    const tokenKey: Record<string, string> = {
      user: 'token',
      volunteer: 'volunteerToken',
      admin: 'adminToken'
    }
    const token = localStorage.getItem(tokenKey[userType])
    if (!token) return

    this.userType = userType
    this.userId = userId
    const url = `${this.baseUrl}/ws/order/${userType}/${userId}?token=${encodeURIComponent(token)}`

    try {
      this.ws = new WebSocket(url)

      this.ws.onopen = () => {
        console.log(`WebSocket连接成功: ${userType}/${userId}`)
        this.isConnected = true
        this.startHeartbeat()
      }

      this.ws.onmessage = (event) => {
        try {
          const message: WebSocketMessage = JSON.parse(event.data)
          this.handleMessage(message)
        } catch (error) {
          console.error('解析WebSocket消息失败:', error)
        }
      }

      this.ws.onerror = (error) => {
        console.error('WebSocket错误:', error)
      }

      this.ws.onclose = () => {
        console.log('WebSocket连接关闭')
        this.isConnected = false
        this.stopHeartbeat()
        this.reconnect()
      }
    } catch (error) {
      console.error('WebSocket连接失败:', error)
      this.reconnect()
    }
  }

  //按消息类型分发通知
  private handleMessage(message: WebSocketMessage) {
    switch (message.type) {
      case 'ORDER_STATUS_CHANGE':
        this.handleOrderStatusChange(message.data as OrderStatusChangeData)
        break
      case 'NEW_MESSAGE':
        ElMessage.info({ message: message.data.title, duration: 5000, showClose: true })
        window.dispatchEvent(new CustomEvent('newMessage', { detail: message.data }))
        break
      default:
        console.warn('未知的WebSocket消息类型:', message.type)
    }
  }

  //提示订单变更并通知页面
  private handleOrderStatusChange(data: OrderStatusChangeData) {
    console.log('收到订单状态变更通知:', data)

    ElMessage.info({
      message: data.message || `订单 ${data.orderNo} 状态变更`,
      duration: 5000,
      showClose: true
    })

    window.dispatchEvent(new CustomEvent('orderStatusChange', {
      detail: data
    }))

    window.dispatchEvent(new CustomEvent(`orderStatusChange_${data.orderId}`, {
      detail: data
    }))
  }

  //启动连接心跳
  private startHeartbeat() {
    this.heartbeatTimer = window.setInterval(() => {
      if (this.ws?.readyState === WebSocket.OPEN) {
        this.ws.send(JSON.stringify({ type: 'ping' }))
      }
    }, 30000)
  }

  //停止连接心跳
  private stopHeartbeat() {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer)
      this.heartbeatTimer = null
    }
  }

  //安排断线重连
  private reconnect() {
    if (this.reconnectTimer) return

    this.reconnectTimer = window.setTimeout(() => {
      console.log('尝试重新连接WebSocket...')
      this.reconnectTimer = null
      if (this.userType && this.userId) {
        this.connect(this.userType, this.userId)
      }
    }, 5000)
  }

  //断开连接并清理定时器
  disconnect() {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
    this.stopHeartbeat()
    if (this.ws) {
      this.ws.close()
      this.ws = null
    }
    this.isConnected = false
    this.userType = ''
    this.userId = ''
  }

  //获取实时连接状态
  getConnectStatus(): boolean {
    return this.isConnected
  }
}

export const wsManager = new WebSocketManager()
export default wsManager
