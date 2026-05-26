三、关键说明（对接后端必看）
接口约定
后端接口地址：GET /api/admin/orders
请求参数：
status：订单状态（active/completed/cancelled）
page：当前页码
pageSize：每页条数
返回格式：
json
{
"list": [/* 订单数组 */],
"total": 128 // 总条数
}
跳转逻辑
点击卡片时，会携带 ?status=active 跳转到 /admin/order
页面加载时，自动读取路由参数，设置筛选条件并调用接口
状态映射
代码里的 active/completed 等状态值，必须和后端数据库里的状态字段完全一致，否则筛选会失效。
