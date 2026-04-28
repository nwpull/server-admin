<template>
  <div class="dashboard-page">
    <div class="page-header">
      <h2 class="page-title">仪表盘</h2>
      <el-button icon="Refresh" @click="refreshData">刷新</el-button>
    </div>

    <!-- 告警区域 -->
    <div v-if="alerts.length > 0" class="alert-area">
      <el-alert
        v-for="(alert, index) in alerts"
        :key="index"
        :title="alert"
        type="warning"
        show-icon
        :closable="false"
        class="alert-item"
      />
    </div>

    <!-- 服务器概览卡片 -->
    <el-row :gutter="16">
      <el-col
        v-for="server in serverOverviews"
        :key="server.id"
        :xs="24"
        :sm="12"
        :lg="8"
        :xl="6"
        class="card-col"
      >
        <el-card class="server-card" shadow="hover" @click="goToMonitor(server.id)">
          <div class="card-header">
            <div class="card-title-area">
              <span
                class="status-indicator"
                :class="server.status"
              ></span>
              <span class="card-name">{{ server.name }}</span>
            </div>
            <el-tag :type="server.status === 'online' ? 'success' : 'danger'" size="small">
              {{ server.status === 'online' ? '在线' : '离线' }}
            </el-tag>
          </div>

          <div class="card-info">
            <span class="card-host">{{ server.host }}:{{ server.port }}</span>
            <span v-if="server.os" class="card-os">{{ server.os }}</span>
          </div>

          <div v-if="server.status === 'online'" class="card-metrics">
            <div class="metric-item">
              <div class="metric-label">CPU</div>
              <el-progress
                :percentage="Number(server.cpuUsage.toFixed(1))"
                :color="getProgressColor(server.cpuUsage)"
                :stroke-width="8"
                :show-text="true"
              />
            </div>
            <div class="metric-item">
              <div class="metric-label">内存</div>
              <el-progress
                :percentage="Number(server.memoryUsage.toFixed(1))"
                :color="getProgressColor(server.memoryUsage)"
                :stroke-width="8"
                :show-text="true"
              />
            </div>
            <div class="metric-item">
              <div class="metric-label">磁盘</div>
              <el-progress
                :percentage="Number(server.diskUsage.toFixed(1))"
                :color="getProgressColor(server.diskUsage)"
                :stroke-width="8"
                :show-text="true"
              />
            </div>
          </div>

          <div v-if="server.status === 'online'" class="card-footer">
            <span class="load-info">
              负载: {{ server.loadAvg1?.toFixed(2) }} / {{ server.loadAvg5?.toFixed(2) }} / {{ server.loadAvg15?.toFixed(2) }}
            </span>
            <span class="uptime-info">
              运行: {{ formatUptime(server.uptime) }}
            </span>
          </div>

          <div v-else class="card-offline">
            服务器离线，无法获取监控数据
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 空状态 -->
    <el-empty
      v-if="serverOverviews.length === 0"
      description="暂无服务器，请先添加服务器"
    >
      <el-button type="primary" @click="$router.push('/settings')">添加服务器</el-button>
    </el-empty>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import useServerStore from '@/stores/server'
import { getServerOverview, type ServerOverview } from '@/api/server'
import type { ServerInfo } from '@/api/server'

interface ServerOverviewWithInfo {
  id: number
  name: string
  host: string
  port: number
  status: string
  cpuUsage: number
  memoryUsage: number
  diskUsage: number
  loadAvg1: number
  loadAvg5: number
  loadAvg15: number
  uptime: number
  os: string
  kernel: string
}

const router = useRouter()
const serverStore = useServerStore()
const overviews = ref<Map<number, ServerOverview>>(new Map())
let refreshTimer: ReturnType<typeof setInterval> | null = null

const serverOverviews = computed<ServerOverviewWithInfo[]>(() => {
  return serverStore.servers.map((server) => {
    const overview = overviews.value.get(server.id)
    return {
      id: server.id,
      name: server.name,
      host: server.host,
      port: server.port,
      status: server.status,
      cpuUsage: overview?.cpuUsage ?? 0,
      memoryUsage: overview?.memoryUsage ?? 0,
      diskUsage: overview?.diskUsage ?? 0,
      loadAvg1: overview?.loadAvg1 ?? 0,
      loadAvg5: overview?.loadAvg5 ?? 0,
      loadAvg15: overview?.loadAvg15 ?? 0,
      uptime: overview?.uptime ?? 0,
      os: overview?.os ?? '',
      kernel: overview?.kernel ?? '',
    }
  })
})

const alerts = computed<string[]>(() => {
  const result: string[] = []
  for (const server of serverOverviews.value) {
    if (server.status !== 'online') continue
    if (server.cpuUsage > 90) {
      result.push(`[告警] ${server.name}: CPU 使用率 ${server.cpuUsage.toFixed(1)}% 超过 90%`)
    }
    if (server.memoryUsage > 90) {
      result.push(`[告警] ${server.name}: 内存使用率 ${server.memoryUsage.toFixed(1)}% 超过 90%`)
    }
    if (server.diskUsage > 90) {
      result.push(`[告警] ${server.name}: 磁盘使用率 ${server.diskUsage.toFixed(1)}% 超过 90%`)
    }
  }
  return result
})

onMounted(async () => {
  await serverStore.fetchServers()
  await refreshData()
  // 每30秒自动刷新
  refreshTimer = setInterval(refreshData, 30000)
})

onBeforeUnmount(() => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
})

async function refreshData() {
  const promises = serverStore.servers
    .filter((s) => s.status === 'online')
    .map(async (server) => {
      try {
        const overview = await getServerOverview(server.id)
        overviews.value.set(server.id, overview)
      } catch {
        // 忽略单个服务器的错误
      }
    })
  await Promise.all(promises)
}

function getProgressColor(percentage: number): string {
  if (percentage >= 90) return '#f56c6c'
  if (percentage >= 70) return '#e6a23c'
  return '#409eff'
}

function formatUptime(seconds: number): string {
  if (!seconds) return '-'
  const days = Math.floor(seconds / 86400)
  const hours = Math.floor((seconds % 86400) / 3600)
  if (days > 0) return `${days}天${hours}小时`
  const minutes = Math.floor((seconds % 3600) / 60)
  return `${hours}小时${minutes}分钟`
}

function goToMonitor(serverId: number) {
  serverStore.setCurrentServer(serverId)
  router.push('/monitor')
}
</script>

<style scoped lang="scss">
.dashboard-page {
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

  .alert-area {
    margin-bottom: 20px;

    .alert-item {
      margin-bottom: 8px;
      background-color: #2b2617 !important;
      border-color: #4c3a1a !important;

      :deep(.el-alert__title) {
        color: #e6a23c !important;
      }
    }
  }

  .card-col {
    margin-bottom: 16px;
  }

  .server-card {
    cursor: pointer;
    transition: transform 0.2s, box-shadow 0.2s;
    border-color: #2b2b2c;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 8px 24px rgba(0, 0, 0, 0.3);
    }

    .card-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 12px;

      .card-title-area {
        display: flex;
        align-items: center;
        gap: 8px;

        .status-indicator {
          width: 8px;
          height: 8px;
          border-radius: 50%;

          &.online {
            background-color: #67c23a;
            box-shadow: 0 0 6px rgba(103, 194, 58, 0.5);
          }

          &.offline {
            background-color: #f56c6c;
          }

          &.unknown {
            background-color: #909399;
          }
        }

        .card-name {
          font-size: 16px;
          font-weight: 600;
          color: #e5eaf3;
        }
      }
    }

    .card-info {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 16px;
      font-size: 12px;
      color: #a3a6ad;

      .card-host {
        color: #8d9095;
      }

      .card-os {
        color: #8d9095;
      }
    }

    .card-metrics {
      .metric-item {
        margin-bottom: 12px;

        &:last-child {
          margin-bottom: 0;
        }

        .metric-label {
          font-size: 12px;
          color: #a3a6ad;
          margin-bottom: 4px;
        }
      }
    }

    .card-footer {
      margin-top: 16px;
      padding-top: 12px;
      border-top: 1px solid #2b2b2c;
      display: flex;
      justify-content: space-between;
      font-size: 12px;
      color: #8d9095;

      .load-info,
      .uptime-info {
        white-space: nowrap;
      }
    }

    .card-offline {
      margin-top: 16px;
      padding-top: 12px;
      border-top: 1px solid #2b2b2c;
      text-align: center;
      font-size: 13px;
      color: #636466;
    }
  }
}
</style>
