import request from '@/utils/request'

// 登录
export function loginApi(data) {
  return request({
    url: '/auth/login',
    method: 'post',
    data
  })
}

export function logoutApi() {
  return request({
    url: '/auth/logout',
    method: 'post'
  })
}
// 获取用户列表
export function getUserList() {
  return request({
    url: '/user/list',
    method: 'get'
  })
}
