<template>
  <div class="page">
    <div class="toolbar">
      <div>
        <h1 class="page-title">常用收款人</h1>
        <p class="page-subtitle">维护转账时可快速选择的收款账户</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openDialog()">新增收款人</el-button>
    </div>

    <el-card class="section-card">
      <el-table v-loading="loading" :data="contacts">
        <el-table-column prop="contactName" label="姓名" width="140" />
        <el-table-column prop="accountNumberMasked" label="账户" min-width="180" />
        <el-table-column prop="bankName" label="银行" width="140" />
        <el-table-column prop="phoneMasked" label="手机号" width="150" />
        <el-table-column label="操作" width="170">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" title="收款人" width="430px">
      <el-form :model="form" label-position="top">
        <el-form-item label="姓名"><el-input v-model="form.contactName" /></el-form-item>
        <el-form-item label="账户"><el-input v-model="form.accountNumber" /></el-form-item>
        <el-form-item label="银行"><el-input v-model="form.bankName" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import api from '../api/client'

const loading = ref(false)
const saving = ref(false)
const contacts = ref([])
const dialogVisible = ref(false)
const form = reactive({ id: null, contactName: '', accountNumber: '', bankName: '本行', phone: '' })

onMounted(loadContacts)

async function loadContacts() {
  loading.value = true
  try {
    const res = await api.get('/api/contacts')
    contacts.value = res.data
  } finally {
    loading.value = false
  }
}

function openDialog(row) {
  Object.assign(form, row ? { ...row, phone: '' } : { id: null, contactName: '', accountNumber: '', bankName: '本行', phone: '' })
  dialogVisible.value = true
}

async function save() {
  saving.value = true
  try {
    await api.post('/api/contacts', form)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    await loadContacts()
  } finally {
    saving.value = false
  }
}

async function remove(id) {
  await ElMessageBox.confirm('确认删除该收款人吗？', '提示')
  await api.delete(`/api/contacts/${id}`)
  ElMessage.success('删除成功')
  await loadContacts()
}
</script>
