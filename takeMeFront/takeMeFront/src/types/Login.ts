export interface  Login {
  username: string        // 账号
  password: string        // 密码
  userType: 0 | 1 | 2     // 0=用户 1=志愿者 2=管理员
  phone?: string          // 可选（短信登录用）
  code?: string           // 可选（验证码）
}
