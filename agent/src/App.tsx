import { FormEvent, useEffect, useMemo, useState } from 'react'
import {
  Bot,
  CreditCard,
  FileText,
  KeyRound,
  ListChecks,
  LogOut,
  MessageSquare,
  Play,
  RefreshCw,
  ShieldCheck,
  Sparkles,
  Wallet
} from 'lucide-react'
import { chat, clearSession, fetchSkills, getToken, getUser, invokeSkill, login } from './api/client'
import { emptySkillForm, fallbackSkills, normalizeParams } from './skills/registry'
import type { ChatMessage, SkillDefinition } from './types'
import './styles.css'

const demoUsers = [
  { label: '张明', idCard: '110101199001010011', password: 'Demo@123' },
  { label: '李娜', idCard: '110101199002020022', password: 'Demo@123' },
  { label: '管理员', idCard: '110101198801010099', password: 'Admin@123' }
]

const skillIcons: Record<string, JSX.Element> = {
  'account-summary': <Wallet size={18} />,
  'recent-records': <FileText size={18} />,
  contacts: <ListChecks size={18} />,
  products: <CreditCard size={18} />,
  'loan-progress': <FileText size={18} />,
  'credit-card-bills': <CreditCard size={18} />,
  notifications: <MessageSquare size={18} />,
  'transfer-precheck': <ShieldCheck size={18} />,
  'risk-evaluate': <ShieldCheck size={18} />,
  'create-transfer-draft': <KeyRound size={18} />,
  'create-loan-draft': <KeyRound size={18} />,
  'create-product-draft': <KeyRound size={18} />,
  'bill-analysis': <Sparkles size={18} />
}

function now() {
  return new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
}

function toMessage(role: ChatMessage['role'], content: unknown, title?: string): ChatMessage {
  return {
    id: crypto.randomUUID?.() || `${Date.now()}${Math.random()}`,
    role,
    title,
    content,
    createdAt: now()
  }
}

export default function App() {
  const [idCard, setIdCard] = useState(demoUsers[0].idCard)
  const [password, setPassword] = useState(demoUsers[0].password)
  const [user, setUser] = useState(getUser())
  const [skills, setSkills] = useState<SkillDefinition[]>(fallbackSkills)
  const [activeSkillName, setActiveSkillName] = useState(fallbackSkills[0].name)
  const [skillForm, setSkillForm] = useState<Record<string, string>>({})
  const [message, setMessage] = useState('帮我看一下本月账单')
  const [loading, setLoading] = useState(false)
  const [messages, setMessages] = useState<ChatMessage[]>([
    toMessage('assistant', 'Agent 工作台已连接白名单技能。', '系统')
  ])

  const authed = Boolean(getToken())
  const activeSkill = useMemo(
    () => skills.find((skill) => skill.name === activeSkillName) || skills[0],
    [activeSkillName, skills]
  )

  const groupedSkills = useMemo(() => {
    return skills.reduce<Record<string, SkillDefinition[]>>((acc, skill) => {
      acc[skill.category] ||= []
      acc[skill.category].push(skill)
      return acc
    }, {})
  }, [skills])

  useEffect(() => {
    if (!activeSkill) {
      return
    }
    setSkillForm(emptySkillForm(activeSkill))
  }, [activeSkill])

  useEffect(() => {
    if (!authed) {
      return
    }
    void refreshSkills()
  }, [authed])

  async function refreshSkills() {
    try {
      const remoteSkills = await fetchSkills()
      if (remoteSkills.length > 0) {
        setSkills(remoteSkills)
        setActiveSkillName(remoteSkills[0].name)
      }
    } catch (error) {
      setMessages((items) => [...items, toMessage('tool', errorMessage(error), '技能加载')])
    }
  }

  async function handleLogin(event: FormEvent) {
    event.preventDefault()
    setLoading(true)
    try {
      const data = await login(idCard, password)
      setUser(data.user)
      setMessages((items) => [...items, toMessage('assistant', { user: data.user, accounts: data.accounts }, '登录成功')])
      await refreshSkills()
    } catch (error) {
      setMessages((items) => [...items, toMessage('tool', errorMessage(error), '登录失败')])
    } finally {
      setLoading(false)
    }
  }

  function logout() {
    clearSession()
    setUser(null)
    setMessages((items) => [...items, toMessage('assistant', '已退出 Agent 会话。', '系统')])
  }

  async function sendChat(event: FormEvent) {
    event.preventDefault()
    const text = message.trim()
    if (!text) {
      return
    }
    setMessage('')
    setMessages((items) => [...items, toMessage('user', text)])
    setLoading(true)
    try {
      const result = await chat(text)
      setMessages((items) => [...items, toMessage('assistant', result, 'Agent')])
    } catch (error) {
      setMessages((items) => [...items, toMessage('tool', errorMessage(error), 'Agent 错误')])
    } finally {
      setLoading(false)
    }
  }

  async function runSkill(event: FormEvent) {
    event.preventDefault()
    if (!activeSkill) {
      return
    }
    setLoading(true)
    try {
      const params = normalizeParams(skillForm, activeSkill)
      const result = await invokeSkill(activeSkill.name, params)
      setMessages((items) => [...items, toMessage('tool', { skill: activeSkill.name, params, result }, activeSkill.title)])
    } catch (error) {
      setMessages((items) => [...items, toMessage('tool', errorMessage(error), activeSkill.title)])
    } finally {
      setLoading(false)
    }
  }

  function selectDemoUser(index: number) {
    setIdCard(demoUsers[index].idCard)
    setPassword(demoUsers[index].password)
  }

  return (
    <main className="shell">
      <aside className="sidebar">
        <section className="brand">
          <div className="brand-mark"><Bot size={22} /></div>
          <div>
            <h1>Bank Agent</h1>
            <p>TypeScript Workbench</p>
          </div>
        </section>

        <form className="login-panel" onSubmit={handleLogin}>
          <div className="field">
            <label htmlFor="id-card">身份证号</label>
            <input id="id-card" value={idCard} onChange={(event) => setIdCard(event.target.value)} />
          </div>
          <div className="field">
            <label htmlFor="password">密码</label>
            <input id="password" type="password" value={password} onChange={(event) => setPassword(event.target.value)} />
          </div>
          <div className="demo-tabs">
            {demoUsers.map((item, index) => (
              <button type="button" key={item.label} onClick={() => selectDemoUser(index)}>{item.label}</button>
            ))}
          </div>
          <button className="primary-button" type="submit" disabled={loading}>
            <Play size={16} />
            登录
          </button>
        </form>

        <section className="user-strip">
          <span>{user?.userName || '未登录'}</span>
          <button type="button" onClick={logout} disabled={!authed} title="退出">
            <LogOut size={16} />
          </button>
        </section>

        <section className="skill-library">
          <div className="section-title">
            <span>技能</span>
            <button type="button" onClick={refreshSkills} disabled={!authed || loading} title="刷新技能">
              <RefreshCw size={15} />
            </button>
          </div>
          {Object.entries(groupedSkills).map(([category, items]) => (
            <div className="skill-group" key={category}>
              <p>{category}</p>
              {items.map((skill) => (
                <button
                  type="button"
                  key={skill.name}
                  className={skill.name === activeSkillName ? 'skill active' : 'skill'}
                  onClick={() => setActiveSkillName(skill.name)}
                >
                  {skillIcons[skill.name] || <Sparkles size={18} />}
                  <span>{skill.title}</span>
                  <small>{skill.riskLevel}</small>
                </button>
              ))}
            </div>
          ))}
        </section>
      </aside>

      <section className="workspace">
        <div className="topbar">
          <div>
            <h2>Agent 工作台</h2>
            <p>{activeSkill?.title || '技能'} · {activeSkill?.category || 'READ'}</p>
          </div>
          <span className={authed ? 'status ok' : 'status'}>{authed ? '已授权' : '未授权'}</span>
        </div>

        <div className="main-grid">
          <section className="conversation">
            <div className="messages">
              {messages.map((item) => (
                <article className={`message ${item.role}`} key={item.id}>
                  <header>
                    <span>{item.title || item.role}</span>
                    <time>{item.createdAt}</time>
                  </header>
                  <MessageBody content={item.content} />
                </article>
              ))}
            </div>
            <form className="composer" onSubmit={sendChat}>
              <input
                value={message}
                onChange={(event) => setMessage(event.target.value)}
                placeholder="例如：帮我给李娜转 500 备注饭钱"
              />
              <button type="submit" disabled={loading || !authed}>
                <MessageSquare size={17} />
                发送
              </button>
            </form>
          </section>

          <section className="toolbox">
            <header>
              <div>
                <h3>{activeSkill?.title}</h3>
                <p>{activeSkill?.confirmRequired ? '需要页面确认' : '可直接调用'}</p>
              </div>
              <span>{activeSkill?.name}</span>
            </header>
            <form onSubmit={runSkill} className="skill-form">
              {Object.entries(activeSkill?.inputSchema?.properties || {}).map(([name, property]) => (
                <div className="field" key={name}>
                  <label htmlFor={`param-${name}`}>
                    {name}
                    {activeSkill?.inputSchema?.required?.includes(name) ? <strong>*</strong> : null}
                  </label>
                  {property.type === 'boolean' ? (
                    <select
                      id={`param-${name}`}
                      value={skillForm[name] || ''}
                      onChange={(event) => setSkillForm((current) => ({ ...current, [name]: event.target.value }))}
                    >
                      <option value="">空</option>
                      <option value="true">true</option>
                      <option value="false">false</option>
                    </select>
                  ) : (
                    <input
                      id={`param-${name}`}
                      type={property.type === 'number' ? 'number' : 'text'}
                      value={skillForm[name] || ''}
                      placeholder={property.description}
                      onChange={(event) => setSkillForm((current) => ({ ...current, [name]: event.target.value }))}
                    />
                  )}
                </div>
              ))}
              <button className="primary-button" type="submit" disabled={loading || !authed}>
                <Play size={16} />
                调用技能
              </button>
            </form>
          </section>
        </div>
      </section>
    </main>
  )
}

function errorMessage(error: unknown) {
  return error instanceof Error ? error.message : String(error)
}

function MessageBody({ content }: { content: unknown }) {
  if (typeof content === 'string') {
    return <p className="message-text">{content}</p>
  }
  if (!isRecord(content)) {
    return <p className="message-text">{String(content)}</p>
  }

  if (isSkillEnvelope(content)) {
    return (
      <div className="result-stack">
        <SummaryRow label="技能" value={content.skill} />
        <StructuredResult value={content.result} />
      </div>
    )
  }

  return <StructuredResult value={content} />
}

function StructuredResult({ value }: { value: unknown }) {
  if (!isRecord(value)) {
    return <p className="message-text">{String(value)}</p>
  }

  if (isRecord(value.user) || Array.isArray(value.accounts)) {
    return <AccountSummary value={value} />
  }
  if (Array.isArray(value.accounts) || value.totalAssets !== undefined) {
    return <AccountSummary value={value} />
  }
  if (Array.isArray(value.records)) {
    return <RecordSummary value={value} />
  }
  if (Array.isArray(value.categoryChart)) {
    return <BillSummary value={value} />
  }
  if (value.intent || value.message || value.summary) {
    return <AgentIntent value={value} />
  }

  return <KeyValuePanel value={value} />
}

function AccountSummary({ value }: { value: Record<string, unknown> }) {
  const user = isRecord(value.user) ? value.user : null
  const accounts = asRecordArray(value.accounts)
  const total = value.totalAssets ?? accounts.reduce((sum, item) => sum + Number(item.availableBalance || item.balance || 0), 0)
  return (
    <div className="result-stack">
      {user ? (
        <div className="summary-card">
          <div>
            <span>当前用户</span>
            <strong>{formatValue(user.userName)}</strong>
          </div>
          <div>
            <span>身份</span>
            <strong>{formatValue(user.role)}</strong>
          </div>
          <div>
            <span>KYC</span>
            <strong>{formatValue(user.kycStatus)}</strong>
          </div>
        </div>
      ) : null}
      <div className="summary-card">
        <div>
          <span>账户数量</span>
          <strong>{accounts.length}</strong>
        </div>
        <div>
          <span>总资产</span>
          <strong>{formatMoney(total)}</strong>
        </div>
      </div>
      {accounts.length > 0 ? (
        <div className="mini-table">
          <div className="mini-row mini-head">
            <span>账户</span>
            <span>类型</span>
            <span>余额</span>
          </div>
          {accounts.map((account, index) => (
            <div className="mini-row" key={`${account.accountNumber || account.rawAccountNumber || index}`}>
              <span>{formatValue(account.accountNumber || account.accountNumberMasked || account.rawAccountNumber)}</span>
              <span>{formatValue(account.accountType)}</span>
              <strong>{formatMoney(account.availableBalance ?? account.balance)}</strong>
            </div>
          ))}
        </div>
      ) : null}
    </div>
  )
}

function RecordSummary({ value }: { value: Record<string, unknown> }) {
  const records = asRecordArray(value.records).slice(0, 8)
  return (
    <div className="result-stack">
      <div className="summary-card">
        <div>
          <span>流水数量</span>
          <strong>{formatValue(value.total ?? records.length)}</strong>
        </div>
        <div>
          <span>页码</span>
          <strong>{formatValue(value.pageNo ?? 1)}</strong>
        </div>
      </div>
      <div className="mini-table">
        <div className="mini-row mini-head">
          <span>摘要</span>
          <span>方向</span>
          <span>金额</span>
        </div>
        {records.map((record, index) => (
          <div className="mini-row" key={`${record.recordNo || index}`}>
            <span>{formatValue(record.remark || record.transactionType || record.category)}</span>
            <span>{formatValue(record.direction)}</span>
            <strong className={record.direction === 'IN' ? 'up' : 'down'}>{formatMoney(record.amount)}</strong>
          </div>
        ))}
      </div>
    </div>
  )
}

function BillSummary({ value }: { value: Record<string, unknown> }) {
  const chart = asRecordArray(value.categoryChart)
  return (
    <div className="result-stack">
      <div className="summary-card">
        <div>
          <span>月份</span>
          <strong>{formatValue(value.month)}</strong>
        </div>
        <div>
          <span>总支出</span>
          <strong>{formatMoney(value.totalExpense)}</strong>
        </div>
        <div>
          <span>最大分类</span>
          <strong>{formatValue(value.largestCategory)}</strong>
        </div>
      </div>
      {value.tips ? <p className="message-text">{formatValue(value.tips)}</p> : null}
      {chart.length > 0 ? (
        <div className="mini-table">
          <div className="mini-row mini-head">
            <span>分类</span>
            <span></span>
            <span>金额</span>
          </div>
          {chart.map((item, index) => (
            <div className="mini-row" key={`${item.category || index}`}>
              <span>{formatValue(item.category)}</span>
              <span></span>
              <strong>{formatMoney(item.amount)}</strong>
            </div>
          ))}
        </div>
      ) : null}
    </div>
  )
}

function AgentIntent({ value }: { value: Record<string, unknown> }) {
  return (
    <div className="result-stack">
      {value.intent ? <SummaryRow label="识别意图" value={value.intent} /> : null}
      {value.message ? <p className="message-text">{formatValue(value.message)}</p> : null}
      {value.summary ? <StructuredResult value={value.summary} /> : null}
      {value.draft ? <KeyValuePanel value={isRecord(value.draft) ? value.draft : { draft: value.draft }} /> : null}
      {Array.isArray(value.candidates) ? <RecordTable records={asRecordArray(value.candidates)} /> : null}
    </div>
  )
}

function KeyValuePanel({ value }: { value: Record<string, unknown> }) {
  const entries = Object.entries(value).filter(([, item]) => item !== undefined && item !== null)
  return (
    <div className="kv-panel">
      {entries.map(([key, item]) => (
        <div className="kv-row" key={key}>
          <span>{humanizeKey(key)}</span>
          {Array.isArray(item) ? (
            <RecordTable records={asRecordArray(item)} />
          ) : isRecord(item) ? (
            <KeyValuePanel value={item} />
          ) : (
            <strong>{formatValue(item)}</strong>
          )}
        </div>
      ))}
    </div>
  )
}

function RecordTable({ records }: { records: Array<Record<string, unknown>> }) {
  if (records.length === 0) {
    return <p className="message-text">暂无数据</p>
  }
  const keys = Object.keys(records[0]).slice(0, 4)
  return (
    <div className="mini-table embedded">
      <div className="mini-row mini-head" style={{ gridTemplateColumns: `repeat(${keys.length}, minmax(0, 1fr))` }}>
        {keys.map((key) => <span key={key}>{humanizeKey(key)}</span>)}
      </div>
      {records.slice(0, 6).map((record, index) => (
        <div className="mini-row" key={index} style={{ gridTemplateColumns: `repeat(${keys.length}, minmax(0, 1fr))` }}>
          {keys.map((key) => <span key={key}>{formatValue(record[key])}</span>)}
        </div>
      ))}
    </div>
  )
}

function SummaryRow({ label, value }: { label: string; value: unknown }) {
  return (
    <div className="summary-row">
      <span>{label}</span>
      <strong>{formatValue(value)}</strong>
    </div>
  )
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

function isSkillEnvelope(value: Record<string, unknown>): value is { skill: string; result: unknown } {
  return typeof value.skill === 'string' && 'result' in value
}

function asRecordArray(value: unknown): Array<Record<string, unknown>> {
  return Array.isArray(value) ? value.filter(isRecord) : []
}

function formatMoney(value: unknown) {
  const number = Number(value || 0)
  return `¥ ${Number.isFinite(number) ? number.toFixed(2) : '0.00'}`
}

function formatValue(value: unknown) {
  if (value === undefined || value === null || value === '') return '-'
  if (typeof value === 'number') return String(value)
  if (typeof value === 'boolean') return value ? '是' : '否'
  if (typeof value === 'string') return value
  return JSON.stringify(value)
}

function humanizeKey(key: string) {
  const names: Record<string, string> = {
    action: '动作',
    amount: '金额',
    bankName: '银行',
    category: '分类',
    confirmRequired: '确认',
    draftNo: '草稿号',
    riskLevel: '风险等级',
    riskScore: '风险分',
    status: '状态',
    total: '总数',
    totalAssets: '总资产',
    userName: '姓名'
  }
  return names[key] || key
}
