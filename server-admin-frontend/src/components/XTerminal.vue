<template>
  <div ref="terminalRef" class="x-terminal"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import { Terminal } from '@xterm/xterm'
import { FitAddon } from '@xterm/addon-fit'
import { WebLinksAddon } from '@xterm/addon-web-links'
import { SearchAddon } from '@xterm/addon-search'
import '@xterm/xterm/css/xterm.css'

interface Props {
  serverId: number
  token: string
}

const props = defineProps<Props>()
const emit = defineEmits<{
  (e: 'connected'): void
  (e: 'disconnected'): void
  (e: 'error', message: string): void
}>()

const terminalRef = ref<HTMLDivElement>()
let terminal: Terminal | null = null
let fitAddon: FitAddon | null = null
let ws: WebSocket | null = null
let resizeObserver: ResizeObserver | null = null

onMounted(() => {
  initTerminal()
  connectWebSocket()
})

onBeforeUnmount(() => {
  disconnect()
  if (terminal) {
    terminal.dispose()
    terminal = null
  }
  if (resizeObserver) {
    resizeObserver.disconnect()
    resizeObserver = null
  }
})

watch(
  () => [props.serverId, props.token],
  () => {
    disconnect()
    if (terminal) {
      terminal.clear()
      terminal.dispose()
    }
    initTerminal()
    connectWebSocket()
  }
)

function initTerminal() {
  if (!terminalRef.value) return

  terminal = new Terminal({
    cursorBlink: true,
    cursorStyle: 'block',
    fontSize: 14,
    fontFamily: 'Menlo, Monaco, "Courier New", monospace',
    theme: {
      background: '#0a0a0a',
      foreground: '#e5eaf3',
      cursor: '#409eff',
      cursorAccent: '#0a0a0a',
      selectionBackground: 'rgba(64, 158, 255, 0.3)',
      black: '#000000',
      red: '#ff5555',
      green: '#50fa7b',
      yellow: '#f1fa8c',
      blue: '#409eff',
      magenta: '#ff79c6',
      cyan: '#8be9fd',
      white: '#e5eaf3',
      brightBlack: '#6272a4',
      brightRed: '#ff6e6e',
      brightGreen: '#69ff94',
      brightYellow: '#ffffa5',
      brightBlue: '#d6acff',
      brightMagenta: '#ff92df',
      brightCyan: '#a4ffff',
      brightWhite: '#ffffff',
    },
    allowTransparency: false,
    scrollback: 5000,
    convertEol: true,
  })

  fitAddon = new FitAddon()
  terminal.loadAddon(fitAddon)
  terminal.loadAddon(new WebLinksAddon())
  terminal.loadAddon(new SearchAddon())

  terminal.open(terminalRef.value)

  // 监听容器大小变化
  resizeObserver = new ResizeObserver(() => {
    fitAddon?.fit()
    sendResize()
  })
  resizeObserver.observe(terminalRef.value)

  // 延迟 fit 以确保容器已渲染
  setTimeout(() => {
    fitAddon?.fit()
    sendResize()
  }, 100)

  // 用户输入
  terminal.onData((data) => {
    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify({ type: 'input', data }))
    }
  })
}

function connectWebSocket() {
  const wsBase = import.meta.env.VITE_WS_BASE_URL
  const url = `${wsBase}/ws/terminal?serverId=${props.serverId}&token=${encodeURIComponent(props.token)}`

  ws = new WebSocket(url)

  ws.onopen = () => {
    emit('connected')
    // 发送初始尺寸
    setTimeout(() => sendResize(), 200)
  }

  ws.onmessage = (event) => {
    try {
      const msg = JSON.parse(event.data)
      if (msg.type === 'output' && terminal) {
        terminal.write(msg.data)
      } else if (msg.type === 'error') {
        emit('error', msg.data)
      }
    } catch {
      // 纯文本输出
      if (terminal) {
        terminal.write(event.data)
      }
    }
  }

  ws.onclose = () => {
    emit('disconnected')
  }

  ws.onerror = () => {
    emit('error', 'WebSocket 连接失败')
  }
}

function sendResize() {
  if (!ws || ws.readyState !== WebSocket.OPEN || !terminal || !fitAddon) return
  try {
    ws.send(
      JSON.stringify({
        type: 'resize',
        cols: terminal.cols,
        rows: terminal.rows,
      })
    )
  } catch {
    // 忽略
  }
}

function disconnect() {
  if (ws) {
    ws.close()
    ws = null
  }
}

function focus() {
  terminal?.focus()
}

function clear() {
  terminal?.clear()
}

defineExpose({
  focus,
  clear,
  disconnect,
})
</script>

<style scoped>
.x-terminal {
  width: 100%;
  height: 100%;
  padding: 4px;
}

.x-terminal :deep(.xterm) {
  height: 100%;
}

.x-terminal :deep(.xterm-viewport) {
  overflow-y: auto !important;
}
</style>
