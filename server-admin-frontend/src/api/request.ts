import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

/** 后端统一响应结构 */
export interface Result<T = any> {
  code: number
  message: string
  data: T
}

/** 分页请求参数 */
export interface PageParams {
  page: number
  pageSize: number
}

/** 分页响应结构 */
export interface PageResult<T = any> {
  list: T[]
  total: number
  page: number
  pageSize: number
}

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// 请求拦截器
http.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
http.interceptors.response.use(
  (response) => {
    const res = response.data as Result
    if (res.code !== 0) {
      ElMessage.error(res.message || '请求失败')
      // 401 未授权，跳转登录
      if (res.code === 401) {
        localStorage.removeItem('token')
        router.push('/login')
      }
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res.data
  },
  (error) => {
    if (error.response) {
      const status = error.response.status
      if (status === 401) {
        localStorage.removeItem('token')
        router.push('/login')
        ElMessage.error('登录已过期，请重新登录')
      } else if (status === 403) {
        ElMessage.error('没有权限访问')
      } else if (status === 500) {
        ElMessage.error('服务器内部错误')
      } else {
        ElMessage.error(error.response.data?.message || '请求失败')
      }
    } else {
      ElMessage.error('网络连接失败')
    }
    return Promise.reject(error)
  }
)

export default http
