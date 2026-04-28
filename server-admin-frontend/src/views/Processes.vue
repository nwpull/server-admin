<template>
  <div class="processes-page">
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">进程管理</h2>
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
        <el-input
          v-model="keyword"
          placeholder="搜索进程..."
          clearable
          style="width: 250px"
          prefix-icon="Search"
          @keyup.enter="loadProcesses"
          @clear="loadProcesses"
        />
      </div>
      <div class="header-right">
        <el-button icon="Refresh" @click="loadProcesses">刷新</el-button>
      </div>
    </div>

    <el-table
      :data="filteredProcesses"
      stripe
      style="width: 100%"
      :default-sort="{ prop: 'cpu', order: 'descending' }"
    >
      <el-table-column prop="pid" label="PID" width="90" sortable />
      <el-table-column prop="user" label="用户" width="100" />
      <el-table-column prop="cpu" label="CPU%" width="90" sortable>
        <template #default="{ row }">
          <span :class="{ 'high-usage': row.cpu > 50 }">{{ row.cpu.toFixed(1) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="mem" label="MEM%" width="90" sortable>
        <template #default="{ row }">
          <span :class="{ 'high-usage': row.mem > 50 }">{{ row.mem.toFixed(1) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="VSZ" width="100">
        <template #default="{ row }">{{ formatBytes(row.vsz) }}</template>
      </el-table-column>
      <el-table-column label="RSS" width="100">
        <template #default="{ row }">{{ formatBytes(row.rss) }}</template>
      </el-table-column>
      <el-table-column prop="stat" label="STAT" width="70" />
      <el-table-column prop="startTime" label="启动时间" width="100" />
      <el-table-column prop="command" label="命令" min-width="300" show-overflow-tooltip />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-popconfirm
            title="确定要终止此进程吗？"
            confirm-button-text="确定"
            cancel-button-text="取消"
            @confirm="handleKill(row.pid, false)"
          >
            <template #reference>
              <el-button type="danger" link size="small">终止</el-button>
            </template>
          </el-popconfirm>
          <el-popconfirm
            title="确定要强制终止此进程吗？"
            confirm-button-text="确定"
            cancel-button-text="取消"
            @confirm="handleKill(row.pid, true)"
          >
            <template #reference>
              <el-button type="danger" link size="small">强制终止</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import useServerStore from '@/stores/server'
import { getProcessList, killProcess, forceKillProcess, type ProcessInfo } from '@/api/process'

const serverStore = useServerStore()

const selectedServerId = ref<number>(serverStore.currentServerId)
const keyword = ref('')
const processes = ref<ProcessInfo[]>([])
const loading = ref(false)

const filteredProcesses = computed(() => {
  if (!keyword.value.trim()) return processes.value
  const kw = keyword.value.toLowerCase()
  return processes.value.filter(
    (p) =>
      p.command.toLowerCase().includes(kw) ||
      p.user.toLowerCase().includes(kw) ||
      String(p.pid).includes(kw)
  )
})

onMounted(async () => {
  await serverStore.fetchServers()
  if (serverStore.currentServerId) {
    selectedServerId.value = serverStore.currentServerId
    await loadProcesses()
  }
})

async function loadProcesses() {
  if (!selectedServerId.value) return
  loading.value = true
  try {
    processes.value = await getProcessList({
      serverId: selectedServerId.value,
      keyword: keyword.value || undefined,
    })
  } catch {
    // 错误已由拦截器处理
  } finally {
    loading.value = false
  }
}

function handleServerChange() {
  loadProcesses()
}

async function handleKill(pid: number, force: boolean) {
  try {
    if (force) {
      await forceKillProcess(selectedServerId.value, pid)
      ElMessage.success(`已强制终止进程 ${pid}`)
    } else {
      await killProcess(selectedServerId.value, pid)
      ElMessage.success(`已终止进程 ${pid}`)
    }
    await loadProcesses()
  } catch {
    // 错误已由拦截器处理
  }
}

function formatBytes(bytes: number): string {
  if (!bytes) return '0'
  if (bytes < 1024) return bytes + ' KB'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' MB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' GB'
}
</script>

<style scoped lang="scss">
.processes-page {
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

  .high-usage {
    color: #f56c6c;
    font-weight: 600;
  }
}
</style>
