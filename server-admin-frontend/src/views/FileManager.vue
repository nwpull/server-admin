<template>
  <div class="file-manager-page">
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">文件管理</h2>
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
        <el-button icon="Upload" @click="handleUpload">上传</el-button>
        <el-button icon="FolderAdd" @click="handleCreateDir">新建目录</el-button>
        <el-button icon="DocumentAdd" @click="handleCreateFile">新建文件</el-button>
        <el-button icon="Refresh" @click="loadFiles">刷新</el-button>
      </div>
    </div>

    <!-- 面包屑导航 -->
    <div class="breadcrumb-area">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item @click="navigateTo('/')">
          <span class="breadcrumb-link">/</span>
        </el-breadcrumb-item>
        <el-breadcrumb-item
          v-for="(part, index) in pathParts"
          :key="index"
        >
          <span
            v-if="index < pathParts.length - 1"
            class="breadcrumb-link"
            @click="navigateTo('/' + pathParts.slice(0, index + 1).join('/'))"
          >
            {{ part }}
          </span>
          <span v-else>{{ part }}</span>
        </el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <!-- 文件列表 -->
    <el-table
      :data="fileList"
      stripe
      style="width: 100%"
      @row-dblclick="handleRowDblClick"
    >
      <el-table-column label="名称" min-width="250">
        <template #default="{ row }">
          <div class="file-name-cell">
            <el-icon :size="18" :color="row.isDir ? '#409EFF' : '#a3a6ad'">
              <Folder v-if="row.isDir" />
              <Document v-else />
            </el-icon>
            <span class="file-name">{{ row.name }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="大小" width="120">
        <template #default="{ row }">
          {{ row.isDir ? '-' : formatFileSize(row.size) }}
        </template>
      </el-table-column>
      <el-table-column prop="mode" label="权限" width="100" />
      <el-table-column prop="owner" label="所有者" width="100" />
      <el-table-column prop="group" label="用户组" width="100" />
      <el-table-column label="修改时间" width="180">
        <template #default="{ row }">
          {{ formatTime(row.modTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="!row.isDir"
            type="primary"
            link
            size="small"
            @click.stop="handleDownload(row)"
          >
            下载
          </el-button>
          <el-button
            v-if="!row.isDir"
            type="primary"
            link
            size="small"
            @click.stop="handleEditFile(row)"
          >
            编辑
          </el-button>
          <el-button type="primary" link size="small" @click.stop="handleRename(row)">
            重命名
          </el-button>
          <el-popconfirm
            title="确定要删除吗？"
            confirm-button-text="确定"
            cancel-button-text="取消"
            @confirm="handleDelete(row)"
          >
            <template #reference>
              <el-button type="danger" link size="small" @click.stop>删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <!-- 上传文件 -->
    <input
      ref="uploadInputRef"
      type="file"
      style="display: none"
      multiple
      @change="handleFileSelected"
    />

    <!-- 新建目录/文件弹窗 -->
    <el-dialog
      v-model="createDialogVisible"
      :title="createType === 'dir' ? '新建目录' : '新建文件'"
      width="400px"
      destroy-on-close
    >
      <el-input
        v-model="createName"
        :placeholder="createType === 'dir' ? '请输入目录名称' : '请输入文件名称'"
        @keyup.enter="handleCreateConfirm"
      />
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreateConfirm">确定</el-button>
      </template>
    </el-dialog>

    <!-- 重命名弹窗 -->
    <el-dialog v-model="renameDialogVisible" title="重命名" width="400px" destroy-on-close>
      <el-input
        v-model="renameName"
        placeholder="请输入新名称"
        @keyup.enter="handleRenameConfirm"
      />
      <template #footer>
        <el-button @click="renameDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleRenameConfirm">确定</el-button>
      </template>
    </el-dialog>

    <!-- 编辑文件弹窗 -->
    <el-dialog
      v-model="editDialogVisible"
      :title="`编辑文件: ${editingFileName}`"
      width="800px"
      top="5vh"
      destroy-on-close
    >
      <el-input
        v-model="editFileContent"
        type="textarea"
        :rows="20"
        placeholder="文件内容"
        class="file-editor"
      />
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="editSaving" @click="handleSaveFile">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import useServerStore from '@/stores/server'
import {
  getFileList,
  uploadFile,
  downloadFile,
  createDirectory,
  deleteFile,
  renameFile,
  readFileContent,
  writeFileContent,
  type FileItem,
} from '@/api/file'

const serverStore = useServerStore()

const selectedServerId = ref<number>(serverStore.currentServerId)
const currentPath = ref('/')
const fileList = ref<FileItem[]>([])
const loading = ref(false)

// 上传
const uploadInputRef = ref<HTMLInputElement>()

// 新建
const createDialogVisible = ref(false)
const createType = ref<'dir' | 'file'>('dir')
const createName = ref('')

// 重命名
const renameDialogVisible = ref(false)
const renameItem = ref<FileItem | null>(null)
const renameName = ref('')

// 编辑文件
const editDialogVisible = ref(false)
const editingFilePath = ref('')
const editingFileName = ref('')
const editFileContent = ref('')
const editSaving = ref(false)

const pathParts = computed(() => {
  if (currentPath.value === '/') return []
  return currentPath.value.split('/').filter(Boolean)
})

onMounted(async () => {
  await serverStore.fetchServers()
  if (serverStore.currentServerId) {
    selectedServerId.value = serverStore.currentServerId
    await loadFiles()
  }
})

async function loadFiles() {
  if (!selectedServerId.value) return
  loading.value = true
  try {
    const res = await getFileList({
      serverId: selectedServerId.value,
      path: currentPath.value,
    })
    // 排序：目录在前
    fileList.value = res.sort((a, b) => {
      if (a.isDir && !b.isDir) return -1
      if (!a.isDir && b.isDir) return 1
      return a.name.localeCompare(b.name)
    })
  } catch {
    // 错误已由拦截器处理
  } finally {
    loading.value = false
  }
}

function handleServerChange() {
  currentPath.value = '/'
  loadFiles()
}

function navigateTo(path: string) {
  currentPath.value = path || '/'
  loadFiles()
}

function handleRowDblClick(row: FileItem) {
  if (row.isDir) {
    const newPath = currentPath.value === '/'
      ? `/${row.name}`
      : `${currentPath.value}/${row.name}`
    navigateTo(newPath)
  }
}

function handleUpload() {
  uploadInputRef.value?.click()
}

async function handleFileSelected(event: Event) {
  const input = event.target as HTMLInputElement
  const files = input.files
  if (!files || files.length === 0) return

  for (const file of Array.from(files)) {
    try {
      await uploadFile({
        serverId: selectedServerId.value,
        path: currentPath.value,
        file,
      })
    } catch {
      // 错误已由拦截器处理
    }
  }
  input.value = ''
  await loadFiles()
  ElMessage.success('上传完成')
}

function handleCreateDir() {
  createType.value = 'dir'
  createName.value = ''
  createDialogVisible.value = true
}

function handleCreateFile() {
  createType.value = 'file'
  createName.value = ''
  createDialogVisible.value = true
}

async function handleCreateConfirm() {
  if (!createName.value.trim()) {
    ElMessage.warning('请输入名称')
    return
  }
  try {
    const fullPath = currentPath.value === '/'
      ? `/${createName.value.trim()}`
      : `${currentPath.value}/${createName.value.trim()}`

    if (createType.value === 'dir') {
      await createDirectory(selectedServerId.value, fullPath)
    } else {
      await writeFileContent(selectedServerId.value, fullPath, '')
    }
    createDialogVisible.value = false
    await loadFiles()
    ElMessage.success('创建成功')
  } catch {
    // 错误已由拦截器处理
  }
}

function handleRename(row: FileItem) {
  renameItem.value = row
  renameName.value = row.name
  renameDialogVisible.value = true
}

async function handleRenameConfirm() {
  if (!renameItem.value || !renameName.value.trim()) {
    ElMessage.warning('请输入名称')
    return
  }
  try {
    const oldPath = currentPath.value === '/'
      ? `/${renameItem.value.name}`
      : `${currentPath.value}/${renameItem.value.name}`
    const newPath = currentPath.value === '/'
      ? `/${renameName.value.trim()}`
      : `${currentPath.value}/${renameName.value.trim()}`

    await renameFile(selectedServerId.value, oldPath, newPath)
    renameDialogVisible.value = false
    await loadFiles()
    ElMessage.success('重命名成功')
  } catch {
    // 错误已由拦截器处理
  }
}

async function handleDelete(row: FileItem) {
  try {
    const fullPath = currentPath.value === '/'
      ? `/${row.name}`
      : `${currentPath.value}/${row.name}`
    await deleteFile(selectedServerId.value, fullPath)
    await loadFiles()
    ElMessage.success('删除成功')
  } catch {
    // 错误已由拦截器处理
  }
}

async function handleDownload(row: FileItem) {
  try {
    const fullPath = currentPath.value === '/'
      ? `/${row.name}`
      : `${currentPath.value}/${row.name}`
    const res = await downloadFile(selectedServerId.value, fullPath)
    const blob = new Blob([res as any])
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = row.name
    link.click()
    window.URL.revokeObjectURL(url)
  } catch {
    // 错误已由拦截器处理
  }
}

async function handleEditFile(row: FileItem) {
  try {
    const fullPath = currentPath.value === '/'
      ? `/${row.name}`
      : `${currentPath.value}/${row.name}`
    const content = await readFileContent(selectedServerId.value, fullPath)
    editingFilePath.value = fullPath
    editingFileName.value = row.name
    editFileContent.value = content
    editDialogVisible.value = true
  } catch {
    // 错误已由拦截器处理
  }
}

async function handleSaveFile() {
  editSaving.value = true
  try {
    await writeFileContent(selectedServerId.value, editingFilePath.value, editFileContent.value)
    editDialogVisible.value = false
    ElMessage.success('保存成功')
  } catch {
    // 错误已由拦截器处理
  } finally {
    editSaving.value = false
  }
}

function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  const k = 1024
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return (bytes / Math.pow(k, i)).toFixed(i > 0 ? 1 : 0) + ' ' + units[i]
}

function formatTime(time: string): string {
  if (!time) return '-'
  return time.replace('T', ' ').substring(0, 19)
}
</script>

<style scoped lang="scss">
.file-manager-page {
  .page-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 16px;

    .header-left {
      display: flex;
      align-items: center;
      gap: 16px;

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

  .breadcrumb-area {
    margin-bottom: 16px;
    padding: 12px 16px;
    background-color: #1f1f1f;
    border-radius: 6px;
    border: 1px solid #2b2b2c;

    .breadcrumb-link {
      cursor: pointer;
      color: #409eff;

      &:hover {
        text-decoration: underline;
      }
    }
  }

  .file-name-cell {
    display: flex;
    align-items: center;
    gap: 8px;

    .file-name {
      color: #e5eaf3;
    }
  }

  .file-editor {
    :deep(.el-textarea__inner) {
      font-family: 'Menlo', 'Monaco', 'Courier New', monospace;
      font-size: 13px;
      line-height: 1.6;
      background-color: #141414 !important;
      color: #e5eaf3 !important;
    }
  }
}
</style>
