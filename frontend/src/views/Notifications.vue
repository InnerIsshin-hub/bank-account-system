<template>
  <div class="page">
    <div>
      <h1 class="page-title">通知中心</h1>
      <p class="page-subtitle">动账提醒、登录提醒和业务处理结果</p>
    </div>
    <el-card>
      <el-table v-loading="loading" :data="records" stripe>
        <el-table-column label="状态" width="80">
          <template #default="{ row }"><el-tag :type="row.readFlag ? 'info' : 'warning'">{{ row.readFlag ? '已读' : '未读' }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="title" label="标题" width="180" />
        <el-table-column prop="content" label="内容" min-width="260" />
        <el-table-column prop="businessType" label="类型" width="150" />
        <el-table-column prop="createdAt" label="时间" width="180" />
        <el-table-column label="操作" width="110">
          <template #default="{ row }"><el-button v-if="!row.readFlag" link type="primary" @click="markRead(row.id)">标记已读</el-button></template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import api from '../api/client'

const loading = ref(false)
const records = ref([])

onMounted(load)

async function load() {
  loading.value = true
  try {
    const res = await api.get('/api/notifications')
    records.value = res.data.records
  } finally {
    loading.value = false
  }
}

async function markRead(id) {
  await api.put(`/api/notifications/${id}/read`)
  await load()
}
</script>
