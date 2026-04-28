import http from './request'

export interface ServerInfo {
  id: number
  name: string
  host: string
  port: number
  username: string
  authType: 'password' | 'key'
  password?: string
  privateKey?: string
  status: 'online' | 'offline' | 'unknown'
  group?: string
  description?: string
  createdAt?: string
  updatedAt?: string
}

export interface ServerCreateParams {
  name: string
  host: string
  port: number
  username: string
  authType: 'password' | 'key'
  password?: string
  privateKey?: string
  group?: string
  description?: string
}

export interface ServerUpdateParams extends Partial<ServerCreateParams> {
  id: number
}

/** 获取服务器列表 */
export function getServerList() {
  return http.get<any, ServerInfo[]>('/servers')
}

/** 获取服务器详情 */
export function getServerDetail(id: number) {
  return http.get<any, ServerInfo>(`/servers/${id}`)
}

/** 添加服务器 */
export function createServer(data: ServerCreateParams) {
  return http.post<any, ServerInfo>('/servers', data)
}

/** 更新服务器 */
export function updateServer(id: number, data: Partial<ServerCreateParams>) {
  return http.put<any, ServerInfo>(`/servers/${id}`, data)
}

/** 删除服务器 */
export function deleteServer(id: number) {
  return http.delete(`/servers/${id}`)
}

/** 测试服务器连接 */
export function testServerConnection(id: number) {
  return http.post<any, { success: boolean; message: string }>(`/servers/${id}/test`)
}

/** 获取服务器概览信息 */
export function getServerOverview(id: number) {
  return http.get<any, ServerOverview>(`/servers/${id}/overview`)
}

export interface ServerOverview {
  cpuUsage: number
  memoryUsage: number
  memoryTotal: number
  memoryUsed: number
  diskUsage: number
  diskTotal: number
  diskUsed: number
  loadAvg1: number
  loadAvg5: number
  loadAvg15: number
  uptime: number
  os: string
  kernel: string
}
