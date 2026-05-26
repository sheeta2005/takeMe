/**
 * 老人用户信息
 */
export interface user {
  // 基础
  realName: string
  username: string
  phone: string
  password?: string
  avatar: string

  // 信息
  gender: '男' | '女'
  age: number
  address: string

  // 紧急联系人
  emergencyName: string
  emergencyPhone: string

  // 系统
  userId?: number
  createTime?: string
  lastLoginTime?: string
}
