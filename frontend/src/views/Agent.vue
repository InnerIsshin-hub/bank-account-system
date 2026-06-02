<template>
  <div class="page">
    <div>
      <h1 class="page-title">智能助手</h1>
      <p class="page-subtitle">TypeScript Agent 工作台</p>
    </div>

    <el-card class="agent-launcher">
      <div class="launcher-main">
        <div class="launcher-mark">AI</div>
        <div>
          <h2>Bank Agent Workbench</h2>
          <p>根目录 <code>agent/</code></p>
        </div>
      </div>
      <div class="launcher-actions">
        <el-button type="primary" @click="openAgent">打开工作台</el-button>
        <el-button @click="copyUrl">复制地址</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ElMessage } from 'element-plus'

const agentUrl = import.meta.env.VITE_AGENT_URL || 'http://localhost:5174'

function openAgent() {
  window.open(agentUrl, '_blank', 'noopener,noreferrer')
}

async function copyUrl() {
  await navigator.clipboard.writeText(agentUrl)
  ElMessage.success('已复制')
}
</script>

<style scoped>
.agent-launcher {
  max-width: 720px;
}

.launcher-main {
  display: grid;
  grid-template-columns: 56px 1fr;
  gap: 16px;
  align-items: center;
}

.launcher-mark {
  width: 56px;
  height: 56px;
  display: grid;
  place-items: center;
  border-radius: 8px;
  color: #fff;
  background: var(--primary);
  font-weight: 800;
}

.launcher-main h2 {
  margin: 0;
  font-size: 22px;
}

.launcher-main p {
  margin: 6px 0 0;
  color: var(--text-secondary);
}

.launcher-actions {
  margin-top: 22px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
</style>
