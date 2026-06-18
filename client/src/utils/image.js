// src/utils/image.js
export const IS_LOCAL_MOCK = false

// 真实北京地域的阿里云 OSS 根域名
const OSS_ROOT_URL = 'https://sportshop-pictures.oss-cn-beijing.aliyuncs.com' 

// 默认兜底图
export const defaultPlaceholder = `${OSS_ROOT_URL}/upload/80007_main.png`

/**
 * 智能图片路由与路径补全函数（全项目通用兼容版）
 * @param {string} url - 后端返回的图片 URL (可能是完整长链、带 upload/ 的相对路径、或纯文件名)
 * @param {number} width - 缩略图宽度
 * @returns {string} 最终图片地址
 */
export const optimizeImage = (url, width = 400) => {
  // 1. 防空校验
  if (!url || typeof url !== 'string' || url.trim() === '') return defaultPlaceholder

  let finalUrl = url

  // 2. 本地 Mock 模式
  if (IS_LOCAL_MOCK) {
    const cleanUrl = url.split('?')[0]
    const fileName = cleanUrl.substring(cleanUrl.lastIndexOf('/') + 1)
    return `/products/${fileName}`
  }

  // 3. 智能补全路径与域名
  if (!finalUrl.startsWith('http') && !finalUrl.startsWith('//')) {
    // 移除开头可能存在的斜杠，统一格式
    let cleanPath = finalUrl.startsWith('/') ? finalUrl.slice(1) : finalUrl
    
    // 【核心兼容逻辑】
    // 如果相对路径里不包含 upload/，说明是购物车这种纯文件名裸奔的情况，手动帮它加上
    if (!cleanPath.startsWith('upload/')) {
      cleanPath = `upload/${cleanPath}`
    }
    
    // 统一拼接上 OSS 根域名
    finalUrl = `${OSS_ROOT_URL}/${cleanPath}`
  }

  // 4. 防重复拼接：如果已经有 OSS 处理参数，或者不是你这个 OSS 的域名，直接原样返回
  if (finalUrl.includes('x-oss-process') || !finalUrl.includes('sportshop-pictures')) {
    return finalUrl
  }

  // 5. 拼接正确的阿里云 OSS 裁剪与 WebP 优化参数
  return `${finalUrl}?x-oss-process=image/resize,w_${width}/quality,q_70/format,webp`
}