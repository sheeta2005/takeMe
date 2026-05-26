<template>
  <div class="page-container">
    <h2 class="page-title">今日待办</h2>
    <el-list :data="todoList">
      <el-list-item v-for="item in todoList" :key="item.id">
        <el-checkbox v-model="item.done">{{ item.content }}</el-checkbox>
      </el-list-item>
    </el-list>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
const todoList = ref([
  { id: 1, content: '审核志愿者请假申请', done: false },
  { id: 2, content: '处理超时未接单订单', done: false }
])
</script>

<style scoped>
.page-container { max-width: 1200px; }
.page-title { font-size: 28px; font-weight: bold; color: #333; margin-bottom: 24px; }
</style>
