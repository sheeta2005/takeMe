import fs from 'node:fs'
import crypto from 'node:crypto'

const account = JSON.parse(fs.readFileSync(process.argv[2], 'utf8'))[0]
const headers = { Authorization: `Bearer ${account.userToken}`, 'Content-Type': 'application/json' }
const socket = new WebSocket(`ws://127.0.0.1:9081/ws/order/user/${account.userId}?token=${account.userToken}`)
let expectedOrderId
let resolveNotification
const notification = new Promise(resolve => { resolveNotification = resolve })
socket.addEventListener('message', event => {
  const message = JSON.parse(event.data)
  if (message.type === 'ORDER_STATUS_CHANGE' && message.data.orderId === expectedOrderId
      && message.data.newStatus === 5) resolveNotification(message)
})
const call = async (path, body) => {
  const response = await fetch(`http://127.0.0.1:9081${path}`, {
    method: body === undefined ? 'GET' : 'POST', headers,
    body: body === undefined ? undefined : JSON.stringify(body)
  }).then(result => result.json())
  if (response.code !== 200) throw new Error(`业务失败：${path}，${response.msg}`)
  return response.data
}
try {
  await new Promise((resolve, reject) => {
    socket.addEventListener('open', resolve, { once: true })
    socket.addEventListener('error', () => reject(new Error('推送连接失败')), { once: true })
  })
  const booking = new Date(Date.now() + 3 * 3600000)
  const date = `${booking.getFullYear()}-${String(booking.getMonth() + 1).padStart(2, '0')}-${String(booking.getDate()).padStart(2, '0')}`
  const time = `${String(booking.getHours()).padStart(2, '0')}:${String(booking.getMinutes()).padStart(2, '0')}`
  const order = await call('/api/user/order/create', {
    order: { requestId: crypto.randomUUID() },
    items: [{ serviceId: 3, quantity: 1, serviceDate: date, serviceTime: time, address: '推送功能合成验证地址' }]
  })
  expectedOrderId = order.id
  await call('/api/user/payment/mock', { orderId: order.id })
  await call(`/api/user/order/cancel?orderId=${order.id}`, {})
  // 只做功能验证，不把这次观测当作全体用户的推送延迟分位数。
  const timer = setTimeout(() => resolveNotification(null), 10000)
  const pushed = await notification
  clearTimeout(timer)
  if (!pushed) throw new Error('未收到取消提醒')
  const messages = await call('/api/user/message/list?pageNum=1&pageSize=50')
  if (!messages.records.some(message => message.relatedOrderId === order.id && message.title === '用户取消订单')) {
    throw new Error('推送到达时未找到已经落库的通知')
  }
  console.log(`订单 ${order.id} 取消提醒已推送，消息列表已持久化`)
} finally {
  socket.close()
}
