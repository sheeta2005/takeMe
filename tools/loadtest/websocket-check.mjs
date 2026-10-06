import fs from 'node:fs'

// 使用 Node 标准 WebSocket 客户端，不改写协议实现。
const accounts = JSON.parse(fs.readFileSync(process.argv[2], 'utf8'))
const holdSeconds = Number(process.argv[3] || 0)
const stopFile = process.argv[4]
const sockets = []
let heartbeat
try {
  // 分批建立长连接，连接建立本身不计入后续业务吞吐，避免瞬时握手挤满 accept 队列。
  for (let offset = 0; offset < accounts.length; offset += 50) {
  await Promise.all(accounts.slice(offset, offset + 50).map(account => new Promise((resolve, reject) => {
    const socket = new WebSocket(`ws://127.0.0.1:9081/ws/order/user/${account.userId}?token=${account.userToken}`)
    sockets.push(socket)
    const timer = setTimeout(() => reject(new Error('连接建立超时')), 10000)
    socket.addEventListener('open', () => { clearTimeout(timer); resolve() }, { once: true })
    socket.addEventListener('error', () => { clearTimeout(timer); reject(new Error('连接建立失败')) }, { once: true })
  })))
  }
  let result
  // 客户端握手完成略早于服务端登记完成，仍须核实真实在线数达到目标。
  for (let attempt = 0; attempt < 20; attempt++) {
    result = await fetch('http://127.0.0.1:9081/api/admin/online', {
      headers: { Authorization: `Bearer ${accounts[0].adminToken}` }
    }).then(response => response.json())
    if (result.code === 200 && result.data.userCount === accounts.length) break
    await new Promise(resolve => setTimeout(resolve, 250))
  }
  if (result.code !== 200 || result.data.userCount !== accounts.length) {
    throw new Error(`在线统计不一致：${JSON.stringify(result)}`)
  }
  console.log(JSON.stringify({ 连接数: sockets.length, 统计: result.data }))
  if (holdSeconds > 0) {
    heartbeat = setInterval(() => {
      for (const socket of sockets) {
        if (socket.readyState === WebSocket.OPEN) socket.send(JSON.stringify({ type: 'ping' }))
      }
    }, 30000)
    const deadline = Date.now() + holdSeconds * 1000
    while (Date.now() < deadline && (!stopFile || !fs.existsSync(stopFile))) {
      await new Promise(resolve => setTimeout(resolve, 1000))
      if (sockets.some(socket => socket.readyState !== WebSocket.OPEN)) throw new Error('推送连接在持续测量中断开')
    }
  }
} finally {
  clearInterval(heartbeat)
  for (const socket of sockets) socket.close()
}
await new Promise(resolve => setTimeout(resolve, 1000))
const offline = await fetch('http://127.0.0.1:9081/api/admin/online', {
  headers: { Authorization: `Bearer ${accounts[0].adminToken}` }
}).then(response => response.json())
if (offline.data?.userCount !== 0) throw new Error('断开后仍残留在线人数')
console.log('连接释放后在线人数归零')
