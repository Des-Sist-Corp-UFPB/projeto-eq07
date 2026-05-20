import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import axios from 'axios'
import { api } from '../config/api'
import { decodeJWT } from '../utils/jwt'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || null)
  const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))
  const loading = ref(false)
  const error = ref(null)

  const isAuthenticated = computed(() => !!token.value)
  const isAdmin = computed(() => user.value?.papel === 'ADMINISTRADOR')

  const setToken = (newToken) => {
    token.value = newToken
    localStorage.setItem('token', newToken)
    axios.defaults.headers.common['Authorization'] = `Bearer ${newToken}`
  }

  const setUser = (userData) => {
    user.value = userData
    localStorage.setItem('user', JSON.stringify(userData))
  }

  const extractUserFromToken = (jwtToken) => {
    const payload = decodeJWT(jwtToken)
    if (payload) {
      return {
        id: payload.id,
        nome: payload.nome,
        login: payload.sub,
        papel: payload.papel
      }
    }
    return null
  }

  const register = async (formData) => {
    loading.value = true
    error.value = null
    try {
      const response = await axios.post(`${api.baseURL}${api.endpoints.user.register}`, formData)
      setToken(response.data.token)
      const userData = extractUserFromToken(response.data.token)
      if (userData) {
        setUser(userData)
      }
      return response.data
    } catch (err) {
      error.value = err.response?.data?.mensagem || 'Erro ao registrar'
      throw error.value
    } finally {
      loading.value = false
    }
  }

  const login = async (formData) => {
    loading.value = true
    error.value = null
    try {
      const response = await axios.post(`${api.baseURL}${api.endpoints.user.login}`, formData)
      setToken(response.data.token)
      const userData = extractUserFromToken(response.data.token)
      if (userData) {
        setUser(userData)
      }
      return response.data
    } catch (err) {
      error.value = err.response?.data?.mensagem || 'Erro ao fazer login'
      throw error.value
    } finally {
      loading.value = false
    }
  }

  const logout = () => {
    token.value = null
    user.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    delete axios.defaults.headers.common['Authorization']
  }

  if (token.value) {
    axios.defaults.headers.common['Authorization'] = `Bearer ${token.value}`
  }

  return {
    token,
    user,
    loading,
    error,
    isAuthenticated,
    isAdmin,
    setToken,
    setUser,
    register,
    login,
    logout
  }
})
