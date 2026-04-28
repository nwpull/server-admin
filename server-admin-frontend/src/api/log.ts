import http from './request'

export interface LogReadParams {
  serverId: number
  path: string
  lines?: number
  keyword?: string
}

export interface LogLine {
  line: number
  content: string
  timestamp?: string
}

/** 读取日志文件 */
export function readLogs(params: LogReadParams) {
  return http.get<any, string[]>('/logs', { params })
}

/** 获取日志文件列表 */
export function getLogFiles(serverId: number, dir: string) {
  return http.get<any, string[]>('/logs/files', { params: { serverId, dir } })
}

/** 获取 WebSocket 日志跟踪 URL */
export function getLogTailWsUrl(serverId: number, path: string, token: string): string {
  const wsBase = import.meta.env.VITE_WS_BASE_URL
  return `${wsBase}/ws/logs?serverId=${serverId}&path=${encodeURIComponent(path)}&token=${encodeURIComponent(token)}`
}
