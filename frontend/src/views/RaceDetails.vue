<template>
  <div class="max-w-7xl mx-auto px-4 py-12">
    <div v-if="loading" class="bg-blue-100 border border-blue-400 text-blue-700 px-6 py-4 rounded-lg">
      Carregando corrida...
    </div>

    <div v-if="error" class="bg-red-100 border border-red-400 text-red-700 px-6 py-4 rounded-lg">
      {{ error }}
    </div>

    <template v-if="!loading && race">
      <div class="mb-8">
        <RouterLink to="/races" class="text-blue-600 hover:underline mb-4 inline-block">← Voltar</RouterLink>
        <h1 class="text-4xl font-bold">{{ race.nome }}</h1>
      </div>

      <div class="grid lg:grid-cols-3 gap-6 mb-12">
        <div class="lg:col-span-2 bg-white p-6 rounded-lg shadow">
          <h2 class="text-2xl font-bold mb-6">Informações da Corrida</h2>

          <div v-if="authStore.isAdmin" class="mb-6">
            <RouterLink :to="`/races/${race.id}/edit`" class="bg-blue-500 text-white px-4 py-2 rounded hover:bg-blue-600 mr-2">
              Editar
            </RouterLink>
            <button @click="deleteRaceHandler" class="bg-red-500 text-white px-4 py-2 rounded hover:bg-red-600">
              Deletar
            </button>
          </div>

          <div class="grid grid-cols-2 gap-6 mb-6">
            <div>
              <p class="text-gray-600">Data</p>
              <p class="text-xl font-semibold">{{ formatDate(race.data) }}</p>
            </div>
            <div>
              <p class="text-gray-600">Local</p>
              <p class="text-xl font-semibold">{{ race.local }}</p>
            </div>
            <div>
              <p class="text-gray-600">Distância</p>
              <p class="text-xl font-semibold">{{ race.distancia }} km</p>
            </div>
            <div>
              <p class="text-gray-600">Status</p>
              <p class="text-xl font-semibold" :class="getStatusClass(race.status)">{{ race.status }}</p>
            </div>
          </div>

          <div v-if="race.descricao" class="mb-6 p-4 bg-gray-50 rounded">
            <p class="text-gray-800">{{ race.descricao }}</p>
          </div>

          <div v-if="race.limiteParticipantes" class="text-sm text-gray-600">
            Limite de participantes: {{ race.limiteParticipantes }}
          </div>
        </div>

        <div class="bg-white p-6 rounded-lg shadow h-fit">
          <h2 class="text-xl font-bold mb-4">Estatísticas</h2>
          <div class="space-y-4">
            <div>
              <p class="text-gray-600 text-sm">Participantes</p>
              <p class="text-3xl font-bold">{{ race.numeroParticipantes || 0 }}</p>
            </div>
            <div>
              <p class="text-gray-600 text-sm">Completados</p>
              <p class="text-2xl font-bold text-green-600">{{ finishedCount }}</p>
            </div>
            <div>
              <p class="text-gray-600 text-sm">Em Progresso</p>
              <p class="text-2xl font-bold text-blue-600">{{ inProgressCount }}</p>
            </div>
          </div>
          <button v-if="!authStore.isAdmin && !isParticipant" @click="joinRace" class="w-full mt-6 bg-green-600 text-white px-4 py-2 rounded hover:bg-green-700 font-medium">
            Participar
          </button>
          <button v-else-if="!authStore.isAdmin && isParticipant" @click="leaveRace" class="w-full mt-6 bg-red-600 text-white px-4 py-2 rounded hover:bg-red-700 font-medium">
            Deixar de Participar
          </button>
        </div>
      </div>

      <div class="grid lg:grid-cols-2 gap-6">
        <div class="bg-white p-6 rounded-lg shadow">
          <h2 class="text-2xl font-bold mb-6">Participantes</h2>
          <div v-if="participants.length === 0" class="text-gray-600">
            Nenhum participante ainda.
          </div>
          <div v-else class="space-y-3">
            <div v-for="participant in participants" :key="participant.id" class="flex justify-between items-center p-3 border rounded hover:bg-gray-50">
              <span class="font-medium">{{ participant.nome }}</span>
              <span class="text-sm text-gray-600">{{ participant.status || 'Inscrito' }}</span>
            </div>
          </div>
        </div>

        <div class="bg-white p-6 rounded-lg shadow">
          <h2 class="text-2xl font-bold mb-6">Resultados</h2>
          <div v-if="results.length === 0" class="text-gray-600">
            Nenhum resultado registrado ainda.
          </div>
          <div v-else class="space-y-3">
            <div v-for="(result, index) in results.slice(0, 10)" :key="result.id" class="flex justify-between items-center p-3 border rounded hover:bg-gray-50">
              <div>
                <p class="font-medium">#{{ index + 1 }} {{ result.participanteNome }}</p>
                <p class="text-sm text-gray-600">{{ result.tempo }}</p>
              </div>
              <span v-if="result.colocacao === 1" class="text-yellow-600 font-bold">🥇</span>
              <span v-else-if="result.colocacao === 2" class="text-gray-400 font-bold">🥈</span>
              <span v-else-if="result.colocacao === 3" class="text-orange-600 font-bold">🥉</span>
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/authStore'
import { useRaces } from '../composables/useRaces'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const { race, loading, error, fetchRaceById, deleteRace, addParticipant, removeParticipant } = useRaces()

const participants = ref([])
const results = ref([])
const isParticipant = ref(false)

const finishedCount = computed(() => results.value.filter(r => r.status === 'COMPLETO').length)
const inProgressCount = computed(() => results.value.filter(r => r.status === 'EM_PROGRESSO').length)

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

const deleteRaceHandler = async () => {
  if (confirm('Tem certeza que deseja deletar esta corrida?')) {
    try {
      await deleteRace(route.params.id)
      router.push('/races')
    } catch (err) {
      console.error('Erro ao deletar:', err)
    }
  }
}

const joinRace = async () => {
  try {
    await addParticipant(route.params.id, authStore.user.id)
    isParticipant.value = true
    participants.value.push({ id: authStore.user.id, nome: authStore.user.nome })
  } catch (err) {
    console.error('Erro ao participar:', err)
  }
}

const leaveRace = async () => {
  if (confirm('Tem certeza que deseja deixar de participar?')) {
    try {
      await removeParticipant(route.params.id, authStore.user.id)
      isParticipant.value = false
      participants.value = participants.value.filter(p => p.id !== authStore.user.id)
    } catch (err) {
      console.error('Erro ao sair da corrida:', err)
    }
  }
}

onMounted(async () => {
  try {
    await fetchRaceById(route.params.id)
    if (race.value) {
      participants.value = race.value.participantes || []
      results.value = race.value.resultados || []
      isParticipant.value = participants.value.some(p => p.id === authStore.user.id)
    }
  } catch (err) {
    console.error('Erro ao carregar corrida:', err)
  }
})
</script>