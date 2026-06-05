<template>
  <div class="page">
    <div class="toolbar">
      <div>
        <h1 class="page-title">信用卡中心</h1>
        <p class="page-subtitle">在线申卡、激活、账单查询和分期 demo</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="applyVisible = true">在线申卡</el-button>
    </div>
    <el-card class="section-card">
      <el-table v-loading="loading" :data="cards" @row-click="loadBills">
        <el-table-column prop="cardNumber" label="卡号" />
        <el-table-column label="额度">
          <template #default="{ row }">¥ {{ Number(row.creditLimit || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="已用">
          <template #default="{ row }">¥ {{ Number(row.usedAmount || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="points" label="积分" />
        <el-table-column label="状态">
          <template #default="{ row }">
            <el-tag class="status-tag" :type="row.status === 'ACTIVE' ? 'success' : 'warning'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }"><el-button v-if="row.status !== 'ACTIVE'" link type="primary" @click.stop="openActivate(row)">激活</el-button></template>
        </el-table-column>
      </el-table>
    </el-card>
    <el-card class="section-card mt">
      <template #header>账单</template>
      <el-table :data="bills">
        <el-table-column prop="billMonth" label="月份" />
        <el-table-column label="账单金额">
          <template #default="{ row }">¥ {{ Number(row.billAmount || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="最低还款">
          <template #default="{ row }">¥ {{ Number(row.minRepayment || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="dueDate" label="还款日" />
        <el-table-column label="状态">
          <template #default="{ row }">
            <el-tag effect="plain">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }"><el-button link type="primary" @click="installment(row.id)">分期</el-button></template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="applyVisible" title="信用卡申请" width="460px">
      <el-form :model="applyForm" label-position="top">
        <el-form-item label="产品编码"><el-input v-model="applyForm.productCode" /></el-form-item>
        <el-form-item label="职业"><el-input v-model="applyForm.occupation" /></el-form-item>
        <el-form-item label="月收入"><el-input-number v-model="applyForm.monthlyIncome" :min="0" :precision="2" class="full-width" /></el-form-item>
        <el-form-item label="地址"><el-input v-model="applyForm.address" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="applyVisible = false">取消</el-button>
        <el-button type="primary" @click="applyCard">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="activateVisible" title="激活信用卡" width="360px">
      <el-input v-model="tradePassword" type="password" maxlength="6" placeholder="交易密码" show-password />
      <template #footer>
        <el-button @click="activateVisible = false">取消</el-button>
        <el-button type="primary" @click="activate">确认激活</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import api from '../api/client'

const loading = ref(false)
const cards = ref([])
const bills = ref([])
const applyVisible = ref(false)
const activateVisible = ref(false)
const currentCard = ref(null)
const tradePassword = ref('')
const applyForm = reactive({ productCode: 'CARD-GOLD', occupation: '', monthlyIncome: 8000, address: '' })

onMounted(loadCards)

async function loadCards() {
  loading.value = true
  try {
    const res = await api.get('/api/credit-cards')
    cards.value = res.data
  } finally {
    loading.value = false
  }
}

async function applyCard() {
  await api.post('/api/credit-cards/apply', applyForm)
  ElMessage.success('申请成功')
  applyVisible.value = false
  await loadCards()
}

async function loadBills(row) {
  const res = await api.get(`/api/credit-cards/${row.id}/bills`)
  bills.value = res.data
}

function openActivate(row) {
  currentCard.value = row
  tradePassword.value = ''
  activateVisible.value = true
}

async function activate() {
  await api.post(`/api/credit-cards/${currentCard.value.id}/activate`, { tradePassword: tradePassword.value })
  ElMessage.success('激活成功')
  activateVisible.value = false
  await loadCards()
}

async function installment(billId) {
  const res = await api.post(`/api/credit-cards/bills/${billId}/installment`, null, { params: { periods: 3 } })
  ElMessage.success(`分期成功，手续费 ${res.data.totalFee}`)
}
</script>

<style scoped>
.mt {
  margin-top: 16px;
}
</style>
