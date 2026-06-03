<template>
  <div class="min-h-screen flex items-center justify-center bg-gradient-to-br from-green-600 to-blue-600 py-12 px-4 sm:px-6 lg:px-8">
    <div class="max-w-md w-full">
      <div class="bg-white p-8 rounded-lg shadow-lg">
        <div class="text-center mb-8">
          <h1 class="text-3xl font-bold text-blue-600 mb-2">🏃 Corridas</h1>
          <p class="text-gray-600">Sistema de Gerenciamento de Corridas</p>
        </div>
        <h2 class="text-2xl font-bold text-center text-gray-900 mb-8">Registre-se Agora</h2>
        <Alert :message="errorMessage" type="error" />
        <form @submit.prevent="handleRegister" class="space-y-4">
          <FormInput
            id="nome"
            label="Nome Completo"
            v-model="form.nome"
            placeholder="João Silva"
            required
          />
          <FormInput
            id="username"
            label="Nome de Usuário"
            v-model="form.username"
            placeholder="joaosilva"
            required
          />
          <FormInput
            id="login"
            label="Email"
            type="email"
            v-model="form.login"
            placeholder="joao@exemplo.com"
            required
          />
          <FormInput
            id="senha"
            label="Senha"
            type="password"
            v-model="form.senha"
            placeholder="Mínimo 8 caracteres"
            required
          />
          <Button label="Criar Minha Conta" :loading="authStore.loading" />
        </form>
        <p class="text-center mt-6 text-gray-600">
          Já participa?
          <RouterLink to="/login" class="text-blue-600 font-semibold hover:underline">Faça login</RouterLink>
        </p>
      </div>
      <p class="text-center mt-6 text-white text-sm">Seus dados estão seguros com nós</p>
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
  nome: '',
  username: '',
  login: '',
  senha: ''
})

const handleRegister = async () => {
  errorMessage.value = ''
  try {
    await authStore.register(form.value)
    router.push('/dashboard')
  } catch (error) {
    errorMessage.value = error
  }
}
</script>
