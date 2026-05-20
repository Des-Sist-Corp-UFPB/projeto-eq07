<template>
  <nav class="bg-white shadow">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
      <div class="flex justify-between h-16">
        <div class="flex items-center">
          <RouterLink to="/" class="text-2xl font-bold text-blue-600">Mercado</RouterLink>
        </div>
        <div class="flex items-center gap-4">
          <RouterLink v-if="authStore.isAuthenticated" to="/dashboard" class="text-gray-600 hover:text-gray-900">
            Dashboard
          </RouterLink>
          <RouterLink v-if="authStore.isAuthenticated" to="/profile" class="text-gray-600 hover:text-gray-900">
            Perfil
          </RouterLink>
          <button
            v-if="authStore.isAuthenticated"
            @click="logout"
            class="bg-red-600 text-white px-4 py-2 rounded hover:bg-red-700 transition"
          >
            Sair
          </button>
          <div v-else class="flex gap-2">
            <RouterLink to="/login" class="text-gray-600 hover:text-gray-900">Login</RouterLink>
            <RouterLink to="/register" class="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700 transition">
              Registrar
            </RouterLink>
          </div>
        </div>
      </div>
    </div>
  </nav>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/authStore'

const router = useRouter()
const authStore = useAuthStore()

const logout = () => {
  authStore.logout()
  router.push('/')
}
</script>
