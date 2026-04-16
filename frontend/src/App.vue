<template>
  <router-view />
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

onMounted(async () => {
  if (userStore.token && !userStore.user) {
    try {
      await userStore.hydrateUser()
    } catch {
      // handled in store
    }
  }
})
</script>
