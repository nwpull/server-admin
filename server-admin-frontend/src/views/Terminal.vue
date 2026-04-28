<template>
  <div class="terminal-page">
    <div class="terminal-toolbar">
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
      <el-button type="primary" icon="Plus" @click="openNewTab">新建终端</el-button>
    </div>

    <el-tabs
      v-model="activeTabId"
      type="card"
      closable
      class="terminal-tabs"
      @tab-remove="closeTab"
      @tab-change="handleTabChange"
    >
      <el-tab-pane
        v-for="tab in tabs"
        :key="tab.id"
        :label="tab.title"
        :name="tab.id"
        class="terminal-tab-pane"
      >
        <div class="terminal-container">
          <XTerminal
            v-if="tab.id === activeTabId"
            :ref="(el: any) => setTerminalRef(tab.id, el)"
            :server-id="tab.serverId"
            :token="userStore.getToken()"
            @connected="handleConnected(tab.id)"
            @disconnected="handleDisconnected(tab.id)"
            @error="handleError(tab.id, $event)"
          />
          <div v-if="tab.error" class="terminal-error">
            <el-icon><WarningFilled /></el-icon>
            <span>{{ tab.error }}</span>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 空状态 -->
    <div v-if="tabs.length === 0" class="empty-state">
      <el-empty description="请点击新建终端按钮打开一个终端连接">
        <el-button type="primary" @click="openNewTab">新建终端</el-button>
      </el-empty>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import useServerStore from '@/stores/server'
import useUserStore from '@/stores/user'
import XTerminal from '@/components/XTerminal.vue'

interface TerminalTab {
  id: string
  title: string
  serverId: number
  connected: boolean
  error: string
}

const serverStore = useServerStore()
const userStore = useUserStore()

const tabs = ref<TerminalTab[]>([])
const activeTabId = ref('')
const selectedServerId = ref<number>(serverStore.currentServerId)
const terminalRefs = new Map<string, InstanceType<typeof XTerminal>>()

let tabCounter = 0

onMounted(async () => {
  await serverStore.fetchServers()
  if (serverStore.servers.length > 0) {
    selectedServerId.value = serverStore.currentServerId || serverStore.servers[0].id
    openNewTab()
  }
})

onBeforeUnmount(() => {
  // 关闭所有终端连接
  terminalRefs.forEach((terminal) => {
    terminal?.disconnect()
  })
})

function setTerminalRef(tabId: string, el: any) {
  if (el) {
    terminalRefs.set(tabId, el)
  }
}

function openNewTab() {
  const serverId = selectedServerId.value
  if (!serverId) {
    ElMessage.warning('请先选择服务器')
    return
  }
  const server = serverStore.servers.find((s) => s.id === serverId)
  if (!server) {
    ElMessage.warning('服务器不存在')
    return
  }

  tabCounter++
  const tabId = `terminal-${tabCounter}`
  const tab: TerminalTab = {
    id: tabId,
    title: `${server.name} #${tabCounter}`,
    serverId,
    connected: false,
    error: '',
  }
  tabs.value.push(tab)
  activeTabId.value = tabId
}

function closeTab(tabId: string) {
  const index = tabs.value.findIndex((t) => t.id === tabId)
  if (index === -1) return

  // 断开终端连接
  const terminal = terminalRefs.get(tabId)
  terminal?.disconnect()
  terminalRefs.delete(tabId)

  tabs.value.splice(index, 1)

  // 如果关闭的是当前活动标签，切换到相邻标签
  if (activeTabId.value === tabId) {
    if (tabs.value.length > 0) {
      const newIndex = Math.min(index, tabs.value.length - 1)
      activeTabId.value = tabs.value[newIndex].id
    } else {
      activeTabId.value = ''
    }
  }
}

function handleTabChange(tabId: string) {
  // 切换标签时聚焦终端
  setTimeout(() => {
    const terminal = terminalRefs.get(tabId)
    terminal?.focus()
  }, 100)
}

function handleServerChange(serverId: number) {
  selectedServerId.value = serverId
}

function handleConnected(tabId: string) {
  const tab = tabs.value.find((t) => t.id === tabId)
  if (tab) {
    tab.connected = true
    tab.error = ''
  }
}

function handleDisconnected(tabId: string) {
  const tab = tabs.value.find((t) => t.id === tabId)
  if (tab) {
    tab.connected = false
  }
}

function handleError(tabId: string, message: string) {
  const tab = tabs.value.find((t) => t.id === tabId)
  if (tab) {
    tab.error = message
    tab.connected = false
  }
}
</script>

<style scoped lang="scss">
.terminal-page {
  height: calc(100vh - 120px);
  display: flex;
  flex-direction: column;
  overflow: hidden;

  .terminal-toolbar {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 12px;
    flex-shrink: 0;
  }

  .terminal-tabs {
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow: hidden;

    :deep(.el-tabs__header) {
      margin-bottom: 0;
      flex-shrink: 0;
    }

    :deep(.el-tabs__content) {
      flex: 1;
      overflow: hidden;
    }

    :deep(.el-tabs__item) {
      background-color: #1f1f1f;
      border-color: #2b2b2c;
      color: #a3a6ad;
    }

    :deep(.el-tabs__item.is-active) {
      background-color: #0a0a0a;
      color: #e5eaf3;
    }

    :deep(.el-tabs__nav) {
      border-color: #2b2b2c;
    }
  }

  .terminal-tab-pane {
    height: 100%;
  }

  .terminal-container {
    height: 100%;
    background-color: #0a0a0a;
    border-radius: 4px;
    position: relative;
    overflow: hidden;
  }

  .terminal-error {
    position: absolute;
    bottom: 16px;
    left: 16px;
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 16px;
    background-color: rgba(245, 108, 108, 0.15);
    border: 1px solid rgba(245, 108, 108, 0.3);
    border-radius: 4px;
    color: #f56c6c;
    font-size: 13px;
    z-index: 10;
  }

  .empty-state {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
  }
}
</style>
