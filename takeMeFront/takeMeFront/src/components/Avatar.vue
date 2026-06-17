<template>
  <el-avatar
    :size="size"
    :src="displaySrc"
    fit="cover"
    @error="handleError"
  />
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import defaultAvatar from '@/assets/佐仓杏子.png'

const props = withDefaults(defineProps<{
  src?: string
  size?: number | string
}>(), {
  size: 40
})

const displaySrc = ref(props.src || defaultAvatar)

watch(() => props.src, (newVal) => {
  displaySrc.value = newVal || defaultAvatar
})

const handleError = () => {
  displaySrc.value = defaultAvatar
}
</script>
