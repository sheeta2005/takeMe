/**
 * 分页返回结果
 */
export interface PageResult<T> {
  total: number        // 总条数
  records: T[]         // 当前页数据
}
