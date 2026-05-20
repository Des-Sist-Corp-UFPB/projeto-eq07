import { ref } from 'vue'
import axios from 'axios'
import { api } from '../config/api'

export function useRaces() {
  const races = ref([])
  const race = ref(null)
  const loading = ref(false)
  const error = ref(null)

  const fetchRaces = async () => {
    loading.value = true
    error.value = null
    try {
      const response = await axios.get(`${api.baseURL}${api.endpoints.races.list}`)
      races.value = response.data
    } catch (err) {
      error.value = err.response?.data?.mensagem || 'Erro ao carregar corridas'
      throw error.value
    } finally {
      loading.value = false
    }
  }

  const fetchRaceById = async (id) => {
    loading.value = true
    error.value = null
    try {
      const response = await axios.get(`${api.baseURL}${api.endpoints.races.getById(id)}`)
      race.value = response.data
      return response.data
    } catch (err) {
      error.value = err.response?.data?.mensagem || 'Erro ao carregar corrida'
      throw error.value
    } finally {
      loading.value = false
    }
  }

  const createRace = async (raceData) => {
    loading.value = true
    error.value = null
    try {
      const response = await axios.post(`${api.baseURL}${api.endpoints.races.create}`, raceData)
      races.value.push(response.data)
      return response.data
    } catch (err) {
      error.value = err.response?.data?.mensagem || 'Erro ao criar corrida'
      throw error.value
    } finally {
      loading.value = false
    }
  }

  const updateRace = async (id, raceData) => {
    loading.value = true
    error.value = null
    try {
      const response = await axios.put(`${api.baseURL}${api.endpoints.races.update(id)}`, raceData)
      const index = races.value.findIndex(r => r.id === id)
      if (index !== -1) {
        races.value[index] = response.data
      }
      return response.data
    } catch (err) {
      error.value = err.response?.data?.mensagem || 'Erro ao atualizar corrida'
      throw error.value
    } finally {
      loading.value = false
    }
  }

  const deleteRace = async (id) => {
    loading.value = true
    error.value = null
    try {
      await axios.delete(`${api.baseURL}${api.endpoints.races.delete(id)}`)
      races.value = races.value.filter(r => r.id !== id)
    } catch (err) {
      error.value = err.response?.data?.mensagem || 'Erro ao deletar corrida'
      throw error.value
    } finally {
      loading.value = false
    }
  }

  const addParticipant = async (raceId, participantId) => {
    loading.value = true
    error.value = null
    try {
      const response = await axios.post(`${api.baseURL}${api.endpoints.addParticipant(raceId)}`, { participantId })
      return response.data
    } catch (err) {
      error.value = err.response?.data?.mensagem || 'Erro ao adicionar participante'
      throw error.value
    } finally {
      loading.value = false
    }
  }

  const removeParticipant = async (raceId, participantId) => {
    loading.value = true
    error.value = null
    try {
      await axios.delete(`${api.baseURL}${api.endpoints.removeParticipant(raceId, participantId)}`)
    } catch (err) {
      error.value = err.response?.data?.mensagem || 'Erro ao remover participante'
      throw error.value
    } finally {
      loading.value = false
    }
  }

  const updateResult = async (raceId, resultId, resultData) => {
    loading.value = true
    error.value = null
    try {
      const response = await axios.patch(`${api.baseURL}${api.endpoints.updateResult(raceId, resultId)}`, resultData)
      return response.data
    } catch (err) {
      error.value = err.response?.data?.mensagem || 'Erro ao atualizar resultado'
      throw error.value
    } finally {
      loading.value = false
    }
  }

  return {
    races,
    race,
    loading,
    error,
    fetchRaces,
    fetchRaceById,
    createRace,
    updateRace,
    deleteRace,
    addParticipant,
    removeParticipant,
    updateResult
  }
}
