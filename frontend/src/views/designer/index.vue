<template>
  <div v-loading="loading" class="designer-page">
    <el-card shadow="never" class="designer-card">
      <div class="page-toolbar">
        <div class="toolbar-left">
          <el-input
            v-model="processName"
            placeholder="请输入流程名称"
            maxlength="128"
            class="process-name-input"
            @keyup.enter="handleSave"
          />
          <el-tag type="info" effect="plain">
            {{ draftStatus === 'PUBLISHED' ? '已发布' : '草稿' }}
          </el-tag>
        </div>
        <div class="toolbar-right">
          <el-button :icon="Refresh" @click="loadDraft">刷新回显</el-button>
          <el-button type="primary" :icon="Check" :loading="saving" @click="handleSave">
            保存草稿
          </el-button>
        </div>
      </div>

      <div class="canvas-board">
        <ProcessCanvas :nodes="nodes" />
      </div>
    </el-card>

    <NodeConfigDrawer v-model="drawerVisible" :node="editingNode" @confirm="handleConfigConfirm" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, provide } from 'vue'
import { ElMessage } from 'element-plus'
import { Check, Refresh } from '@element-plus/icons-vue'
import ProcessCanvas from './components/ProcessCanvas.vue'
import NodeConfigDrawer from './components/NodeConfigDrawer.vue'
import { getProcessDraft, saveProcessDraft, APPROVER_TYPES } from '@/api/process'

const loading = ref(false)
const saving = ref(false)

const processId = ref(null)
const processName = ref('')
const processRemark = ref('')
const draftStatus = ref('DRAFT')
// 主链节点树（分支内 nodes 与之同构）
const nodes = ref([])

const drawerVisible = ref(false)
const editingNode = ref(null)

// 画布节点客户端临时键：仅用于 v-for 稳定渲染，保存时剥离
let keySeed = 0
const nextKey = () => `k${++keySeed}`

// ---------------- 树 ↔ 接口结构转换 ----------------

const mapNodeFromServer = (n) => {
  if (n.type === 'GATEWAY') {
    return {
      _key: nextKey(),
      id: n.id ?? null,
      joinId: n.joinId ?? null,
      type: 'GATEWAY',
      nodeName: n.nodeName || '',
      gatewayMode: n.gatewayMode || 'PARALLEL',
      branches: (n.branches || []).map((b) => ({
        _key: nextKey(),
        branchId: b.branchId ?? null,
        conditionName: b.conditionName || '',
        conditionExpr: b.conditionExpr || '',
        nodes: (b.nodes || []).map(mapNodeFromServer)
      }))
    }
  }
  return {
    _key: nextKey(),
    id: n.id ?? null,
    type: 'APPROVAL',
    nodeName: n.nodeName || '',
    approverType: n.approverType || 'USER',
    approverRefId: n.approverRefId ?? null,
    approverRefName: n.approverRefName || ''
  }
}

const toNodePayload = (node) => {
  if (node.type === 'GATEWAY') {
    return {
      id: node.id,
      type: 'GATEWAY',
      nodeName: node.nodeName || '',
      gatewayMode: node.gatewayMode,
      branches: node.branches.map((b) => ({
        conditionName: b.conditionName?.trim() || '',
        conditionExpr: node.gatewayMode === 'EXCLUSIVE' ? b.conditionExpr?.trim() || null : null,
        nodes: b.nodes.map(toNodePayload)
      }))
    }
  }
  return {
    id: node.id,
    type: 'APPROVAL',
    nodeName: node.nodeName?.trim() || '',
    approverType: node.approverType,
    approverRefId: node.approverType === 'USER' ? node.approverRefId : null
  }
}

// ---------------- 草稿回显 / 保存 ----------------

const applyDraft = (draft) => {
  processId.value = draft.id
  processName.value = draft.processName || ''
  processRemark.value = draft.remark || ''
  draftStatus.value = draft.status || 'DRAFT'
  nodes.value = (draft.nodes || []).map(mapNodeFromServer)
  designerActions.activeKey = null
}

const loadDraft = async () => {
  loading.value = true
  try {
    applyDraft(await getProcessDraft())
  } catch (e) {
    // 错误提示由拦截器统一处理（非管理员会收到 403）
  } finally {
    loading.value = false
  }
}

/** 递归画布级校验，返回第一条错误信息（同时高亮问题节点） */
const validateTree = (chain) => {
  for (const node of chain) {
    if (node.type === 'APPROVAL') {
      if (!node.nodeName?.trim()) {
        designerActions.activeKey = node._key
        return '存在未配置名称的审批节点'
      }
      if (!APPROVER_TYPES.some((t) => t.value === node.approverType)) {
        designerActions.activeKey = node._key
        return `节点「${node.nodeName}」的审批人类别不合法`
      }
      if (node.approverType === 'USER' && !node.approverRefId) {
        designerActions.activeKey = node._key
        return `节点「${node.nodeName}」需选择指定审批人`
      }
    } else if (node.type === 'GATEWAY') {
      if (!['PARALLEL', 'EXCLUSIVE'].includes(node.gatewayMode)) {
        return '存在网关模式不合法的分支节点'
      }
      if (!node.branches || node.branches.length < 2) {
        return '条件分支至少需要 2 条分支'
      }
      if (node.gatewayMode === 'EXCLUSIVE') {
        for (const b of node.branches) {
          if (!b.conditionName?.trim()) {
            return '互斥网关的每条分支都需要填写条件名称'
          }
        }
      }
      for (const b of node.branches) {
        const childError = validateTree(b.nodes)
        if (childError) return childError
      }
    } else {
      return '存在未知类型的节点'
    }
  }
  return ''
}

const handleSave = async () => {
  if (!processName.value.trim()) {
    ElMessage.warning('请输入流程名称')
    return
  }
  const error = validateTree(nodes.value)
  if (error) {
    ElMessage.warning(error)
    return
  }

  const payload = {
    processName: processName.value.trim(),
    remark: processRemark.value || null,
    nodes: nodes.value.map(toNodePayload)
  }

  saving.value = true
  try {
    // 全量重建：以服务端返回的拓扑为准重新装载（新节点获得行ID、分支锚点ID等）
    applyDraft(await saveProcessDraft(payload))
    drawerVisible.value = false
    editingNode.value = null
    ElMessage.success('草稿保存成功')
  } catch (e) {
    // 403 / 拓扑校验错误信息已由拦截器弹出
  } finally {
    saving.value = false
  }
}

// ---------------- 画布动作（注入给任意深度的递归链/网关组件） ----------------

const createApprovalNode = () => ({
  _key: nextKey(),
  id: null,
  type: 'APPROVAL',
  nodeName: `审批节点`,
  approverType: 'USER',
  approverRefId: null,
  approverRefName: ''
})

const createGatewayNode = () => ({
  _key: nextKey(),
  id: null,
  type: 'GATEWAY',
  nodeName: '',
  gatewayMode: 'PARALLEL',
  branches: [0, 1].map(() => ({
    _key: nextKey(),
    branchId: null,
    conditionName: '',
    conditionExpr: '',
    nodes: []
  }))
})

const openNodeConfig = (node) => {
  designerActions.activeKey = node._key
  editingNode.value = node
  drawerVisible.value = true
}

/** 任意层链共用：chain 为该作用域的真实数组引用，就地修改即响应式更新 */
const designerActions = reactive({
  activeKey: null,

  addNode(chain, index, type) {
    if (type === 'GATEWAY') {
      chain.splice(index, 0, createGatewayNode())
      ElMessage.success('已插入条件分支，可在分叉头切换并行/互斥模式')
      return
    }
    const node = createApprovalNode()
    chain.splice(index, 0, node)
    openNodeConfig(node)
  },

  removeNode(chain, index) {
    const [removed] = chain.splice(index, 1)
    if (removed?.type === 'APPROVAL' && designerActions.activeKey === removed._key) {
      designerActions.activeKey = null
      drawerVisible.value = false
      editingNode.value = null
    }
  },

  selectNode(node) {
    openNodeConfig(node)
  },

  addBranch(gateway) {
    gateway.branches.push({
      _key: nextKey(),
      branchId: null,
      conditionName: '',
      conditionExpr: '',
      nodes: []
    })
  },

  removeBranch(gateway, index) {
    if (gateway.branches.length <= 2) return
    gateway.branches.splice(index, 1)
  },

  moveBranch(gateway, from, to) {
    if (from === to || from < 0 || to < 0 || from >= gateway.branches.length || to >= gateway.branches.length) {
      return
    }
    const [moved] = gateway.branches.splice(from, 1)
    gateway.branches.splice(to, 0, moved)
  },

  switchGatewayMode(gateway, mode) {
    gateway.gatewayMode = mode
    if (mode === 'EXCLUSIVE') {
      // 切换为互斥时为尚无名的分支补占位条件名，便于直接保存
      gateway.branches.forEach((b, i) => {
        if (!b.conditionName?.trim()) b.conditionName = `条件${i + 1}`
      })
    }
  }
})

provide('designerActions', designerActions)

/** 抽屉确认：回写节点名称与审批人类别（节点为树中的活引用，直接改即可） */
const handleConfigConfirm = (config) => {
  const node = editingNode.value
  if (!node) return
  node.nodeName = config.nodeName
  node.approverType = config.approverType
  node.approverRefId = config.approverRefId
  node.approverRefName = config.approverRefName || ''
}

onMounted(loadDraft)
</script>

<style scoped>
.designer-page {
  height: 100%;
}

.designer-card {
  min-height: calc(100vh - 122px);
  display: flex;
  flex-direction: column;
}

:deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.page-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  padding-bottom: 16px;
  border-bottom: 1px solid #ebeef5;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.process-name-input {
  width: 280px;
}

.canvas-board {
  flex: 1;
  margin-top: 8px;
  background: #fafbfc;
  border: 1px dashed #e4e7ed;
  border-radius: 8px;
  overflow: auto;
}
</style>
