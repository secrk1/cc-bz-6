import request from './request'

/** 审批人类别字典（与后端 ProcessNode.approverType 对齐） */
export const APPROVER_TYPES = [
  { value: 'USER', label: '指定人员', desc: '由选定的具体人员审批' },
  { value: 'DEPT_LEADER', label: '部门负责人', desc: '由发起人所在部门的负责人审批' },
  { value: 'ROLE_ADMIN', label: '管理员', desc: '由系统管理员审批' }
]

export const approverTypeLabel = (value) =>
  APPROVER_TYPES.find((item) => item.value === value)?.label || value || '未设置'

/** 回显流程画布草稿（后端在首次访问时自动创建仅含开始/结束的默认草稿） */
export function getProcessDraft() {
  return request.get('/api/process-designer/draft')
}

/** 保存流程画布草稿（流程主信息 + 主干审批链全量覆盖） */
export function saveProcessDraft(data) {
  return request.put('/api/process-designer/draft', data)
}

/** 审批人候选：复用人员管理的启用用户选项接口（仅管理员可调用） */
export function getApproverOptions() {
  return request.get('/api/users/options')
}
