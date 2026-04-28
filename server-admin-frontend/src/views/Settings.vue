<template>
  <div class="settings-page">
    <div class="page-header">
      <h2 class="page-title">服务器管理</h2>
      <el-button type="primary" icon="Plus" @click="handleAdd">添加服务器</el-button>
    </div>

    <el-table :data="serverStore.servers" stripe style="width: 100%">
      <el-table-column prop="name" label="名称" min-width="120" />
      <el-table-column prop="host" label="主机" min-width="150" />
      <el-table-column prop="port" label="端口" width="80" />
      <el-table-column prop="username" label="用户名" width="120" />
      <el-table-column label="认证方式" width="100">
        <template #default="{ row }">
          <el-tag :type="row.authType === 'key' ? 'success' : 'info'" size="small">
            {{ row.authType === 'key' ? '密钥' : '密码' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag
            :type="row.status === 'online' ? 'success' : row.status === 'offline' ? 'danger' : 'info'"
            size="small"
          >
            {{ row.status === 'online' ? '在线' : row.status === 'offline' ? '离线' : '未知' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="描述" min-width="150" show-overflow-tooltip />
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="handleTest(row)">测试连接</el-button>
          <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
          <el-popconfirm
            title="确定要删除此服务器吗？"
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
      :title="isEdit ? '编辑服务器' : '添加服务器'"
      width="560px"
      destroy-on-close
    >
      <el-form
        ref="serverFormRef"
        :model="serverForm"
        :rules="serverFormRules"
        label-width="90px"
        label-position="right"
      >
        <el-form-item label="名称" prop="name">
          <el-input v-model="serverForm.name" placeholder="请输入服务器名称" />
        </el-form-item>
        <el-form-item label="主机" prop="host">
          <el-input v-model="serverForm.host" placeholder="请输入主机地址" />
        </el-form-item>
        <el-form-item label="端口" prop="port">
          <el-input-number v-model="serverForm.port" :min="1" :max="65535" />
        </el-form-item>
        <el-form-item label="用户名" prop="username">
          <el-input v-model="serverForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="认证方式" prop="authType">
          <el-radio-group v-model="serverForm.authType">
            <el-radio value="password">密码认证</el-radio>
            <el-radio value="key">密钥认证</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="serverForm.authType === 'password'" label="密码" prop="password">
          <el-input
            v-model="serverForm.password"
            type="password"
            placeholder="请输入密码"
            show-password
          />
        </el-form-item>
        <el-form-item v-if="serverForm.authType === 'key'" label="私钥" prop="privateKey">
          <el-input
            v-model="serverForm.privateKey"
            type="textarea"
            :rows="4"
            placeholder="请粘贴 SSH 私钥内容"
          />
        </el-form-item>
        <el-form-item label="分组" prop="group">
          <el-input v-model="serverForm.group" placeholder="可选，服务器分组" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="serverForm.description"
            type="textarea"
            :rows="2"
            placeholder="可选，服务器描述"
          />
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
  createServer,
  updateServer,
  deleteServer as deleteServerApi,
  testServerConnection,
  type ServerInfo,
  type ServerCreateParams,
} from '@/api/server'

const serverStore = useServerStore()
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)
const editingId = ref<number>(0)
const serverFormRef = ref<FormInstance>()

const serverForm = reactive<ServerCreateParams & { id?: number }>({
  name: '',
  host: '',
  port: 22,
  username: 'root',
  authType: 'password',
  password: '',
  privateKey: '',
  group: '',
  description: '',
})

const serverFormRules: FormRules = {
  name: [{ required: true, message: '请输入服务器名称', trigger: 'blur' }],
  host: [{ required: true, message: '请输入主机地址', trigger: 'blur' }],
  port: [{ required: true, message: '请输入端口', trigger: 'blur' }],
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  authType: [{ required: true, message: '请选择认证方式', trigger: 'change' }],
  password: [
    {
      required: true,
      message: '请输入密码',
      trigger: 'blur',
      validator: (_rule, _value, callback) => {
        if (serverForm.authType === 'password' && !serverForm.password) {
          callback(new Error('请输入密码'))
        } else {
          callback()
        }
      },
    },
  ],
  privateKey: [
    {
      required: true,
      message: '请输入私钥',
      trigger: 'blur',
      validator: (_rule, _value, callback) => {
        if (serverForm.authType === 'key' && !serverForm.privateKey) {
          callback(new Error('请输入私钥'))
        } else {
          callback()
        }
      },
    },
  ],
}

onMounted(() => {
  serverStore.fetchServers()
})

function resetForm() {
  serverForm.name = ''
  serverForm.host = ''
  serverForm.port = 22
  serverForm.username = 'root'
  serverForm.authType = 'password'
  serverForm.password = ''
  serverForm.privateKey = ''
  serverForm.group = ''
  serverForm.description = ''
  editingId.value = 0
}

function handleAdd() {
  resetForm()
  isEdit.value = false
  dialogVisible.value = true
}

function handleEdit(row: ServerInfo) {
  resetForm()
  isEdit.value = true
  editingId.value = row.id
  serverForm.name = row.name
  serverForm.host = row.host
  serverForm.port = row.port
  serverForm.username = row.username
  serverForm.authType = row.authType
  serverForm.group = row.group || ''
  serverForm.description = row.description || ''
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!serverFormRef.value) return
  const valid = await serverFormRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    if (isEdit.value) {
      await updateServer(editingId.value, serverForm)
      ElMessage.success('更新成功')
    } else {
      await createServer(serverForm)
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    await serverStore.fetchServers()
  } catch {
    // 错误已由拦截器处理
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(row: ServerInfo) {
  try {
    await deleteServerApi(row.id)
    ElMessage.success('删除成功')
    await serverStore.fetchServers()
  } catch {
    // 错误已由拦截器处理
  }
}

async function handleTest(row: ServerInfo) {
  try {
    const res = await testServerConnection(row.id)
    if (res.success) {
      ElMessage.success(`连接成功: ${res.message}`)
    } else {
      ElMessage.error(`连接失败: ${res.message}`)
    }
  } catch {
    // 错误已由拦截器处理
  }
}
</script>

<style scoped lang="scss">
.settings-page {
  .page-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 20px;

    .page-title {
      margin: 0;
      font-size: 20px;
      font-weight: 600;
      color: #e5eaf3;
    }
  }
}
</style>
