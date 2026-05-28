/**
 * 请假信息 VolunteerLeave
 */

export interface VolunteerLeave {
  id: number                           // 主键ID
  volunteerId: number                  // 关联志愿者ID
  type: 0 | 1                          // 0=事假 1=病假
  startTime: string                     // 开始时间
  endTime: string                       // 结束时间
  reason: string                        // 请假原因
  status: 0 | 1 | 2                    // 0=待审核 1=已通过 2=已拒绝
  createTime: string                    // 提交时间
}
