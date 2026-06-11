import request from '@/utils/request' // 👈 确保这个路径和你项目里的 axios 实例一致

export const addressApi = {
  /**
   * 1. 获取当前用户所有地址
   * GET /address/list
   */
  getAddressList() {
    return request({
      url: '/address/list',
      method: 'get'
    })
  },

  /**
   * 2. 新增当前用户地址
   * POST /address
   * @param {Object} data 对应后端的 AddressDTO.AddDTO
   */
  addAddress(data) {
    return request({
      url: '/address',
      method: 'post',
      data
    })
  },

  /**
   * 3. 修改用户地址
   * PUT /address/{id}
   * @param {Number|String} addressId 地址ID
   * @param {Object} data 对应后端的 AddressDTO.UpdateDTO
   */
  updateAddress(addressId, data) {
    return request({
      url: `/address/${addressId}`,
      method: 'put',
      data
    })
  },

  /**
   * 4. 删除用户地址
   * DELETE /address/{id}
   * @param {Number|String} addressId 地址ID
   */
  deleteAddress(addressId) {
    return request({
      url: `/address/${addressId}`,
      method: 'delete'
    })
  }
}