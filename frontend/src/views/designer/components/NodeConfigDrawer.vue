<template>
  <el-drawer
    :model-value="modelValue"
    title="审批节点配置"
    size="420px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <el-form
      v-if="form"
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="96px"
      @keyup.enter="handleSubmit"
    >
      <el-form-item label="节点名称" prop="nodeName">
        <el-input
          v-model="form.nodeName"
          placeholder="如：部门经理审批"
          maxlength="64"
          show-word-limit
        />
      </el-form-item>

      <el-form-item label="审批人类别" prop="approverType">
        <el-radio-group v-model="form.approverType">
          <el-radio
            v-for="item in APPROVER_TYPES"
            :key="item.value"
            :value="item.value"
            class="approver-radio"
          >
            {{ item.label }}
          </el-radio>
        </el-radio-group>
        <div class="approver-desc">{{ currentTypeDesc }}</div>
      </el-form-item>

      <el-form-item v-if="form.approverType === 'USER'" label="审批人" prop="approverRefId">
        <el-select
          v-model="form.approverRefId"
          placeholder="请选择审批人"
          filterable
          clearable
          style="width: 100%"
          :loading="optionsLoading"
        >
          <el-option
            v-for="user in approverOptions"
            :key="user.id"
            :label="userLabel(user)"
            :value="user.id"
          />
        </el-select>
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="drawer-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </div>
    </template>
  </el-drawer>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { APPROVER_TYPES, getApproverOptions } from '@/api/process'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  // 当前画布上被选中的审批节点（由父组件以响应式对象传入）
  node: { type: Object, default: null }
})

const emit = defineEmits(['update:modelValue', 'confirm'])

const formRef = ref()
const optionsLoading = ref(false)
const approverOptions = ref([])
const form = reactive({
  nodeName: '',
  approverType: 'USER',
  approverRefId: null
})

const rules = {
  nodeName: [
    { required: true, message: '请输入节点名称', trigger: 'blur' },
    { max: 64, message: '节点名称最长 64 个字符', trigger: 'blur' }
  ],
  approverType: [{ required: true, message: '请选择审批人类别', trigger: 'change' }],
  approverRefId: [{ required: true, message: '请选择审批人', trigger: 'change' }]
}

const currentTypeDesc = computed(
  () => APPROVER_TYPES.find((item) => item.value === form.approverType)?.desc || ''
)

const userLabel = (user) =>
  user.nickname ? `${user.nickname}（${user.username}）` : user.username

watch(
  () => props.modelValue,
  async (visible) => {
    if (!visible || !props.node) return

    form.nodeName = props.node.nodeName || ''
    form.approverType = props.node.approverType || 'USER'
    form.approverRefId = props.node.approverRefId ?? null
    formRef.value?.clearValidate()
    await loadApproverOptions()
  }
)

const loadApproverOptions = async () => {
  optionsLoading.value = true
  try {
    approverOptions.value = (await getApproverOptions()) || []
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
  let approverRefName = ''
  if (form.approverType === 'USER' && form.approverRefId) {
    const user = approverOptions.value.find((u) => u.id === form.approverRefId)
    approverRefName = user ? user.nickname || user.username : ''
  }
  emit('confirm', {
    nodeName: form.nodeName.trim(),
    approverType: form.approverType,
    approverRefId: form.approverType === 'USER' ? form.approverRefId : null,
    approverRefName
  })
  emit('update:modelValue', false)
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

.approver-radio {
  display: block;
  margin-right: 0;
  margin-bottom: 6px;
}

.approver-desc {
  font-size: 12px;
  color: #909399;
  line-height: 1.6;
}
</style>
