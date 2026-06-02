import axios from 'axios'
import { ElMessage } from 'element-plus'
import { clearSession, getToken, isTokenExpired } from '../utils/auth'
import router from '../router'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 15000
})

api.interceptors.request.use((config) => {
  const token = getToken()
  if (token && !isTokenExpired(token)) {
    config.headers.Authorization = `Bearer ${token}`
  }
  config.headers['X-Trace-Id'] = crypto.randomUUID?.() || `${Date.now()}${Math.random()}`
  return config
})

api.interceptors.response.use(
  (response) => {
    const data = response.data
    if (data && typeof data.code === 'number') {
      if (data.code === 200) return data
      if (data.code === 40101) {
        clearSession()
        router.push('/login')
      }
      ElMessage.error(data.msg || '操作失败')
      return Promise.reject(new Error(data.msg || '操作失败'))
    }
    return response
  },
  (error) => {
    ElMessage.error(error.response?.data?.msg || '网络异常，请稍后重试')
    return Promise.reject(error)
  }
)

export default api
