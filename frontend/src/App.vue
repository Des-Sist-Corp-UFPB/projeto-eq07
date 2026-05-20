<template>
  <div class="flex flex-col min-h-screen bg-gray-50">
    <Nav v-if="!hideNav" />
    <main class="flex-1">
      <RouterView />
    </main>
    <Footer v-if="!hideNav" />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import Nav from './components/Nav.vue'
import Footer from './components/Footer.vue'

const route = useRoute()
const hideNav = computed(() => route.meta.hideNav)
</script>
