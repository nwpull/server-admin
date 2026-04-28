import http from './request'

export interface LoginParams {
  username: string
  password: string
}

export interface LoginResult {
  token: string
  userInfo: UserInfo
}

export interface UserInfo {
  id: number
  username: string
  nickname: string
  avatar?: string
  role?: string
}

/** 用户登录 */
export function login(data: LoginParams) {
  return http.post<any, LoginResult>('/auth/login', data)
}

/** 获取当前用户信息 */
export function getUserInfo() {
  return http.get<any, UserInfo>('/auth/userinfo')
}

/** 用户登出 */
export function logout() {
  return http.post('/auth/logout')
}

/** 修改密码 */
export function changePassword(data: { oldPassword: string; newPassword: string }) {
  return http.put('/auth/password', data)
}
