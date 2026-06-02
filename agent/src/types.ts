export type Role = 'user' | 'assistant' | 'tool'

export type SkillCategory = 'READ' | 'PRECHECK' | 'CONTROLLED_WRITE'

export interface ApiResult<T> {
  code: number
  msg: string
  data: T
}

export interface LoginResponse {
  accessToken: string
  tokenType: string
  expiresIn: number
  user: {
    userId: number
    userName: string
    role: string
    phoneMasked?: string
    idCardMasked?: string
  }
  accounts: Array<Record<string, unknown>>
}

export interface SkillProperty {
  type: 'string' | 'number' | 'boolean' | string
  description?: string
}

export interface SkillSchema {
  type?: string
  properties?: Record<string, SkillProperty>
  required?: string[]
}

export interface SkillDefinition {
  name: string
  title: string
  category: SkillCategory
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH' | string
  endpoint: string
  confirmRequired: boolean
  inputSchema?: SkillSchema
}

export interface ChatMessage {
  id: string
  role: Role
  title?: string
  content: string
  createdAt: string
}

export type SkillParams = Record<string, string | number | boolean | null>
