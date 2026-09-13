<template>
  <div class="canvas-wrapper">
    <div class="canvas-chain">
      <!-- 开始节点（内置，发起人） -->
      <div class="flow-node node-start">
        <el-icon><VideoPlay /></el-icon>
        <span class="node-title">开始</span>
        <span class="node-sub">发起人提交</span>
      </div>

      <!-- 递归拓扑链：审批节点与条件分支网关可任意嵌套 -->
      <FlowChain :chain="nodes" />

      <!-- 结束节点（内置） -->
      <div class="flow-node node-end">
        <el-icon><Flag /></el-icon>
        <span class="node-title">结束</span>
        <span class="node-sub">流程完结</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { VideoPlay, Flag } from '@element-plus/icons-vue'
import FlowChain from './FlowChain.vue'

defineProps({
  // 主链节点数组（与分支内 nodes 同构，可就地增删）
  nodes: { type: Array, default: () => [] }
})
</script>

<style scoped>
.canvas-wrapper {
  display: flex;
  justify-content: center;
  padding: 28px 16px 40px;
  overflow: auto;
}

.canvas-chain {
  display: flex;
  flex-direction: column;
  align-items: center;
}

/* ---------- 开始 / 结束 ---------- */
.flow-node {
  user-select: none;
}

.node-start,
.node-end {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 2px;
  width: 96px;
  padding: 10px 0;
  border-radius: 24px;
  color: #fff;
}

.node-start {
  background: linear-gradient(135deg, #67c23a, #4eaa2f);
}

.node-end {
  background: linear-gradient(135deg, #909399, #73767a);
}

.node-title {
  font-size: 14px;
  font-weight: 600;
  line-height: 20px;
}

.node-sub {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.82);
}
</style>
