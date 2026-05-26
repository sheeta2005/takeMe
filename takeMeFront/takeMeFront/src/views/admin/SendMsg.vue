<template>
  <div class="page-container">
    <h2 class="page-title">发送消息</h2>
    <el-form>
      <el-form-item label="接收方">
        <el-select>
          <el-option label="所有志愿者" value="volunteer" />
          <el-option label="所有老人" value="elder" />
        </el-select>
      </el-form-item>
      <el-form-item label="消息内容">
        <el-input type="textarea" rows="4" placeholder="请输入消息内容" />
      </el-form-item>
      <el-button type="primary">发送</el-button>
    </el-form>
  </div>
</template>

<script setup lang="ts"></script>
<style scoped>
.page-container { max-width: 1200px; }
.page-title { font-size: 28px; font-weight: bold; color: #333; margin-bottom: 24px; }
</style>
