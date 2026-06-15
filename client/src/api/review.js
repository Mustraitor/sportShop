import request from '@/utils/request'

// 发表评价
export function submitReview(data) {
  return request({
    url: '/review',
    method: 'post',
    data  // { productId, rating, content }
  })
}

// 获取商品评价列表
export function getProductReviews(productId, params = {}) {
  return request({
    url: `/review/product/${productId}`,
    method: 'get',
    params: { page: 1, size: 10, ...params }
  })
}