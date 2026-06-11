/**
 * 根据后端真实的数字 ID，通过加盐哈希算法生成一个固定长度的 16 位伪随机长订单号
 * @param {number|string} realId 后端真实自增 ID
 * @returns {string} 16位电商风格长订单号
 */
export const generateDisplayOrderNo = (realId) => {
  if (!realId) return ''
  const idNum = Number(realId)
  
  // 算法思路：固定前缀 + 真实ID左填充 + 伪随机扰乱因子
  // 扰乱因子：用 realId 乘以大质数再取模，确保不同的 id 映射出完全不同的尾数
  const salt = (idNum * 1140671485 + 12820163) % 100000
  const paddedSalt = String(salt).padStart(5, '0')
  
  // 固定前缀（如代表业务线或系统立项年份）
  const prefix = '2026'
  
  // 将真实 ID 转为 7 位长字符串，不够的前面补 0 (例如: 6 -> 0000006)
  const paddedId = String(idNum).padStart(7, '0')
  
  // 最终组合：4位 + 7位 + 5位 = 16位专业长单号
  return `${prefix}${paddedId}${paddedSalt}`
}

/**
 * 根据后端的数字状态，返回对应的中文状态文本
 * @param {number} status 后端返回的状态数字
 * @returns {string} 中文状态文本
 */
export const getStatusText = (status) => {
  const statusMap = {
    0: '待付款',
    1: '待收货',
    2: '已取消',
    3: '已完成'
  }
  return statusMap[status] ?? '未知状态'
}