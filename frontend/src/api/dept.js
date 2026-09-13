import request from './request'

/** 获取部门树 */
export function getDeptTree() {
  return request.get('/api/depts/tree')
}

/** 新增部门 */
export function createDept(data) {
  return request.post('/api/depts', data)
}

/** 编辑部门 */
export function updateDept(data) {
  return request.put('/api/depts', data)
}

/** 删除空部门 */
export function deleteDept(id) {
  return request.delete(`/api/depts/${id}`)
}

/** 用户选项（选择负责人，仅管理员可用） */
export function getUserOptions(deptId) {
  return request.get('/api/users/options', { params: deptId ? { deptId } : undefined })
}
