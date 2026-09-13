<template>
  <div class="gateway-block">
    <!-- 分叉头 -->
    <div class="gateway-fork">
      <el-icon class="fork-icon"><Share /></el-icon>
      <span class="fork-title">{{ gatewayTitle }}</span>
      <el-radio-group
        :model-value="node.gatewayMode"
        size="small"
        @change="(mode) => actions.switchGatewayMode(node, mode)"
      >
        <el-radio-button value="PARALLEL">并行</el-radio-button>
        <el-radio-button value="EXCLUSIVE">互斥</el-radio-button>
      </el-radio-group>
      <el-popconfirm
        title="删除该网关将同时删除其下全部分支与节点，确定删除吗？"
        confirm-button-text="删除"
        cancel-button-text="取消"
        @confirm.stop="actions.removeNode(chain, index)"
      >
        <el-icon class="fork-remove" @click.stop><Close /></el-icon>
      </el-popconfirm>
    </div>

    <!-- 分支区（左侧为参与拓扑的分支列，右侧为“添加分支”按钮，不参与连线） -->
    <div class="branches">
      <div class="branch-main">
        <div class="cols-row">
          <div
            v-for="(branch, bi) in node.branches"
            :key="branch._key"
            class="branch-col"
            :class="{ 'drag-over': dragOverIndex === bi, 'dragging': dragIndex === bi }"
            draggable="true"
            @dragstart="onDragStart($event, bi)"
            @dragover.prevent="onDragOver($event, bi)"
            @dragleave="onDragLeave(bi)"
            @drop.prevent="onDrop(bi)"
            @dragend="resetDrag"
          >
            <div class="branch-head">
              <el-icon class="branch-drag-handle" title="拖动调整分支顺序"><Rank /></el-icon>
              <el-input
                v-model="branch.conditionName"
                :placeholder="node.gatewayMode === 'EXCLUSIVE' ? `条件${bi + 1}名称（必填）` : `分支${bi + 1}`"
                size="small"
                class="branch-name-input"
              />
              <el-icon
                v-if="node.branches.length > 2"
                class="branch-remove"
                title="删除分支"
                @click="actions.removeBranch(node, bi)"
              >
                <Close />
              </el-icon>
            </div>

            <el-input
              v-if="node.gatewayMode === 'EXCLUSIVE'"
              v-model="branch.conditionExpr"
              placeholder="条件表达式（选填，如：金额 > 1000）"
              size="small"
              class="branch-expr-input"
            />

            <div class="branch-chain">
              <FlowChain :chain="branch.nodes" />
            </div>
          </div>
        </div>

        <!-- 各分支等高出线后汇聚 -->
        <div class="join-line" />
        <div class="gateway-join">
          <el-icon><CircleCheck /></el-icon>
          <span>合流</span>
        </div>
      </div>

      <button type="button" class="branch-add" @click="actions.addBranch(node)">
        <el-icon><Plus /></el-icon>
        <span>添加<br />分支</span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed, inject, ref } from 'vue'
import { Share, Close, Rank, Plus, CircleCheck } from '@element-plus/icons-vue'
import FlowChain from './FlowChain.vue'

const props = defineProps({
  node: { type: Object, required: true },
  // 网关所在的父链与其下标（整网关删除时使用）
  chain: { type: Array, required: true },
  index: { type: Number, required: true }
})

const actions = inject('designerActions')

const gatewayTitle = computed(() =>
  props.node.gatewayMode === 'PARALLEL' ? '并行分支（同时执行）' : '条件分支（满足其一）'
)

// ---------- 分支拖拽排序（列可拖拽，仅提示用户从手柄发起） ----------
const dragIndex = ref(-1)
const dragOverIndex = ref(-1)

const onDragStart = (event, bi) => {
  // 从输入框发起的拖拽（文本选择）不触发排序
  if (event.target.tagName === 'INPUT' || event.target.tagName === 'TEXTAREA') {
    event.preventDefault()
    return
  }
  dragIndex.value = bi
  event.dataTransfer.effectAllowed = 'move'
  try {
    event.dataTransfer.setData('text/plain', String(bi))
  } catch {
    // 部分浏览器对 setData 敏感，忽略即可
  }
}

const onDragOver = (event, bi) => {
  if (dragIndex.value === -1 || dragIndex.value === bi) return
  event.dataTransfer.dropEffect = 'move'
  dragOverIndex.value = bi
}

const onDragLeave = (bi) => {
  if (dragOverIndex.value === bi) dragOverIndex.value = -1
}

const onDrop = (bi) => {
  if (dragIndex.value !== -1 && dragIndex.value !== bi) {
    actions.moveBranch(props.node, dragIndex.value, bi)
  }
  resetDrag()
}

const resetDrag = () => {
  dragIndex.value = -1
  dragOverIndex.value = -1
}
</script>

<style scoped>
.gateway-block {
  display: flex;
  flex-direction: column;
  align-items: center;
}

/* ---------- 分叉头 ---------- */
.gateway-fork {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  border-radius: 8px;
  background: linear-gradient(135deg, #7c6cf0, #5b8def);
  color: #fff;
  box-shadow: 0 2px 8px rgba(91, 141, 239, 0.3);
  user-select: none;
}

.fork-icon {
  font-size: 18px;
}

.fork-title {
  font-size: 13px;
  font-weight: 600;
}

.fork-remove {
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
  z-index: 2;
}

.gateway-fork:hover .fork-remove {
  display: flex;
}

/* ---------- 分支区 ---------- */
.branches {
  display: flex;
  align-items: flex-start;
  gap: 18px;
  padding: 22px 0 0;
  border-top: 2px solid #a0c4f2;
}

.branch-main {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.cols-row {
  position: relative;
  display: flex;
  align-items: stretch;
  gap: 18px;
  /* 让出各分支底部竖线（18px）+ 汇聚竖线的空间 */
  margin-bottom: 18px;
}

.cols-row::after {
  content: '';
  position: absolute;
  /* 横线位于各分支竖线末端：列宽 264，中点为 132 */
  bottom: -18px;
  left: 132px;
  right: 132px;
  height: 2px;
  background: #a0c4f2;
}

.branch-col {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 264px;
  padding: 0 10px 10px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #f8faff;
  transition: all 0.15s;
}

/* 顶部分叉竖线 */
.branch-col::before {
  content: '';
  position: absolute;
  top: -22px;
  left: 50%;
  width: 2px;
  height: 22px;
  background: #a0c4f2;
}

/* 底部到汇聚横线的竖线（列等高，长度固定） */
.branch-col::after {
  content: '';
  position: absolute;
  bottom: -18px;
  left: 50%;
  width: 2px;
  height: 18px;
  background: #a0c4f2;
}

.branch-col.dragging {
  opacity: 0.45;
}

.branch-col.drag-over {
  border-color: #409eff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
}

.branch-head {
  display: flex;
  align-items: center;
  gap: 6px;
  width: 100%;
  padding: 10px 2px 8px;
}

.branch-drag-handle {
  cursor: grab;
  color: #909399;
  flex-shrink: 0;
}

.branch-drag-handle:active {
  cursor: grabbing;
}

.branch-name-input {
  flex: 1;
  min-width: 0;
}

.branch-remove {
  color: #f56c6c;
  cursor: pointer;
  flex-shrink: 0;
}

.branch-remove:hover {
  color: #e25050;
}

.branch-expr-input {
  width: 100%;
  margin-bottom: 8px;
}

.branch-chain {
  padding: 4px 0 12px;
}

.branch-add {
  align-self: flex-start;
  width: 52px;
  min-height: 120px;
  margin-top: 0;
  border: 1px dashed #a8abb2;
  border-radius: 8px;
  background: #fff;
  color: #909399;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 12px;
  line-height: 1.5;
  transition: all 0.15s;
}

.branch-add:hover {
  border-color: #409eff;
  color: #409eff;
}

/* ---------- 汇聚 ---------- */
.join-line {
  width: 2px;
  height: 16px;
  background: #a0c4f2;
}

.gateway-join {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 22px;
  border-radius: 20px;
  background: #e1f3d8;
  border: 1px solid #b3e19d;
  color: #529b2e;
  font-size: 13px;
  font-weight: 600;
  user-select: none;
}
</style>
