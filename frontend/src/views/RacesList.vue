<template>
  <div class="max-w-7xl mx-auto px-4 py-12">
    <div class="flex justify-between items-center mb-8">
      <h1 class="text-4xl font-bold">Gerenciamento de Corridas</h1>
      <RouterLink v-if="authStore.isAdmin" to="/races/new" class="bg-blue-600 text-white px-6 py-2 rounded-lg hover:bg-blue-700 font-medium">
        + Nova Corrida
      </RouterLink>
    </div>

    <div v-if="loading" class="bg-blue-100 border border-blue-400 text-blue-700 px-6 py-4 rounded-lg">
      Carregando corridas...
    </div>

    <div v-if="error" class="bg-red-100 border border-red-400 text-red-700 px-6 py-4 rounded-lg mb-6">
      {{ error }}
    </div>

    <div v-if="!loading && races.length === 0" class="bg-gray-100 border border-gray-400 text-gray-700 px-6 py-4 rounded-lg">
      Nenhuma corrida disponível.
    </div>

    <div v-if="!loading && races.length > 0" class="grid gap-6">
      <div v-for="race in races" :key="race.id" class="bg-white p-6 rounded-lg shadow-md border-l-4 border-blue-600">
        <div class="flex justify-between items-start mb-4">
          <div class="flex-1">
            <RouterLink :to="`/races/${race.id}`" class="text-2xl font-bold text-blue-600 hover:underline">
              {{ race.nome }}
            </RouterLink>
            <p class="text-gray-600 mt-1">{{ race.descricao }}</p>
          </div>
          <div class="flex gap-2">
            <RouterLink v-if="authStore.isAdmin" :to="`/races/${race.id}/edit`" class="bg-blue-500 text-white px-4 py-2 rounded hover:bg-blue-600 text-sm">
              Editar
            </RouterLink>
            <button v-if="authStore.isAdmin" @click="deleteRaceHandler(race.id)" class="bg-red-500 text-white px-4 py-2 rounded hover:bg-red-600 text-sm">
              Deletar
            </button>
            <RouterLink v-else :to="`/races/${race.id}`" class="bg-green-500 text-white px-4 py-2 rounded hover:bg-green-600 text-sm">
              Ver Detalhes
            </RouterLink>
          </div>
        </div>

        <div class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-4 text-sm">
          <div>
            <span class="text-gray-600">Data:</span>
            <p class="font-semibold">{{ formatDate(race.data) }}</p>
          </div>
          <div>
            <span class="text-gray-600">Local:</span>
            <p class="font-semibold">{{ race.local || 'N/A' }}</p>
          </div>
          <div>
            <span class="text-gray-600">Status:</span>
            <p class="font-semibold" :class="getStatusClass(race.status)">{{ race.status }}</p>
          </div>
          <div>
            <span class="text-gray-600">Participantes:</span>
            <p class="font-semibold">{{ race.numeroParticipantes || 0 }}</p>
          </div>
        </div>

        <div v-if="race.distancia" class="text-sm text-gray-600">
          Distância: <span class="font-semibold">{{ race.distancia }} km</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useAuthStore } from '../stores/authStore'
import { useRaces } from '../composables/useRaces'

const authStore = useAuthStore()
const { races, loading, error, fetchRaces, deleteRace } = useRaces()

const formatDate = (dateString) => {
  return new Date(dateString).toLocaleDateString('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const getStatusClass = (status) => {
  const classes = {
    'PLANEJADA': 'text-yellow-600',
    'ATIVA': 'text-green-600',
    'EM_ANDAMENTO': 'text-blue-600',
    'ENCERRADA': 'text-gray-600',
    'CANCELADA': 'text-red-600'
  }
  return classes[status] || 'text-gray-600'
}

const deleteRaceHandler = async (raceId) => {
  if (confirm('Tem certeza que deseja deletar esta corrida?')) {
    try {
      await deleteRace(raceId)
    } catch (err) {
      console.error('Erro ao deletar:', err)
    }
  }
}

onMounted(() => {
  fetchRaces().catch(() => {})
})
</script>