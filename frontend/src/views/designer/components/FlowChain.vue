<template>
  <div class="flow-chain">
    <AddLink v-if="chain.length === 0" @add="(type) => actions.addNode(chain, 0, type)" />
    <template v-else>
      <template v-for="(node, index) in chain" :key="node._key">
        <AddLink @add="(type) => actions.addNode(chain, index, type)" />

        <ApprovalNode
          v-if="node.type === 'APPROVAL'"
          :node="node"
          :active="actions.activeKey === node._key"
          @select="actions.selectNode(node)"
          @delete="actions.removeNode(chain, index)"
        />
        <GatewayNode
          v-else-if="node.type === 'GATEWAY'"
          :node="node"
          :chain="chain"
          :index="index"
        />
        <div v-else class="node-error">未知节点类型</div>
      </template>

      <AddLink @add="(type) => actions.addNode(chain, chain.length, type)" />
    </template>
  </div>
</template>

<script setup>
import { inject } from 'vue'
import AddLink from './AddLink.vue'
import ApprovalNode from './ApprovalNode.vue'
import GatewayNode from './GatewayNode.vue'

const props = defineProps({
  // 当前作用域的节点数组（根链或某分支的 nodes），就地增删即可响应式更新
  chain: { type: Array, required: true }
})

// 由设计器主页面注入的画布操作集合，递归嵌套的各层链共用同一套动作
const actions = inject('designerActions')
</script>

<style scoped>
.flow-chain {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.node-error {
  color: #f56c6c;
  font-size: 12px;
  padding: 8px;
}
</style>
