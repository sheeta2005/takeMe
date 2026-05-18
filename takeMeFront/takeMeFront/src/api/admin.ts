import request from '@/utils/request'

// 分页查询志愿者列表
export const getVolunteerPage = (page: number, pageSize: number) => {
  return request({
    url: '/api/admin/volunteer/page',
    method: 'get',
    params: { page, pageSize }
  })
}

// 搜索志愿者（补全所有参数类型）
export const volunteerSearch = (
  page: number,
  pageSize: number,
  username: string,
  id: number | null,
  availableRange: string
) => {
  return request({
    url: '/api/admin/volunteer/search',
    method: 'get',
    params: { page, pageSize, username, id, availableRange }
  })
}

// 新增志愿者
export const volunteerAdd = (data: {
  username: string
  phone: string
  age: number
  gender: number
  address: string
  availableRange: string
  freeTime: number
  workDay: number
  status: number
}) => {
  return request({
    url: '/api/admin/volunteer/add',
    method: 'post',
    data
  })
}

// 修改志愿者
export const volunteerUpdate = (data: {
  id: number
  username: string
  phone: string
  age: number
  gender: number
  address: string
  availableRange: string
  freeTime: number
  workDay: number
  status: number
}) => {
  return request({
    url: '/api/admin/volunteer/update',
    method: 'post',
    data
  })
}

// 删除志愿者
export const volunteerDelete = (id: number) => {
  return request({
    url: '/api/admin/volunteer/delete',
    method: 'get',
    params: { id }
  })
}
