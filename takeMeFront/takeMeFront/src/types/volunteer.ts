/**
 * 志愿者信息
 */
export interface volunteer {
  // 基础
  realName: string
  username: string
  phone: string
  password?: string
  avatar: string

  // 服务信息
  serviceDays: string
  serviceType: 0 | 1 | 2 | 3 // 0=代购 1=助洁 2=助餐 3=助医
  workStatus: 0 | 1 | 2       // 0=休息中 1=待命中 2=服务中

  // 信息
  gender: '男' | '女'
  age: number
  address: string

  // 紧急联系人
  emergencyName: string
  emergencyPhone: string

  // 系统
  volunteerId?: number
  totalServiceHours?: number
  createTime?: string
  lastLoginTime?: string
}



