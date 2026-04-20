<template>
  <div id="app">
    <div v-if="flowMessage" class="flow-toast">{{ flowMessage }}</div>

    <!-- 首页 -->
    <div v-if="currentPage === 'home'">
      <header class="page-header">
        <div class="search-panel">
          <input
            v-model="searchText"
            type="search"
            placeholder="搜索物品名称 / 地点 / 描述"
            @keyup.enter="applySearch"
          />
          <button type="button" class="search-button" @click="applySearch">搜索</button>
        </div>

        <div class="announcement">
          <span class="announce-label">公告：</span>
          <span class="announce-text">{{ announcement }}</span>
        </div>
      </header>

      <section class="tab-panel">
        <button
          :class="['tab-button', activeTab === 'lost' ? 'active' : '']"
          @click="setTab('lost')"
        >
          失物寻主
        </button>
        <button
          :class="['tab-button', activeTab === 'found' ? 'active' : '']"
          @click="setTab('found')"
        >
          拾物招领
        </button>
      </section>

      <section class="filter-panel">
        <div class="filter-group">
          <span class="filter-title">分类：</span>
          <button
            v-for="category in categories"
            :key="category.value"
            :class="['filter-pill', selectedCategory === category.value ? 'selected' : '']"
            @click="selectCategory(category.value)"
          >
            {{ category.label }}
          </button>
        </div>
        <div class="filter-group">
          <span class="filter-title">时间：</span>
          <button
            v-for="option in timeOptions"
            :key="option.value"
            :class="['filter-pill', selectedTime === option.value ? 'selected' : '']"
            @click="selectTime(option.value)"
          >
            {{ option.label }}
          </button>
        </div>
      </section>

      <section class="meta-bar">
        <span class="meta-text">共 {{ totalCount }} 条结果（加载 {{ items.length }}）</span>
        <span class="meta-text">当前类型：{{ activeTab === 'lost' ? '失物寻主' : '拾物招领' }}</span>
      </section>

      <section class="list-container">
        <div v-if="!loading && filteredItems.length === 0" class="empty-card">
          暂无匹配信息，建议切换筛选条件或点击发布。
        </div>

        <article v-for="item in filteredItems" :key="item.id" class="item-card" @click="goDetail(item, 'home')">
          <div class="item-card-top">
            <div class="item-thumb" :class="item.imageUrl ? 'has-image' : ''">
              <template v-if="item.imageUrl">
                <SmartImage :src="item.imageUrl" alt="物品图片" variant="thumb" />
              </template>
              <template v-else>
                <div class="thumb-placeholder">图片</div>
              </template>
            </div>

            <div class="item-card-body">
              <div class="item-title-row">
                <h2 class="item-title">{{ item.title || '无标题物品' }}</h2>
                <span class="status-label" :class="item.statusClass">{{ item.statusLabel }}</span>
              </div>
              <p class="item-description">{{ item.description || '暂无描述' }}</p>
              <div class="item-meta-row">
                <span>📍 {{ item.location || '校园未知地点' }}</span>
                <span>⏰ {{ item.createTime }}</span>
              </div>
            </div>
          </div>
          <div class="item-card-footer">
            <span class="type-tag" :class="item.typeClass">{{ item.typeLabel }}</span>
            <span class="contact-info">联系方式：{{ item.contactInfo || '未填写' }}</span>
          </div>
        </article>

        <!-- 加载更多提示 -->
        <div v-if="!loading && items.length > 0" class="load-more-section">
          <div v-if="loadingMore" class="loading-indicator">
            <div class="spinner-dot"></div>
            <span>加载中...</span>
          </div>
          <div v-else-if="hasMore" class="load-more-text">
            <span>向下滚动加载更多</span>
          </div>
          <div v-else class="no-more-text">
            <span>已加载全部信息</span>
          </div>
        </div>
      </section>

      <button class="publish-button" @click="onPublishClick($event)">
        <span class="publish-btn-icon">＋</span>
        <span class="publish-btn-text">发布</span>
      </button>

    </div>

    <!-- 个人中心 -->
    <ProfilePage
      v-if="currentPage === 'profile'"
      :items="myItems"
      :user="currentUser"
      @open-detail="goDetail($event, 'profile')"
      @mark-done="markItemDone"
      @delete-item="deleteItem"
      @edit-item="editItem"
      @logout="handleLogout"
    />

    <!-- 发布页面 -->
    <PublishPage v-if="currentPage === 'publish'" :draft-item="editingItem" :current-user="currentUser" @back="goBack" />

    <!-- 详情页面 -->
    <DetailPage v-if="currentPage === 'detail'" :item="selectedItem" :current-user="currentUser" @back="goBackFromDetail" />

    <!-- 管理后台 -->
    <AdminPage v-if="currentPage === 'admin'" :items="items" @back="goHome" />

    <!-- 登录/注册模态框 -->
    <div v-if="showLoginModal" class="modal-overlay" @click.self="showLoginModal = false">
      <div class="modal-box">
        <button class="modal-close" @click="showLoginModal = false">×</button>
        <h2 class="modal-title">{{ loginMode === 'login' ? '登录账号' : '注册新账号' }}</h2>
        <form @submit.prevent="handleLoginSubmit">
          <div class="modal-field">
            <label>用户名</label>
            <input v-model="loginForm.username" type="text" placeholder="请输入用户名" required />
          </div>
          <div class="modal-field">
            <label>密码</label>
            <input v-model="loginForm.password" type="password" placeholder="请输入密码" required />
          </div>
          <div v-if="loginMode === 'register'" class="modal-field">
            <label>联系方式</label>
            <input v-model="loginForm.contactInfo" type="text" placeholder="手机号或微信号" />
          </div>
          <div v-if="loginError" class="modal-error">{{ loginError }}</div>
          <button type="submit" class="modal-submit" :disabled="loginLoading">
            {{ loginLoading ? '处理中...' : (loginMode === 'login' ? '登录' : '注册') }}
          </button>
        </form>
        <p class="modal-switch">
          {{ loginMode === 'login' ? '还没有账号？' : '已有账号？' }}
          <button type="button" class="link-btn" @click="toggleLoginMode">
            {{ loginMode === 'login' ? '立即注册' : '去登录' }}
          </button>
        </p>
      </div>
    </div>

    <nav v-if="currentPage === 'home' || currentPage === 'profile'" class="bottom-nav">
      <button :class="['nav-item', currentPage === 'home' ? 'active' : '']" @click="goHome">首页</button>
      <button :class="['nav-item', currentPage === 'profile' ? 'active' : '']" @click="goProfile">个人中心</button>
      <button :class="['nav-item', 'nav-admin']" @click="goAdmin" title="管理员入口">⚙️</button>
    </nav>
  </div>
</template>

<script>
import PublishPage from './Publish.vue'
import DetailPage from './Detail.vue'
import ProfilePage from './Profile.vue'
import AdminPage from './Admin.vue'
import SmartImage from './components/SmartImage.vue'
import { itemApi, userApi } from './api/index.js'
import { storage } from './utils/storage.js'

export default {
  name: 'App',
  components: {
    PublishPage,
    DetailPage,
    ProfilePage,
    AdminPage,
    SmartImage
  },
  data() {
    return {
      currentPage: 'home', // 'home' | 'publish' | 'detail' | 'profile'
      previousPage: 'home',
      publishSourcePage: 'home',
      selectedItem: null,
      editingItem: null,
        myItems: [],
      showLoginModal: false,
      loginMode: 'login', // 'login' | 'register'
      loginForm: { username: '', password: '', contactInfo: '' },
      loginError: '',
      loginLoading: false,
      pendingPageAfterLogin: null,
      flowMessage: '',
      flowMessageTimer: null,
      currentUser: {
        id: null,
        name: '校园用户',
        role: 0,
        contactInfo: '13800138001'
      },
      items: [],
      loading: false,
      loadingMore: false,
      error: null,
      searchText: '',
      activeTab: 'lost',
      selectedCategory: 'all',
      selectedTime: 'all',
      announcement: '欢迎使用校园失物招领系统，发布信息请确保描述清晰。',
      // 分页相关
      pageNumber: 1,
      pageSize: 20,
      totalCount: 0,
      hasMore: true,
      categories: [
        { label: '全部', value: 'all' },
        { label: '证件', value: '证件' },
        { label: '电子', value: '电子产品' },
        { label: '文具', value: '文具饰品' },
        { label: '衣物', value: '衣物箱包' },
        { label: '其他', value: '其他' }
      ],
      timeOptions: [
        { label: '全部', value: 'all' },
        { label: '1天内', value: '1' },
        { label: '3天内', value: '3' },
        { label: '7天内', value: '7' }
      ]
    }
  },
  mounted() {
    const session = storage.getUserSession()
    if (session.user) {
      const stored = session.user
      this.currentUser = {
        id: stored.id || null,
        name: stored.username || stored.name || '校园用户',
        role: stored.role || 0,
        contactInfo: stored.contactInfo || ''
      }
      // 如果 storage 中有 token，恢复到 API 模块
      if (stored.token) {
        import('./api/index.js').then(({ setToken }) => {
          setToken(stored.token)
        })
      }
    } else if (session.expired) {
      alert('登录状态已过期，请重新登录')
    }
    this.fetchData()
    // 添加滚动事件监听器用于无限滚动
    window.addEventListener('scroll', this.handleScroll)
  },
  beforeUnmount() {
    // 移除滚动事件监听器
    window.removeEventListener('scroll', this.handleScroll)
  },
  computed: {
    filteredItems() {
      // 注：时间和分类过滤已由后端处理，前端仅做本地搜索过滤
      const keyword = this.searchText.trim().toLowerCase()
      if (!keyword) {
        return this.items
      }
      return this.items.filter(item => {
        return [
          item.title,
          item.description,
          item.location,
          item.contactInfo
        ].some(field => field && field.toString().toLowerCase().includes(keyword))
      })
    }
  },
  methods: {
    promptLogin(targetPage = 'profile') {
      this.pendingPageAfterLogin = targetPage
      this.loginMode = 'login'
      this.loginError = ''
      this.loginForm = { username: '', password: '', contactInfo: '' }
      this.showLoginModal = true
    },
    setTab(tab) {
      this.activeTab = tab
      this.resetPagination()
      this.fetchData()
    },
    selectCategory(value) {
      this.selectedCategory = value
      this.resetPagination()
      this.fetchData()
    },
    selectTime(value) {
      this.selectedTime = value
      this.resetPagination()
      this.fetchData()
    },
    applySearch() {
      this.resetPagination()
      this.fetchData()
    },
    resetPagination() {
      this.pageNumber = 1
      this.totalCount = 0
      this.hasMore = true
      this.items = []
    },
    formatType(type) {
      return type === 1 ? '拾物招领' : '失物寻主'
    },
    normalizeCategory(category) {
      const categoryMap = {
        certificate: '证件',
        electronic: '电子产品',
        stationery: '文具饰品',
        clothing: '衣物箱包',
        other: '其他'
      }
      return categoryMap[category] || category || '其他'
    },
    formatStatus(status) {
      const statusMap = {
        0: '待审核',
        1: '已通过',
        2: '已驳回',
        3: '已完结'
      }
      return statusMap[status] || '待审核'
    },
    statusClassOf(status) {
      const classMap = {
        0: 'status-review',
        1: 'status-approved',
        2: 'status-rejected',
        3: 'status-completed'
      }
      return classMap[status] || 'status-review'
    },
    formatTime(dateTime) {
      if (!dateTime) {
        return '未知时间'
      }
      const date = new Date(dateTime)
      const now = Date.now()
      const diff = now - date.getTime()
      const hours = Math.floor(diff / (1000 * 3600))
      if (hours < 1) {
        return '刚刚'
      }
      if (hours < 24) {
        return `${hours}小时前`
      }
      return date.toLocaleDateString()
    },
    buildListParams() {
      const params = {
        page: this.pageNumber,
        size: this.pageSize,
        status: 1,
        type: this.activeTab === 'lost' ? 0 : 1
      }
      const keyword = this.searchText.trim()
      if (keyword) {
        params.keyword = keyword
      }
      if (this.selectedCategory !== 'all') {
        params.category = this.selectedCategory
      }
      if (this.selectedTime !== 'all') {
        params.days = Number(this.selectedTime)
      }
      return params
    },
    async fetchData() {
      this.loading = true
      this.error = null
      try {
        const params = this.buildListParams()
        const body = await itemApi.list(params)
        let records = []
        let total = 0
        if (Array.isArray(body)) {
          records = body
          total = body.length
        } else if (body && Array.isArray(body.data)) {
          records = body.data
          total = body.data.length
        } else if (body && body.data && Array.isArray(body.data.records)) {
          records = body.data.records
          total = body.data.total || 0
        } else if (body && body.data) {
          records = body.data
          total = records.length
        }

        this.totalCount = total
        this.hasMore = (this.pageNumber * this.pageSize) < total

        const formattedRecords = records.map(item => {
          const formattedCreateTime = this.formatTime(item.createTime)
          const typeLabel = this.formatType(item.type)
          const statusLabel = this.formatStatus(item.status)
          return {
            ...item,
            title: item.title || '未命名物品',
            description: item.description || '暂无描述',
            category: this.normalizeCategory(item.category),
            location: item.location || '校园未知地点',
            contactInfo: item.contactInfo || '',
            imageUrl: item.imageUrl || '',
            rawCreateTime: item.createTime,
            createTime: formattedCreateTime,
            typeLabel,
            statusLabel,
            typeClass: item.type === 1 ? 'tag-found' : 'tag-lost',
            statusClass: this.statusClassOf(item.status)
          }
        })

        this.items = formattedRecords
      } catch (err) {
        console.error('请求出错：', err)
        this.error = '无法连接后端，请检查后端是否启动，或者查看控制台报错'
      } finally {
        this.loading = false
      }
    },
    async loadMore() {
      if (!this.hasMore || this.loadingMore) {
        return
      }
      this.loadingMore = true
      try {
        this.pageNumber += 1
        const params = this.buildListParams()
        const body = await itemApi.list(params)
        let records = []
        let total = 0
        if (Array.isArray(body)) {
          records = body
          total = body.length
        } else if (body && Array.isArray(body.data)) {
          records = body.data
          total = body.data.length
        } else if (body && body.data && Array.isArray(body.data.records)) {
          records = body.data.records
          total = body.data.total || 0
        } else if (body && body.data) {
          records = body.data
          total = records.length
        }

        this.totalCount = total
        this.hasMore = (this.pageNumber * this.pageSize) < total

        const formattedRecords = records.map(item => {
          const formattedCreateTime = this.formatTime(item.createTime)
          const typeLabel = this.formatType(item.type)
          const statusLabel = this.formatStatus(item.status)
          return {
            ...item,
            title: item.title || '未命名物品',
            description: item.description || '暂无描述',
            category: this.normalizeCategory(item.category),
            location: item.location || '校园未知地点',
            contactInfo: item.contactInfo || '',
            imageUrl: item.imageUrl || '',
            rawCreateTime: item.createTime,
            createTime: formattedCreateTime,
            typeLabel,
            statusLabel,
            typeClass: item.type === 1 ? 'tag-found' : 'tag-lost',
            statusClass: this.statusClassOf(item.status)
          }
        })

        this.items = this.items.concat(formattedRecords)
      } catch (err) {
        console.error('加载更多出错：', err)
        this.pageNumber -= 1 // 恢复页码
      } finally {
        this.loadingMore = false
      }
    },
    onPublishClick(event) {
      // 添加点击反馈动画
      const button = event.target.closest('.publish-button')
      if (button) {
        button.style.transform = 'scale(0.95)'
        setTimeout(() => {
          button.style.transform = ''
        }, 150)
      }

      // 切换到发布页面，未登录则弹登录
      if (!this.currentUser.id) {
        this.promptLogin('publish')
        return
      }
      this.publishSourcePage = this.currentPage
      this.editingItem = null
      this.currentPage = 'publish'
    },
    showFlowMessage(message) {
      if (!message) {
        return
      }
      this.flowMessage = message
      if (this.flowMessageTimer) {
        clearTimeout(this.flowMessageTimer)
      }
      this.flowMessageTimer = setTimeout(() => {
        this.flowMessage = ''
        this.flowMessageTimer = null
      }, 2200)
    },
    goBack(payload = null) {
      const isEditMode = payload && payload.isEditMode === true
      const targetPage = payload && payload.targetPage ? payload.targetPage : (this.publishSourcePage || 'home')
      this.currentPage = targetPage
      this.editingItem = null
      if (this.currentPage === 'profile') {
        this.fetchMyItems()
      }
      if (payload && payload.message) {
        this.showFlowMessage(payload.message)
      }
      // 刷新数据以显示新发布的物品
      this.fetchData()
      if (!isEditMode && this.currentPage !== 'profile' && this.currentUser.id) {
        this.fetchMyItems()
      }
    },
    goDetail(item, fromPage = 'home') {
      this.previousPage = fromPage
      this.selectedItem = item
      this.currentPage = 'detail'
    },
    goBackFromDetail() {
      this.selectedItem = null
      this.currentPage = this.previousPage || 'home'
    },
    goHome() {
      this.currentPage = 'home'
    },
    goProfile() {
      if (!this.currentUser.id) {
        this.promptLogin('profile')
        return
      }
      this.currentPage = 'profile'
      this.fetchMyItems()
    },
    toggleLoginMode() {
      this.loginMode = this.loginMode === 'login' ? 'register' : 'login'
      this.loginError = ''
      this.loginForm = { username: '', password: '', contactInfo: '' }
    },
    async handleLoginSubmit() {
      if (this.loginLoading) return
      this.loginError = ''
      this.loginLoading = true
      try {
        const { userApi, setToken } = await import('./api/index.js')
        let result
        if (this.loginMode === 'login') {
          result = await userApi.login({ username: this.loginForm.username, password: this.loginForm.password })
        } else {
          result = await userApi.register({
            username: this.loginForm.username,
            password: this.loginForm.password,
            contactInfo: this.loginForm.contactInfo
          })
        }
        
        // 处理登录返回的结果：{ code, msg, data: { user, token } }
        if (!result || !result.data) {
          throw new Error('返回数据异常')
        }
        
        const { user, token } = result.data
        if (!user || !user.id) {
          throw new Error('用户信息缺失')
        }
        
        // 保存 token 到 API 模块（用于后续请求自动添加到 header）
        if (token) {
          setToken(token)
        }
        
        // 保存用户信息到 storage（包含 token）
        storage.saveUser({ ...user, token })
        
        this.currentUser = {
          id: user.id,
          name: user.username || '校园用户',
          role: user.role || 0,
          contactInfo: user.contactInfo || ''
        }
        this.showLoginModal = false
        const targetPage = this.pendingPageAfterLogin || 'profile'
        this.pendingPageAfterLogin = null
        if (targetPage === 'publish') {
          this.publishSourcePage = this.currentPage
          this.editingItem = null
          this.currentPage = 'publish'
          return
        }
        this.currentPage = 'profile'
        this.fetchMyItems()
      } catch (err) {
        this.loginError = this.loginMode === 'login' ? '用户名或密码错误' : '注册失败，用户名可能已存在'
        console.error('登录错误：', err)
      } finally {
        this.loginLoading = false
      }
    },
    async handleLogout() {
      try {
        const { clearToken } = await import('./api/index.js')
        if (this.currentUser && this.currentUser.id) {
          await userApi.logout()
        }
        clearToken()
      } catch (err) {
        // 登出接口失败不阻断本地退出
      }
      storage.clearUser()
      this.pendingPageAfterLogin = null
      if (this.flowMessageTimer) {
        clearTimeout(this.flowMessageTimer)
        this.flowMessageTimer = null
      }
      this.flowMessage = ''
      this.currentUser = { id: null, name: '校园用户', role: 0, contactInfo: '' }
      this.currentPage = 'home'
    },
    goAdmin() {
      this.currentPage = 'admin'
      this.fetchData()
    },
    editItem(item) {
      if (!item || !item.id) {
        return
      }
      this.publishSourcePage = 'profile'
      this.editingItem = { ...item }
      this.currentPage = 'publish'
    },
    async markItemDone(item) {
      if (!item || !item.id) {
        return
      }
      if (!this.currentUser.id) {
        alert('请先登录')
        return
      }
      if (!confirm('确认将该信息标记为已完结吗？')) {
        return
      }
      try {
        await itemApi.updateStatus(item.id, 1)
        await this.fetchMyItems()
        await this.fetchData()
        alert('已标记为完结')
      } catch (err) {
        console.error('状态更新失败：', err)
        alert('操作失败，请稍后重试')
      }
    },
    async deleteItem(item) {
      if (!item || !item.id) {
        return
      }
      if (!this.currentUser.id) {
        alert('请先登录')
        return
      }
      if (!confirm('确认删除该条发布信息吗？删除后不可恢复。')) {
        return
      }
      try {
        await itemApi.remove(item.id)
        await this.fetchMyItems()
        await this.fetchData()
        alert('删除成功')
      } catch (err) {
        console.error('删除失败：', err)
        alert('删除失败，请稍后重试')
      }
    },
    async fetchMyItems() {
      if (!this.currentUser.id) {
        this.myItems = []
        return
      }
      try {
        const body = await itemApi.myItems()
        const records = Array.isArray(body?.data) ? body.data : []
        this.myItems = records.map(item => {
          const formattedCreateTime = this.formatTime(item.createTime)
          return {
            ...item,
            title: item.title || '未命名物品',
            description: item.description || '暂无描述',
            category: this.normalizeCategory(item.category),
            location: item.location || '校园未知地点',
            contactInfo: item.contactInfo || '',
            imageUrl: item.imageUrl || '',
            rawCreateTime: item.createTime,
            createTime: formattedCreateTime,
            typeLabel: this.formatType(item.type),
            statusLabel: this.formatStatus(item.status),
            typeClass: item.type === 1 ? 'tag-found' : 'tag-lost',
            statusClass: this.statusClassOf(item.status)
          }
        })
        if (!this.currentUser.contactInfo && this.myItems.length > 0) {
          this.currentUser = {
            ...this.currentUser,
            contactInfo: this.myItems.find(item => item.contactInfo)?.contactInfo || ''
          }
        }
      } catch (err) {
        console.error('加载我的发布失败：', err)
        this.myItems = []
      }
    },
    handleScroll() {
      // 只在首页时处理无限滚动
      if (this.currentPage !== 'home') return
      
      const scrollTop = window.scrollY || document.documentElement.scrollTop
      const windowHeight = window.innerHeight
      const documentHeight = document.documentElement.scrollHeight
      
      // 当用户滚动到距离底部 200px 时触发加载更多
      if (scrollTop + windowHeight >= documentHeight - 200) {
        this.loadMore()
      }
    }
  }
}
</script>

<style>
#app {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  background: #f5f7fb;
  min-height: 100vh;
  color: #1f2937;
  padding: 16px;
}

.flow-toast {
  position: fixed;
  top: 20px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 1200;
  background: rgba(15, 23, 42, 0.92);
  color: #fff;
  padding: 10px 16px;
  border-radius: 999px;
  font-size: 13px;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.18);
}

.page-header {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 18px;
}

.search-panel {
  display: flex;
  gap: 10px;
}

.search-panel input {
  flex: 1;
  height: 44px;
  border: 1px solid #dbe3ee;
  border-radius: 22px;
  padding: 0 16px;
  font-size: 14px;
  outline: none;
}

.search-button {
  min-width: 84px;
  border: none;
  border-radius: 22px;
  background-color: #1677ff;
  color: #fff;
  font-weight: 600;
  cursor: pointer;
}

.announcement {
  display: flex;
  align-items: center;
  gap: 8px;
  background: #e6f7ff;
  border: 1px solid #91d5ff;
  border-radius: 14px;
  padding: 12px 16px;
  color: #0958d9;
  font-size: 13px;
}

.announce-label {
  font-weight: 700;
}

.tab-panel {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
}

.tab-button {
  flex: 1;
  border: 1px solid #dbe3ee;
  background: #fff;
  color: #344054;
  border-radius: 20px;
  padding: 10px 0;
  font-size: 14px;
  cursor: pointer;
}

.tab-button.active {
  border-color: #1677ff;
  background: #1677ff;
  color: #fff;
}

.filter-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 12px;
}

.filter-group {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.filter-title {
  font-size: 14px;
  color: #344054;
  min-width: 58px;
}

.filter-pill {
  padding: 8px 14px;
  border: 1px solid #dbe3ee;
  border-radius: 18px;
  background: #fff;
  color: #475569;
  font-size: 13px;
  cursor: pointer;
}

.filter-pill.selected {
  border-color: #1677ff;
  background: #e6f7ff;
  color: #0f61ff;
}

.meta-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  color: #667085;
  font-size: 13px;
  margin-bottom: 8px;
}

.list-container {
  display: grid;
  gap: 12px;
}

.item-card {
  background: #fff;
  border-radius: 18px;
  padding: 16px;
  box-shadow: 0 10px 20px rgba(15, 23, 42, 0.05);
}

.item-card-top {
  display: flex;
  gap: 12px;
}

.item-thumb {
  width: 100px;
  min-width: 100px;
  height: 100px;
  border-radius: 16px;
  background: linear-gradient(180deg, #eaf4ff 0%, #f8fcff 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #4b5563;
  font-size: 13px;
  overflow: hidden;
}

.thumb-placeholder {
  font-size: 14px;
}

.item-card-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.item-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.item-title {
  margin: 0;
  font-size: 17px;
  color: #0f172a;
  font-weight: 700;
}

.status-label {
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  color: #fff;
}

.status-approved {
  background: #1677ff;
}

.status-completed {
  background: #10b981;
}

.status-review {
  background: #f59e0b;
}

.status-rejected {
  background: #ef4444;
}

.item-description {
  margin: 0 0 12px;
  color: #475569;
  line-height: 1.6;
}

.item-meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  font-size: 13px;
  color: #64748b;
}

.item-card-footer {
  margin-top: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.type-tag {
  padding: 6px 12px;
  border-radius: 999px;
  font-size: 12px;
  color: #fff;
}

.tag-lost {
  background: #1677ff;
}

.tag-found {
  background: #13c2c2;
}

.contact-info {
  color: #475569;
  font-size: 13px;
}

.publish-button {
  position: fixed;
  right: 24px;
  bottom: 88px;
  width: 64px;
  height: 64px;
  border-radius: 50%;
  border: none;
  background: linear-gradient(135deg, #1677ff 0%, #13c2c2 100%);
  color: #fff;
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 0.5px;
  line-height: 1.2;
  padding: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  box-shadow: 0 8px 32px rgba(22, 119, 255, 0.3), 0 4px 16px rgba(19, 194, 194, 0.2);
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  z-index: 1000;
  overflow: hidden;
}

.publish-button:hover {
  transform: translateY(-4px) scale(1.05);
  box-shadow: 0 12px 40px rgba(22, 119, 255, 0.4), 0 6px 20px rgba(19, 194, 194, 0.3);
}

.publish-button:active {
  transform: translateY(-2px) scale(0.98);
  box-shadow: 0 6px 24px rgba(22, 119, 255, 0.35), 0 3px 12px rgba(19, 194, 194, 0.25);
}

.publish-btn-icon {
  font-size: 22px;
  font-weight: 400;
  line-height: 1;
}

.publish-btn-text {
  font-size: 12px;
  font-weight: 600;
  line-height: 1;
}

.bottom-nav {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  background: #ffffff;
  border-top: 1px solid #e2e8f0;
  display: flex;
  justify-content: space-around;
  padding: 12px 0;
  box-shadow: 0 -4px 20px rgba(0, 0, 0, 0.08);
  backdrop-filter: blur(10px);
  z-index: 999;
}

.nav-item {
  background: transparent;
  border: none;
  color: #475569;
  font-size: 13px;
  cursor: pointer;
  padding: 8px 16px;
  border-radius: 20px;
  transition: all 0.2s ease;
  font-weight: 500;
}

.nav-item:hover {
  background: #f1f5f9;
  color: #1677ff;
}

.nav-item.active {
  color: #1677ff;
  font-weight: 700;
  background: #e6f7ff;
}

.nav-admin {
  color: #f5222d;
  background: linear-gradient(135deg, rgba(245, 34, 45, 0.1) 0%, transparent 100%);
}

.nav-admin.active {
  color: #f5222d;
  background: #fff2f0;
  font-weight: 700;
}

.empty-card {
  padding: 30px;
  text-align: center;
  border: 1px dashed #cbd5e1;
  border-radius: 18px;
  color: #64748b;
  background: #f8fafc;
}

/* ===== 登录/注册模态框 ===== */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}
.modal-box {
  background: #fff;
  border-radius: 20px;
  padding: 32px 28px 24px;
  width: 90%;
  max-width: 380px;
  position: relative;
  box-shadow: 0 8px 32px rgba(0,0,0,0.18);
}
.modal-close {
  position: absolute;
  top: 14px;
  right: 18px;
  background: none;
  border: none;
  font-size: 22px;
  color: #94a3b8;
  cursor: pointer;
  line-height: 1;
}
.modal-title {
  font-size: 20px;
  font-weight: 700;
  color: #1f2937;
  margin: 0 0 20px;
}
.modal-field {
  margin-bottom: 14px;
}
.modal-field label {
  display: block;
  font-size: 13px;
  color: #475569;
  margin-bottom: 5px;
}
.modal-field input {
  width: 100%;
  box-sizing: border-box;
  padding: 10px 14px;
  border: 1.5px solid #e2e8f0;
  border-radius: 10px;
  font-size: 15px;
  outline: none;
  transition: border-color 0.2s;
}
.modal-field input:focus {
  border-color: #1677ff;
}
.modal-error {
  color: #f5222d;
  font-size: 13px;
  margin-bottom: 10px;
  padding: 6px 10px;
  background: #fff2f0;
  border-radius: 8px;
}
.modal-submit {
  width: 100%;
  background: #1677ff;
  color: #fff;
  border: none;
  border-radius: 12px;
  padding: 12px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  margin-top: 6px;
  transition: background 0.2s;
}
.modal-submit:disabled {
  background: #93c5fd;
}
.modal-switch {
  text-align: center;
  font-size: 13px;
  color: #64748b;
  margin: 16px 0 0;
}
.link-btn {
  background: none;
  border: none;
  color: #1677ff;
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
  padding: 0;
}

/* 加载更多部分 */
.load-more-section {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 24px 16px;
  margin-bottom: 80px;
}

.loading-indicator {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #64748b;
  font-size: 14px;
}

.spinner-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #1677ff;
  animation: pulse 1.5s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}

.load-more-text {
  color: #94a3b8;
  font-size: 13px;
  padding: 12px;
  background: #f8fafc;
  border-radius: 12px;
}

.no-more-text {
  color: #cbd5e1;
  font-size: 13px;
  padding: 12px;
  background: #f1f5f9;
  border-radius: 12px;
}
</style>