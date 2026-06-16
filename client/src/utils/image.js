// src/utils/image.js
// 图片智能路由工具
export const IS_LOCAL_MOCK = false
export const defaultPlaceholder = '/products/10001_1.png'

/**
 * 智能图片路由函数
 * @param {string} url - 后端返回的图片完整 URL
 * @param {number} width - 缩略图宽度
 * @returns {string} 最终图片地址
 */
export const optimizeImage = (url, width = 400) => {
  if (!url) return defaultPlaceholder

  // 从 URL 中提取纯文件名
  const fileName = url.substring(url.lastIndexOf('/') + 1)

  if (IS_LOCAL_MOCK) {
    // 本地模式：直接走 public 文件夹
    return `/products/${fileName}`
  }

  // OSS 模式
  if (!url.startsWith('http')) return defaultPlaceholder

  // 已经有参数或非阿里云 OSS 链接，原样返回
  if (url.includes('x-oss-process') || !url.includes('aliyuncs.com')) return url

  // OSS 裁剪/压缩/WebP
  return `${url}?x-oss-process=image/resize,w_${width}/quality,q_70/format,webp`
}