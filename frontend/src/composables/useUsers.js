import { ref } from 'vue'
import axios from 'axios'
import { api } from '../config/api'

export function useUsers() {
  const loading = ref(false)
  const error = ref(null)

  const editUser = async (id, formData) => {
    loading.value = true
    error.value = null
    try {
      const response = await axios.patch(`${api.baseURL}${api.endpoints.user.edit(id)}`, formData)
      return response.data
    } catch (err) {
      error.value = err.response?.data?.mensagem || 'Erro ao editar usuário'
      throw error.value
    } finally {
      loading.value = false
    }
  }

  const deleteUser = async (id) => {
    loading.value = true
    error.value = null
    try {
      await axios.delete(`${api.baseURL}${api.endpoints.user.delete(id)}`)
      return true
    } catch (err) {
      error.value = err.response?.data?.mensagem || 'Erro ao deletar usuário'
      throw error.value
    } finally {
      loading.value = false
    }
  }

  return {
    loading,
    error,
    editUser,
    deleteUser
  }
}
