<template>
  <div class="page-container">
    <h3>老人管理</h3>
    <el-table :data="[]" border>
      <el-table-column prop="id" label="ID" />
      <el-table-column prop="name" label="姓名" />
      <el-table-column prop="phone" label="电话" />
    </el-table>
  </div>
</template>
