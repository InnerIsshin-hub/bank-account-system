<template>
  <div class="auth-shell register-shell">
    <section class="auth-visual">
      <div class="brand-panel">
        <div class="brand-mark"><CreditCard /></div>
        <h1>开户注册</h1>
        <p>完成实名资料、登录密码和交易密码设置后即可获得本行账户</p>
      </div>
    </section>

    <section class="auth-form-area">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="auth-form">
        <h2>开户信息</h2>
        <el-form-item label="真实姓名" prop="userName">
          <el-input v-model="form.userName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="身份证号" prop="idCard">
          <el-input v-model="form.idCard" placeholder="18 位身份证号" />
        </el-form-item>
        <el-form-item label="手机号码" prop="phone">
          <el-input v-model="form.phone" placeholder="11 位手机号" />
        </el-form-item>
        <el-form-item label="登录密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="8-32 位，含字母、数字和特殊字符" show-password />
        </el-form-item>
        <el-form-item label="确认登录密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="交易密码" prop="tradePassword">
          <el-input v-model="form.tradePassword" type="password" maxlength="6" placeholder="6 位数字" show-password />
        </el-form-item>
        <div class="form-actions">
          <el-button type="primary" :loading="loading" @click="handleRegister">立即开户</el-button>
          <el-button @click="$router.push('/login')">返回登录</el-button>
        </div>
      </el-form>
    </section>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CreditCard } from '@element-plus/icons-vue'
import api from '../api/client'

const router = useRouter()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  userName: '',
  idCard: '',
  phone: '',
  password: '',
  confirmPassword: '',
  tradePassword: ''
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== form.password) callback(new Error('两次输入密码不一致'))
  else callback()
}

const rules = {
  userName: [
    { required: true, message: '请输入真实姓名', trigger: 'blur' },
    { min: 2, max: 32, message: '长度在 2 到 32 个字符', trigger: 'blur' }
  ],
  idCard: [
    { required: true, message: '请输入身份证号', trigger: 'blur' },
    { pattern: /^[1-9]\d{5}(18|19|20)?\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\d|3[01])\d{3}[\dXx]$/, message: '身份证格式不正确', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入登录密码', trigger: 'blur' },
    { pattern: /^(?=.*[A-Za-z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,32}$/, message: '需 8-32 位，含字母、数字和特殊字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认登录密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ],
  tradePassword: [
    { required: true, message: '请输入交易密码', trigger: 'blur' },
    { pattern: /^\d{6}$/, message: '交易密码为 6 位数字', trigger: 'blur' }
  ]
}

async function handleRegister() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const res = await api.post('/api/user/register', {
      userName: form.userName,
      idCard: form.idCard,
      phone: form.phone,
      password: form.password,
      tradePassword: form.tradePassword
    })
    await ElMessageBox.alert(`开户成功，您的卡号为 ${res.data}`, '开户完成', {
      confirmButtonText: '去登录'
    })
    router.push('/login')
  } catch {
    ElMessage.error('开户失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-shell {
  grid-template-columns: minmax(360px, 0.9fr) 520px;
}

.form-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
}

@media (max-width: 860px) {
  .register-shell {
    grid-template-columns: 1fr;
  }
}
</style>
