import request from './request'

/** 账号密码登录 */
export function login(data) {
  return request.post('/api/auth/login', data)
}

/** 令牌校验 / 获取当前登录用户信息 */
export function getUserInfo() {
  return request.get('/api/auth/info')
}

/** 登出 */
export function logout() {
  return request.post('/api/auth/logout')
}
