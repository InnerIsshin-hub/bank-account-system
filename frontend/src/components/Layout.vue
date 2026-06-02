<template>
  <el-container class="layout-container">
    <el-aside width="232px" class="sidebar">
      <div class="logo">
        <el-icon><Wallet /></el-icon>
        <span>网上银行</span>
      </div>
      <el-menu :default-active="$route.path" router background-color="#1f2f46" text-color="#c8d2df" active-text-color="#ffffff">
        <el-menu-item index="/dashboard"><el-icon><DataAnalysis /></el-icon><span>主控面板</span></el-menu-item>
        <el-menu-item index="/transfer"><el-icon><Money /></el-icon><span>转账汇款</span></el-menu-item>
        <el-menu-item index="/records"><el-icon><Document /></el-icon><span>交易流水</span></el-menu-item>
        <el-menu-item index="/contacts"><el-icon><User /></el-icon><span>常用收款人</span></el-menu-item>
        <el-menu-item index="/products"><el-icon><TrendCharts /></el-icon><span>存款理财</span></el-menu-item>
        <el-menu-item index="/loans"><el-icon><Tickets /></el-icon><span>贷款中心</span></el-menu-item>
        <el-menu-item index="/credit-cards"><el-icon><CreditCard /></el-icon><span>信用卡中心</span></el-menu-item>
        <el-menu-item index="/notifications"><el-icon><Bell /></el-icon><span>通知中心</span></el-menu-item>
        <el-menu-item index="/agent"><el-icon><ChatDotRound /></el-icon><span>智能助手</span></el-menu-item>
        <el-menu-item index="/profile"><el-icon><UserFilled /></el-icon><span>个人中心</span></el-menu-item>
        <el-menu-item v-if="user.role === 'ADMIN'" index="/admin"><el-icon><Setting /></el-icon><span>后台管理</span></el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
          <el-breadcrumb-item>{{ $route.meta.title || $route.name }}</el-breadcrumb-item>
        </el-breadcrumb>
        <el-dropdown @command="handleCommand">
          <span class="user-info">
            <el-avatar :size="32" :icon="UserFilled" />
            <span>{{ user.userName || '用户' }}</span>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人中心</el-dropdown-item>
              <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import {
  Bell,
  ChatDotRound,
  CreditCard,
  DataAnalysis,
  Document,
  Money,
  Setting,
  Tickets,
  TrendCharts,
  User,
  UserFilled,
  Wallet
} from '@element-plus/icons-vue'
import api from '../api/client'
import { clearSession, getUser } from '../utils/auth'

const router = useRouter()
const user = reactive(getUser())

async function handleCommand(command) {
  if (command === 'logout') {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await api.post('/api/user/logout').catch(() => {})
    clearSession()
    router.push('/login')
  } else if (command === 'profile') {
    router.push('/profile')
  }
}
</script>

<style scoped>
.layout-container {
  min-height: 100vh;
}

.sidebar {
  background: #1f2f46;
}

.logo {
  height: 64px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 20px;
  color: white;
  font-weight: 700;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.logo .el-icon {
  font-size: 24px;
}

.el-menu {
  border-right: 0;
}

.header {
  background: var(--surface);
  border-bottom: 1px solid var(--border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.el-main {
  background: var(--page-bg);
  padding: 20px;
}

@media (max-width: 820px) {
  .sidebar {
    display: none;
  }
}
</style>
