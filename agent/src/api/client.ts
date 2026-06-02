import axios from 'axios'
import type { ApiResult, LoginResponse, SkillDefinition, SkillParams } from '../types'

const TOKEN_KEY = 'bankAgentAccessToken'
const USER_KEY = 'bankAgentUser'

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 15000
})

api.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  config.headers['X-Trace-Id'] = crypto.randomUUID?.() || `${Date.now()}${Math.random()}`
  return config
})

api.interceptors.response.use((response) => {
  const body = response.data as ApiResult<unknown>
  if (body && typeof body.code === 'number') {
    if (body.code === 200) {
      return body.data
    }
    if (body.code === 40101) {
      clearSession()
    }
    throw new Error(body.msg || '请求失败')
  }
  return response.data
})

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function getUser() {
  const raw = localStorage.getItem(USER_KEY)
  return raw ? JSON.parse(raw) as LoginResponse['user'] : null
}

export function saveSession(data: LoginResponse) {
  localStorage.setItem(TOKEN_KEY, data.accessToken)
  localStorage.setItem(USER_KEY, JSON.stringify(data.user))
}

export function clearSession() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

export async function login(idCard: string, password: string) {
  const data = await api.post<unknown, LoginResponse>('/api/user/login', { idCard, password })
  saveSession(data)
  return data
}

export async function fetchSkills() {
  return api.get<unknown, SkillDefinition[]>('/api/agent/skills')
}

export async function chat(message: string) {
  return api.post<unknown, Record<string, unknown>>('/api/agent/chat', { message })
}

export async function invokeSkill(skillName: string, params: SkillParams) {
  return api.post<unknown, Record<string, unknown>>('/api/agent/tools/invoke', { skillName, params })
}
