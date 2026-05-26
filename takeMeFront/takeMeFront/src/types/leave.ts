/**
 * 请假信息
 */
export interface leave {
  id: number
  volunteerId: number
  type: 0 | 1 // 0=事假 1=病假
  startTime: string
  endTime: string
  reason: string
  status: 0 | 1 | 2 // 0=待审核 1=已通过 2=已拒绝
  createTime?: string
}
