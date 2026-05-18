<template>
  <div class="page-container">
    <h3>订单管理（仿苍穹外卖）</h3>
    <el-table :data="[]" border>
      <el-table-column prop="id" label="订单ID" />
      <el-table-column prop="username" label="老人姓名" />
      <el-table-column prop="status" label="状态" />
    </el-table>
  </div>
</template>

<script setup lang="ts">
</script>
