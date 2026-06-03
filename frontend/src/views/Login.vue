<template>
  <div class="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-600 to-blue-800 py-12 px-4 sm:px-6 lg:px-8">
    <div class="max-w-md w-full">
      <div class="bg-white p-8 rounded-lg shadow-lg">
        <div class="text-center mb-8">
          <h1 class="text-3xl font-bold text-blue-600 mb-2">🏃 Corridas</h1>
          <p class="text-gray-600">Sistema de Gerenciamento de Corridas</p>
        </div>
        <h2 class="text-2xl font-bold text-center text-gray-900 mb-8">Acesse sua Conta</h2>
        <Alert :message="errorMessage" type="error" />
        <form @submit.prevent="handleLogin" class="space-y-4">
          <FormInput
            id="login"
            label="Usuário ou Email"
            v-model="form.login"
            placeholder="seu-usuario"
            required
          />
          <FormInput
            id="senha"
            label="Senha"
            type="password"
            v-model="form.senha"
            placeholder="••••••••"
            required
          />
          <Button label="Entrar na Corrida" :loading="authStore.loading" />
        </form>
        <p class="text-center mt-6 text-gray-600">
          Novo participante?
          <RouterLink to="/register" class="text-blue-600 font-semibold hover:underline">Registre-se agora</RouterLink>
        </p>
      </div>
      <p class="text-center mt-6 text-white text-sm">Plataforma segura com autenticação JWT</p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/authStore'
import FormInput from '../components/FormInput.vue'
import Button from '../components/Button.vue'
import Alert from '../components/Alert.vue'

const router = useRouter()
const authStore = useAuthStore()
const errorMessage = ref('')
const form = ref({
  login: '',
  senha: ''
})

const handleLogin = async () => {
  errorMessage.value = ''
  try {
    await authStore.login(form.value)
    router.push('/dashboard')
  } catch (error) {
    errorMessage.value = error
  }
}
</script>
