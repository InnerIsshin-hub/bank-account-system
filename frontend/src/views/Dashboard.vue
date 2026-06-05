<template>
  <div class="page">
    <div class="toolbar">
      <div>
        <h1 class="page-title">主控面板</h1>
        <p class="page-subtitle">欢迎回来，{{ user.userName || '用户' }}</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="applyDialogVisible = true">申请新账户</el-button>
    </div>

    <div class="metric-grid">
      <div class="metric">
        <div class="metric-label">总资产</div>
        <div class="metric-value">¥ {{ totalBalance.toFixed(2) }}</div>
      </div>
      <div class="metric">
        <div class="metric-label">账户数量</div>
        <div class="metric-value">{{ accounts.length }}</div>
      </div>
      <div class="metric">
        <div class="metric-label">未读通知</div>
        <div class="metric-value">{{ unreadCount }}</div>
      </div>
    </div>

    <el-row :gutter="16" class="dashboard-grid">
      <el-col :xs="24" :lg="16">
        <el-card class="section-card">
          <template #header>我的账户</template>
          <el-table v-loading="loading" :data="accounts">
            <el-table-column label="账号" min-width="220">
              <template #default="{ row }">
                <div class="account-cell">
                  <span class="mono">{{ row.showFull ? formatCard(row.accountNumber) : row.accountNumberMasked }}</span>
                  <el-button type="primary" link size="small" :icon="View" @click="handleShowCard(row)">
                    {{ row.showFull ? '隐藏' : '显示' }}
                  </el-button>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="accountType" label="类型" width="120">
              <template #default="{ row }">
                <el-tag>{{ typeName(row.accountType) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="110" />
            <el-table-column label="余额" min-width="150">
              <template #default="{ row }">
                <strong>¥ {{ Number(row.availableBalance || 0).toFixed(2) }}</strong>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="190">
              <template #default="{ row }">
                <div class="table-actions">
                  <el-button link type="primary" @click="$router.push(`/accounts/${row.accountNumber}`)">详情</el-button>
                  <el-button link type="primary" @click="$router.push({ path: '/records', query: { account: row.accountNumber } })">流水</el-button>
                  <el-button link type="primary" @click="$router.push({ path: '/transfer', query: { from: row.accountNumber } })">转账</el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="8">
        <el-card class="section-card">
          <template #header>快速操作</template>
          <div class="quick-actions">
            <button type="button" class="quick-action primary" @click="$router.push('/transfer')">
              <el-icon><Money /></el-icon>
              <span>转账汇款</span>
            </button>
            <button type="button" class="quick-action" @click="$router.push('/records')">
              <el-icon><Document /></el-icon>
              <span>交易流水</span>
            </button>
            <button type="button" class="quick-action" @click="$router.push('/notifications')">
              <el-icon><Bell /></el-icon>
              <span>通知中心</span>
            </button>
            <button type="button" class="quick-action" @click="$router.push('/agent')">
              <el-icon><ChatDotRound /></el-icon>
              <span>智能助手</span>
            </button>
          </div>
        </el-card>

        <el-card class="section-card mt">
          <template #header>最近流水</template>
          <el-empty v-if="recentRecords.length === 0" description="暂无流水" />
          <div v-for="record in recentRecords" :key="record.recordNo" class="record-line">
            <div>
              <div>{{ record.remark || record.transactionType }}</div>
              <small>{{ record.createdAt }}</small>
            </div>
            <strong :class="record.direction === 'IN' ? 'money-up' : 'money-down'">
              {{ record.direction === 'IN' ? '+' : '-' }}{{ Number(record.amount).toFixed(2) }}
            </strong>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="verifyDialogVisible" title="身份验证" width="380px" :close-on-click-modal="false">
      <el-form ref="verifyFormRef" :model="verifyForm" :rules="verifyRules" label-position="top">
        <el-form-item label="登录密码" prop="password">
          <el-input v-model="verifyForm.password" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="verifyDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="verifying" @click="handleVerify">确认</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="applyDialogVisible" title="申请新账户" width="420px">
      <el-form :model="applyForm" label-position="top">
        <el-form-item label="账户类型">
          <el-select v-model="applyForm.accountType" class="full-width">
            <el-option label="活期账户" value="CURRENT" />
            <el-option label="定期账户" value="FIXED" />
          </el-select>
        </el-form-item>
        <el-form-item label="交易密码">
          <el-input v-model="applyForm.tradePassword" type="password" maxlength="6" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="applyDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="applying" @click="applyAccount">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Bell, ChatDotRound, Document, Money, Plus, View } from '@element-plus/icons-vue'
import api from '../api/client'
import { getUser } from '../utils/auth'

const user = ref(getUser())
const loading = ref(false)
const accounts = ref([])
const recentRecords = ref([])
const unreadCount = ref(0)
const verifyDialogVisible = ref(false)
const verifyFormRef = ref()
const verifying = ref(false)
const currentRow = ref(null)
const verifyForm = reactive({ password: '' })
const verifyRules = { password: [{ required: true, message: '请输入登录密码', trigger: 'blur' }] }
const applyDialogVisible = ref(false)
const applying = ref(false)
const applyForm = reactive({ accountType: 'CURRENT', tradePassword: '' })

const totalBalance = computed(() => accounts.value.reduce((sum, acc) => sum + Number(acc.availableBalance || 0), 0))

onMounted(loadDashboard)

async function loadDashboard() {
  loading.value = true
  try {
    const [accountRes, recordRes, notifyRes] = await Promise.all([
      api.get('/api/account/list'),
      api.get('/api/records', { params: { pageNo: 1, pageSize: 6 } }),
      api.get('/api/notifications')
    ])
    accounts.value = accountRes.data.map((item) => ({ ...item, showFull: false }))
    recentRecords.value = recordRes.data.records || []
    unreadCount.value = notifyRes.data.unreadCount || 0
  } finally {
    loading.value = false
  }
}

function handleShowCard(row) {
  if (row.showFull) {
    row.showFull = false
    return
  }
  currentRow.value = row
  verifyForm.password = ''
  verifyDialogVisible.value = true
}

async function handleVerify() {
  const valid = await verifyFormRef.value.validate().catch(() => false)
  if (!valid) return
  verifying.value = true
  try {
    const res = await api.post('/api/user/verify-password', { password: verifyForm.password, type: 'login' })
    if (res.data) {
      currentRow.value.showFull = true
      verifyDialogVisible.value = false
      setTimeout(() => {
        if (currentRow.value) currentRow.value.showFull = false
      }, 10000)
    }
  } finally {
    verifying.value = false
  }
}

async function applyAccount() {
  applying.value = true
  try {
    await api.post('/api/account/apply', applyForm)
    ElMessage.success('申请成功')
    applyDialogVisible.value = false
    await loadDashboard()
  } finally {
    applying.value = false
  }
}

function formatCard(cardNo) {
  return cardNo?.replace(/(.{4})/g, '$1 ').trim()
}

function typeName(type) {
  return ({ CURRENT: '活期', FIXED: '定期', CREDIT_CARD: '信用卡', LOAN_REPAYMENT: '贷款' }[type] || type)
}
</script>

<style scoped>
.quick-actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.quick-action {
  min-height: 78px;
  display: grid;
  align-content: center;
  justify-items: start;
  gap: 8px;
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 12px;
  color: var(--text);
  background: var(--surface);
  cursor: pointer;
}

.quick-action:hover {
  border-color: #b9d2ff;
  color: var(--primary);
  background: var(--primary-soft);
}

.quick-action.primary {
  border-color: var(--primary);
  color: #fff;
  background: var(--primary);
}

.quick-action.primary:hover {
  background: var(--primary-hover);
}

.quick-action .el-icon {
  font-size: 18px;
}

.quick-action span {
  font-weight: 650;
}

.mt {
  margin-top: 16px;
}

.record-line {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid var(--border);
}

.record-line:last-child {
  border-bottom: 0;
}

.record-line small {
  color: var(--muted);
}

.account-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

@media (max-width: 560px) {
  .quick-actions {
    grid-template-columns: 1fr;
  }
}
</style>
