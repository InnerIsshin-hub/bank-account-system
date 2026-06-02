<template>
  <div class="auth-shell">
    <section class="auth-visual">
      <div class="brand-panel">
        <div class="brand-mark"><Wallet /></div>
        <h1>网上银行系统</h1>
        <p>账户、转账、流水、通知与智能助手统一工作台</p>
      </div>
    </section>

    <section class="auth-form-area">
      <el-form ref="formRef" :model="loginForm" :rules="rules" class="auth-form">
        <h2>登录</h2>
        <el-form-item prop="idCard">
          <el-input v-model="loginForm.idCard" placeholder="身份证号" size="large" clearable>
            <template #prefix><el-icon><User /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="loginForm.password" type="password" placeholder="登录密码" size="large" show-password>
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-alert v-if="failHint" :title="failHint" type="warning" show-icon :closable="false" />
        <el-button type="primary" size="large" :loading="loading" class="full-width" @click="handleLogin">
          登录
        </el-button>
        <div class="auth-switch">
          <span>还没有账户</span>
          <el-button link type="primary" @click="$router.push('/register')">立即开户</el-button>
        </div>
      </el-form>
    </section>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lock, User, Wallet } from '@element-plus/icons-vue'
import api from '../api/client'
import { saveSession } from '../utils/auth'

const router = useRouter()
const formRef = ref()
const loading = ref(false)
const failHint = ref('')

const loginForm = reactive({
  idCard: '',
  password: ''
})

const rules = {
  idCard: [
    { required: true, message: '请输入身份证号', trigger: 'blur' },
    { pattern: /^[1-9]\d{5}(18|19|20)?\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\d|3[01])\d{3}[\dXx]$/, message: '身份证号格式不正确', trigger: 'blur' }
  ],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  failHint.value = ''
  try {
    const res = await api.post('/api/user/login', loginForm)
    saveSession(res.data)
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } catch (error) {
    failHint.value = '登录失败，多次失败后账户会短时间锁定'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-shell {
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(360px, 1fr) 440px;
  background: var(--page-bg);
}

.auth-visual {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  background:
    linear-gradient(180deg, rgba(18, 35, 68, 0.72), rgba(23, 62, 78, 0.64)),
    url('../assets/hero.png') center/cover;
}

.brand-panel {
  max-width: 620px;
  color: white;
}

.brand-mark {
  width: 54px;
  height: 54px;
  display: grid;
  place-items: center;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.16);
  margin-bottom: 18px;
}

.brand-mark :deep(svg) {
  width: 30px;
  height: 30px;
}

.brand-panel h1 {
  margin: 0 0 12px;
  font-size: 38px;
  line-height: 1.15;
}

.brand-panel p {
  margin: 0;
  font-size: 17px;
  opacity: 0.9;
}

.auth-form-area {
  display: flex;
  align-items: center;
  padding: 42px;
  background: var(--surface);
}

.auth-form {
  width: 100%;
}

.auth-form h2 {
  margin: 0 0 24px;
  font-size: 28px;
}

.auth-switch {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  margin-top: 18px;
  color: var(--muted);
}

@media (max-width: 860px) {
  .auth-shell {
    grid-template-columns: 1fr;
  }

  .auth-visual {
    min-height: 220px;
  }

  .auth-form-area {
    padding: 28px 20px;
  }
}
</style>
