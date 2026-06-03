import { ref, computed } from 'vue'
import axios from 'axios'
import { api } from '../config/api'

const token = ref(localStorage.getItem('token') || null)
const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))
const loading = ref(false)
const error = ref(null)

const isAuthenticated = computed(() => !!token.value)

export function useAuth() {
  const register = async (formData) => {
    loading.value = true
    error.value = null
    try {
      const response = await axios.post(`${api.baseURL}${api.endpoints.user.register}`, formData)
      token.value = response.data.token
      localStorage.setItem('token', token.value)
      setAuthHeader()
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
      token.value = response.data.token
      localStorage.setItem('token', token.value)
      setAuthHeader()
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

  const setAuthHeader = () => {
    if (token.value) {
      axios.defaults.headers.common['Authorization'] = `Bearer ${token.value}`
    }
  }

  const setUser = (userData) => {
    user.value = userData
    localStorage.setItem('user', JSON.stringify(userData))
  }

  if (token.value) {
    setAuthHeader()
  }

  return {
    token,
    user,
    loading,
    error,
    isAuthenticated,
    register,
    login,
    logout,
    setUser,
    setAuthHeader
  }
}
