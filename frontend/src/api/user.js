import request from './request'

/** 人员分页查询 */
export function pageUsers(params) {
  return request.get('/api/users/page', { params })
}

/** 新增人员 */
export function createUser(data) {
  return request.post('/api/users', data)
}

/** 编辑人员 */
export function updateUser(data) {
  return request.put('/api/users', data)
}

/** 删除人员 */
export function deleteUser(id) {
  return request.delete(`/api/users/${id}`)
}
