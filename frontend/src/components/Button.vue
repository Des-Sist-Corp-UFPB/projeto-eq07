<template>
  <button
    :class="[
      'px-6 py-2 rounded-lg font-medium transition',
      {
        'bg-blue-600 text-white hover:bg-blue-700': variant === 'primary',
        'bg-red-600 text-white hover:bg-red-700': variant === 'danger',
        'bg-gray-300 text-gray-700 hover:bg-gray-400': variant === 'secondary',
        'opacity-50 cursor-not-allowed': loading || disabled
      }
    ]"
    :disabled="loading || disabled"
  >
    <span v-if="loading" class="inline-block mr-2">⏳</span>
    {{ label }}
  </button>
</template>

<script setup>
defineProps({
  label: String,
  variant: { type: String, default: 'primary' },
  loading: Boolean,
  disabled: Boolean
})
</script>
