<template>
  <div class="page">
    <div class="toolbar">
      <div>
        <h1 class="page-title">交易流水</h1>
        <p class="page-subtitle">按账户、方向、类型、日期、金额和关键字筛选</p>
      </div>
      <el-button :icon="Download" @click="exportCsv">导出 CSV</el-button>
    </div>

    <el-card class="section-card">
      <el-form :inline="true" :model="queryForm" class="filter-panel">
        <el-form-item label="账户">
          <el-select v-model="queryForm.accountNumber" placeholder="全部账户" clearable style="width: 220px">
            <el-option v-for="acc in accounts" :key="acc.accountNumber" :label="acc.accountNumberMasked" :value="acc.accountNumber" />
          </el-select>
        </el-form-item>
        <el-form-item label="方向">
          <el-select v-model="queryForm.direction" placeholder="全部" clearable style="width: 120px">
            <el-option label="转入" value="IN" />
            <el-option label="转出" value="OUT" />
          </el-select>
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="queryForm.transactionType" placeholder="全部" clearable style="width: 150px">
            <el-option label="转账" value="TRANSFER" />
            <el-option label="存款" value="DEPOSIT" />
            <el-option label="取款" value="WITHDRAW" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期">
          <el-date-picker v-model="queryForm.dateRange" type="daterange" value-format="YYYY-MM-DD" range-separator="至" start-placeholder="开始" end-placeholder="结束" />
        </el-form-item>
        <el-form-item label="关键字">
          <el-input v-model="queryForm.keyword" clearable placeholder="备注/对方信息" style="width: 180px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleQuery">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="records">
        <el-table-column prop="createdAt" label="交易时间" min-width="170" />
        <el-table-column prop="accountNumber" label="本方账户" min-width="160" />
        <el-table-column label="方向" width="90">
          <template #default="{ row }">
            <el-tag :type="row.direction === 'IN' ? 'success' : 'danger'">{{ row.direction === 'IN' ? '转入' : '转出' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="金额" width="140">
          <template #default="{ row }">
            <strong :class="['amount-strong', row.direction === 'IN' ? 'money-up' : 'money-down']">
              {{ row.direction === 'IN' ? '+' : '-' }} ¥ {{ Number(row.amount).toFixed(2) }}
            </strong>
          </template>
        </el-table-column>
        <el-table-column prop="balanceAfter" label="交易后余额" min-width="130" />
        <el-table-column label="对方" min-width="180">
          <template #default="{ row }">{{ row.counterpartyName }} {{ row.counterpartyAccount }}</template>
        </el-table-column>
        <el-table-column prop="remark" label="摘要" min-width="160" />
      </el-table>

      <el-pagination
        class="pager"
        background
        layout="prev, pager, next, sizes, total"
        :current-page="page.pageNo"
        :page-size="page.pageSize"
        :page-sizes="[10, 20, 50]"
        :total="page.total"
        @current-change="(value) => { page.pageNo = value; loadRecords() }"
        @size-change="(value) => { page.pageSize = value; page.pageNo = 1; loadRecords() }"
      />
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { Download, Search } from '@element-plus/icons-vue'
import api from '../api/client'

const route = useRoute()
const loading = ref(false)
const accounts = ref([])
const records = ref([])
const page = reactive({ pageNo: 1, pageSize: 10, total: 0 })
const queryForm = reactive({
  accountNumber: '',
  direction: '',
  transactionType: '',
  dateRange: [],
  keyword: ''
})

onMounted(async () => {
  await loadAccounts()
  if (route.query.account) queryForm.accountNumber = route.query.account
  await loadRecords()
})

async function loadAccounts() {
  const res = await api.get('/api/account/list')
  accounts.value = res.data
}

function params() {
  return {
    accountNumber: queryForm.accountNumber || undefined,
    direction: queryForm.direction || undefined,
    transactionType: queryForm.transactionType || undefined,
    startDate: queryForm.dateRange?.[0],
    endDate: queryForm.dateRange?.[1],
    keyword: queryForm.keyword || undefined,
    pageNo: page.pageNo,
    pageSize: page.pageSize
  }
}

async function loadRecords() {
  loading.value = true
  try {
    const res = await api.get('/api/records', { params: params() })
    records.value = res.data.records
    page.total = res.data.total
  } finally {
    loading.value = false
  }
}

function handleQuery() {
  page.pageNo = 1
  loadRecords()
}

function resetQuery() {
  Object.assign(queryForm, { accountNumber: '', direction: '', transactionType: '', dateRange: [], keyword: '' })
  handleQuery()
}

async function exportCsv() {
  const res = await api.get('/api/records/export', { params: params(), responseType: 'blob' })
  const blob = new Blob([res.data], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = 'records.csv'
  link.click()
  URL.revokeObjectURL(url)
}
</script>

<style scoped>
.pager {
  margin-top: 18px;
  justify-content: flex-end;
}
</style>
