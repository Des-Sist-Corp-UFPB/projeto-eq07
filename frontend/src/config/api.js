const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

export const api = {
  baseURL: API_BASE_URL,
  endpoints: {
    user: {
      register: '/user/registrar',
      login: '/user/login',
      edit: (id) => `/user/${id}`,
      delete: (id) => `/user/${id}`
    },
    races: {
      list: '/races',
      create: '/races',
      getById: (id) => `/races/${id}`,
      update: (id) => `/races/${id}`,
      delete: (id) => `/races/${id}`,
      participants: (id) => `/races/${id}/participants`,
      addParticipant: (id) => `/races/${id}/participants`,
      removeParticipant: (id, participantId) => `/races/${id}/participants/${participantId}`,
      results: (id) => `/races/${id}/results`,
      updateResult: (id, resultId) => `/races/${id}/results/${resultId}`
    },
    rankings: {
      byRace: (id) => `/rankings/race/${id}`,
      global: '/rankings/global'
    }
  }
}
