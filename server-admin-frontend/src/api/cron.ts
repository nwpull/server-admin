import http from './request'

export interface CronJob {
  id: number
  expression: string
  command: string
  comment: string
  user: string
  enabled: boolean
}

export interface CronJobCreateParams {
  expression: string
  command: string
  comment?: string
  user?: string
}

export interface CronJobUpdateParams extends Partial<CronJobCreateParams> {
  id: number
  enabled?: boolean
}

export interface CronJobListParams {
  serverId: number
}

/** 获取定时任务列表 */
export function getCronJobs(params: CronJobListParams) {
  return http.get<any, CronJob[]>('/cron', { params })
}

/** 添加定时任务 */
export function createCronJob(serverId: number, data: CronJobCreateParams) {
  return http.post<any, CronJob>('/cron', { serverId, ...data })
}

/** 更新定时任务 */
export function updateCronJob(serverId: number, data: CronJobUpdateParams) {
  return http.put<any, CronJob>(`/cron/${data.id}`, { serverId, ...data })
}

/** 删除定时任务 */
export function deleteCronJob(serverId: number, id: number) {
  return http.delete<any, void>(`/cron/${id}`, { params: { serverId } })
}

/** 切换定时任务启用/禁用 */
export function toggleCronJob(serverId: number, id: number, enabled: boolean) {
  return http.put<any, void>(`/cron/${id}/toggle`, { serverId, enabled })
}

/** 常用 Cron 表达式 */
export const commonCronExpressions = [
  { label: '每分钟', value: '* * * * *' },
  { label: '每5分钟', value: '*/5 * * * *' },
  { label: '每10分钟', value: '*/10 * * * *' },
  { label: '每30分钟', value: '*/30 * * * *' },
  { label: '每小时', value: '0 * * * *' },
  { label: '每天凌晨0点', value: '0 0 * * *' },
  { label: '每天凌晨2点', value: '0 2 * * *' },
  { label: '每天中午12点', value: '0 12 * * *' },
  { label: '每周一凌晨0点', value: '0 0 * * 1' },
  { label: '每月1号凌晨0点', value: '0 0 1 * *' },
  { label: '工作日每天9点', value: '0 9 * * 1-5' },
  { label: '每天18点', value: '0 18 * * *' },
]
