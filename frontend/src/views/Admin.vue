<template>
  <div class="page">
    <div class="toolbar">
      <div>
        <h1 class="page-title">后台管理</h1>
        <p class="page-subtitle">用户、账户、交易、风控与审计的集中视图</p>
      </div>
      <div class="table-actions">
        <el-button :icon="Refresh" @click="loadAll">刷新</el-button>
        <el-button @click="runReconciliation">日终对账</el-button>
        <el-button @click="scanCompensation">补偿扫描</el-button>
      </div>
    </div>

    <div class="metric-grid admin-metrics">
      <div class="metric">
        <div class="metric-label">用户数</div>
        <div class="metric-value">{{ users.length }}</div>
      </div>
      <div class="metric">
        <div class="metric-label">账户数</div>
        <div class="metric-value">{{ accounts.length }}</div>
      </div>
      <div class="metric">
        <div class="metric-label">交易订单</div>
        <div class="metric-value">{{ transfers.length }}</div>
      </div>
      <div class="metric">
        <div class="metric-label">未关闭风险</div>
        <div class="metric-value">{{ openRiskCount }}</div>
      </div>
    </div>

    <el-card class="section-card">
      <el-tabs>
        <el-tab-pane label="用户">
          <el-table v-loading="loading" :data="users">
            <el-table-column prop="userId" label="ID" width="80" />
            <el-table-column prop="userName" label="姓名" />
            <el-table-column prop="phoneMasked" label="手机号" />
            <el-table-column label="状态">
              <template #default="{ row }">
                <el-tag class="status-tag" :type="row.status === 'NORMAL' ? 'success' : 'danger'">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="KYC">
              <template #default="{ row }">
                <el-tag effect="plain">{{ row.kycStatus }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="160">
              <template #default="{ row }">
                <el-button link type="danger" @click="freezeUser(row.userId)">冻结</el-button>
                <el-button link type="primary" @click="unfreezeUser(row.userId)">解冻</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="账户">
          <el-table :data="accounts">
            <el-table-column prop="accountNumber" label="账户" />
            <el-table-column prop="userId" label="用户" />
            <el-table-column prop="accountType" label="类型" />
            <el-table-column label="状态">
              <template #default="{ row }">
                <el-tag class="status-tag" :type="row.status === 'NORMAL' ? 'success' : 'warning'">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="余额">
              <template #default="{ row }">¥ {{ Number(row.availableBalance || 0).toFixed(2) }}</template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="交易订单">
          <el-table :data="transfers">
            <el-table-column prop="orderNo" label="订单号" min-width="190" />
            <el-table-column prop="fromAccount" label="付款账户" />
            <el-table-column prop="toAccount" label="收款账户" />
            <el-table-column label="金额">
              <template #default="{ row }">¥ {{ Number(row.amount || 0).toFixed(2) }}</template>
            </el-table-column>
            <el-table-column label="状态">
              <template #default="{ row }">
                <el-tag class="status-tag" :type="row.status === 'SUCCESS' ? 'success' : 'info'">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="风控">
              <template #default="{ row }">
                <el-tag :type="row.riskAction === 'PASS' ? 'success' : 'warning'">{{ row.riskAction }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="风控事件">
          <el-table :data="risks">
            <el-table-column prop="eventId" label="事件" min-width="190" />
            <el-table-column prop="riskType" label="类型" />
            <el-table-column label="等级">
              <template #default="{ row }">
                <el-tag :type="row.riskLevel === 'HIGH' ? 'danger' : row.riskLevel === 'MEDIUM' ? 'warning' : 'success'">{{ row.riskLevel }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="riskScore" label="分数" />
            <el-table-column prop="action" label="动作" />
            <el-table-column prop="reason" label="原因" min-width="220" />
            <el-table-column label="状态">
              <template #default="{ row }">
                <el-tag effect="plain">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="审计日志">
          <el-table :data="audits">
            <el-table-column prop="operationType" label="操作" />
            <el-table-column prop="resourceType" label="资源" />
            <el-table-column prop="result" label="结果" />
            <el-table-column prop="traceId" label="TraceId" />
            <el-table-column prop="createdAt" label="时间" />
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import api from '../api/client'

const loading = ref(false)
const users = ref([])
const accounts = ref([])
const transfers = ref([])
const audits = ref([])
const risks = ref([])
const openRiskCount = computed(() => risks.value.filter((item) => item.status === 'OPEN').length)

onMounted(loadAll)

async function loadAll() {
  loading.value = true
  try {
    const [userRes, accountRes, transferRes, riskRes, auditRes] = await Promise.all([
      api.get('/api/admin/users'),
      api.get('/api/admin/accounts'),
      api.get('/api/admin/transfers'),
      api.get('/api/risk/events'),
      api.get('/api/admin/audits')
    ])
    users.value = userRes.data
    accounts.value = accountRes.data
    transfers.value = transferRes.data
    risks.value = riskRes.data
    audits.value = auditRes.data
  } finally {
    loading.value = false
  }
}

async function freezeUser(userId) {
  await api.post(`/api/admin/users/${userId}/freeze`)
  ElMessage.success('用户已冻结')
  await loadAll()
}

async function unfreezeUser(userId) {
  await api.post(`/api/admin/users/${userId}/unfreeze`)
  ElMessage.success('用户已解冻')
  await loadAll()
}

async function runReconciliation() {
  const res = await api.post('/api/ops/reconciliation/run')
  ElMessage.success(`对账完成：${res.data.status}`)
}

async function scanCompensation() {
  const res = await api.post('/api/ops/compensation/scan')
  ElMessage.success(`补偿扫描完成：发现 ${res.data.processingOlderThan30Min} 笔待处理`)
}
</script>
