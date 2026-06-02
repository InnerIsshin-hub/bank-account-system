import type { SkillDefinition } from '../types'

export const fallbackSkills: SkillDefinition[] = [
  {
    name: 'account-summary',
    title: '账户摘要',
    category: 'READ',
    riskLevel: 'LOW',
    endpoint: '/api/agent/tools/invoke',
    confirmRequired: false,
    inputSchema: { type: 'object', properties: {}, required: [] }
  },
  {
    name: 'transfer-precheck',
    title: '转账预校验',
    category: 'PRECHECK',
    riskLevel: 'MEDIUM',
    endpoint: '/api/agent/tools/invoke',
    confirmRequired: false,
    inputSchema: {
      type: 'object',
      properties: {
        fromAccount: { type: 'string', description: '付款账户' },
        toAccount: { type: 'string', description: '收款账户' },
        amount: { type: 'number', description: '转账金额' }
      },
      required: ['fromAccount', 'toAccount']
    }
  },
  {
    name: 'create-transfer-draft',
    title: '创建转账草稿',
    category: 'CONTROLLED_WRITE',
    riskLevel: 'HIGH',
    endpoint: '/api/agent/tools/invoke',
    confirmRequired: true,
    inputSchema: {
      type: 'object',
      properties: {
        fromAccount: { type: 'string', description: '付款账户' },
        toAccount: { type: 'string', description: '收款账户' },
        amount: { type: 'number', description: '转账金额' },
        remark: { type: 'string', description: '备注' }
      },
      required: ['toAccount', 'amount']
    }
  },
  {
    name: 'bill-analysis',
    title: '智能账单分析',
    category: 'READ',
    riskLevel: 'LOW',
    endpoint: '/api/agent/tools/invoke',
    confirmRequired: false,
    inputSchema: { type: 'object', properties: {}, required: [] }
  }
]

export function normalizeParams(form: Record<string, string>, skill: SkillDefinition) {
  const schema = skill.inputSchema?.properties || {}
  return Object.entries(form).reduce<Record<string, string | number | boolean | null>>((acc, [key, value]) => {
    if (value === '') {
      return acc
    }
    const type = schema[key]?.type
    if (type === 'number') {
      acc[key] = Number(value)
    } else if (type === 'boolean') {
      acc[key] = value === 'true'
    } else {
      acc[key] = value
    }
    return acc
  }, {})
}

export function emptySkillForm(skill: SkillDefinition) {
  const properties = skill.inputSchema?.properties || {}
  return Object.keys(properties).reduce<Record<string, string>>((acc, key) => {
    acc[key] = ''
    return acc
  }, {})
}
