import request from '@/utils/request'


// 获取商品列表
export function getCategoryList() {
  return request({
    url: '/category/list',
    method: 'get'
  })
}
