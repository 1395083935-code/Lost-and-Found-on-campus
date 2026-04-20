<template>
  <div class="profile-page">
    <header class="profile-header">
      <div class="profile-card">
        <div class="avatar">{{ userInitial }}</div>
        <div class="profile-meta">
          <h2 class="user-name">{{ user.name }}</h2>
          <p class="user-contact">联系方式：{{ displayContact }}</p>
          <p class="user-note">我的发布 {{ myItems.length }} 条</p>
        </div>
        <button type="button" class="logout-btn" @click="$emit('logout')">退出登录</button>
      </div>
    </header>

    <section class="status-tabs">
      <!-- 一级 Tab: 我的发布 / 我的收藏 / 消息通知 -->
      <div class="main-tabs">
        <button :class="['main-tab', mainTab === 'publish' ? 'active' : '']" @click="mainTab = 'publish'">我的发布</button>
        <button :class="['main-tab', mainTab === 'favorite' ? 'active' : '']" @click="switchToFavorite">我的收藏</button>
        <button :class="['main-tab', mainTab === 'notice' ? 'active' : '']" @click="switchToNotice">消息通知<span v-if="unreadCount > 0" class="badge">{{ unreadCount }}</span></button>
      </div>
      <!-- 二级 Tab(发布状态): 仅在发布页显示 -->
      <div v-if="mainTab === 'publish'" class="sub-tabs">
        <button
          v-for="tab in statusTabs"
          :key="tab.value"
          :class="['status-tab', activeStatus === tab.value ? 'active' : '']"
          @click="activeStatus = tab.value"
        >
          {{ tab.label }}
        </button>
      </div>
    </section>

    <!-- 我的发布列表 -->
    <section v-if="mainTab === 'publish'" class="my-list">
      <div v-if="filteredItems.length === 0" class="empty-card">
        暂无对应状态的发布记录。
      </div>

      <article
        v-for="item in filteredItems"
        :key="item.id"
        class="my-item-card"
        @click="$emit('open-detail', item)"
      >
        <div class="title-row">
          <h3 class="item-title">{{ item.title || '未命名物品' }}</h3>
          <span class="status-label" :class="item.statusClass">{{ item.statusLabel }}</span>
        </div>
        <p class="item-desc">{{ item.description || '暂无描述' }}</p>
        <p v-if="item.status === 3 && item.rejectReason" class="reject-reason-text">
          驳回原因：{{ item.rejectReason }}
        </p>
        <div class="meta-row">
          <span>📍 {{ item.location || '校园未知地点' }}</span>
          <span>⏰ {{ item.createTime }}</span>
        </div>
        <div class="footer-row">
          <span class="type-tag" :class="item.typeClass">{{ item.typeLabel }}</span>
          <div class="actions-row" @click.stop>
            <button
              v-if="item.status !== 1"
              type="button"
              class="mini-btn done"
              @click="$emit('mark-done', item)"
            >
              标记完结
            </button>
            <button
              v-if="item.status === 2 || item.status === 3"
              type="button"
              class="mini-btn edit"
              @click="$emit('edit-item', item)"
            >
              编辑重提
            </button>
            <button
              type="button"
              class="mini-btn danger"
              @click="$emit('delete-item', item)"
            >
              删除
            </button>
          </div>
        </div>
        <div class="contact-row">联系方式：{{ maskContact(item.contactInfo) }}</div>
      </article>
    </section>

    <!-- 我的收藏列表 -->
    <section v-if="mainTab === 'favorite'" class="my-list">
      <div v-if="favLoading" class="empty-card">加载中...</div>
      <div v-else-if="favorites.length === 0" class="empty-card">暂无收藏的信息</div>
      <article
        v-for="fav in favorites"
        :key="fav.id"
        class="my-item-card"
        @click="$emit('open-detail', fav)"
      >
        <div class="title-row">
          <h3 class="item-title">{{ fav.title || '未命名物品' }}</h3>
          <span class="type-tag" :class="fav.type === 1 ? 'tag-found' : 'tag-lost'">{{ fav.type === 1 ? '拾物招领' : '失物寻主' }}</span>
        </div>
        <p class="item-desc">{{ fav.description || '暂无描述' }}</p>
        <div class="meta-row">
          <span>📍 {{ fav.location || '未知地点' }}</span>
        </div>
        <div class="footer-row">
          <div class="actions-row" @click.stop>
            <button type="button" class="mini-btn danger" @click="removeFavorite(fav)">取消收藏</button>
          </div>
        </div>
      </article>
    </section>

    <!-- 消息通知列表 -->
    <section v-if="mainTab === 'notice'" class="my-list">
      <div v-if="noticeLoading" class="empty-card">加载中...</div>
      <div v-else-if="notices.length === 0" class="empty-card">暂无消息通知</div>
      <div v-else class="notice-header">
        <button type="button" class="notice-btn" @click="markAllAsRead">全部标记已读</button>
        <button type="button" class="notice-btn danger" @click="deleteAllNotices">全部删除</button>
      </div>
      <article
        v-for="notice in notices"
        :key="notice.id"
        class="notice-card"
        :class="notice.isRead === 0 ? 'unread' : 'read'"
      >
        <div class="notice-header-row">
          <span class="notice-type" :class="'type-' + notice.type">{{ noticeTypeLabel(notice.type) }}</span>
          <span class="notice-time">{{ formatTime(notice.createTime) }}</span>
          <div class="notice-actions" @click.stop>
            <button v-if="notice.isRead === 0" type="button" class="icon-btn" @click="markAsRead(notice)">✓</button>
            <button type="button" class="icon-btn danger" @click="deleteNotice(notice)">×</button>
          </div>
        </div>
        <p class="notice-content">{{ notice.content }}</p>
      </article>
    </section>

    <LoadingSpinner v-if="(favLoading && mainTab === 'favorite') || (noticeLoading && mainTab === 'notice')" message="加载中..." />
  </div>
</template>

<script>
import { favoriteApi, noticeApi } from './api/index.js'
import LoadingSpinner from './components/Spinner.vue'

export default {
  name: 'ProfilePage',
  components: {
    LoadingSpinner
  },
  emits: ['open-detail', 'mark-done', 'delete-item', 'edit-item', 'logout'],
  props: {
    items: {
      type: Array,
      default: () => []
    },
    user: {
      type: Object,
      required: true
    }
  },
  data() {
    return {
      mainTab: 'publish',  // 'publish' | 'favorite' | 'notice'
      activeStatus: 'all',
      favorites: [],
      favLoading: false,
      notices: [],
      noticeLoading: false,
      unreadCount: 0,
      statusTabs: [
        { label: '全部', value: 'all' },
        { label: '待审核', value: 0 },
        { label: '已通过', value: 1 },
        { label: '已驳回', value: 2 },
        { label: '已完结', value: 3 }
      ]
    }
  },
  mounted() {
    // 如果已登录预加载收藏列表和消息
    if (this.user && this.user.id) {
      this.loadFavorites()
      this.loadNotices()
      this.loadUnreadCount()
      // 每30秒自动刷新未读数
      this.noticeRefreshInterval = setInterval(() => {
        this.loadUnreadCount()
      }, 30000)
    }
  },
  beforeUnmount() {
    if (this.noticeRefreshInterval) {
      clearInterval(this.noticeRefreshInterval)
    }
  },
  computed: {
    userInitial() {
      return (this.user.name || '用户').slice(0, 1)
    },
    displayContact() {
      return this.user.contactInfo || this.myItems.find(item => item.contactInfo)?.contactInfo || '未填写'
    },
    myItems() {
      return this.items
    },
    filteredItems() {
      if (this.activeStatus === 'all') {
        return this.myItems
      }
      return this.myItems.filter(item => item.status === this.activeStatus)
    }
  },
  methods: {
    maskContact(contact) {
      if (!contact) return '未填写'
      if (contact.length < 7) return contact
      return `${contact.slice(0, 3)}****${contact.slice(-4)}`
    },
    async switchToFavorite() {
      this.mainTab = 'favorite'
      await this.loadFavorites()
    },
    async loadFavorites() {
      if (!this.user || !this.user.id) return
      this.favLoading = true
      try {
        const res = await favoriteApi.list()
        const records = res?.data?.records || res?.data || []
        this.favorites = records.map(item => ({
          ...item,
          title: item.title || '未命名物品',
          description: item.description || '暂无描述',
          location: item.location || '未知地点',
          contactInfo: item.contactInfo || '',
          typeLabel: item.type === 1 ? '拾物招领' : '失物寻主',
          typeClass: item.type === 1 ? 'tag-found' : 'tag-lost',
          statusLabel: ({ 0: '待审核', 1: '已通过', 2: '已驳回', 3: '已完结' })[item.status] || '待审核',
          statusClass: ({ 0: 'status-review', 1: 'status-approved', 2: 'status-rejected', 3: 'status-completed' })[item.status] || 'status-review'
        }))
      } catch (e) {
        console.error('加载收藏失败', e)
      } finally {
        this.favLoading = false
      }
    },
    async removeFavorite(item) {
      if (!confirm(`确认取消收藏「${item.title}」？`)) return
      try {
        await favoriteApi.remove(item.id)
        this.favorites = this.favorites.filter(f => f.id !== item.id)
      } catch (e) {
        alert('取消收藏失败')
      }
    },
    async switchToNotice() {
      this.mainTab = 'notice'
      await this.loadNotices()
    },
    async loadNotices() {
      if (!this.user || !this.user.id) return
      this.noticeLoading = true
      try {
        const res = await noticeApi.list()
        this.notices = (res?.data || []).map(notice => ({
          ...notice,
          typeLabel: this.noticeTypeLabel(notice.type)
        }))
      } catch (e) {
        console.error('加载消息失败', e)
      } finally {
        this.noticeLoading = false
      }
    },
    async loadUnreadCount() {
      if (!this.user || !this.user.id) return
      try {
        const res = await noticeApi.getUnreadCount()
        this.unreadCount = res?.data?.unreadCount || 0
      } catch (e) {
        console.error('加载未读数失败', e)
      }
    },
    async markAsRead(notice) {
      if (!notice || notice.isRead === 1) return
      try {
        await noticeApi.markAsRead(notice.id)
        notice.isRead = 1
        this.unreadCount = Math.max(0, this.unreadCount - 1)
      } catch (e) {
        console.error('标记已读失败', e)
      }
    },
    async markAllAsRead() {
      if (this.notices.length === 0) return
      try {
        await noticeApi.markAllAsRead()
        this.notices.forEach(notice => notice.isRead = 1)
        this.unreadCount = 0
      } catch (e) {
        console.error('标记全部已读失败', e)
      }
    },
    async deleteNotice(notice) {
      if (!confirm('确认删除此消息？')) return
      try {
        await noticeApi.delete(notice.id)
        this.notices = this.notices.filter(n => n.id !== notice.id)
        if (notice.isRead === 0) {
          this.unreadCount = Math.max(0, this.unreadCount - 1)
        }
      } catch (e) {
        console.error('删除消息失败', e)
      }
    },
    async deleteAllNotices() {
      if (this.notices.length === 0) return
      if (!confirm('确认删除全部消息？')) return
      try {
        await noticeApi.deleteAll()
        this.notices = []
        this.unreadCount = 0
      } catch (e) {
        console.error('删除全部消息失败', e)
      }
    },
    noticeTypeLabel(type) {
      const labels = {
        1: '审核通过',
        2: '审核驳回',
        3: '匹配提醒',
        4: '举报处理'
      }
      return labels[type] || '系统消息'
    },
    formatTime(dateStr) {
      if (!dateStr) return ''
      const date = new Date(dateStr)
      const now = new Date()
      const diff = now - date
      const hours = Math.floor(diff / 3600000)
      const minutes = Math.floor(diff / 60000)
      const days = Math.floor(diff / 86400000)
      
      if (minutes < 1) return '刚刚'
      if (hours < 1) return `${minutes}分钟前`
      if (days < 1) return `${hours}小时前`
      if (days < 30) return `${days}天前`
      return date.toLocaleDateString('zh-CN')
    }
  }
}
</script>

<style scoped>
.profile-page {
  min-height: 100vh;
  background: #f5f7fb;
  padding: 16px;
  padding-bottom: 90px;
}

.profile-card {
  background: linear-gradient(135deg, #1677ff 0%, #13c2c2 100%);
  border-radius: 20px;
  padding: 18px;
  color: #fff;
  display: flex;
  gap: 14px;
  align-items: center;
  box-shadow: 0 10px 24px rgba(22, 119, 255, 0.25);
}

.avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.24);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: 700;
}

.user-name {
  margin: 0 0 4px;
  font-size: 18px;
}

.user-contact,
.user-note {
  margin: 0;
  font-size: 13px;
  opacity: 0.95;
}

.status-tabs {
  margin-top: 14px;
  display: flex;
  gap: 10px;
}

.status-tab {
  flex: 1;
  border: 1px solid #dbe3ee;
  border-radius: 18px;
  background: #fff;
  color: #475569;
  padding: 10px 0;
  font-size: 13px;
}

.status-tab.active {
  border-color: #1677ff;
  background: #e6f7ff;
  color: #1677ff;
  font-weight: 700;
}

.my-list {
  margin-top: 14px;
  display: grid;
  gap: 12px;
}

.my-item-card {
  background: #fff;
  border-radius: 16px;
  padding: 14px;
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.05);
  cursor: pointer;
}

.title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.item-title {
  margin: 0;
  font-size: 16px;
  color: #0f172a;
}

.item-desc {
  margin: 10px 0;
  color: #64748b;
  font-size: 14px;
}

.reject-reason-text {
  margin: -2px 0 10px;
  color: #b45309;
  font-size: 12px;
}

.meta-row {
  display: flex;
  gap: 12px;
  color: #64748b;
  font-size: 13px;
}

.footer-row {
  margin-top: 10px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.actions-row {
  display: flex;
  gap: 8px;
}

.mini-btn {
  border: none;
  border-radius: 14px;
  padding: 5px 10px;
  font-size: 12px;
  cursor: pointer;
}

.mini-btn.done {
  background: #e6f7ff;
  color: #1677ff;
}

.mini-btn.edit {
  background: #fff7e6;
  color: #d97706;
}

.mini-btn.danger {
  background: #fef2f2;
  color: #dc2626;
}

.contact-row {
  margin-top: 8px;
  font-size: 12px;
  color: #64748b;
}

.status-label {
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  color: #fff;
}

.type-tag {
  padding: 6px 10px;
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

.status-pending {
  background: #f59e0b;
}

.status-processed {
  background: #10b981;
}

.empty-card {
  padding: 30px;
  text-align: center;
  border: 1px dashed #cbd5e1;
  border-radius: 16px;
  color: #64748b;
  background: #f8fafc;
}

.logout-btn {
  margin-left: auto;
  background: rgba(255,255,255,0.2);
  border: 1.5px solid rgba(255,255,255,0.5);
  color: #fff;
  border-radius: 10px;
  padding: 7px 14px;
  font-size: 13px;
  cursor: pointer;
  white-space: nowrap;
  transition: background 0.2s;
}
.logout-btn:hover {
  background: rgba(255,255,255,0.35);
}

.main-tabs {
  display: flex;
  gap: 6px;
  margin-bottom: 8px;
}

.main-tab {
  flex: 1;
  padding: 10px 0;
  border: none;
  border-radius: 12px;
  background: #f1f5f9;
  color: #64748b;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.main-tab.active {
  background: #1677ff;
  color: #fff;
  font-weight: 700;
}

.sub-tabs {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  padding: 4px 0;
}

/* 消息相关样式 */
.badge {
  display: inline-block;
  background: #ef4444;
  color: #fff;
  border-radius: 999px;
  font-size: 11px;
  padding: 2px 6px;
  margin-left: 4px;
  font-weight: 700;
  min-width: 20px;
  text-align: center;
}

.notice-header {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
}

.notice-btn {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  background: #fff;
  color: #1677ff;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.notice-btn:hover {
  background: #e6f7ff;
}

.notice-btn.danger {
  color: #dc2626;
}

.notice-btn.danger:hover {
  background: #fef2f2;
}

.notice-card {
  background: #fff;
  border-radius: 12px;
  padding: 12px;
  margin-bottom: 10px;
  border-left: 4px solid #1677ff;
  transition: all 0.2s;
}

.notice-card.unread {
  background: #f0f9ff;
  border-left-color: #ef4444;
}

.notice-card.read {
  opacity: 0.7;
}

.notice-header-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.notice-type {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 600;
  color: #fff;
}

.notice-type.type-1 {
  background: #10b981;
}

.notice-type.type-2 {
  background: #ef4444;
}

.notice-type.type-3 {
  background: #f59e0b;
}

.notice-type.type-4 {
  background: #8b5cf6;
}

.notice-time {
  margin-left: auto;
  font-size: 12px;
  color: #94a3b8;
}

.notice-actions {
  display: flex;
  gap: 6px;
}

.icon-btn {
  padding: 4px 8px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: #64748b;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.icon-btn:hover {
  background: #e2e8f0;
  color: #1e293b;
}

.icon-btn.danger:hover {
  background: #fee2e2;
  color: #dc2626;
}

.notice-content {
  margin: 0;
  font-size: 14px;
  color: #1f2937;
  line-height: 1.5;
}

</style>
