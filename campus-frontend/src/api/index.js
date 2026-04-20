/**
 * 统一 API 层
 * 所有接口调用集中在此，便于后续统一加 token/错误处理
 */
const BASE = '/api'
const REQUEST_TIMEOUT = 3000

// 用于存储当前 token
let currentToken = null

function formatHttpError(status) {
  if (status >= 500) return '服务器开小差了，请稍后重试'
  if (status === 404) return '请求的资源不存在'
  if (status === 401) return '登录状态已失效，请重新登录'
  if (status === 403) return '当前操作没有权限'
  if (status >= 400) return '请求参数有误，请检查后重试'
  return `HTTP ${status}`
}

function mapErrorMessage(err) {
  if (!err) return '请求失败，请稍后重试'
  if (err.name === 'AbortError') return '网络超时（超过 3 秒），请稍后重试'
  if (typeof err.message === 'string' && err.message.startsWith('HTTP_')) {
    const status = Number(err.message.replace('HTTP_', ''))
    return formatHttpError(status)
  }
  return err.message || '请求失败，请稍后重试'
}

async function request(url, options = {}) {
  const controller = new AbortController()
  const timeoutId = setTimeout(() => controller.abort(), REQUEST_TIMEOUT)

  try {
    const headers = { 'Content-Type': 'application/json', ...options.headers }
    
    // 如果有 token，自动添加到请求头
    if (currentToken) {
      headers.Authorization = `Bearer ${currentToken}`
    }
    
    const res = await fetch(BASE + url, {
      headers,
      signal: controller.signal,
      ...options
    })
    if (!res.ok) throw new Error(`HTTP_${res.status}`)
    const data = await res.json()

    // 统一处理业务错误码
    if (data && typeof data === 'object' && data.code !== 200) {
      throw new Error(data.msg || '请求失败')
    }
    return data
  } catch (err) {
    throw new Error(mapErrorMessage(err))
  } finally {
    clearTimeout(timeoutId)
  }
}

// 导出 token 管理函数
export function setToken(token) {
  currentToken = token
}

export function getToken() {
  return currentToken
}

export function clearToken() {
  currentToken = null
}

// ---------- 信息接口 ----------
export const itemApi = {
  /** 分页列表，支持 type / keyword / category 筛选 */
  list(params = {}) {
    const q = new URLSearchParams()
    if (params.page)     q.set('page', params.page)
    if (params.size)     q.set('size', params.size)
    if (params.type != null) q.set('type', params.type)
    if (params.status != null) q.set('status', params.status)
    if (params.keyword)  q.set('keyword', params.keyword)
    if (params.category) q.set('category', params.category)
    return request(`/items?${q}`)
  },
  /** 详情 */
  detail(id) {
    return request(`/items/${id}`)
  },
  /** 发布新信息 */
  publish(data) {
    return request('/items', { method: 'POST', body: JSON.stringify(data) })
  },
  /** 编辑并重提 */
  update(id, data) {
    return request(`/items/${id}`, { method: 'PUT', body: JSON.stringify(data) })
  },
  /** 更新状态（标记完结等） */
  updateStatus(id, status) {
    return request(`/items/${id}/status`, { method: 'PATCH', body: JSON.stringify({ status }) })
  },
  /** 删除 */
  remove(id) {
    return request(`/items/${id}`, { method: 'DELETE' })
  },
  /** 个人中心：我的发布 */
  myItems() {
    return request(`/items/my`)
  },
  /** 管理员审核：通过(0) / 驳回(3) */
  review(id, status, rejectReason) {
    return request(`/items/${id}/review`, {
      method: 'PATCH',
      body: JSON.stringify({ status, rejectReason })
    })
  },
  /** 管理员统计看板 */
  stats() {
    return request('/admin/stats')
  }
}

// ---------- 管理员接口 ----------
export const adminApi = {
  login(data) {
    return request('/admin/login', { method: 'POST', body: JSON.stringify(data) })
  },
  pending() {
    return request('/admin/pending')
  },
  approve(id) {
    return request(`/admin/${id}/approve`, { method: 'PATCH' })
  },
  reject(id, rejectReason) {
    return request(`/admin/${id}/reject`, {
      method: 'PATCH',
      body: JSON.stringify({ rejectReason })
    })
  },
  batchAction(itemIds, action, rejectReason = '') {
    const payload = { itemIds, action }
    if (rejectReason) payload.rejectReason = rejectReason
    return request('/admin/batch-action', {
      method: 'POST',
      body: JSON.stringify(payload)
    })
  },
  stats() {
    return request('/admin/stats')
  }
}

// ---------- 用户接口 ----------
export const userApi = {
  register(data) {
    return request('/auth/register', { method: 'POST', body: JSON.stringify(data) })
  },
  login(data) {
    return request('/auth/login', { method: 'POST', body: JSON.stringify(data) })
  },
  logout() {
    return request(`/auth/logout`, { method: 'POST' })
  },
  info() {
    return request(`/user/info`)
  }
}

// ---------- 文件上传 ----------
export const fileApi = {
  async upload(file) {
    const formData = new FormData()
    formData.append('file', file)

    const controller = new AbortController()
    const timeoutId = setTimeout(() => controller.abort(), REQUEST_TIMEOUT)
    try {
      // 上传不设 Content-Type，让浏览器自动设置 multipart boundary
      const headers = {}
      
      // 如果有 token，自动添加到请求头
      if (currentToken) {
        headers.Authorization = `Bearer ${currentToken}`
      }
      
      const response = await fetch(`${BASE}/upload`, {
        method: 'POST',
        headers,
        body: formData,
        signal: controller.signal
      })
      if (!response.ok) throw new Error(`HTTP_${response.status}`)
      const data = await response.json()
      if (data && typeof data === 'object' && data.code !== 200) {
        throw new Error(data.msg || '上传失败')
      }
      return data
    } catch (err) {
      throw new Error(mapErrorMessage(err))
    } finally {
      clearTimeout(timeoutId)
    }
  }
}

// ---------- 收藏接口 ----------
export const favoriteApi = {
  /** 添加收藏 */
  add(itemId) {
    return request('/favorites', { method: 'POST', body: JSON.stringify({ itemId }) })
  },
  /** 取消收藏 */
  remove(itemId) {
    return request(`/favorites/${itemId}`, { method: 'DELETE' })
  },
  /** 获取收藏列表 */
  list(page = 1, size = 20) {
    return request(`/favorites?current=${page}&size=${size}`)
  },
  /** 检查是否已收藏 */
  check(itemId) {
    return request(`/favorites/check?itemId=${itemId}`)
  }
}

// ---------- 消息通知接口 ----------
export const noticeApi = {
  /** 获取所有通知 */
  list() {
    return request('/notice/list')
  },
  /** 获取未读通知数 */
  getUnreadCount() {
    return request('/notice/unread-count')
  },
  /** 标记单个通知为已读 */
  markAsRead(noticeId) {
    return request(`/notice/${noticeId}/mark-read`, { method: 'POST' })
  },
  /** 标记全部通知为已读 */
  markAllAsRead() {
    return request('/notice/mark-all-read', { method: 'POST' })
  },
  /** 删除单个通知 */
  delete(noticeId) {
    return request(`/notice/${noticeId}`, { method: 'DELETE' })
  },
  /** 删除全部通知 */
  deleteAll() {
    return request('/notice/delete-all', { method: 'DELETE' })
  }
}
