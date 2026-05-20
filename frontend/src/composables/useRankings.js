import { ref } from 'vue'
import axios from 'axios'
import { api } from '../config/api'

export function useRankings() {
  const rankings = ref([])
  const loading = ref(false)
  const error = ref(null)

  const fetchRankingsByRace = async (raceId) => {
    loading.value = true
    error.value = null
    try {
      const response = await axios.get(`${api.baseURL}${api.endpoints.rankings.byRace(raceId)}`)
      rankings.value = response.data
      return response.data
    } catch (err) {
      error.value = err.response?.data?.mensagem || 'Erro ao carregar ranking'
      throw error.value
    } finally {
      loading.value = false
    }
  }

  const fetchGlobalRanking = async () => {
    loading.value = true
    error.value = null
    try {
      const response = await axios.get(`${api.baseURL}${api.endpoints.rankings.global}`)
      rankings.value = response.data
      return response.data
    } catch (err) {
      error.value = err.response?.data?.mensagem || 'Erro ao carregar ranking global'
      throw error.value
    } finally {
      loading.value = false
    }
  }

  return {
    rankings,
    loading,
    error,
    fetchRankingsByRace,
    fetchGlobalRanking
  }
}
