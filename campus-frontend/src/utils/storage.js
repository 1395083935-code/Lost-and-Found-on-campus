/**
 * localStorage 工具
 * 统一管理用户登录态的读写，避免 key 散落在多处
 *
 * 注：登录态写入时机由登录/注册成功后主动调用 saveUser()
 *     Phase 2 建好骨架，Phase 3 接入真实登录后再写入
 */
const USER_KEY = 'campus_user'
const SESSION_TTL = 2 * 60 * 60 * 1000

export const storage = {
  /** 持久化登录用户信息，密码字段应在写入前置空 */
  saveUser(user) {
    if (!user) return
    localStorage.setItem(USER_KEY, JSON.stringify({
      ...user,
      expiresAt: Date.now() + SESSION_TTL
    }))
  },
  /** 读取登录用户信息，未登录返回 null */
  getUser() {
    const session = this.getUserSession()
    return session.user
  },
  /** 读取登录用户信息并返回状态 */
  getUserSession() {
    try {
      const raw = localStorage.getItem(USER_KEY)
      if (!raw) {
        return { user: null, expired: false }
      }
      const parsed = JSON.parse(raw)
      if (parsed && parsed.expiresAt && parsed.expiresAt <= Date.now()) {
        localStorage.removeItem(USER_KEY)
        return { user: null, expired: true }
      }
      return { user: parsed, expired: false }
    } catch {
      localStorage.removeItem(USER_KEY)
      return { user: null, expired: false }
    }
  },
  /** 退出登录时清空 */
  clearUser() {
    localStorage.removeItem(USER_KEY)
  },
  /** 是否已登录 */
  isLoggedIn() {
    return this.getUserSession().user !== null
  }
}
