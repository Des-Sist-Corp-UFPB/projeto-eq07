<template>
  <div
    v-if="message"
    :class="[
      'p-4 rounded-lg mb-4',
      {
        'bg-green-100 text-green-700': type === 'success',
        'bg-red-100 text-red-700': type === 'error',
        'bg-blue-100 text-blue-700': type === 'info',
        'bg-yellow-100 text-yellow-700': type === 'warning'
      }
    ]"
  >
    {{ message }}
  </div>
</template>

<script setup>
defineProps({
  message: String,
  type: { type: String, default: 'info' }
})
</script>
