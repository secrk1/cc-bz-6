<template>
  <el-drawer
    :model-value="modelValue"
    :title="isEdit ? '编辑部门' : '新增部门'"
    size="420px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="92px"
      @keyup.enter="handleSubmit"
    >
      <el-form-item label="上级部门">
        <el-input :model-value="parentName" disabled />
      </el-form-item>

      <el-form-item label="部门名称" prop="deptName">
        <el-input v-model="form.deptName" placeholder="请输入部门名称" maxlength="64" show-word-limit />
      </el-form-item>

      <el-form-item label="负责人" prop="leaderId">
        <el-select
          v-model="form.leaderId"
          placeholder="请选择负责人（可暂不设置）"
          clearable
          filterable
          style="width: 100%"
          :loading="optionsLoading"
        >
          <el-option
            v-for="user in userOptions"
            :key="user.id"
            :label="userLabel(user)"
            :value="user.id"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="显示排序" prop="sort">
        <el-input-number v-model="form.sort" :min="0" :max="9999" controls-position="right" />
      </el-form-item>

      <el-form-item label="状态" prop="status">
        <el-switch
          v-model="form.status"
          :active-value="1"
          :inactive-value="0"
          active-text="启用"
          inactive-text="禁用"
        />
      </el-form-item>

      <el-form-item label="备注" prop="remark">
        <el-input
          v-model="form.remark"
          type="textarea"
          :rows="3"
          maxlength="255"
          show-word-limit
          placeholder="请输入备注（选填）"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="drawer-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </div>
    </template>
  </el-drawer>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { createDept, updateDept, getUserOptions } from '@/api/dept'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  // create / edit
  mode: { type: String, default: 'create' },
  // 新增时的挂载父节点：null 表示根部门；编辑时为当前部门
  dept: { type: Object, default: null },
  // 父部门名称（由父组件计算传入）
  parentName: { type: String, default: '顶级部门' }
})

const emit = defineEmits(['update:modelValue', 'success'])

const formRef = ref()
const submitting = ref(false)
const optionsLoading = ref(false)
const userOptions = ref([])

const isEdit = computed(() => props.mode === 'edit')

const defaultForm = () => ({
  id: undefined,
  parentId: 0,
  deptName: '',
  leaderId: null,
  sort: 0,
  status: 1,
  remark: ''
})

const form = reactive(defaultForm())

const rules = {
  deptName: [
    { required: true, message: '请输入部门名称', trigger: 'blur' },
    { min: 1, max: 64, message: '部门名称最长 64 个字符', trigger: 'blur' }
  ],
  sort: [{ type: 'number', message: '排序必须为数字', trigger: 'change' }]
}

const userLabel = (user) => {
  const name = user.nickname || user.username
  return user.nickname ? `${user.nickname}（${user.username}）` : name
}

watch(
  () => props.modelValue,
  async (visible) => {
    if (!visible) return

    Object.assign(form, defaultForm())
    if (isEdit.value && props.dept) {
      Object.assign(form, {
        id: props.dept.id,
        parentId: props.dept.parentId,
        deptName: props.dept.deptName,
        leaderId: props.dept.leaderId ?? null,
        sort: props.dept.sort ?? 0,
        status: props.dept.status ?? 1,
        remark: props.dept.remark ?? ''
      })
    } else if (props.dept) {
      // 在指定节点下新增子部门
      form.parentId = props.dept.id
    }

    formRef.value?.clearValidate()
    await loadUserOptions()
  }
)

const loadUserOptions = async () => {
  optionsLoading.value = true
  try {
    userOptions.value = await getUserOptions()
  } catch (e) {
    // 错误提示由拦截器统一处理
  } finally {
    optionsLoading.value = false
  }
}

const handleSubmit = async () => {
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    if (isEdit.value) {
      await updateDept({ ...form })
      ElMessage.success('部门更新成功')
    } else {
      await createDept({ ...form })
      ElMessage.success('部门新增成功')
    }
    emit('success')
    emit('update:modelValue', false)
  } catch (e) {
    // 后端重名等校验信息已由拦截器弹出
  } finally {
    submitting.value = false
  }
}

const handleClose = () => {
  emit('update:modelValue', false)
}
</script>

<style scoped>
.drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
