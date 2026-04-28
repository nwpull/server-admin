<template>
  <div class="services-page">
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">服务管理</h2>
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
          placeholder="搜索服务..."
          clearable
          style="width: 250px"
          prefix-icon="Search"
          @keyup.enter="loadServices"
          @clear="loadServices"
        />
      </div>
      <div class="header-right">
        <el-button icon="Refresh" @click="loadServices">刷新</el-button>
      </div>
    </div>

    <el-table :data="filteredServices" stripe style="width: 100%">
      <el-table-column prop="name" label="名称" min-width="180" />
      <el-table-column label="加载状态" width="120">
        <template #default="{ row }">
          <el-tag :type="getLoadStateType(row.loadState)" size="small">
            {{ row.loadState }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="活跃状态" width="120">
        <template #default="{ row }">
          <el-tag :type="getActiveStateType(row.activeState)" size="small">
            {{ row.activeState }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="子状态" width="120">
        <template #default="{ row }">
          <el-tag :type="getSubStateType(row.subState)" size="small">
            {{ row.subState }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="描述" min-width="250" show-overflow-tooltip />
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-dropdown trigger="click" @command="(cmd: string) => handleCommand(cmd, row)">
            <el-button type="primary" link size="small">
              操作 <el-icon><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="start" :disabled="row.activeState === 'active'">
                  启动
                </el-dropdown-item>
                <el-dropdown-item command="stop" :disabled="row.activeState === 'inactive'">
                  停止
                </el-dropdown-item>
                <el-dropdown-item command="restart">重启</el-dropdown-item>
                <el-dropdown-item command="enable">启用（自启）</el-dropdown-item>
                <el-dropdown-item command="disable">禁用（自启）</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import useServerStore from '@/stores/server'
import {
  getServiceList,
  startService,
  stopService,
  restartService,
  enableService,
  disableService,
  type ServiceInfo,
} from '@/api/service'

const serverStore = useServerStore()

const selectedServerId = ref<number>(serverStore.currentServerId)
const keyword = ref('')
const services = ref<ServiceInfo[]>([])
const loading = ref(false)

const filteredServices = computed(() => {
  if (!keyword.value.trim()) return services.value
  const kw = keyword.value.toLowerCase()
  return services.value.filter(
    (s) =>
      s.name.toLowerCase().includes(kw) ||
      s.description.toLowerCase().includes(kw)
  )
})

onMounted(async () => {
  await serverStore.fetchServers()
  if (serverStore.currentServerId) {
    selectedServerId.value = serverStore.currentServerId
    await loadServices()
  }
})

async function loadServices() {
  if (!selectedServerId.value) return
  loading.value = true
  try {
    services.value = await getServiceList({
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
  loadServices()
}

async function handleCommand(command: string, row: ServiceInfo) {
  try {
    switch (command) {
      case 'start':
        await startService(selectedServerId.value, row.name)
        ElMessage.success(`已启动 ${row.name}`)
        break
      case 'stop':
        await stopService(selectedServerId.value, row.name)
        ElMessage.success(`已停止 ${row.name}`)
        break
      case 'restart':
        await restartService(selectedServerId.value, row.name)
        ElMessage.success(`已重启 ${row.name}`)
        break
      case 'enable':
        await enableService(selectedServerId.value, row.name)
        ElMessage.success(`已启用 ${row.name} 开机自启`)
        break
      case 'disable':
        await disableService(selectedServerId.value, row.name)
        ElMessage.success(`已禁用 ${row.name} 开机自启`)
        break
    }
    await loadServices()
  } catch {
    // 错误已由拦截器处理
  }
}

function getLoadStateType(state: string): 'success' | 'danger' | 'info' {
  if (state === 'loaded') return 'success'
  if (state === 'not-found' || state === 'masked') return 'danger'
  return 'info'
}

function getActiveStateType(state: string): 'success' | 'danger' | 'info' | 'warning' {
  if (state === 'active') return 'success'
  if (state === 'inactive' || state === 'failed') return 'danger'
  if (state === 'activating' || state === 'deactivating') return 'warning'
  return 'info'
}

function getSubStateType(state: string): 'success' | 'danger' | 'info' | 'warning' {
  if (state === 'running' || state === 'exited') return 'success'
  if (state === 'failed' || state === 'dead') return 'danger'
  if (state === 'start' || state === 'stop') return 'warning'
  return 'info'
}
</script>

<style scoped lang="scss">
.services-page {
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
}
</style>
