<template>
  <div class="max-w-4xl mx-auto px-4 py-12">
    <div class="bg-white rounded-lg shadow p-8">
      <h1 class="text-3xl font-bold mb-8">Meu Perfil</h1>
      <Alert v-if="successMessage" :message="successMessage" type="success" />
      <Alert v-if="errorMessage" :message="errorMessage" type="error" />
      
      <div class="grid md:grid-cols-2 gap-8">
        <div>
          <h2 class="text-xl font-bold mb-4">Informações Pessoais</h2>
          <div class="space-y-4">
            <div>
              <label class="block text-gray-700 font-medium">Nome</label>
              <p class="text-gray-600">{{ authStore.user?.nome }}</p>
            </div>
            <div>
              <label class="block text-gray-700 font-medium">Nome de Usuário</label>
              <p class="text-gray-600">{{ authStore.user?.username }}</p>
            </div>
            <div>
              <label class="block text-gray-700 font-medium">Login</label>
              <p class="text-gray-600">{{ authStore.user?.login }}</p>
            </div>
            <div>
              <label class="block text-gray-700 font-medium">Papel</label>
              <p class="text-gray-600">{{ authStore.user?.papel }}</p>
            </div>
          </div>
        </div>
        
        <div>
          <h2 class="text-xl font-bold mb-4">Editar Informações</h2>
          <form @submit.prevent="handleUpdate" class="space-y-4">
            <FormInput
              id="edit-nome"
              label="Nome"
              v-model="editForm.nome"
              placeholder="Digite seu nome"
            />
            <FormInput
              id="edit-username"
              label="Nome de Usuário"
              v-model="editForm.username"
              placeholder="Digite seu nome de usuário"
            />
            <FormInput
              id="edit-login"
              label="Login"
              v-model="editForm.login"
              placeholder="Digite seu login"
            />
            <FormInput
              id="edit-senha"
              label="Nova Senha (deixe em branco para não alterar)"
              type="password"
              v-model="editForm.senha"
              placeholder="Digite a nova senha"
            />
            <Button label="Atualizar" :loading="loading" />
          </form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useAuthStore } from '../stores/authStore'
import { useUsers } from '../composables/useUsers'
import FormInput from '../components/FormInput.vue'
import Button from '../components/Button.vue'
import Alert from '../components/Alert.vue'

const authStore = useAuthStore()
const { loading, error: apiError, editUser } = useUsers()
const successMessage = ref('')
const errorMessage = ref('')
const editForm = ref({
  nome: '',
  username: '',
  login: '',
  senha: ''
})

watch(() => authStore.user, (newUser) => {
  if (newUser) {
    editForm.value = {
      nome: newUser.nome || '',
      username: newUser.username || '',
      login: newUser.login || '',
      senha: ''
    }
  }
}, { immediate: true })

const handleUpdate = async () => {
  successMessage.value = ''
  errorMessage.value = ''
  try {
    const updated = await editUser(authStore.user.id, editForm.value)
    authStore.setUser(updated)
    successMessage.value = 'Perfil atualizado com sucesso!'
  } catch (error) {
    errorMessage.value = error
  }
}
</script>
