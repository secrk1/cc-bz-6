<template>
  <div class="dept-page">
    <el-card shadow="never">
      <div class="page-toolbar">
        <div class="toolbar-left">
          <span class="page-title">部门管理</span>
          <el-tag v-if="!isAdmin" type="info" effect="plain" size="small">
            当前账号为只读权限，维护操作需管理员
          </el-tag>
        </div>
        <div class="toolbar-right">
          <el-button :icon="Expand" @click="expandAll">展开</el-button>
          <el-button :icon="Fold" @click="collapseAll">折叠</el-button>
          <el-button :icon="Refresh" @click="loadTree">刷新</el-button>
          <el-button
            v-if="isAdmin"
            type="primary"
            :icon="Plus"
            @click="openCreate(null)"
          >
            新增根部门
          </el-button>
        </div>
      </div>

      <el-table
        ref="tableRef"
        v-loading="loading"
        :data="deptTree"
        row-key="id"
        :tree-props="{ children: 'children' }"
        default-expand-all
        border
        :empty-text="'暂无部门数据'"
      >
        <el-table-column prop="deptName" label="部门名称" min-width="240" />
        <el-table-column label="负责人" width="160">
          <template #default="{ row }">
            {{ row.leaderName || '—' }}
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="90" align="center" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.remark || '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <template v-if="isAdmin">
              <el-button link type="primary" :icon="Plus" @click="openCreate(row)">
                新增子部门
              </el-button>
              <el-button link type="primary" :icon="Edit" @click="openEdit(row)">
                编辑
              </el-button>
              <el-button link type="danger" :icon="Delete" @click="handleDelete(row)">
                删除
              </el-button>
            </template>
            <span v-else class="no-permission">—</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <DeptDrawer
      v-model="drawerVisible"
      :mode="drawerMode"
      :dept="drawerDept"
      :parent-name="drawerParentName"
      @success="loadTree"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, Refresh, Expand, Fold } from '@element-plus/icons-vue'
import { getDeptTree, deleteDept } from '@/api/dept'
import { useAuthStore } from '@/stores/auth'
import DeptDrawer from './components/DeptDrawer.vue'

const authStore = useAuthStore()
const isAdmin = computed(() => authStore.role === 'ADMIN')

const tableRef = ref()
const loading = ref(false)
const deptTree = ref([])

const drawerVisible = ref(false)
const drawerMode = ref('create')
const drawerDept = ref(null)

const flatten = (nodes) =>
  nodes.reduce((acc, node) => {
    acc.push(node)
    if (node.children?.length) acc.push(...flatten(node.children))
    return acc
  }, [])

const findNode = (nodes, id) => {
  for (const node of nodes) {
    if (node.id === id) return node
    if (node.children?.length) {
      const hit = findNode(node.children, id)
      if (hit) return hit
    }
  }
  return null
}

const drawerParentName = computed(() => {
  if (drawerMode.value === 'edit') {
    const parentId = drawerDept.value?.parentId
    if (!parentId || parentId === 0) return '顶级部门'
    return findNode(deptTree.value, parentId)?.deptName || '顶级部门'
  }
  // 新增
  return drawerDept.value ? drawerDept.value.deptName : '顶级部门'
})

const loadTree = async () => {
  loading.value = true
  try {
    deptTree.value = (await getDeptTree()) || []
  } catch (e) {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}

const openCreate = (parent) => {
  drawerMode.value = 'create'
  drawerDept.value = parent
  drawerVisible.value = true
}

const openEdit = (row) => {
  drawerMode.value = 'edit'
  drawerDept.value = row
  drawerVisible.value = true
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定删除部门「${row.deptName}」吗？仅无子部门且无关联用户的空部门可删除。`,
      '删除确认',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }

  try {
    await deleteDept(row.id)
    ElMessage.success('删除成功')
    await loadTree()
  } catch (e) {
    // 关联校验错误信息已由拦截器弹出
  }
}

const setExpansion = (expanded) => {
  flatten(deptTree.value).forEach((row) => {
    tableRef.value?.toggleRowExpansion(row, expanded)
  })
}
const expandAll = () => setExpansion(true)
const collapseAll = () => setExpansion(false)

onMounted(loadTree)
</script>

<style scoped>
.page-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
}

.no-permission {
  color: #c0c4cc;
}
</style>
