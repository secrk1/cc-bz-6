<template>
  <div class="dashboard">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>欢迎，{{ authStore.username }}</span>
          <el-tag type="success" effect="plain">{{ roleText }}</el-tag>
        </div>
      </template>
      <p class="welcome-desc">
        登录认证模块已就绪。后续业务模块（流程发起、待办审批、流程定义等）将在此工作台基础上扩展。
      </p>
    </el-card>

    <el-row :gutter="20" class="stat-row">
      <el-col :xs="24" :sm="8">
        <el-card shadow="hover" class="stat-card">
          <el-icon class="stat-icon" color="#409eff"><Document /></el-icon>
          <div class="stat-text">
            <div class="stat-value">0</div>
            <div class="stat-label">我的申请</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-card shadow="hover" class="stat-card">
          <el-icon class="stat-icon" color="#e6a23c"><Bell /></el-icon>
          <div class="stat-text">
            <div class="stat-value">0</div>
            <div class="stat-label">待我审批</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-card shadow="hover" class="stat-card">
          <el-icon class="stat-icon" color="#67c23a"><CircleCheck /></el-icon>
          <div class="stat-text">
            <div class="stat-value">0</div>
            <div class="stat-label">已办结</div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const roleText = computed(() => (authStore.role === 'ADMIN' ? '管理员' : '普通用户'))
</script>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
}

.welcome-desc {
  margin: 0;
  color: #6b7280;
  font-size: 14px;
  line-height: 1.8;
}

.stat-row {
  margin-top: 20px;
}

.stat-card {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
}

.stat-card :deep(.el-card__body) {
  display: flex;
  align-items: center;
  gap: 16px;
  width: 100%;
}

.stat-icon {
  font-size: 42px;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #1f2937;
}

.stat-label {
  font-size: 13px;
  color: #9ca3af;
  margin-top: 4px;
}
</style>
