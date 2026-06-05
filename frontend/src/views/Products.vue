<template>
  <div class="page">
    <div class="toolbar">
      <div>
        <h1 class="page-title">存款理财</h1>
        <p class="page-subtitle">定期存款、理财产品和持仓管理</p>
      </div>
      <el-segmented v-model="productType" :options="typeOptions" @change="loadProducts" />
    </div>

    <el-row :gutter="16">
      <el-col :xs="24" :lg="14">
        <el-card>
          <template #header>产品列表</template>
          <el-table v-loading="loading" :data="products">
            <el-table-column label="产品" min-width="220">
              <template #default="{ row }">
                <div class="product-cell">
                  <div class="product-name">{{ row.productName }}</div>
                  <div class="product-meta">
                    <el-tag size="small" effect="plain">{{ typeName(row.productType) }}</el-tag>
                    <span>{{ row.productCode }}</span>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="风险" width="110">
              <template #default="{ row }">
                <el-tag :type="riskTag(row.riskLevel)" effect="light">{{ riskName(row.riskLevel) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="termDays" label="期限(天)" width="100" />
            <el-table-column label="预期收益" width="120">
              <template #default="{ row }">
                <strong class="yield">{{ Number(row.expectedYield || 0).toFixed(2) }}%</strong>
              </template>
            </el-table-column>
            <el-table-column label="起购金额" width="130">
              <template #default="{ row }">¥ {{ Number(row.minAmount || 0).toFixed(2) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="100">
              <template #default="{ row }"><el-button link type="primary" @click="openPurchase(row)">购买</el-button></template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="10">
        <el-card>
          <template #header>我的持仓</template>
          <el-table :data="holdings">
            <el-table-column label="产品" min-width="150">
              <template #default="{ row }">
                <span class="mono">{{ row.productCode }}</span>
              </template>
            </el-table-column>
            <el-table-column label="金额" width="130">
              <template #default="{ row }">¥ {{ Number(row.amount || 0).toFixed(2) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 'HOLDING' ? 'success' : 'info'" effect="light">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column width="90">
              <template #default="{ row }"><el-button v-if="row.status === 'HOLDING'" link type="primary" @click="redeem(row.id)">赎回</el-button></template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="purchaseVisible" title="购买产品" width="460px">
      <el-form :model="purchaseForm" label-position="top">
        <el-form-item label="付款账户">
          <el-select v-model="purchaseForm.accountNumber" class="full-width">
            <el-option v-for="acc in accounts" :key="acc.accountNumber" :label="`${acc.accountNumberMasked} ¥${Number(acc.availableBalance).toFixed(2)}`" :value="acc.accountNumber" />
          </el-select>
        </el-form-item>
        <el-form-item label="购买金额"><el-input-number v-model="purchaseForm.amount" :min="1" :precision="2" class="full-width" /></el-form-item>
        <el-form-item label="交易密码"><el-input v-model="purchaseForm.tradePassword" type="password" maxlength="6" show-password /></el-form-item>
        <el-checkbox v-model="purchaseForm.riskAccepted">我已阅读并确认风险提示</el-checkbox>
      </el-form>
      <template #footer>
        <el-button @click="purchaseVisible = false">取消</el-button>
        <el-button type="primary" :loading="purchasing" @click="purchase">确认购买</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api/client'

const typeOptions = [{ label: '全部', value: '' }, { label: '定存', value: 'DEPOSIT' }, { label: '理财', value: 'WEALTH' }]
const productType = ref('')
const loading = ref(false)
const purchasing = ref(false)
const products = ref([])
const holdings = ref([])
const accounts = ref([])
const selectedProduct = ref(null)
const purchaseVisible = ref(false)
const purchaseForm = reactive({ accountNumber: '', amount: 0, tradePassword: '', riskAccepted: false, productCode: '' })

onMounted(async () => {
  await Promise.all([loadProducts(), loadHoldings(), loadAccounts()])
})

async function loadProducts() {
  loading.value = true
  try {
    const res = await api.get('/api/products', { params: { productType: productType.value || undefined } })
    products.value = res.data
  } finally {
    loading.value = false
  }
}

async function loadHoldings() {
  const res = await api.get('/api/products/holdings')
  holdings.value = res.data
}

async function loadAccounts() {
  const res = await api.get('/api/account/list')
  accounts.value = res.data
  purchaseForm.accountNumber = accounts.value[0]?.accountNumber || ''
}

function openPurchase(product) {
  selectedProduct.value = product
  Object.assign(purchaseForm, { productCode: product.productCode, amount: Number(product.minAmount), tradePassword: '', riskAccepted: product.productType === 'DEPOSIT' })
  purchaseVisible.value = true
}

function typeName(type) {
  return ({ DEPOSIT: '定期存款', WEALTH: '理财', LOAN: '贷款', CREDIT_CARD: '信用卡' }[type] || type)
}

function riskName(level) {
  return ({ LOW: '低风险', MEDIUM: '中风险', HIGH: '高风险' }[level] || level)
}

function riskTag(level) {
  return ({ LOW: 'success', MEDIUM: 'warning', HIGH: 'danger' }[level] || 'info')
}

async function purchase() {
  purchasing.value = true
  try {
    await api.post('/api/products/purchase', purchaseForm)
    ElMessage.success('购买成功')
    purchaseVisible.value = false
    await Promise.all([loadHoldings(), loadAccounts()])
  } finally {
    purchasing.value = false
  }
}

async function redeem(id) {
  await api.post(`/api/products/holdings/${id}/redeem`)
  ElMessage.success('赎回成功')
  await Promise.all([loadHoldings(), loadAccounts()])
}
</script>

<style scoped>
.product-cell {
  display: grid;
  gap: 6px;
}

.product-name {
  color: var(--text);
  font-weight: 650;
}

.product-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--muted);
  font-size: 12px;
}

.yield {
  color: var(--primary);
  font-weight: 650;
}
</style>
