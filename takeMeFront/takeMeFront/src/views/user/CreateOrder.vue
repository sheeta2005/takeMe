<template>
  <div class="page-container">
    <h3>创建服务订单</h3>
    <el-form label-width="100px">
      <el-form-item label="服务类型">
        <el-select placeholder="请选择服务类型">
          <el-option label="助餐" value="meal" />
          <el-option label="助洁" value="clean" />
          <el-option label="助医" value="medical" />
        </el-select>
      </el-form-item>
      <el-form-item label="备注">
        <el-input type="textarea" rows="3" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary">提交订单</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>
