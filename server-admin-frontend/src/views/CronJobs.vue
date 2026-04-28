<template>
  <div class="cron-page">
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">定时任务</h2>
        <el-select
          v-model="selectedServerId"
          placeholder="选择服务器"
          style="width: 200px"
          @change="handleServerChange"
        >
          <el-option
            v-for="server in serverStore.servers"
            :key="server.id"
            :label="server.name"
            :value="server.id"
          />
        </el-select>
      </div>
      <div class="header-right">
        <el-button type="primary" icon="Plus" @click="handleAdd">添加任务</el-button>
        <el-button icon="Refresh" @click="loadCronJobs">刷新</el-button>
      </div>
    </div>

    <el-table :data="cronJobs" stripe style="width: 100%">
      <el-table-column type="index" label="序号" width="70" />
      <el-table-column prop="expression" label="表达式" width="120">
        <template #default="{ row }">
          <el-tag size="small" type="info">{{ row.expression }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="command" label="命令" min-width="300" show-overflow-tooltip />
      <el-table-column prop="comment" label="备注" min-width="150" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.comment || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="user" label="用户" width="100">
        <template #default="{ row }">
          {{ row.user || 'root' }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-switch
            :model-value="row.enabled"
            size="small"
            @change="(val: boolean) => handleToggle(row, val)"
          />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
          <el-popconfirm
            title="确定要删除此定时任务吗？"
            confirm-button-text="确定"
            cancel-button-text="取消"
            @confirm="handleDelete(row)"
          >
            <template #reference>
              <el-button type="danger" link size="small">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <!-- 添加/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑定时任务' : '添加定时任务'"
      width="560px"
      destroy-on-close
    >
      <el-form
        ref="cronFormRef"
        :model="cronForm"
        :rules="cronFormRules"
        label-width="80px"
      >
        <el-form-item label="表达式" prop="expression">
          <el-input v-model="cronForm.expression" placeholder="如: */5 * * * *" />
        </el-form-item>
        <el-form-item label="快捷选择">
          <div class="quick-expressions">
            <el-tag
              v-for="expr in commonCronExpressions"
              :key="expr.value"
              class="quick-tag"
              :type="cronForm.expression === expr.value ? 'primary' : 'info'"
              size="small"
              @click="cronForm.expression = expr.value"
            >
              {{ expr.label }}
            </el-tag>
          </div>
        </el-form-item>
        <el-form-item label="命令" prop="command">
          <el-input
            v-model="cronForm.command"
            type="textarea"
            :rows="3"
            placeholder="请输入要执行的命令"
          />
        </el-form-item>
        <el-form-item label="备注" prop="comment">
          <el-input v-model="cronForm.comment" placeholder="可选，任务备注" />
        </el-form-item>
        <el-form-item label="用户" prop="user">
          <el-input v-model="cronForm.user" placeholder="默认 root" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
          {{ isEdit ? '保存' : '添加' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import useServerStore from '@/stores/server'
import {
  getCronJobs,
  createCronJob,
  updateCronJob,
  deleteCronJob as deleteCronJobApi,
  toggleCronJob,
  commonCronExpressions,
  type CronJob,
  type CronJobCreateParams,
} from '@/api/cron'

const serverStore = useServerStore()

const selectedServerId = ref<number>(serverStore.currentServerId)
const cronJobs = ref<CronJob[]>([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)
const editingId = ref<number>(0)
const cronFormRef = ref<FormInstance>()

const cronForm = reactive<CronJobCreateParams & { id?: number }>({
  expression: '',
  command: '',
  comment: '',
  user: 'root',
})

const cronFormRules: FormRules = {
  expression: [{ required: true, message: '请输入 Cron 表达式', trigger: 'blur' }],
  command: [{ required: true, message: '请输入命令', trigger: 'blur' }],
}

onMounted(async () => {
  await serverStore.fetchServers()
  if (serverStore.currentServerId) {
    selectedServerId.value = serverStore.currentServerId
    await loadCronJobs()
  }
})

async function loadCronJobs() {
  if (!selectedServerId.value) return
  try {
    cronJobs.value = await getCronJobs({ serverId: selectedServerId.value })
  } catch {
    // 错误已由拦截器处理
  }
}

function handleServerChange() {
  loadCronJobs()
}

function resetForm() {
  cronForm.expression = ''
  cronForm.command = ''
  cronForm.comment = ''
  cronForm.user = 'root'
  editingId.value = 0
}

function handleAdd() {
  resetForm()
  isEdit.value = false
  dialogVisible.value = true
}

function handleEdit(row: CronJob) {
  resetForm()
  isEdit.value = true
  editingId.value = row.id
  cronForm.expression = row.expression
  cronForm.command = row.command
  cronForm.comment = row.comment || ''
  cronForm.user = row.user || 'root'
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!cronFormRef.value) return
  const valid = await cronFormRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    if (isEdit.value) {
      await updateCronJob(selectedServerId.value, {
        id: editingId.value,
        expression: cronForm.expression,
        command: cronForm.command,
        comment: cronForm.comment,
        user: cronForm.user,
      })
      ElMessage.success('更新成功')
    } else {
      await createCronJob(selectedServerId.value, cronForm)
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    await loadCronJobs()
  } catch {
    // 错误已由拦截器处理
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(row: CronJob) {
  try {
    await deleteCronJobApi(selectedServerId.value, row.id)
    ElMessage.success('删除成功')
    await loadCronJobs()
  } catch {
    // 错误已由拦截器处理
  }
}

async function handleToggle(row: CronJob, enabled: boolean) {
  try {
    await toggleCronJob(selectedServerId.value, row.id, enabled)
    ElMessage.success(enabled ? '已启用' : '已禁用')
    await loadCronJobs()
  } catch {
    // 错误已由拦截器处理
  }
}
</script>

<style scoped lang="scss">
.cron-page {
  .page-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 20px;

    .header-left {
      display: flex;
      align-items: center;
      gap: 12px;

      .page-title {
        margin: 0;
        font-size: 20px;
        font-weight: 600;
        color: #e5eaf3;
      }
    }

    .header-right {
      display: flex;
      align-items: center;
      gap: 8px;
    }
  }

  .quick-expressions {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;

    .quick-tag {
      cursor: pointer;
      transition: all 0.2s;

      &:hover {
        opacity: 0.8;
      }
    }
  }
}
</style>
