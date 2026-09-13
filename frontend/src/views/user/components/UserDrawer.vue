<template>
  <el-drawer
    :model-value="modelValue"
    :title="isEdit ? '编辑人员' : '新增人员'"
    size="460px"
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
      <el-form-item label="用户名" prop="username">
        <el-input
          v-model="form.username"
          placeholder="登录账号，3-64位字母/数字/下划线"
          maxlength="64"
          :disabled="isEdit"
        />
      </el-form-item>

      <el-form-item label="密码" prop="password">
        <el-input
          v-model="form.password"
          type="password"
          show-password
          maxlength="64"
          :placeholder="isEdit ? '留空表示不修改密码' : '6-64位，至少6位'"
        />
      </el-form-item>

      <el-form-item label="昵称" prop="nickname">
        <el-input v-model="form.nickname" placeholder="请输入昵称（选填）" maxlength="64" />
      </el-form-item>

      <el-form-item label="手机号" prop="phone">
        <el-input v-model="form.phone" placeholder="请输入手机号（选填）" maxlength="11" />
      </el-form-item>

      <el-form-item label="邮箱" prop="email">
        <el-input v-model="form.email" placeholder="请输入邮箱（选填）" maxlength="128" />
      </el-form-item>

      <el-form-item label="角色" prop="role">
        <el-select v-model="form.role" placeholder="请选择角色" style="width: 100%">
          <el-option label="管理员" value="ADMIN" />
          <el-option label="员工" value="USER" />
        </el-select>
      </el-form-item>

      <el-form-item label="所属部门" prop="deptId">
        <el-tree-select
          v-model="form.deptId"
          :data="deptTree"
          :props="{ label: 'deptName', value: 'id', children: 'children' }"
          node-key="id"
          placeholder="请选择部门（可暂不分配）"
          clearable
          check-strictly
          default-expand-all
          style="width: 100%"
        />
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
import { createUser, updateUser } from '@/api/user'
import { getDeptTree } from '@/api/dept'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  // create / edit
  mode: { type: String, default: 'create' },
  // 编辑时的当前行
  user: { type: Object, default: null }
})

const emit = defineEmits(['update:modelValue', 'success'])

const formRef = ref()
const submitting = ref(false)
const deptTree = ref([])

const isEdit = computed(() => props.mode === 'edit')

const defaultForm = () => ({
  id: undefined,
  username: '',
  password: '',
  nickname: '',
  phone: '',
  email: '',
  role: 'USER',
  deptId: null,
  status: 1
})

const form = reactive(defaultForm())

const validatePhone = (rule, value, callback) => {
  if (!value) return callback()
  if (!/^1[3-9]\d{9}$/.test(value)) {
    return callback(new Error('手机号格式不正确'))
  }
  callback()
}

const validatePassword = (rule, value, callback) => {
  if (isEdit.value) {
    // 编辑：留空不改；填写则需 6-64
    if (value && (value.length < 6 || value.length > 64)) {
      return callback(new Error('密码长度需在 6-64 个字符之间'))
    }
    return callback()
  }
  if (!value) return callback(new Error('请输入密码'))
  if (value.length < 6 || value.length > 64) {
    return callback(new Error('密码长度需在 6-64 个字符之间'))
  }
  callback()
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 64, message: '用户名长度需在 3-64 个字符之间', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]+$/, message: '用户名只能包含字母、数字和下划线', trigger: 'blur' }
  ],
  password: [{ validator: validatePassword, trigger: 'blur' }],
  phone: [{ validator: validatePhone, trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

watch(
  () => props.modelValue,
  async (visible) => {
    if (!visible) return

    Object.assign(form, defaultForm())
    if (isEdit.value && props.user) {
      Object.assign(form, {
        id: props.user.id,
        username: props.user.username,
        password: '',
        nickname: props.user.nickname || '',
        phone: props.user.phone || '',
        email: props.user.email || '',
        role: props.user.role,
        deptId: props.user.deptId ?? null,
        status: props.user.status ?? 1
      })
    }
    formRef.value?.clearValidate()
    try {
      deptTree.value = (await getDeptTree()) || []
    } catch (e) {
      // 拦截器已提示
    }
  }
)

const handleSubmit = async () => {
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    const payload = { ...form }
    // 编辑时密码留空则不传，后端保持原密码
    if (isEdit.value && !payload.password) {
      delete payload.password
    }
    if (isEdit.value) {
      await updateUser(payload)
      ElMessage.success('人员更新成功')
    } else {
      await createUser(payload)
      ElMessage.success('人员新增成功')
    }
    emit('success')
    emit('update:modelValue', false)
  } catch (e) {
    // 后端校验信息已由拦截器弹出
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
