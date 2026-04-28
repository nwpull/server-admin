<template>
  <div class="logs-page">
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">日志查看</h2>
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
    </div>

    <div class="toolbar">
      <div class="toolbar-left">
        <el-input
          v-model="logPath"
          placeholder="日志文件路径，如 /var/log/syslog"
          clearable
          style="width: 350px"
        />
        <el-input-number
          v-model="lines"
          :min="10"
          :max="10000"
          :step="100"
          placeholder="行数"
          style="width: 140px"
        />
        <el-button type="primary" :loading="readLoading" @click="handleRead">
          读取
        </el-button>
        <el-button
          :type="isTailing ? 'danger' : 'success'"
          @click="handleToggleTail"
        >
          {{ isTailing ? '停止跟踪' : '实时跟踪' }}
        </el-button>
      </div>
      <div class="toolbar-right">
        <el-input
          v-model="filterKeyword"
          placeholder="搜索过滤..."
          clearable
          prefix-icon="Search"
          style="width: 200px"
        />
        <el-button icon="Delete" @click="clearLogs">清空</el-button>
      </div>
    </div>

    <div class="log-container" ref="logContainerRef">
      <div
        v-for="(line, index) in filteredLogs"
        :key="index"
        class="log-line"
        :class="{ highlight: filterKeyword && line.toLowerCase().includes(filterKeyword.toLowerCase()) }"
      >
        <span class="line-number">{{ index + 1 }}</span>
        <span class="line-content">{{ line }}</span>
      </div>
      <div v-if="filteredLogs.length === 0" class="log-empty">
        {{ logs.length === 0 ? '暂无日志数据，请输入路径并点击"读取"或"实时跟踪"' : '无匹配结果' }}
      </div>
    </div>

    <div class="log-footer">
      <span>共 {{ filteredLogs.length }} 行</span>
      <el-checkbox v-model="autoScroll">自动滚动</el-checkbox>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, onBeforeUnmount } from 'vue'
import useServerStore from '@/stores/server'
import useUserStore from '@/stores/user'
import { readLogs, getLogTailWsUrl } from '@/api/log'

const serverStore = useServerStore()
const userStore = useUserStore()

const selectedServerId = ref<number>(serverStore.currentServerId)
const logPath = ref('/var/log/syslog')
const lines = ref(500)
const filterKeyword = ref('')
const autoScroll = ref(true)
const readLoading = ref(false)
const isTailing = ref(false)
const logs = ref<string[]>([])
const logContainerRef = ref<HTMLDivElement>()

let tailWs: WebSocket | null = null

const filteredLogs = computed(() => {
  if (!filterKeyword.value.trim()) return logs.value
  const kw = filterKeyword.value.toLowerCase()
  return logs.value.filter((line) => line.toLowerCase().includes(kw))
})

onBeforeUnmount(() => {
  stopTail()
})

function handleServerChange() {
  stopTail()
  logs.value = []
}

async function handleRead() {
  if (!selectedServerId.value || !logPath.value.trim()) {
    return
  }
  readLoading.value = true
  stopTail()
  try {
    logs.value = await readLogs({
      serverId: selectedServerId.value,
      path: logPath.value,
      lines: lines.value,
    })
    await nextTick()
    scrollToBottom()
  } catch {
    // 错误已由拦截器处理
  } finally {
    readLoading.value = false
  }
}

function handleToggleTail() {
  if (isTailing.value) {
    stopTail()
  } else {
    startTail()
  }
}

function startTail() {
  if (!selectedServerId.value || !logPath.value.trim()) return

  const token = userStore.getToken()
  if (!token) return

  const url = getLogTailWsUrl(selectedServerId.value, logPath.value, token)
  tailWs = new WebSocket(url)

  tailWs.onopen = () => {
    isTailing.value = true
  }

  tailWs.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data)
      if (data.type === 'log' && data.content) {
        logs.value.push(data.content)
        // 限制最大行数
        if (logs.value.length > 10000) {
          logs.value = logs.value.slice(-5000)
        }
        if (autoScroll.value) {
          nextTick(scrollToBottom)
        }
      }
    } catch {
      // 纯文本
      logs.value.push(event.data)
      if (autoScroll.value) {
        nextTick(scrollToBottom)
      }
    }
  }

  tailWs.onclose = () => {
    isTailing.value = false
  }

  tailWs.onerror = () => {
    isTailing.value = false
  }
}

function stopTail() {
  if (tailWs) {
    tailWs.close()
    tailWs = null
  }
  isTailing.value = false
}

function clearLogs() {
  logs.value = []
}

function scrollToBottom() {
  if (logContainerRef.value) {
    logContainerRef.value.scrollTop = logContainerRef.value.scrollHeight
  }
}
</script>

<style scoped lang="scss">
.logs-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 120px);

  .page-header {
    display: flex;
    align-items: center;
    gap: 16px;
    margin-bottom: 16px;
    flex-shrink: 0;

    .page-title {
      margin: 0;
      font-size: 20px;
      font-weight: 600;
      color: #e5eaf3;
    }
  }

  .toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 12px;
    flex-shrink: 0;

    .toolbar-left {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .toolbar-right {
      display: flex;
      align-items: center;
      gap: 8px;
    }
  }

  .log-container {
    flex: 1;
    overflow-y: auto;
    background-color: #0a0a0a;
    border: 1px solid #2b2b2c;
    border-radius: 6px;
    padding: 12px;
    font-family: 'Menlo', 'Monaco', 'Courier New', monospace;
    font-size: 13px;
    line-height: 1.6;

    .log-line {
      display: flex;
      padding: 1px 4px;
      border-radius: 2px;

      &:hover {
        background-color: rgba(255, 255, 255, 0.03);
      }

      &.highlight {
        background-color: rgba(64, 158, 255, 0.1);
      }

      .line-number {
        flex-shrink: 0;
        width: 60px;
        text-align: right;
        padding-right: 12px;
        color: #4c4d4f;
        user-select: none;
      }

      .line-content {
        color: #cfd3dc;
        white-space: pre-wrap;
        word-break: break-all;
      }
    }

    .log-empty {
      text-align: center;
      color: #636466;
      padding: 60px 0;
    }
  }

  .log-footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-top: 8px;
    font-size: 12px;
    color: #636466;
    flex-shrink: 0;
  }
}
</style>
