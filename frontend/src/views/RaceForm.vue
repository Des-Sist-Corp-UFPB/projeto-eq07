<template>
  <div class="max-w-2xl mx-auto px-4 py-12">
    <h1 class="text-4xl font-bold mb-8">{{ raceId ? 'Editar Corrida' : 'Criar Nova Corrida' }}</h1>

    <div v-if="error" class="bg-red-100 border border-red-400 text-red-700 px-6 py-4 rounded-lg mb-6">
      {{ error }}
    </div>

    <form @submit.prevent="submitForm" class="bg-white p-8 rounded-lg shadow-md space-y-6">
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-2">Nome da Corrida</label>
        <input v-model="form.nome" type="text" required class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent" placeholder="Ex: Maratona de João Pessoa">
      </div>

      <div>
        <label class="block text-sm font-medium text-gray-700 mb-2">Descrição</label>
        <textarea v-model="form.descricao" rows="4" class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent" placeholder="Descreva a corrida..."></textarea>
      </div>

      <div class="grid grid-cols-2 gap-6">
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-2">Data e Hora</label>
          <input v-model="form.data" type="datetime-local" required class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent">
        </div>

        <div>
          <label class="block text-sm font-medium text-gray-700 mb-2">Local</label>
          <input v-model="form.local" type="text" required class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent" placeholder="Local de partida">
        </div>
      </div>

      <div class="grid grid-cols-2 gap-6">
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-2">Distância (km)</label>
          <input v-model="form.distancia" type="number" step="0.1" min="0" class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent" placeholder="Ex: 21">
        </div>

        <div>
          <label class="block text-sm font-medium text-gray-700 mb-2">Status</label>
          <select v-model="form.status" required class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent">
            <option value="PLANEJADA">Planejada</option>
            <option value="ATIVA">Ativa</option>
            <option value="EM_ANDAMENTO">Em Andamento</option>
            <option value="ENCERRADA">Encerrada</option>
            <option value="CANCELADA">Cancelada</option>
          </select>
        </div>
      </div>

      <div>
        <label class="block text-sm font-medium text-gray-700 mb-2">Limite de Participantes</label>
        <input v-model="form.limiteParticipantes" type="number" min="1" class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent" placeholder="Ex: 100">
      </div>

      <div class="flex gap-4 pt-6">
        <button type="submit" :disabled="loading" class="flex-1 bg-blue-600 text-white px-6 py-3 rounded-lg hover:bg-blue-700 font-medium disabled:bg-gray-400">
          {{ loading ? 'Salvando...' : raceId ? 'Atualizar Corrida' : 'Criar Corrida' }}
        </button>
        <RouterLink to="/races" class="flex-1 bg-gray-400 text-white px-6 py-3 rounded-lg hover:bg-gray-500 font-medium text-center">
          Cancelar
        </RouterLink>
      </div>
    </form>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useRaces } from '../composables/useRaces'

const route = useRoute()
const router = useRouter()
const raceId = ref(route.params.id || null)
const { loading, error, createRace, updateRace, fetchRaceById } = useRaces()

const form = reactive({
  nome: '',
  descricao: '',
  data: '',
  local: '',
  distancia: '',
  status: 'PLANEJADA',
  limiteParticipantes: ''
})

const submitForm = async () => {
  try {
    if (raceId.value) {
      await updateRace(raceId.value, form)
    } else {
      await createRace(form)
    }
    router.push('/races')
  } catch (err) {
    console.error('Erro ao salvar:', err)
  }
}

onMounted(async () => {
  if (raceId.value) {
    try {
      const race = await fetchRaceById(raceId.value)
      Object.assign(form, race)
    } catch (err) {
      console.error('Erro ao carregar corrida:', err)
    }
  }
})
</script>