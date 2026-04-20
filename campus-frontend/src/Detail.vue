<template>
  <div class="detail-page">
    <header class="detail-header">
      <button class="back-button" @click="$emit('back')">
        <span>←</span>
        返回
      </button>
      <h1 class="header-title">{{ item.type === 1 ? '拾物详情' : '失物详情' }}</h1>
    </header>

    <!-- 图片区域 -->
    <div class="image-area">
      <button
        v-if="item.imageUrl"
        class="image-preview-trigger"
        type="button"
        @click="openLightbox"
        aria-label="查看大图"
      >
        <SmartImage :src="item.imageUrl" alt="物品图片" variant="detail" />
      </button>
      <div v-else class="image-placeholder">
        <span class="placeholder-icon">📦</span>
        <span class="placeholder-text">暂无图片</span>
      </div>
    </div>

    <div v-if="lightboxVisible" class="lightbox" @click.self="closeLightbox">
      <button type="button" class="lightbox-close" @click="closeLightbox">×</button>
      <img class="lightbox-image" :src="item.imageUrl" alt="预览大图" />
    </div>

    <!-- 主体信息 -->
    <div class="detail-card">
      <!-- 标题行 -->
      <div class="title-row">
        <h2 class="detail-title">{{ item.title }}</h2>
        <span class="status-badge" :class="resolvedStatusClass">{{ resolvedStatusLabel }}</span>
      </div>

      <!-- 类型标签 -->
      <div class="type-row">
        <span class="type-tag" :class="item.typeClass || (item.type === 1 ? 'tag-found' : 'tag-lost')">{{ resolvedTypeLabel }}</span>
        <span class="category-tag">{{ categoryLabel }}</span>
      </div>

      <!-- 基本信息 -->
      <div class="info-list">
        <div class="info-item">
          <span class="info-icon">📍</span>
          <span class="info-label">地点</span>
          <span class="info-value">{{ item.location || '未填写' }}</span>
        </div>
        <div class="info-item">
          <span class="info-icon">⏰</span>
          <span class="info-label">时间</span>
          <span class="info-value">{{ formatDateTime(item.createTime) }}</span>
        </div>
        <div v-if="item.storageLocation" class="info-item">
          <span class="info-icon">🏠</span>
          <span class="info-label">存放地点</span>
          <span class="info-value">{{ item.storageLocation }}</span>
        </div>
      </div>

      <!-- 描述 -->
      <div class="desc-section">
        <div class="desc-label">📝 物品描述</div>
        <p class="desc-text">{{ item.description || '暂无描述' }}</p>
      </div>

      <!-- 联系方式 -->
      <div class="contact-section">
        <div class="contact-label">联系方式</div>
        <div class="contact-row">
          <span class="contact-number">{{ maskedPhone }}</span>
          <button
            class="reveal-button"
            v-if="!phoneRevealed"
            @click="revealPhone"
          >
            查看完整号码
          </button>
          <span v-else class="contact-full">{{ item.contactInfo }}</span>
        </div>
        <button class="call-button" @click="callPhone">
          <span>📞</span>
          一键联系
        </button>
      </div>
    </div>

    <!-- 底部操作栏 -->
    <div class="action-bar">
      <button class="action-btn" @click="handleCollect">
        <span>{{ collected ? '❤️' : '🤍' }}</span>
        <span class="action-label">{{ collected ? '已收藏' : '收藏' }}</span>
      </button>
      <button class="action-btn" @click="handleShare">
        <span>📤</span>
        <span class="action-label">分享</span>
      </button>
      <button class="action-btn danger" @click="handleReport">
        <span>🚩</span>
        <span class="action-label">举报</span>
      </button>
    </div>

    <!-- 分享提示 Toast -->
    <div v-if="shareMessage" class="share-toast">{{ shareMessage }}</div>
  </div>
</template>

<script>
import SmartImage from './components/SmartImage.vue'
import { favoriteApi } from './api/index.js'

export default {
  name: 'DetailPage',
  components: {
    SmartImage
  },
  props: {
    item: {
      type: Object,
      required: true
    },
    currentUser: {
      type: Object,
      default: null
    }
  },
  data() {
    return {
      phoneRevealed: false,
      collected: false,
      lightboxVisible: false,
      shareMessage: ''
    }
  },
  mounted() {
    window.addEventListener('keydown', this.onKeydown)
    this.syncFavoriteStatus()
  },
  beforeUnmount() {
    window.removeEventListener('keydown', this.onKeydown)
  },
  watch: {
    item: {
      handler() {
        this.phoneRevealed = false
        this.syncFavoriteStatus()
      },
      deep: true
    },
    currentUser: {
      handler() {
        this.syncFavoriteStatus()
      },
      deep: true
    }
  },
  computed: {
    resolvedTypeLabel() {
      if (this.item.typeLabel) return this.item.typeLabel
      return this.item.type === 1 ? '拾物招领' : '失物寻主'
    },
    resolvedStatusLabel() {
      if (this.item.statusLabel) return this.item.statusLabel
      const statusMap = {
        0: '审核中',
        1: this.item.type === 1 ? '待认领' : '待认领',
        2: '已驳回',
        3: this.item.type === 1 ? '已归还' : '已找回'
      }
      return statusMap[this.item.status] || '审核中'
    },
    resolvedStatusClass() {
      if (this.item.statusClass) return this.item.statusClass
      const classMap = {
        0: 'status-review',      // 审核中
        1: 'status-approved',    // 待认领
        2: 'status-rejected',    // 已驳回
        3: 'status-completed'    // 已完结
      }
      return classMap[this.item.status] || 'status-review'
    },
    maskedPhone() {
      const phone = this.item.contactInfo || ''
      if (!phone || phone.length < 7) return phone || '未填写'
      // 显示格式：1234****6789
      return phone.slice(0, 3) + '****' + phone.slice(-4)
    },
    categoryLabel() {
      const map = {
        certificate: '证件',
        electronic: '电子产品',
        stationery: '文具饰品',
        clothing: '衣物箱包',
        keycard: '钥匙卡包',
        other: '其他',
        // 也支持中文直接映射
        '证件': '证件',
        '电子产品': '电子产品',
        '文具饰品': '文具饰品',
        '衣物箱包': '衣物箱包',
        '钥匙卡包': '钥匙卡包',
        '其他': '其他'
      }
      return map[this.item.category] || this.item.category || '其他'
    },
    isAnonymous() {
      return !!this.item.anonymous
    },
    displayUserInfo() {
      // 如果是匿名发布，返回匿名用户信息
      if (this.isAnonymous) {
        return {
          nickname: '匿名用户',
          avatar: null,
          isAnonymous: true
        }
      }
      return {
        nickname: this.item.userNickname || '用户',
        avatar: this.item.userAvatar || null,
        isAnonymous: false
      }
    }
  },
  methods: {
    async syncFavoriteStatus() {
      if (!this.currentUser || !this.currentUser.id || !this.item.id) return
      try {
        const res = await favoriteApi.check(this.item.id)
        this.collected = res.data === true
      } catch (e) {
        // 静默失败不影响 UI
      }
    },
    openLightbox() {
      this.lightboxVisible = true
    },
    closeLightbox() {
      this.lightboxVisible = false
    },
    onKeydown(event) {
      if (event.key === 'Escape' && this.lightboxVisible) {
        this.closeLightbox()
      }
    },
    revealPhone() {
      this.phoneRevealed = true
    },
    formatDateTime(val) {
      if (!val) return '未知时间'
      const d = new Date(val)
      if (isNaN(d.getTime())) return val
      const pad = n => String(n).padStart(2, '0')
      return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
    },
    callPhone() {
      const phone = this.item.contactInfo
      if (!phone) {
        alert('该信息未填写联系方式')
        return
      }
      window.location.href = `tel:${phone}`
    },
    async handleCollect() {
      if (!this.currentUser || !this.currentUser.id) {
        alert('请先登录再收藏')
        return
      }
      const itemId = this.item.id
      try {
        if (this.collected) {
          await favoriteApi.remove(itemId)
          this.collected = false
        } else {
          await favoriteApi.add(itemId)
          this.collected = true
          alert('收藏成功！可在个人中心查看')
        }
      } catch (e) {
        alert('操作失败，请稍后重试')
      }
    },
    handleShare() {
      const shareTitle = `[${this.resolvedTypeLabel}] ${this.item.title}`
      const shareText = `${shareTitle} - ${this.item.location}，联系方式：${this.maskedPhone}`
      
      if (navigator.share) {
        navigator.share({
          title: shareTitle,
          text: shareText,
          url: window.location.href
        }).catch(() => {})
      } else {
        // 降级：复制完整分享链接
        const fullShare = `${shareText}\n\n${window.location.href}`
        navigator.clipboard.writeText(fullShare).then(() => {
          this.showShareToast('分享链接已复制，可粘贴给好友')
        }).catch(() => {
          // 备用方案：仅复制URL
          navigator.clipboard.writeText(window.location.href).then(() => {
            this.showShareToast('页面链接已复制')
          }).catch(() => {
            alert('当前浏览器不支持复制，请手动分享')
          })
        })
      }
    },
    showShareToast(msg) {
      this.shareMessage = msg
      setTimeout(() => {
        this.shareMessage = ''
      }, 2500)
    },
    handleReport() {
      alert('举报功能正在开发中，如发现违规信息请联系管理员')
    }
  }
}
</script>

<style scoped>
.detail-page {
  min-height: 100vh;
  background: #f5f7fb;
  padding-bottom: 80px;
}

.detail-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
  position: sticky;
  top: 0;
  z-index: 10;
}

.back-button {
  display: flex;
  align-items: center;
  gap: 6px;
  border: none;
  background: none;
  color: #1677ff;
  font-size: 15px;
  cursor: pointer;
  padding: 6px 8px;
  border-radius: 8px;
  transition: background 0.2s;
}

.back-button:hover {
  background: #f0f8ff;
}

.header-title {
  font-size: 17px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
}

/* 图片区域 */
.image-area {
  width: 100%;
  height: clamp(220px, 36vw, 340px);
  overflow: hidden;
  background: #f1f5f9;
}

.image-preview-trigger {
  width: 100%;
  height: 100%;
  border: none;
  padding: 0;
  background: transparent;
  cursor: zoom-in;
}

@media (max-width: 768px) {
  .image-area {
    height: 240px;
  }
}

.image-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  background: linear-gradient(135deg, #e8f4fd 0%, #e0f7f7 100%);
}

.placeholder-icon {
  font-size: 48px;
}

.placeholder-text {
  font-size: 14px;
  color: #9ca3af;
}

.lightbox {
  position: fixed;
  inset: 0;
  background: rgba(2, 6, 23, 0.82);
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.lightbox-image {
  max-width: min(92vw, 1200px);
  max-height: 90vh;
  object-fit: contain;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.4);
}

.lightbox-close {
  position: absolute;
  top: 14px;
  right: 16px;
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
  font-size: 24px;
  line-height: 1;
  cursor: pointer;
}

/* 主体 card */
.detail-card {
  margin: 12px 16px;
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.detail-title {
  font-size: 20px;
  font-weight: 700;
  color: #111827;
  margin: 0;
  flex: 1;
  line-height: 1.4;
}

.status-badge {
  flex-shrink: 0;
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
}

.status-badge.status-review {
  background: #fff7e6;
  color: #d97706;
}

.status-badge.status-approved {
  background: #f0fdf4;
  color: #16a34a;
}

.status-badge.status-rejected {
  background: #fef2f2;
  color: #dc2626;
}

.status-badge.status-completed {
  background: #f3f4f6;
  color: #6b7280;
}

.status-badge.status-pending {
  background: #fff7e6;
  color: #d97706;
}

.status-badge.status-processed {
  background: #f0fdf4;
  color: #16a34a;
}

.type-row {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.type-tag {
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
}

.type-tag.tag-lost {
  background: #fef2f2;
  color: #dc2626;
}

.type-tag.tag-found {
  background: #eff6ff;
  color: #1677ff;
}

.category-tag {
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
  background: #f3f4f6;
  color: #6b7280;
}

/* 信息列表 */
.info-list {
  border-top: 1px solid #f3f4f6;
  padding-top: 16px;
  margin-bottom: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.info-icon {
  font-size: 16px;
  width: 20px;
  text-align: center;
}

.info-label {
  font-size: 13px;
  color: #9ca3af;
  width: 56px;
  flex-shrink: 0;
}

.info-value {
  font-size: 14px;
  color: #374151;
  flex: 1;
}

/* 描述 */
.desc-section {
  border-top: 1px solid #f3f4f6;
  padding-top: 16px;
  margin-bottom: 16px;
}

.desc-label {
  font-size: 14px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 8px;
}

.desc-text {
  font-size: 14px;
  color: #4b5563;
  line-height: 1.7;
  margin: 0;
  white-space: pre-wrap;
}

/* 联系方式 */
.contact-section {
  border-top: 1px solid #f3f4f6;
  padding-top: 16px;
}

.contact-label {
  font-size: 14px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 10px;
}

.contact-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.contact-number {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  letter-spacing: 1px;
}

.contact-full {
  font-size: 16px;
  font-weight: 600;
  color: #1677ff;
}

.reveal-button {
  border: 1px solid #1677ff;
  background: none;
  color: #1677ff;
  font-size: 13px;
  padding: 4px 10px;
  border-radius: 20px;
  cursor: pointer;
}

.call-button {
  width: 100%;
  height: 48px;
  border: none;
  border-radius: 24px;
  background: linear-gradient(135deg, #1677ff 0%, #13c2c2 100%);
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  box-shadow: 0 4px 12px rgba(22, 119, 255, 0.3);
  transition: all 0.2s;
}

.call-button:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(22, 119, 255, 0.4);
}

/* 底部操作栏 */
.action-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: #fff;
  border-top: 1px solid #f0f0f0;
  display: flex;
  padding: 12px 24px;
  gap: 0;
}

.action-btn {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  border: none;
  background: none;
  cursor: pointer;
  padding: 8px;
  border-radius: 12px;
  transition: background 0.2s;
  font-size: 20px;
}

.action-btn:hover {
  background: #f5f7fb;
}

.action-btn.danger .action-label {
  color: #ef4444;
}

.action-label {
  font-size: 12px;
  color: #6b7280;
}

/* 分享 Toast */
.share-toast {
  position: fixed;
  bottom: 100px;
  left: 50%;
  transform: translateX(-50%);
  background: #1f2937;
  color: #fff;
  padding: 12px 20px;
  border-radius: 8px;
  font-size: 14px;
  z-index: 500;
  animation: slideUp 0.3s ease-out;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateX(-50%) translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateX(-50%) translateY(0);
  }
}
</style>
