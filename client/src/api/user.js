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

export function getAuthInfoApi() {
  return request({
    url: '/user/info',
    method: 'get'
  })
}
export const registerApi = (data) => {
  return request({
    url: '/auth/register',
    method: 'post',
    data
  })
  
}
/**
 * 发送短信验证码
 * @param {Object} data - { phoneNumber, scene }
 */
export function sendSmsCodeApi(data) {
  return request({
    url: '/sms/send',
    method: 'post',
    data
  })
}

/**
 * 校验短信验证码（可选，一般登录接口内部会调用，但也可单独使用）
 * @param {Object} data - { phoneNumber, scene, code }
 */
export function verifySmsCodeApi(data) {
  return request({
    url: '/sms/verify',
    method: 'post',
    data
  })
}

/**
 * 短信登录（手机号 + 验证码）
 * 如果后端没有独立接口，也可以先调用 verifySmsCodeApi，再调用 loginApi 模拟手机号登录
 * @param {Object} data - { phone, code, guestId? }
 */
export function smsLoginApi(data) {
  return request({
    // 请根据实际后端接口路径修改，例如 '/auth/sms-login' 或 '/sms/login'
    url: '/auth/sms-login',
    method: 'post',
    data
  })
}

export function updateUserInfoApi(data) {
  return request({
    url: '/user/info',
    method: 'put',
    data
  })
}