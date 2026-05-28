/**
 * 老人用户实体
 */
export interface User {
  id: number                   // 主键ID（统一命名）
  realName: string             // 真实姓名
  username: string             // 登录账号
  phone: string                // 手机号
  password?: string            // 密码
  avatar: string               // 头像

  gender: '男' | '女'          // 性别
  age: number                  // 年龄
  address: string              // 居住地址

  emergencyName: string        // 紧急联系人姓名
  emergencyPhone: string       // 紧急联系人电话

  status: 0 | 1                // 0=禁用 1=正常（后端必须要）
  createTime: string           // 创建时间
  lastLoginTime?: string       // 最后登录时间
}
