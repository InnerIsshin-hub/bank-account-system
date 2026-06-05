<template>
  <div class="page">
    <div>
      <h1 class="page-title">智能助手</h1>
      <p class="page-subtitle">TypeScript Agent 工作台</p>
    </div>

    <el-card class="agent-launcher section-card">
      <div class="launcher-grid">
        <div class="launcher-main">
          <div class="launcher-mark">AI</div>
          <div>
            <h2>Bank Agent Workbench</h2>
            <p>{{ agentUrl }}</p>
          </div>
        </div>
        <div class="launcher-actions">
          <el-button type="primary" @click="openAgent">打开工作台</el-button>
          <el-button @click="copyUrl">复制地址</el-button>
        </div>
      </div>
      <div class="agent-kpis">
        <div>
          <span>技能模式</span>
          <strong>白名单</strong>
        </div>
        <div>
          <span>高风险操作</span>
          <strong>草稿确认</strong>
        </div>
        <div>
          <span>入口状态</span>
          <strong>可用</strong>
        </div>
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
  max-width: 880px;
}

.launcher-grid {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  flex-wrap: wrap;
}

.launcher-main {
  display: grid;
  grid-template-columns: 56px minmax(0, 1fr);
  gap: 16px;
  align-items: center;
  min-width: 0;
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
  color: var(--muted);
  word-break: break-all;
}

.launcher-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.agent-kpis {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-top: 24px;
}

.agent-kpis div {
  padding: 14px;
  border: 1px solid var(--border-light);
  border-radius: 8px;
  background: var(--surface-soft);
}

.agent-kpis span {
  display: block;
  color: var(--muted);
  font-size: 12px;
}

.agent-kpis strong {
  display: block;
  margin-top: 8px;
  color: var(--text);
}

@media (max-width: 720px) {
  .agent-kpis {
    grid-template-columns: 1fr;
  }
}
</style>
