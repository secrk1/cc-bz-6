<template>
  <div class="login-container">
    <div class="login-card">
      <div class="login-banner">
        <div class="banner-content">
          <h1>综合工作流审批系统</h1>
          <p>高效协同 · 规范审批 · 流程可视</p>
        </div>
      </div>

      <div class="login-form-wrapper">
        <el-form
          ref="loginFormRef"
          :model="loginForm"
          :rules="loginRules"
          size="large"
          class="login-form"
          @keyup.enter="handleLogin"
        >
          <h2 class="form-title">欢迎登录</h2>

          <el-form-item prop="username">
            <el-input
              v-model="loginForm.username"
              placeholder="请输入用户名"
              :prefix-icon="User"
              clearable
              autocomplete="username"
            />
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              :prefix-icon="Lock"
              show-password
              autocomplete="current-password"
            />
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              class="login-button"
              :loading="loading"
              @click="handleLogin"
            >
              登 录
            </el-button>
          </el-form-item>

          <p class="login-tip">内置管理员账号：admin / admin123</p>
        </el-form>
      </div>
    </div>

    <p class="copyright">© 2026 综合工作流审批系统</p>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { User, Lock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const loginFormRef = ref()
const loading = ref(false)

const loginForm = reactive({
  username: '',
  password: ''
})

const loginRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 64, message: '用户名长度需在 3-64 个字符之间', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 64, message: '密码长度需在 6-64 个字符之间', trigger: 'blur' }
  ]
}

const handleLogin = async () => {
  if (!loginFormRef.value) return
  try {
    await loginFormRef.value.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    await authStore.login({ ...loginForm })
    ElMessage.success('登录成功')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    router.replace(redirect)
  } catch (e) {
    // 错误提示已由响应拦截器统一弹出
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  min-height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1f3a8a 0%, #2563eb 55%, #3b82f6 100%);
  padding: 24px;
}

.login-card {
  display: flex;
  width: 860px;
  max-width: 100%;
  min-height: 460px;
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 18px 50px rgba(0, 0, 0, 0.25);
}

.login-banner {
  flex: 1;
  background: linear-gradient(160deg, #2563eb 0%, #1e40af 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.banner-content {
  color: #fff;
  text-align: center;
}

.banner-content h1 {
  font-size: 26px;
  margin: 0 0 14px;
  letter-spacing: 2px;
}

.banner-content p {
  margin: 0;
  font-size: 14px;
  opacity: 0.85;
  letter-spacing: 1px;
}

.login-form-wrapper {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.login-form {
  width: 100%;
  max-width: 320px;
}

.form-title {
  margin: 0 0 28px;
  font-size: 22px;
  color: #1f2937;
  text-align: center;
}

.login-button {
  width: 100%;
}

.login-tip {
  margin: 8px 0 0;
  font-size: 12px;
  color: #9ca3af;
  text-align: center;
}

.copyright {
  margin-top: 24px;
  color: rgba(255, 255, 255, 0.75);
  font-size: 12px;
}

@media (max-width: 768px) {
  .login-card {
    width: 100%;
  }
  .login-banner {
    display: none;
  }
}
</style>
