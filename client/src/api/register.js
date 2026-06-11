import request from '@/utils/request'

/**
 * 用户注册
 * @param {Object} data - { username, password, confirmPassword? }
 * @returns Promise
 */
export const registerApi = (data) => {
  return request({
    url: '/auth/register',
    method: 'post',
    data
  })
}