<template>
  <div class="user-page">
    <el-card shadow="never">
      <!-- 查询条件 -->
      <el-form :inline="true" :model="query" class="query-form">
        <el-form-item label="关键字">
          <el-input
            v-model="query.keyword"
            placeholder="用户名/昵称/手机号/邮箱"
            clearable
            style="width: 220px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="query.role" placeholder="全部角色" clearable style="width: 130px">
            <el-option label="管理员" value="ADMIN" />
            <el-option label="员工" value="USER" />
          </el-select>
        </el-form-item>
        <el-form-item label="部门">
          <el-tree-select
            v-model="query.deptId"
            :data="deptTree"
            :props="{ label: 'deptName', value: 'id', children: 'children' }"
            node-key="id"
            placeholder="全部部门"
            clearable
            check-strictly
            default-expand-all
            style="width: 180px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="RefreshLeft" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="page-toolbar">
        <el-button type="primary" :icon="Plus" @click="openCreate">新增人员</el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" border>
        <el-table-column type="index" label="#" width="56" align="center" />
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column label="昵称" min-width="120">
          <template #default="{ row }">{{ row.nickname || '—' }}</template>
        </el-table-column>
        <el-table-column label="角色" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.role === 'ADMIN' ? 'warning' : 'info'" size="small">
              {{ row.role === 'ADMIN' ? '管理员' : '员工' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="所属部门" min-width="140">
          <template #default="{ row }">{{ row.deptName || '未分配' }}</template>
        </el-table-column>
        <el-table-column label="手机号" min-width="130">
          <template #default="{ row }">{{ row.phone || '—' }}</template>
        </el-table-column>
        <el-table-column label="邮箱" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.email || '—' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="Edit" @click="openEdit(row)">编辑</el-button>
            <el-button
              link
              type="danger"
              :icon="Delete"
              :disabled="row.id === authStore.user?.id"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="query.current"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <UserDrawer
      v-model="drawerVisible"
      :mode="drawerMode"
      :user="currentRow"
      @success="loadData"
    />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, Search, RefreshLeft } from '@element-plus/icons-vue'
import { pageUsers, deleteUser } from '@/api/user'
import { getDeptTree } from '@/api/dept'
import { useAuthStore } from '@/stores/auth'
import UserDrawer from './components/UserDrawer.vue'

const authStore = useAuthStore()

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const deptTree = ref([])

const query = reactive({
  current: 1,
  size: 10,
  keyword: '',
  role: '',
  deptId: null
})

const drawerVisible = ref(false)
const drawerMode = ref('create')
const currentRow = ref(null)

const loadData = async () => {
  loading.value = true
  try {
    const data = await pageUsers({
      current: query.current,
      size: query.size,
      keyword: query.keyword || undefined,
      role: query.role || undefined,
      deptId: query.deptId || undefined
    })
    tableData.value = data.records || []
    total.value = data.total || 0
  } catch (e) {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}

const loadDeptTree = async () => {
  try {
    deptTree.value = (await getDeptTree()) || []
  } catch (e) {
    // 忽略
  }
}

const handleSearch = () => {
  query.current = 1
  loadData()
}

const handleReset = () => {
  query.keyword = ''
  query.role = ''
  query.deptId = null
  query.current = 1
  loadData()
}

const openCreate = () => {
  drawerMode.value = 'create'
  currentRow.value = null
  drawerVisible.value = true
}

const openEdit = (row) => {
  drawerMode.value = 'edit'
  currentRow.value = row
  drawerVisible.value = true
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定删除人员「${row.nickname || row.username}」吗？`, '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }

  try {
    await deleteUser(row.id)
    ElMessage.success('删除成功')
    // 删除最后一页唯一一条时回退一页
    if (tableData.value.length === 1 && query.current > 1) {
      query.current -= 1
    }
    await loadData()
  } catch (e) {
    // 负责人关联等校验信息已由拦截器弹出
  }
}

onMounted(() => {
  loadDeptTree()
  loadData()
})
</script>

<style scoped>
.query-form {
  margin-bottom: 4px;
}

.page-toolbar {
  margin: 8px 0 16px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
