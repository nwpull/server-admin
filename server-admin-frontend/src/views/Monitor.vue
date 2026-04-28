<template>
  <div class="monitor-page">
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">系统监控</h2>
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
        <el-tag :type="wsConnected ? 'success' : 'info'" size="small">
          {{ wsConnected ? '实时更新中' : '未连接' }}
        </el-tag>
        <el-button icon="Refresh" @click="refreshData">刷新</el-button>
      </div>
    </div>

    <template v-if="monitorData">
      <!-- 概览卡片 -->
      <el-row :gutter="16" class="overview-row">
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-label">CPU 使用率</div>
            <div class="stat-value" :style="{ color: getProgressColor(monitorData.cpuUsage) }">
              {{ monitorData.cpuUsage.toFixed(1) }}%
            </div>
            <div class="stat-sub">核心数: {{ monitorData.cpuCores }}</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-label">内存使用率</div>
            <div class="stat-value" :style="{ color: getProgressColor(monitorData.memoryUsage) }">
              {{ monitorData.memoryUsage.toFixed(1) }}%
            </div>
            <div class="stat-sub">
              {{ formatBytes(monitorData.memoryUsed) }} / {{ formatBytes(monitorData.memoryTotal) }}
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-label">磁盘使用率</div>
            <div class="stat-value" :style="{ color: getProgressColor(monitorData.diskUsage) }">
              {{ monitorData.diskUsage.toFixed(1) }}%
            </div>
            <div class="stat-sub">
              {{ formatBytes(monitorData.diskUsed) }} / {{ formatBytes(monitorData.diskTotal) }}
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-label">系统负载</div>
            <div class="stat-value" style="color: #409eff">
              {{ monitorData.loadAvg1.toFixed(2) }}
            </div>
            <div class="stat-sub">
              {{ monitorData.loadAvg5.toFixed(2) }} / {{ monitorData.loadAvg15.toFixed(2) }}
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 图表区域 -->
      <el-row :gutter="16" class="chart-row">
        <el-col :span="16">
          <el-card class="chart-card">
            <template #header>
              <span>CPU 趋势</span>
            </template>
            <div ref="cpuChartRef" class="chart-container"></div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card class="chart-card">
            <template #header>
              <span>内存使用</span>
            </template>
            <div ref="memoryChartRef" class="chart-container"></div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 磁盘分区 -->
      <el-card class="table-card">
        <template #header>
          <span>磁盘分区</span>
        </template>
        <el-table :data="monitorData.diskPartitions" stripe style="width: 100%">
          <el-table-column prop="device" label="设备" min-width="120" />
          <el-table-column prop="mountPoint" label="挂载点" width="120" />
          <el-table-column prop="filesystem" label="文件系统" width="100" />
          <el-table-column label="总大小" width="120">
            <template #default="{ row }">{{ formatBytes(row.total) }}</template>
          </el-table-column>
          <el-table-column label="已使用" width="120">
            <template #default="{ row }">{{ formatBytes(row.used) }}</template>
          </el-table-column>
          <el-table-column label="可用" width="120">
            <template #default="{ row }">{{ formatBytes(row.free) }}</template>
          </el-table-column>
          <el-table-column label="使用率" width="180">
            <template #default="{ row }">
              <el-progress
                :percentage="Number(row.usage.toFixed(1))"
                :color="getProgressColor(row.usage)"
                :stroke-width="12"
              />
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- 网络流量 -->
      <el-card class="table-card">
        <template #header>
          <span>网络接口</span>
        </template>
        <el-table :data="monitorData.networkInterfaces" stripe style="width: 100%">
          <el-table-column prop="name" label="接口" width="120" />
          <el-table-column label="发送" width="150">
            <template #default="{ row }">{{ formatBytes(row.bytesSent) }}</template>
          </el-table-column>
          <el-table-column label="接收" width="150">
            <template #default="{ row }">{{ formatBytes(row.bytesRecv) }}</template>
          </el-table-column>
          <el-table-column label="发送包" width="120">
            <template #default="{ row }">{{ row.packetsSent }}</template>
          </el-table-column>
          <el-table-column label="接收包" width="120">
            <template #default="{ row }">{{ row.packetsRecv }}</template>
          </el-table-column>
        </el-table>
      </el-card>
    </template>

    <el-empty v-else description="请选择服务器" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import * as echarts from 'echarts'
import useServerStore from '@/stores/server'
import useUserStore from '@/stores/user'
import { getMonitorData, getMonitorWsUrl, type MonitorData, type MonitorHistoryItem } from '@/api/monitor'

const serverStore = useServerStore()
const userStore = useUserStore()

const selectedServerId = ref<number>(serverStore.currentServerId)
const monitorData = ref<MonitorData | null>(null)
const wsConnected = ref(false)

const cpuChartRef = ref<HTMLDivElement>()
const memoryChartRef = ref<HTMLDivElement>()
let cpuChart: echarts.ECharts | null = null
let memoryChart: echarts.ECharts | null = null
let ws: WebSocket | null = null

// CPU 历史数据
const cpuHistory = ref<number[]>([])
const memoryHistory = ref<number[]>([])
const timeLabels = ref<string[]>([])
const MAX_HISTORY = 60

onMounted(async () => {
  await serverStore.fetchServers()
  if (serverStore.currentServerId) {
    selectedServerId.value = serverStore.currentServerId
    await refreshData()
    initCharts()
    connectWs()
  }
})

onBeforeUnmount(() => {
  disconnectWs()
  cpuChart?.dispose()
  memoryChart?.dispose()
})

watch(selectedServerId, () => {
  cpuHistory.value = []
  memoryHistory.value = []
  timeLabels.value = []
  refreshData()
  disconnectWs()
  connectWs()
})

async function refreshData() {
  if (!selectedServerId.value) return
  try {
    monitorData.value = await getMonitorData(selectedServerId.value)
    updateCharts()
  } catch {
    // 错误已由拦截器处理
  }
}

function connectWs() {
  if (!selectedServerId.value) return
  const token = userStore.getToken()
  if (!token) return

  const url = getMonitorWsUrl(selectedServerId.value, token)
  ws = new WebSocket(url)

  ws.onopen = () => {
    wsConnected.value = true
  }

  ws.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data) as MonitorData
      monitorData.value = data

      // 添加历史数据
      const now = new Date()
      const timeStr = `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}:${now.getSeconds().toString().padStart(2, '0')}`
      timeLabels.value.push(timeStr)
      cpuHistory.value.push(data.cpuUsage)
      memoryHistory.value.push(data.memoryUsage)

      if (timeLabels.value.length > MAX_HISTORY) {
        timeLabels.value.shift()
        cpuHistory.value.shift()
        memoryHistory.value.shift()
      }

      updateCharts()
    } catch {
      // 忽略解析错误
    }
  }

  ws.onclose = () => {
    wsConnected.value = false
  }

  ws.onerror = () => {
    wsConnected.value = false
  }
}

function disconnectWs() {
  if (ws) {
    ws.close()
    ws = null
  }
  wsConnected.value = false
}

function initCharts() {
  nextTick(() => {
    if (cpuChartRef.value) {
      cpuChart = echarts.init(cpuChartRef.value, 'dark')
    }
    if (memoryChartRef.value) {
      memoryChart = echarts.init(memoryChartRef.value, 'dark')
    }
    updateCharts()

    // 窗口大小变化时重新调整
    window.addEventListener('resize', handleResize)
  })
}

function handleResize() {
  cpuChart?.resize()
  memoryChart?.resize()
}

function updateCharts() {
  if (cpuChart) {
    cpuChart.setOption({
      backgroundColor: 'transparent',
      tooltip: {
        trigger: 'axis',
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        containLabel: true,
      },
      xAxis: {
        type: 'category',
        boundaryGap: false,
        data: timeLabels.value,
        axisLine: { lineStyle: { color: '#4c4d4f' } },
        axisLabel: { color: '#a3a6ad' },
      },
      yAxis: {
        type: 'value',
        max: 100,
        axisLine: { lineStyle: { color: '#4c4d4f' } },
        axisLabel: { color: '#a3a6ad', formatter: '{value}%' },
        splitLine: { lineStyle: { color: '#2b2b2c' } },
      },
      series: [
        {
          name: 'CPU',
          type: 'line',
          smooth: true,
          data: cpuHistory.value,
          lineStyle: { color: '#409eff', width: 2 },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(64, 158, 255, 0.3)' },
              { offset: 1, color: 'rgba(64, 158, 255, 0.02)' },
            ]),
          },
          itemStyle: { color: '#409eff' },
        },
      ],
    })
  }

  if (memoryChart && monitorData.value) {
    const data = monitorData.value
    memoryChart.setOption({
      backgroundColor: 'transparent',
      tooltip: {
        trigger: 'item',
        formatter: '{b}: {c} ({d}%)',
      },
      series: [
        {
          type: 'pie',
          radius: ['45%', '70%'],
          center: ['50%', '50%'],
          avoidLabelOverlap: false,
          label: {
            show: true,
            color: '#a3a6ad',
            formatter: '{b}\n{d}%',
          },
          data: [
            {
              value: data.memoryUsed,
              name: '已使用',
              itemStyle: { color: '#409eff' },
            },
            {
              value: data.memoryCached || 0,
              name: '缓存',
              itemStyle: { color: '#e6a23c' },
            },
            {
              value: data.memoryFree,
              name: '可用',
              itemStyle: { color: '#262626' },
            },
          ],
        },
      ],
    })
  }
}

function handleServerChange() {
  // watch 会处理
}

function getProgressColor(percentage: number): string {
  if (percentage >= 90) return '#f56c6c'
  if (percentage >= 70) return '#e6a23c'
  return '#409eff'
}

function formatBytes(bytes: number): string {
  if (bytes === 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  const k = 1024
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return (bytes / Math.pow(k, i)).toFixed(i > 0 ? 1 : 0) + ' ' + units[i]
}
</script>

<style scoped lang="scss">
.monitor-page {
  .page-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 20px;

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
      gap: 12px;
    }
  }

  .overview-row {
    margin-bottom: 16px;

    .stat-card {
      border-color: #2b2b2c;

      .stat-label {
        font-size: 13px;
        color: #a3a6ad;
        margin-bottom: 8px;
      }

      .stat-value {
        font-size: 28px;
        font-weight: 700;
        margin-bottom: 4px;
      }

      .stat-sub {
        font-size: 12px;
        color: #636466;
      }
    }
  }

  .chart-row {
    margin-bottom: 16px;

    .chart-card {
      border-color: #2b2b2c;

      .chart-container {
        height: 300px;
      }
    }
  }

  .table-card {
    margin-bottom: 16px;
    border-color: #2b2b2c;
  }
}
</style>
