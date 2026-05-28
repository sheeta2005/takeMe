/**
 * 登录成功返回结果（后端返回给前端）
 */
export interface LoginResult {
  token: string                // 登录凭证（必传）
  userId?: number              // 老人ID
  volunteerId?: number         // 志愿者ID
  adminId?: number             // 管理员ID
  realName: string             // 姓名
  userType: 0 | 1 | 2          // 身份类型
  avatar: string               // 头像
}
