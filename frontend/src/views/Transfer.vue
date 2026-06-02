<template>
  <div class="page">
    <div class="toolbar">
      <div>
        <h1 class="page-title">转账汇款</h1>
        <p class="page-subtitle">行内实时到账，跨行转账进入清算处理状态</p>
      </div>
      <el-button :icon="User" @click="$router.push('/contacts')">常用收款人</el-button>
    </div>

    <el-card>
      <el-tabs v-model="activeTab">
        <el-tab-pane label="单笔转账" name="single">
          <el-form ref="formRef" :model="form" :rules="rules" label-width="120px" class="form-narrow">
            <el-form-item label="付款账户" prop="fromAccount">
              <el-select v-model="form.fromAccount" placeholder="请选择付款账户" class="full-width">
                <el-option
                  v-for="acc in accounts"
                  :key="acc.accountNumber"
                  :label="`${acc.accountNumberMasked} | ¥${Number(acc.availableBalance).toFixed(2)}`"
                  :value="acc.accountNumber"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="常用收款人">
              <el-select v-model="selectedContactId" clearable placeholder="可选" class="full-width" @change="selectContact">
                <el-option v-for="contact in contacts" :key="contact.id" :label="`${contact.contactName} ${contact.accountNumberMasked}`" :value="contact.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="收款账户" prop="toAccount">
              <el-input v-model="form.toAccount" placeholder="请输入对方卡号" @blur="precheck" />
            </el-form-item>
            <el-form-item label="收款人姓名">
              <el-input v-model="form.toName" placeholder="可填写用于核对" />
            </el-form-item>
            <el-form-item label="收款银行">
              <el-input v-model="form.toBankName" placeholder="本行可留空，跨行请填写银行名称" @blur="precheck" />
            </el-form-item>
            <el-form-item label="转账金额" prop="amount">
              <el-input-number v-model="form.amount" :min="0.01" :precision="2" :step="100" controls-position="right" />
            </el-form-item>
            <el-form-item label="定时执行">
              <el-date-picker
                v-model="form.scheduledAt"
                type="datetime"
                value-format="YYYY-MM-DDTHH:mm:ss"
                placeholder="不选择则立即执行"
                class="full-width"
              />
            </el-form-item>
            <el-form-item label="备注">
              <el-input v-model="form.remark" maxlength="200" show-word-limit />
            </el-form-item>
            <el-alert v-if="precheckResult" class="precheck" :type="precheckResult.risk?.action === 'PASS' ? 'success' : 'warning'" show-icon :closable="false">
              <template #title>
                收款人 {{ precheckResult.counterpartyNameMasked }}，账户 {{ precheckResult.counterpartyAccountMasked }}，风控结果 {{ precheckResult.risk?.action }}
              </template>
            </el-alert>
            <el-form-item>
              <el-button type="primary" :loading="loading" @click="submitTransfer">提交确认</el-button>
              <el-button @click="resetForm">重置</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="批量转账" name="batch">
          <div class="batch-layout">
            <el-form :model="batchForm" label-width="120px" class="form-narrow">
              <el-form-item label="付款账户">
                <el-select v-model="batchForm.fromAccount" placeholder="请选择付款账户" class="full-width">
                  <el-option
                    v-for="acc in accounts"
                    :key="acc.accountNumber"
                    :label="`${acc.accountNumberMasked} | ¥${Number(acc.availableBalance).toFixed(2)}`"
                    :value="acc.accountNumber"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="批量明细">
                <el-input
                  v-model="batchForm.text"
                  type="textarea"
                  :rows="7"
                  placeholder="每行：收款账户,收款人,金额,备注,银行名称"
                />
              </el-form-item>
              <el-form-item label="交易密码">
                <el-input v-model="batchForm.tradePassword" type="password" maxlength="6" show-password />
              </el-form-item>
              <el-form-item label="短信/OTP">
                <el-input v-model="batchForm.otpCode" placeholder="中风险 demo 口令 000000" />
              </el-form-item>
              <el-form-item>
                <el-button :loading="batchChecking" @click="batchPrecheck">批量预校验</el-button>
                <el-button type="primary" :loading="batchExecuting" @click="batchExecute">执行批量转账</el-button>
              </el-form-item>
            </el-form>

            <el-table v-if="batchResult.length" :data="batchResult" stripe>
              <el-table-column prop="toAccount" label="收款账户" min-width="160" />
              <el-table-column prop="counterpartyNameMasked" label="收款人" width="120" />
              <el-table-column prop="amount" label="金额" width="110" />
              <el-table-column prop="success" label="结果" width="90">
                <template #default="{ row }">
                  <el-tag :type="row.success === false ? 'danger' : 'success'">{{ row.success === false ? '失败' : '通过' }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="message" label="说明" min-width="180" />
            </el-table>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="confirmVisible" title="转账确认" width="460px" :close-on-click-modal="false">
      <div class="confirm-lines">
        <p><span>订单号</span><strong>{{ order?.orderNo }}</strong></p>
        <p><span>金额</span><strong>¥ {{ Number(form.amount || 0).toFixed(2) }}</strong></p>
        <p><span>收款账户</span><strong>{{ precheckResult?.counterpartyAccountMasked }}</strong></p>
        <p><span>订单状态</span><strong>{{ order?.status }}</strong></p>
        <p><span>风控动作</span><strong>{{ order?.riskAction }}</strong></p>
      </div>
      <el-form :model="executeForm" label-position="top">
        <el-form-item label="交易密码">
          <el-input v-model="executeForm.tradePassword" type="password" maxlength="6" show-password />
        </el-form-item>
        <el-form-item v-if="order?.riskAction !== 'PASS'" label="短信/OTP 验证码">
          <el-input v-model="executeForm.otpCode" placeholder="demo 验证码 000000" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="confirmVisible = false">取消</el-button>
        <el-button type="primary" :loading="executing" @click="executeTransfer">确认转账</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User } from '@element-plus/icons-vue'
import api from '../api/client'

const router = useRouter()
const route = useRoute()
const formRef = ref()
const activeTab = ref('single')
const loading = ref(false)
const executing = ref(false)
const batchChecking = ref(false)
const batchExecuting = ref(false)
const accounts = ref([])
const contacts = ref([])
const selectedContactId = ref(null)
const precheckResult = ref(null)
const confirmVisible = ref(false)
const order = ref(null)
const batchResult = ref([])
const executeForm = reactive({ tradePassword: '', otpCode: '' })
const batchForm = reactive({ fromAccount: '', text: '', tradePassword: '', otpCode: '000000' })

const form = reactive({
  fromAccount: '',
  toAccount: '',
  toName: '',
  toBankName: '',
  amount: null,
  scheduledAt: '',
  remark: ''
})

const rules = {
  fromAccount: [{ required: true, message: '请选择付款账户', trigger: 'change' }],
  toAccount: [
    { required: true, message: '请输入收款账户', trigger: 'blur' },
    { pattern: /^\d{16,19}$/, message: '卡号格式不正确', trigger: 'blur' }
  ],
  amount: [{ required: true, type: 'number', min: 0.01, message: '金额必须大于 0', trigger: 'blur' }]
}

onMounted(async () => {
  await Promise.all([loadAccounts(), loadContacts()])
  if (route.query.from) form.fromAccount = route.query.from
})

async function loadAccounts() {
  const res = await api.get('/api/account/list')
  accounts.value = res.data
  if (!form.fromAccount && accounts.value[0]) form.fromAccount = accounts.value[0].accountNumber
  if (!batchForm.fromAccount && accounts.value[0]) batchForm.fromAccount = accounts.value[0].accountNumber
}

async function loadContacts() {
  const res = await api.get('/api/contacts')
  contacts.value = res.data
}

function selectContact(id) {
  const contact = contacts.value.find((item) => item.id === id)
  if (!contact) return
  form.toAccount = contact.accountNumber
  form.toName = contact.contactName
  precheck()
}

async function precheck() {
  if (!form.toAccount || !/^\d{16,19}$/.test(form.toAccount)) return
  const res = await api.post('/api/transfer/precheck', {
    fromAccount: form.fromAccount,
    toAccount: form.toAccount,
    toName: form.toName,
    toBankName: form.toBankName,
    amount: form.amount
  })
  precheckResult.value = res.data
}

async function submitTransfer() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await precheck()
    const res = await api.post('/api/transfer/confirm', {
      ...form,
      scheduledAt: form.scheduledAt || null,
      idempotencyKey: crypto.randomUUID?.() || `${Date.now()}`
    })
    order.value = res.data
    if (order.value.status === 'SCHEDULED') {
      ElMessage.success('定时转账已创建，到时会重新校验后执行')
      await loadAccounts()
      return
    }
    executeForm.tradePassword = ''
    executeForm.otpCode = order.value.riskAction === 'PASS' ? '' : '000000'
    confirmVisible.value = true
  } finally {
    loading.value = false
  }
}

async function executeTransfer() {
  executing.value = true
  try {
    const res = await api.post('/api/transfer/execute', {
      orderNo: order.value.orderNo,
      tradePassword: executeForm.tradePassword,
      otpCode: executeForm.otpCode
    })
    ElMessage.success(`转账${res.data.status === 'SUCCESS' ? '成功' : '已提交'}`)
    confirmVisible.value = false
    await loadAccounts()
    router.push('/records')
  } finally {
    executing.value = false
  }
}

function resetForm() {
  formRef.value.resetFields()
  form.remark = ''
  form.toName = ''
  form.toBankName = ''
  form.scheduledAt = ''
  precheckResult.value = null
  selectedContactId.value = null
}

function parseBatchItems() {
  return batchForm.text
    .split('\n')
    .map((line) => line.trim())
    .filter(Boolean)
    .map((line, index) => {
      const [toAccount, toName, amount, remark, toBankName] = line.split(',').map((item) => item?.trim() || '')
      return {
        fromAccount: batchForm.fromAccount,
        toAccount,
        toName,
        amount: Number(amount),
        remark,
        toBankName,
        idempotencyKey: `batch-${Date.now()}-${index}-${Math.random().toString(16).slice(2)}`
      }
    })
}

async function batchPrecheck() {
  const items = parseBatchItems()
  if (!items.length) {
    ElMessage.warning('请先填写批量明细')
    return
  }
  batchChecking.value = true
  try {
    const res = await api.post('/api/transfer/batch/precheck', { items })
    batchResult.value = res.data.details.map((item, index) => ({ ...items[index], ...item }))
  } finally {
    batchChecking.value = false
  }
}

async function batchExecute() {
  const items = parseBatchItems()
  if (!items.length) {
    ElMessage.warning('请先填写批量明细')
    return
  }
  batchExecuting.value = true
  try {
    const res = await api.post('/api/transfer/batch/execute', {
      items,
      tradePassword: batchForm.tradePassword,
      otpCode: batchForm.otpCode
    })
    batchResult.value = res.data.details.map((item, index) => ({ ...items[index], ...item }))
    ElMessage.success(`批量任务 ${res.data.status}，成功 ${res.data.successCount} 笔`)
    await loadAccounts()
  } finally {
    batchExecuting.value = false
  }
}
</script>

<style scoped>
.precheck {
  margin: 0 0 18px 120px;
  max-width: 560px;
}

.confirm-lines {
  display: grid;
  gap: 10px;
  margin-bottom: 16px;
}

.confirm-lines p {
  display: flex;
  justify-content: space-between;
  margin: 0;
}

.confirm-lines span {
  color: var(--muted);
}

.batch-layout {
  display: grid;
  gap: 16px;
}

@media (max-width: 720px) {
  .precheck {
    margin-left: 0;
  }
}
</style>
