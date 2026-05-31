import request from '@/utils/request'

export function volunteerLogin(data: { username: string; password: string }) {
  return request({
    url: '/api/volunteer/login',
    method: 'post',
    data
  })
}

// 志愿者注册
export function volunteerRegister(data: {
  username: string;
  password: string;
  realName: string;
  phone: string;
}) {
  return request({
    url: '/api/volunteer/register',
    method: 'post',
    data
  })
}

// ========================= 任务管理 =========================
/**
 * 获取志愿者待办任务
 */
export function getVolunteerTodo() {
  // 真实后端接口
  // return request({
  //   url: '/api/volunteer/todo',
  //   method: 'get'
  // })

  // 模拟数据
  return Promise.resolve({
    code: 200,
    data: [
      {
        id: 'ORD20260520001',
        serviceType: '助餐服务-营养套餐A',
        status: '待确认',
        serviceTime: '2026-05-21 11:30',
        address: '西安市雁塔区科技二路66号'
      },
      {
        id: 'ORD20260520002',
        serviceType: '助洁服务-日常保洁',
        status: '服务中',
        serviceTime: '2026-05-21 14:00',
        address: '西安市高新区锦业路1号'
      }
    ]
  })
}

/**
 * 确认接单
 * @param orderId 订单ID
 */
export function confirmOrder(orderId: string) {
  // return request({
  //   url: `/api/volunteer/confirm/${orderId}`,
  //   method: 'post'
  // })
  return Promise.resolve({ code: 200, message: '接单成功' })
}

/**
 * 完成服务
 * @param orderId 订单ID
 */
export function completeOrder(orderId: string) {
  // return request({
  //   url: `/api/volunteer/complete/${orderId}`,
  //   method: 'post'
  // })
  return Promise.resolve({ code: 200, message: '服务完成' })
}

// ========================= 个人中心 =========================
/**
 * 获取消息列表
 */
export function getMessageList() {
  // return request({
  //   url: '/api/volunteer/message',
  //   method: 'get'
  // })
  return Promise.resolve({
    code: 200,
    data: [
      { id: 1, title: '新订单提醒', content: '您有新的待接单订单', createTime: '2026-05-21 10:00' },
      { id: 2, title: '系统通知', content: '请按时完成服务', createTime: '2026-05-21 09:30' }
    ]
  })
}

/**
 * 获取服务记录
 */
export function getServiceRecord() {
  // return request({
  //   url: '/api/volunteer/record',
  //   method: 'get'
  // })
  return Promise.resolve({
    code: 200,
    data: [
      { id: 1, serviceType: '助餐服务', time: '2026-05-20 11:30', status: '已完成', score: 5 },
      { id: 2, serviceType: '助洁服务', time: '2026-05-19 14:00', status: '已完成', score: 5 }
    ]
  })
}

/**
 * 获取积分与薪酬
 */
export function getPoints() {
  // return request({
  //   url: '/api/volunteer/points',
  //   method: 'get'
  // })
  return Promise.resolve({
    code: 200,
    data: {
      totalPoints: 1250,
      serviceCount: 25,
      money: 125
    }
  })
}

/**
 * 提交请假申请
 */
export function submitLeave(data: any) {
  // return request({
  //   url: '/api/volunteer/leave',
  //   method: 'post',
  //   data
  // })
  return Promise.resolve({ code: 200, message: '请假提交成功' })
}

/**
 * 获取学习规范列表
 */
export function getStudyList() {
  // return request({
  //   url: '/api/volunteer/study',
  //   method: 'get'
  // })
  return Promise.resolve({
    code: 200,
    data: [
      { id: 1, title: '志愿者服务行为规范', createTime: '2026-01-01' },
      { id: 2, title: '安全服务注意事项', createTime: '2026-01-05' }
    ]
  })
}

/**
 * 获取志愿者个人信息
 */
export function getVolunteerInfo() {
  // return request({
  //   url: '/api/volunteer/info',
  //   method: 'get'
  // })
  return Promise.resolve({
    code: 200,
    data: {
      username: 'volunteer01',
      realName: '张三',
      phone: '13800138000',
      idNo: '6101111990XXXX1234',
      avatar: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'
    }
  })
}

/**
 * 修改志愿者信息
 */
export function updateVolunteerInfo(data: any) {
  // return request({
  //   url: '/api/volunteer/update',
  //   method: 'post',
  //   data
  // })
  return Promise.resolve({ code: 200, message: '修改成功' })
}
