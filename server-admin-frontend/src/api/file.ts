import http from './request'

export interface FileItem {
  name: string
  path: string
  isDir: boolean
  size: number
  mode: string
  modTime: string
  owner: string
  group: string
}

export interface FileListParams {
  serverId: number
  path: string
}

export interface UploadParams {
  serverId: number
  path: string
  file: File
}

/** 获取文件列表 */
export function getFileList(params: FileListParams) {
  return http.get<any, FileItem[]>('/files', { params })
}

/** 上传文件 */
export function uploadFile(data: UploadParams) {
  const formData = new FormData()
  formData.append('file', data.file)
  formData.append('path', data.path)
  formData.append('serverId', String(data.serverId))
  return http.post<any, void>('/files/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

/** 下载文件 */
export function downloadFile(serverId: number, path: string) {
  return http.get('/files/download', {
    params: { serverId, path },
    responseType: 'blob',
  })
}

/** 创建目录 */
export function createDirectory(serverId: number, path: string) {
  return http.post<any, void>('/files/mkdir', { serverId, path })
}

/** 删除文件/目录 */
export function deleteFile(serverId: number, path: string) {
  return http.delete<any, void>('/files', { params: { serverId, path } })
}

/** 重命名文件/目录 */
export function renameFile(serverId: number, oldPath: string, newPath: string) {
  return http.put<any, void>('/files/rename', { serverId, oldPath, newPath })
}

/** 读取文件内容 */
export function readFileContent(serverId: number, path: string) {
  return http.get<any, string>('/files/content', { params: { serverId, path } })
}

/** 写入文件内容 */
export function writeFileContent(serverId: number, path: string, content: string) {
  return http.put<any, void>('/files/content', { serverId, path, content })
}
