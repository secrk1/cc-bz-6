<template>
  <div class="flow-link">
    <div class="link-line" />
    <el-dropdown trigger="click" placement="right" @command="(cmd) => emit('add', cmd)">
      <button type="button" class="link-add" title="添加节点">
        <el-icon><Plus /></el-icon>
      </button>
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item command="APPROVAL" :icon="Avatar">审批节点</el-dropdown-item>
          <el-dropdown-item command="GATEWAY" :icon="Share">条件分支网关</el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>
    <div class="link-line" />
  </div>
</template>

<script setup>
import { Plus, Avatar, Share } from '@element-plus/icons-vue'

const emit = defineEmits(['add'])
</script>

<style scoped>
.flow-link {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.link-line {
  width: 2px;
  height: 16px;
  background-color: #c0c4cc;
}

.link-add {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  border: 1px dashed #a8abb2;
  background: #fff;
  color: #909399;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  transition: all 0.15s;
}

.link-add:hover {
  border-style: solid;
  border-color: #409eff;
  color: #409eff;
  transform: scale(1.12);
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.25);
}
</style>
