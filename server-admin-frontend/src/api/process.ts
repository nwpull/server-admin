import http from './request'

export interface ProcessInfo {
  pid: number
  user: string
  cpu: number
  mem: number
  vsz: number
  rss: number
  stat: string
  startTime: string
  command: string
}

export interface ProcessListParams {
  serverId: number
  keyword?: string
}

/** 获取进程列表 */
export function getProcessList(params: ProcessListParams) {
  return http.get<any, ProcessInfo[]>('/processes', { params })
}

/** 终止进程 */
export function killProcess(serverId: number, pid: number) {
  return http.post<any, void>('/processes/kill', { serverId, pid })
}

/** 强制终止进程 */
export function forceKillProcess(serverId: number, pid: number) {
  return http.post<any, void>('/processes/force-kill', { serverId, pid })
}
