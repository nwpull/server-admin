import http from './request'

export interface ServiceInfo {
  name: string
  loadState: string
  activeState: string
  subState: string
  description: string
}

export interface ServiceListParams {
  serverId: number
  keyword?: string
}

/** 获取服务列表 */
export function getServiceList(params: ServiceListParams) {
  return http.get<any, ServiceInfo[]>('/services', { params })
}

/** 启动服务 */
export function startService(serverId: number, name: string) {
  return http.post<any, void>('/services/start', { serverId, name })
}

/** 停止服务 */
export function stopService(serverId: number, name: string) {
  return http.post<any, void>('/services/stop', { serverId, name })
}

/** 重启服务 */
export function restartService(serverId: number, name: string) {
  return http.post<any, void>('/services/restart', { serverId, name })
}

/** 启用服务（开机自启） */
export function enableService(serverId: number, name: string) {
  return http.post<any, void>('/services/enable', { serverId, name })
}

/** 禁用服务 */
export function disableService(serverId: number, name: string) {
  return http.post<any, void>('/services/disable', { serverId, name })
}

/** 获取服务日志 */
export function getServiceLogs(serverId: number, name: string, lines?: number) {
  return http.get<any, string[]>('/services/logs', {
    params: { serverId, name, lines: lines || 100 },
  })
}
