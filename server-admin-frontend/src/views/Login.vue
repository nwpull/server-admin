<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-header">
        <el-icon :size="36" color="#409EFF"><Monitor /></el-icon>
        <h1 class="login-title">Server Admin</h1>
        <p class="login-subtitle">服务器管理平台</p>
      </div>
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="0"
        size="large"
        @keyup.enter="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
            v-model="formData.username"
            placeholder="请输入用户名"
            prefix-icon="User"
            clearable
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="formData.password"
            type="password"
            placeholder="请输入密码"
            prefix-icon="Lock"
            show-password
            clearable
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            class="login-btn"
            @click="handleLogin"
          >
            登 录
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import useUserStore from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref<FormInstance>()
const loading = ref(false)

const formData = reactive({
  username: '',
  password: '',
})

const formRules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function handleLogin() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await userStore.login({
      username: formData.username,
      password: formData.password,
    })
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } catch (err: any) {
    ElMessage.error(err?.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.login-page {
  width: 100vw;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #0a0a0a;
  background-image: radial-gradient(circle at 50% 50%, #1a1a2e 0%, #0a0a0a 70%);
}

.login-card {
  width: 400px;
  padding: 48px 40px 36px;
  background-color: #1f1f1f;
  border-radius: 12px;
  border: 1px solid #2b2b2c;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.5);
}

.login-header {
  text-align: center;
  margin-bottom: 36px;

  .login-title {
    margin: 12px 0 4px;
    font-size: 24px;
    font-weight: 600;
    color: #e5eaf3;
  }

  .login-subtitle {
    margin: 0;
    font-size: 14px;
    color: #a3a6ad;
  }
}

.login-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
}

:deep(.el-input__wrapper) {
  background-color: #141414 !important;
  box-shadow: 0 0 0 1px #4c4d4f inset !important;

  &:hover {
    box-shadow: 0 0 0 1px #409eff inset !important;
  }

  &.is-focus {
    box-shadow: 0 0 0 1px #409eff inset !important;
  }
}

:deep(.el-input__inner) {
  color: #e5eaf3 !important;

  &::placeholder {
    color: #636466 !important;
  }
}

:deep(.el-input__prefix .el-icon) {
  color: #a3a6ad !important;
}
</style>
