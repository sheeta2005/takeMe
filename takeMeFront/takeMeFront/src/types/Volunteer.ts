/**
 * 志愿者信息
 */
export interface Volunteer {
  // 系统
  id: number                         // 主键ID
  status: 0 | 1                      // 0=停用 1=启用
  createTime: string                 // 创建时间
  lastLoginTime?: string             // 最后登录时间

  // 基础账号
  realName: string                    // 真实姓名
  username: string                    // 登录账号
  phone: string                       // 手机号
  password?: string                   // 密码
  avatar: string                      // 头像

  // 个人信息
  gender: number        // 性别 0 :男， 1 ：女
  age: number                         // 年龄
  address: string                     // 居住地址

  // 服务信息
  serviceDays: number                 // 服务日期  每周的周天：0 周一：1周二：2周三：3周四：4周五：5周六：6七选一分别对应
  serviceType: 0 | 1 | 2 | 3         // 0=代购 1=助洁 2=助餐 3=助医
  workStatus: 0 | 1 | 2              // 0=休息中 1=待命中 2=服务中
  totalServiceHours: number           // 累计服务时长

  // 紧急联系人
  emergencyName: string               // 紧急联系人姓名
  emergencyPhone: string              // 紧急联系人电话
}
