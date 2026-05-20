<template>
  <div class="max-w-7xl mx-auto px-4 py-12">
    <h1 class="text-4xl font-bold mb-8">Dashboard — Sistema de Gerenciamento de Corridas</h1>

    <div class="grid md:grid-cols-4 gap-6 mb-12">
      <div class="bg-white p-6 rounded-lg shadow">
        <h3 class="text-lg font-medium text-gray-500">Total de Corridas</h3>
        <p class="text-3xl font-bold">{{ races.length }}</p>
      </div>
      <div class="bg-white p-6 rounded-lg shadow">
        <h3 class="text-lg font-medium text-gray-500">Corridas Ativas</h3>
        <p class="text-3xl font-bold text-blue-600">{{ activeRaces }}</p>
      </div>
      <div class="bg-white p-6 rounded-lg shadow">
        <h3 class="text-lg font-medium text-gray-500">Suas Participações</h3>
        <p class="text-3xl font-bold text-green-600">{{ userParticipations }}</p>
      </div>
      <div class="bg-white p-6 rounded-lg shadow">
        <h3 class="text-lg font-medium text-gray-500">Seu Papel</h3>
        <p class="text-lg font-bold">{{ authStore.user?.papel }}</p>
      </div>
    </div>

    <div class="grid lg:grid-cols-2 gap-6 mb-12">
      <div class="bg-white p-6 rounded-lg shadow">
        <div class="flex justify-between items-center mb-6">
          <h2 class="text-2xl font-bold">Próximas Corridas</h2>
          <RouterLink v-if="authStore.isAdmin" to="/races" class="text-blue-600 hover:underline text-sm">Ver todas</RouterLink>
        </div>
        <div v-if="upcomingRaces.length" class="space-y-3">
          <div v-for="race in upcomingRaces.slice(0, 3)" :key="race.id" class="border-l-4 border-blue-600 pl-4 py-2">
            <p class="font-semibold">{{ race.nome }}</p>
            <p class="text-sm text-gray-600">{{ formatDate(race.data) }}</p>
          </div>
        </div>
        <p v-else class="text-gray-500 text-sm">Nenhuma corrida próxima</p>
      </div>

      <div class="bg-white p-6 rounded-lg shadow">
        <div class="flex justify-between items-center mb-6">
          <h2 class="text-2xl font-bold">Seu Perfil</h2>
          <RouterLink to="/profile" class="text-blue-600 hover:underline text-sm">Editar</RouterLink>
        </div>
        <div class="space-y-3">
          <p><strong>Nome:</strong> {{ authStore.user?.nome }}</p>
          <p><strong>Usuário:</strong> {{ authStore.user?.username }}</p>
          <p><strong>Status:</strong> <span class="text-green-600">Ativo</span></p>
        </div>
      </div>
    </div>

    <div v-if="authStore.isAdmin" class="bg-white p-6 rounded-lg shadow">
      <div class="flex justify-between items-center mb-6">
        <h2 class="text-2xl font-bold">Gerenciamento de Corridas</h2>
        <RouterLink to="/races/new" class="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700 text-sm">Nova Corrida</RouterLink>
      </div>
      <p class="text-gray-600">Acesse o painel de gerenciamento para criar, editar ou remover corridas.</p>
    </div>

    <div v-if="loading" class="text-center py-8">
      <p class="text-gray-600">Carregando dados...</p>
    </div>

    <div v-if="error" class="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mt-6">
      {{ error }}
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '../stores/authStore'
import { useRaces } from '../composables/useRaces'

const authStore = useAuthStore()
const { races, loading, error, fetchRaces } = useRaces()

const userParticipations = ref(0)

const activeRaces = computed(() => {
  return races.value.filter(r => r.status === 'ATIVA' || r.status === 'EM_ANDAMENTO').length
})

const upcomingRaces = computed(() => {
  return races.value.sort((a, b) => new Date(a.data) - new Date(b.data))
})

const formatDate = (dateString) => {
  return new Date(dateString).toLocaleDateString('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

onMounted(() => {
  fetchRaces().catch(() => {})
})
</script>
