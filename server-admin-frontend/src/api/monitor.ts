import http from './request'

export interface MonitorData {
  cpuUsage: number
  cpuCores: number
  memoryUsage: number
  memoryTotal: number
  memoryUsed: number
  memoryFree: number
  memoryCached: number
  swapUsage: number
  swapTotal: number
  swapUsed: number
  diskUsage: number
  diskTotal: number
  diskUsed: number
  diskPartitions: DiskPartition[]
  networkInterfaces: NetworkInterface[]
  loadAvg1: number
  loadAvg5: number
  loadAvg15: number
  uptime: number
  os: string
  kernel: string
  hostname: string
}

export interface DiskPartition {
  device: string
  mountPoint: string
  total: number
  used: number
  free: number
  usage: number
  filesystem: string
}

export interface NetworkInterface {
  name: string
  bytesSent: number
  bytesRecv: number
  packetsSent: number
  packetsRecv: number
  speed?: number
}

export interface MonitorHistoryItem {
  timestamp: number
  cpuUsage: number
  memoryUsage: number
  diskUsage: number
}

/** 获取实时监控数据 */
export function getMonitorData(serverId: number) {
  return http.get<any, MonitorData>(`/monitor/${serverId}`)
}

/** 获取历史监控数据 */
export function getMonitorHistory(serverId: number, duration: string = '1h') {
  return http.get<any, MonitorHistoryItem[]>(`/monitor/${serverId}/history`, { params: { duration } })
}

/** 获取 WebSocket 监控 URL */
export function getMonitorWsUrl(serverId: number, token: string): string {
  const wsBase = import.meta.env.VITE_WS_BASE_URL
  return `${wsBase}/ws/monitor?serverId=${serverId}&token=${encodeURIComponent(token)}`
}

/** 获取终端 WebSocket URL */
export function getTerminalWsUrl(serverId: number, token: string): string {
  const wsBase = import.meta.env.VITE_WS_BASE_URL
  return `${wsBase}/ws/terminal?serverId=${serverId}&token=${encodeURIComponent(token)}`
}
