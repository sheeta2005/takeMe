interface VolunteerInfo {
// 基础
realName: string
username: string
phone: string
password?: string
avatar: string

// 服务
serviceDays: string
serviceType: '代购' | '洁净' | '送餐' | '陪医'
status: string

// 扩展
gender: '男' | '女'
age: number
address: string
emergencyName: string
emergencyPhone: string

// 系统（可选）
volunteerId?: number
totalServiceHours?: number
createTime?: string
lastLoginTime?: string
}