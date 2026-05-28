/**
 * 管理员账号实体
 */
export interface Admin {
  id: number
  username: string
  password: string
  realName: string
  createTime: string
  lastLoginTime?: string
}
