<template>
  <div
    class="flow-node node-approval"
    :class="{ 'is-active': active }"
    @click="emit('select')"
  >
    <el-icon class="approval-icon"><Avatar /></el-icon>
    <div class="node-body">
      <div class="node-title">{{ node.nodeName || '未命名审批节点' }}</div>
      <div class="node-sub">
        <el-tag size="small" type="primary" effect="plain">
          {{ approverTypeLabel(node.approverType) }}
        </el-tag>
        <span v-if="node.approverType === 'USER'" class="approver-name">
          {{ node.approverRefName || '未选择审批人' }}
        </span>
      </div>
    </div>
    <el-icon class="node-config"><Setting /></el-icon>
    <el-popconfirm
      title="确定删除该审批节点吗？删除后其后的节点顺序自动前移。"
      confirm-button-text="删除"
      cancel-button-text="取消"
      @confirm.stop="emit('delete')"
    >
      <template #reference>
        <el-icon class="node-remove" @click.stop><Close /></el-icon>
      </template>
    </el-popconfirm>
  </div>
</template>

<script setup>
import { Close, Avatar, Setting } from '@element-plus/icons-vue'
import { approverTypeLabel } from '@/api/process'

defineProps({
  node: { type: Object, required: true },
  active: { type: Boolean, default: false }
})

const emit = defineEmits(['select', 'delete'])
</script>

<style scoped>
.flow-node {
  position: relative;
  width: 240px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  border-radius: 8px;
  user-select: none;
}

.node-approval {
  background: #fff;
  border: 1px solid #d9ecff;
  box-shadow: 0 2px 6px rgba(31, 45, 61, 0.06);
  cursor: pointer;
  transition: all 0.15s;
}

.node-approval:hover {
  border-color: #409eff;
  box-shadow: 0 4px 14px rgba(64, 158, 255, 0.18);
}

.node-approval.is-active {
  border-color: #409eff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
}

.approval-icon {
  font-size: 20px;
  color: #409eff;
  flex-shrink: 0;
}

.node-body {
  flex: 1;
  min-width: 0;
}

.node-title {
  font-size: 14px;
  font-weight: 600;
  line-height: 20px;
  color: #1f2937;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.node-sub {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

.approver-name {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.node-config {
  color: #c0c4cc;
  flex-shrink: 0;
}

.node-remove {
  position: absolute;
  top: -8px;
  right: -8px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #f56c6c;
  color: #fff;
  font-size: 12px;
  display: none;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 1;
}

.node-approval:hover .node-remove {
  display: flex;
}

.node-remove:hover {
  background: #e25050;
}
</style>
