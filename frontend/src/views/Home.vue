<template>
  <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
    <div class="text-center mb-12">
      <h1 class="text-5xl font-bold text-blue-600 mb-4">🏃 Sistema de Gerenciamento de Corridas</h1>
      <p class="text-xl text-gray-600 mb-8">Plataforma completa para organizar, gerenciar e acompanhar corridas e participantes</p>
      <div v-if="!authStore.isAuthenticated" class="flex gap-4 justify-center">
        <RouterLink to="/login" class="bg-blue-600 text-white px-8 py-3 rounded-lg hover:bg-blue-700 transition font-semibold">
          Entrar
        </RouterLink>
        <RouterLink to="/register" class="bg-green-600 text-white px-8 py-3 rounded-lg hover:bg-green-700 transition font-semibold">
          Registrar-se
        </RouterLink>
      </div>
      <div v-else class="flex gap-4 justify-center">
        <RouterLink to="/dashboard" class="bg-blue-600 text-white px-8 py-3 rounded-lg hover:bg-blue-700 transition font-semibold">
          Ir para Dashboard
        </RouterLink>
      </div>
    </div>
    <div class="grid md:grid-cols-3 gap-8">
      <div class="bg-white p-6 rounded-lg shadow-md border-t-4 border-blue-600">
        <h3 class="text-xl font-bold mb-2">🎯 Organização Completa</h3>
        <p class="text-gray-600">Crie e organize corridas com facilidade. Gerencie participantes, trajetos e cronogramas</p>
      </div>
      <div class="bg-white p-6 rounded-lg shadow-md border-t-4 border-green-600">
        <h3 class="text-xl font-bold mb-2">🔒 Segurança Total</h3>
        <p class="text-gray-600">Autenticação por JWT e controle de acesso. Seus dados estão protegidos</p>
      </div>
      <div class="bg-white p-6 rounded-lg shadow-md border-t-4 border-purple-600">
        <h3 class="text-xl font-bold mb-2">📊 Acompanhamento em Tempo Real</h3>
        <p class="text-gray-600">Rankings, resultados e estatísticas atualizados em tempo real</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useAuthStore } from '../stores/authStore'

const authStore = useAuthStore()
</script>
