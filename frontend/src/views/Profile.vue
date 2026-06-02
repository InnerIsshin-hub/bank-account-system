<template>
  <div class="page">
    <div>
      <h1 class="page-title">个人中心</h1>
      <p class="page-subtitle">管理手机号、登录密码和交易密码</p>
    </div>

    <el-card>
      <el-tabs>
        <el-tab-pane label="基本信息">
          <el-form label-width="120px" class="form-narrow">
            <el-form-item label="真实姓名">
              <el-input v-model="user.userName" disabled />
            </el-form-item>
            <el-form-item label="身份证号">
              <el-input v-model="user.idCardMasked" disabled />
            </el-form-item>
            <el-form-item label="当前手机号">
              <el-input v-model="user.phoneMasked" disabled />
            </el-form-item>
            <el-form-item label="新手机号">
              <el-input v-model="profileForm.newPhone" />
            </el-form-item>
            <el-form-item label="登录密码">
              <el-input v-model="profileForm.oldPassword" type="password" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="profileLoading" @click="updatePhone">更新手机号</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="修改登录密码">
          <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="120px" class="form-narrow">
            <el-form-item label="旧密码" prop="oldPassword">
              <el-input v-model="pwdForm.oldPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="pwdForm.newPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="确认新密码" prop="confirmPassword">
              <el-input v-model="pwdForm.confirmPassword" type="password" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="pwdLoading" @click="updatePassword">确认修改</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="交易密码">
          <el-form label-width="120px" class="form-narrow">
            <el-form-item label="登录密码">
              <el-input v-model="tradeForm.loginPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="原交易密码">
              <el-input v-model="tradeForm.oldTradePassword" type="password" maxlength="6" show-password />
            </el-form-item>
            <el-form-item label="新交易密码">
              <el-input v-model="tradeForm.newTradePassword" type="password" maxlength="6" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="tradeLoading" @click="updateTradePassword">保存交易密码</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="OTP 口令">
          <el-form label-width="120px" class="form-narrow">
            <el-form-item label="绑定状态">
              <el-tag :type="user.otpEnabled ? 'success' : 'info'">{{ user.otpEnabled ? '已启用' : '未启用' }}</el-tag>
            </el-form-item>
            <el-form-item label="登录密码">
              <el-input v-model="otpForm.loginPassword" type="password" show-password />
            </el-form-item>
            <el-form-item v-if="user.otpEnabled" label="OTP 验证码">
              <el-input v-model="otpForm.otpCode" placeholder="demo 验证码 000000" />
            </el-form-item>
            <el-form-item v-if="otpSecret" label="Demo Secret">
              <el-input v-model="otpSecret" readonly />
            </el-form-item>
            <el-form-item>
              <el-button v-if="!user.otpEnabled" type="primary" :loading="otpLoading" @click="bindOtp">绑定 OTP</el-button>
              <el-button v-else type="danger" :loading="otpLoading" @click="unbindOtp">解绑 OTP</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api/client'
import { clearSession } from '../utils/auth'

const router = useRouter()
const user = reactive({})
const profileLoading = ref(false)
const pwdLoading = ref(false)
const tradeLoading = ref(false)
const otpLoading = ref(false)
const otpSecret = ref('')
const pwdFormRef = ref()
const profileForm = reactive({ newPhone: '', oldPassword: '' })
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const tradeForm = reactive({ loginPassword: '', oldTradePassword: '', newTradePassword: '' })
const otpForm = reactive({ loginPassword: '', otpCode: '000000' })

const validateConfirmPwd = (rule, value, callback) => {
  if (value !== pwdForm.newPassword) callback(new Error('两次输入密码不一致'))
  else callback()
}

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { pattern: /^(?=.*[A-Za-z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,32}$/, message: '需 8-32 位，含字母、数字和特殊字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirmPwd, trigger: 'blur' }
  ]
}

onMounted(loadProfile)

async function loadProfile() {
  const res = await api.get('/api/user/me')
  Object.assign(user, res.data.user)
}

async function updatePhone() {
  profileLoading.value = true
  try {
    await api.put('/api/user/profile', profileForm)
    ElMessage.success('手机号已更新')
    await loadProfile()
  } finally {
    profileLoading.value = false
  }
}

async function updatePassword() {
  const valid = await pwdFormRef.value.validate().catch(() => false)
  if (!valid) return
  pwdLoading.value = true
  try {
    await api.put('/api/user/password', {
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword
    })
    await ElMessageBox.alert('登录密码已修改，请重新登录。', '修改成功', { confirmButtonText: '去登录' })
    clearSession()
    router.push('/login')
  } finally {
    pwdLoading.value = false
  }
}

async function updateTradePassword() {
  tradeLoading.value = true
  try {
    await api.post('/api/user/trade-password', tradeForm)
    ElMessage.success('交易密码已更新')
    Object.assign(tradeForm, { loginPassword: '', oldTradePassword: '', newTradePassword: '' })
  } finally {
    tradeLoading.value = false
  }
}

async function bindOtp() {
  otpLoading.value = true
  try {
    const res = await api.post('/api/user/otp/bind', { loginPassword: otpForm.loginPassword })
    otpSecret.value = res.data.secret
    ElMessage.success('OTP 已绑定')
    otpForm.loginPassword = ''
    await loadProfile()
  } finally {
    otpLoading.value = false
  }
}

async function unbindOtp() {
  otpLoading.value = true
  try {
    await api.post('/api/user/otp/unbind', otpForm)
    ElMessage.success('OTP 已解绑')
    otpSecret.value = ''
    Object.assign(otpForm, { loginPassword: '', otpCode: '000000' })
    await loadProfile()
  } finally {
    otpLoading.value = false
  }
}
</script>
