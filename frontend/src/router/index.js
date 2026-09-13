import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', public: true }
  },
  {
    path: '/',
    component: () => import('@/layout/index.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '工作台', icon: 'Odometer' }
      },
      {
        path: 'dept',
        name: 'DeptManage',
        component: () => import('@/views/dept/index.vue'),
        meta: { title: '部门管理', icon: 'OfficeBuilding' }
      },
      {
        path: 'user',
        name: 'UserManage',
        component: () => import('@/views/user/index.vue'),
        meta: { title: '人员管理', icon: 'User', roles: ['ADMIN'] }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '页面不存在', public: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 全局前置守卫：未登录跳转登录页，并携带回跳地址
router.beforeEach(async (to, from, next) => {
  document.title = to.meta.title
    ? `${to.meta.title} - 综合工作流审批系统`
    : '综合工作流审批系统'

  const authStore = useAuthStore()

  if (to.meta.public) {
    // 已登录用户访问登录页时直接进入首页
    if (to.name === 'Login' && authStore.isLoggedIn) {
      return next({ path: '/' })
    }
    return next()
  }

  if (!authStore.isLoggedIn) {
    return next({ path: '/login', query: { redirect: to.fullPath } })
  }

  // 本地有令牌但刷新后无用户信息，向后端校验令牌并拉取用户信息
  if (!authStore.user) {
    try {
      await authStore.fetchUserInfo()
    } catch (e) {
      // 令牌无效或后端不可达：必须先清除本地令牌再跳登录页，
      // 否则登录页守卫会把“仍登录”的用户踢回首页，首页再次请求 info，形成重定向死循环
      authStore.clearAuth()
      return next({ path: '/login', query: { redirect: to.fullPath } })
    }
  }

  // 角色级路由限制（后端接口同样强校验）
  if (to.meta.roles && !to.meta.roles.includes(authStore.role)) {
    return next({ path: '/' })
  }

  next()
})

export default router
