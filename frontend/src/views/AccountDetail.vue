<template>
  <div class="page">
    <div class="toolbar">
      <div>
        <h1 class="page-title">账户详情</h1>
        <p class="page-subtitle">{{ detail.accountNumberMasked || detail.accountNumber }}</p>
      </div>
      <div class="table-actions">
        <el-button :icon="Refresh" @click="loadDetail">刷新</el-button>
        <el-button type="primary" @click="$router.push({ path: '/transfer', query: { from: accountNumber } })">转账</el-button>
        <el-button @click="$router.push({ path: '/records', query: { account: accountNumber } })">流水</el-button>
      </div>
    </div>

    <div class="metric-grid">
      <div class="metric">
        <div class="metric-label">可用余额</div>
        <div class="metric-value">¥ {{ money(detail.availableBalance) }}</div>
      </div>
      <div class="metric">
        <div class="metric-label">冻结余额</div>
        <div class="metric-value">¥ {{ money(detail.frozenBalance) }}</div>
      </div>
      <div class="metric">
        <div class="metric-label">账户状态</div>
        <div class="metric-value">{{ detail.status || '-' }}</div>
      </div>
    </div>

    <el-row :gutter="16">
      <el-col :xs="24" :lg="9">
        <el-card v-loading="loading" class="section-card">
          <template #header>账户资料</template>
          <div class="detail-list">
            <p><span>账号</span><strong class="mono">{{ detail.accountNumberFull || detail.accountNumber }}</strong></p>
            <p><span>类型</span><strong>{{ typeName(detail.accountType) }}</strong></p>
            <p><span>币种</span><strong>{{ detail.currency }}</strong></p>
            <p><span>开户时间</span><strong>{{ detail.openTime || '-' }}</strong></p>
          </div>
          <div class="detail-actions">
            <el-button v-if="detail.status !== 'FROZEN'" type="warning" @click="freeze">冻结</el-button>
            <el-button v-else type="success" @click="unfreeze">解冻</el-button>
            <el-button type="danger" @click="closeAccount">销户</el-button>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="15">
        <el-card class="section-card">
          <template #header>最近交易</template>
          <el-table :data="detail.recentRecords || []">
            <el-table-column prop="createdAt" label="时间" min-width="170" />
            <el-table-column label="方向" width="90">
              <template #default="{ row }">
                <el-tag :type="row.direction === 'IN' ? 'success' : 'danger'">{{ row.direction === 'IN' ? '转入' : '转出' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="金额" width="130">
              <template #default="{ row }">
                <strong :class="['amount-strong', row.direction === 'IN' ? 'money-up' : 'money-down']">
                  {{ row.direction === 'IN' ? '+' : '-' }}{{ money(row.amount) }}
                </strong>
              </template>
            </el-table-column>
            <el-table-column prop="counterpartyName" label="对方" min-width="120" />
            <el-table-column prop="remark" label="摘要" min-width="140" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import api from '../api/client'

const route = useRoute()
const loading = ref(false)
const detail = reactive({})
const accountNumber = computed(() => route.params.accountNumber)

onMounted(loadDetail)

async function loadDetail() {
  loading.value = true
  try {
    const res = await api.get(`/api/account/${accountNumber.value}`)
    Object.assign(detail, res.data)
  } finally {
    loading.value = false
  }
}

async function freeze() {
  const { value } = await ElMessageBox.prompt('请输入冻结原因', '冻结账户', { inputValue: '用户主动挂失' })
  await api.post(`/api/account/${accountNumber.value}/freeze`, { reason: value })
  ElMessage.success('账户已冻结')
  await loadDetail()
}

async function unfreeze() {
  const { value } = await ElMessageBox.prompt('请输入解冻原因', '解冻账户', { inputValue: '风险解除' })
  await api.post(`/api/account/${accountNumber.value}/unfreeze`, { reason: value })
  ElMessage.success('账户已解冻')
  await loadDetail()
}

async function closeAccount() {
  const { value } = await ElMessageBox.prompt('销户要求账户余额为 0，请输入登录密码确认', '销户确认', {
    inputType: 'password'
  })
  await api.delete(`/api/account/${accountNumber.value}`, { data: { password: value, reason: '用户主动销户' } })
  ElMessage.success('销户成功')
  await loadDetail()
}

function money(value) {
  return Number(value || 0).toFixed(2)
}

function typeName(type) {
  return ({ CURRENT: '活期', FIXED: '定期', CREDIT_CARD: '信用卡', LOAN_REPAYMENT: '贷款' }[type] || type || '-')
}
</script>

<style scoped>
.detail-list {
  display: grid;
  gap: 12px;
}

.detail-list p {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin: 0;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--border);
}

.detail-list span {
  color: var(--muted);
}

.detail-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 18px;
}
</style>
