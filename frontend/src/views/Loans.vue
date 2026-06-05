<template>
  <div class="page">
    <div>
      <h1 class="page-title">贷款中心</h1>
      <p class="page-subtitle">消费贷 demo 自动评分，审批通过后可模拟放款到账</p>
    </div>
    <el-row :gutter="16">
      <el-col :xs="24" :lg="9">
        <el-card class="section-card">
          <template #header>贷款申请</template>
          <el-form :model="form" label-position="top">
            <el-form-item label="贷款产品"><el-input v-model="form.productCode" /></el-form-item>
            <el-form-item label="申请人"><el-input v-model="form.applicantName" /></el-form-item>
            <el-form-item label="金额"><el-input-number v-model="form.amount" :min="1000" :precision="2" class="full-width" /></el-form-item>
            <el-form-item label="期限(月)"><el-input-number v-model="form.termMonths" :min="1" :max="60" class="full-width" /></el-form-item>
            <el-form-item label="放款账户">
              <el-select v-model="form.receiveAccount" class="full-width">
                <el-option v-for="acc in accounts" :key="acc.accountNumber" :label="acc.accountNumberMasked" :value="acc.accountNumber" />
              </el-select>
            </el-form-item>
            <el-form-item label="用途"><el-input v-model="form.purpose" /></el-form-item>
            <el-button type="primary" :loading="submitting" @click="apply">提交申请</el-button>
          </el-form>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="15">
        <el-card class="section-card">
          <template #header>我的申请</template>
          <el-table v-loading="loading" :data="applications" @row-click="loadPlans">
            <el-table-column prop="id" label="编号" width="80" />
            <el-table-column prop="productCode" label="产品" />
            <el-table-column label="金额">
              <template #default="{ row }">¥ {{ Number(row.amount || 0).toFixed(2) }}</template>
            </el-table-column>
            <el-table-column prop="termMonths" label="期限" />
            <el-table-column prop="autoScore" label="评分" />
            <el-table-column label="状态">
              <template #default="{ row }">
                <el-tag class="status-tag" :type="row.status === 'APPROVED' ? 'success' : row.status === 'REJECTED' ? 'danger' : 'warning'">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
        <el-card class="section-card mt">
          <template #header>还款计划</template>
          <el-table :data="plans">
            <el-table-column prop="periodNo" label="期数" width="80" />
            <el-table-column prop="dueDate" label="到期日" />
            <el-table-column label="本金">
              <template #default="{ row }">¥ {{ Number(row.principal || 0).toFixed(2) }}</template>
            </el-table-column>
            <el-table-column label="利息">
              <template #default="{ row }">¥ {{ Number(row.interest || 0).toFixed(2) }}</template>
            </el-table-column>
            <el-table-column label="应还">
              <template #default="{ row }">¥ {{ Number(row.totalAmount || 0).toFixed(2) }}</template>
            </el-table-column>
            <el-table-column label="状态">
              <template #default="{ row }">
                <el-tag effect="plain">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api/client'

const loading = ref(false)
const submitting = ref(false)
const applications = ref([])
const plans = ref([])
const accounts = ref([])
const form = reactive({ productCode: 'LOAN-CONSUME', applicantName: '', amount: 10000, termMonths: 12, purpose: '', receiveAccount: '' })

onMounted(async () => {
  await Promise.all([loadAccounts(), loadApplications()])
})

async function loadAccounts() {
  const res = await api.get('/api/account/list')
  accounts.value = res.data
  form.receiveAccount = accounts.value[0]?.accountNumber || ''
}

async function loadApplications() {
  loading.value = true
  try {
    const res = await api.get('/api/loans')
    applications.value = res.data
  } finally {
    loading.value = false
  }
}

async function apply() {
  submitting.value = true
  try {
    await api.post('/api/loans/apply', form)
    ElMessage.success('申请已提交')
    await loadApplications()
  } finally {
    submitting.value = false
  }
}

async function loadPlans(row) {
  const res = await api.get(`/api/loans/${row.id}/repayment-plans`)
  plans.value = res.data
}
</script>

<style scoped>
.mt {
  margin-top: 16px;
}
</style>
