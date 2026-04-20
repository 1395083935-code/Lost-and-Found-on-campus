<template>
  <div class="admin-page">
    <header class="admin-header">
      <h1 class="admin-title">📋 管理后台</h1>
      <button class="back-button" @click="goBack">← 返回</button>
    </header>

    <section class="stats-section">
      <div class="section-title">
        <span>数据概览</span>
      </div>
      <div class="stats-grid">
        <div class="stats-card">
          <div class="stats-value">{{ stats.totalItems }}</div>
          <div class="stats-label">总发布数</div>
        </div>
        <div class="stats-card warn">
          <div class="stats-value">{{ stats.pendingReview }}</div>
          <div class="stats-label">待审核</div>
        </div>
        <div class="stats-card success">
          <div class="stats-value">{{ stats.approved }}</div>
          <div class="stats-label">已通过</div>
        </div>
        <div class="stats-card danger">
          <div class="stats-value">{{ stats.rejected }}</div>
          <div class="stats-label">已驳回</div>
        </div>
        <div class="stats-card">
          <div class="stats-value">{{ stats.totalUsers }}</div>
          <div class="stats-label">用户总数</div>
        </div>
        <div class="stats-card info">
          <div class="stats-value">{{ stats.todayPublished }}</div>
          <div class="stats-label">今日新增发布</div>
        </div>
      </div>
    </section>

    <section class="review-section">
      <div class="section-title">
        <span>待审核的发布</span>
        <span class="count-badge">{{ reviewItems.length }}</span>
      </div>

      <div v-if="reviewItems.length === 0" class="empty-card">
        暂无待审核的发布。
      </div>

      <article v-for="item in reviewItems" :key="item.id" class="review-card">
        <div class="card-header">
          <h3 class="item-title">{{ item.title || '未命名物品' }}</h3>
          <span class="type-tag" :class="item.typeClass">{{ item.typeLabel }}</span>
        </div>

        <div class="card-body">
          <p class="item-desc">{{ item.description || '暂无描述' }}</p>
          <div class="meta-info">
            <span>📍 {{ item.location }}</span>
            <span>⏰ {{ formatDateTime(item.createTime) }}</span>
            <span>📞 {{ item.contactInfo }}</span>
          </div>
          <div class="category-info">
            <span>分类：{{ categoryLabel(item.category) }}</span>
            <span v-if="item.storageLocation">存放：{{ item.storageLocation }}</span>
          </div>
        </div>

        <div class="card-actions">
          <button
            class="action-btn approve"
            @click="approve(item)"
            :disabled="processing"
          >
            ✓ 通过
          </button>
          <select
            class="reason-select"
            v-model="rejectReasonMap[item.id]"
            :disabled="processing"
          >
            <option v-for="reason in rejectReasonOptions" :key="reason" :value="reason">
              {{ reason }}
            </option>
          </select>
          <button
            class="action-btn reject"
            @click="reject(item)"
            :disabled="processing"
          >
            ✕ 驳回
          </button>
        </div>
        <div v-if="rejectReasonMap[item.id] === '其他'" class="custom-reason-row">
          <input
            v-model="rejectCustomReasonMap[item.id]"
            class="custom-reason-input"
            type="text"
            maxlength="255"
            :disabled="processing"
            placeholder="请输入自定义驳回理由（最多255字）"
          />
        </div>
      </article>
    </section>

    <section v-if="pastItems.length > 0" class="past-section">
      <div class="section-title">
        <span>已处理的发布</span>
        <span class="count-badge">{{ pastItems.length }}</span>
      </div>

      <div class="past-list">
        <div v-for="item in pastItems" :key="item.id" class="past-item">
          <div class="past-main">
            <span class="item-title-small">{{ item.title }}</span>
            <span v-if="item.status === 2 && item.rejectReason" class="reject-reason">
              驳回原因：{{ item.rejectReason }}
            </span>
          </div>
          <span :class="['status-tag', item.status === 1 ? 'approved' : (item.status === 2 ? 'rejected' : 'completed')]">{{ item.status === 1 ? '已通过' : (item.status === 2 ? '已驳回' : '已完结') }}</span>
        </div>
      </div>
    </section>

    <LoadingSpinner v-if="loading" message="加载中..." />
  </div>
</template>

<script>
import { adminApi } from './api/index.js'
import LoadingSpinner from './components/Spinner.vue'

export default {
  name: 'AdminPage',
  components: {
    LoadingSpinner
  },
  emits: ['back'],
  props: {
    items: {
      type: Array,
      default: () => []
    }
  },
  data() {
    return {
      processing: false,
      loading: false,
      adminItems: [],   // 独立从后端拉全量数据
      stats: {
        totalItems: 0,
        pendingReview: 0,
        approved: 0,
        rejected: 0,
        totalUsers: 0,
        todayPublished: 0
      },
      rejectReasonMap: {},
      rejectCustomReasonMap: {},
      rejectReasonOptions: ['信息不完整', '联系方式无效', '描述与标题不一致', '疑似重复发布', '其他']
    }
  },
  computed: {
    allItems() {
      // 优先使用自拉数据，否则 fallback 到 prop
      return this.adminItems.length ? this.adminItems : this.items
    },
    reviewItems() {
      return this.allItems
        .filter(item => item.status === 0)
        .sort((a, b) => new Date(b.createTime || 0).getTime() - new Date(a.createTime || 0).getTime())
    },
    pastItems() {
      return this.allItems
        .filter(item => item.status === 1 || item.status === 2 || item.status === 3)
        .sort((a, b) => new Date(b.updateTime || b.createTime || 0).getTime() - new Date(a.updateTime || a.createTime || 0).getTime())
    }
  },
  mounted() {
    this.loading = true
    Promise.all([this.loadItems(), this.loadStats()]).finally(() => {
      this.loading = false
    })
  },
  methods: {
    async loadStats() {
      try {
        const body = await adminApi.stats()
        this.stats = {
          ...this.stats,
          ...(body?.data || {})
        }
      } catch (e) {
        console.error('Admin 统计加载失败', e)
      }
    },
    async loadItems() {
      try {
        const body = await adminApi.pending()
        const records = body?.data || []
        this.adminItems = records
        this.reviewItems.forEach(item => {
          if (!this.rejectReasonMap[item.id]) {
            this.rejectReasonMap[item.id] = this.rejectReasonOptions[0]
          }
          if (!this.rejectCustomReasonMap[item.id]) {
            this.rejectCustomReasonMap[item.id] = ''
          }
        })
      } catch (e) {
        console.error('Admin 加载数据失败', e)
      }
    },
    categoryLabel(category) {
      const map = {
        certificate: '证件',
        electronic: '电子产品',
        stationery: '文具饰品',
        clothing: '衣物箱包',
        other: '其他'
      }
      return map[category] || category || '其他'
    },
    formatDateTime(val) {
      if (!val) return '未知时间'
      const d = new Date(val)
      if (isNaN(d.getTime())) return val
      const pad = n => String(n).padStart(2, '0')
      return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
    },
    async approve(item) {
      if (!confirm(`确认通过 "${item.title}" 吗？`)) return
      await this.review(item.id, 1)
    },
    async reject(item) {
      const selectedReason = this.rejectReasonMap[item.id] || this.rejectReasonOptions[0]
      const customReason = (this.rejectCustomReasonMap[item.id] || '').trim()
      const rejectReason = selectedReason === '其他' ? customReason : selectedReason

      if (!rejectReason) {
        alert('请选择或输入驳回理由')
        return
      }
      if (rejectReason.length > 255) {
        alert('驳回理由不能超过255字')
        return
      }

      if (!confirm(`确认驳回 "${item.title}" 吗？\n驳回理由：${rejectReason}`)) return
      await this.review(item.id, 2, rejectReason)
    },
    async review(id, status, rejectReason = null) {
      this.processing = true
      try {
        if (status === 1) {
          await adminApi.approve(id)
        } else {
          await adminApi.reject(id, rejectReason)
        }
        alert(status === 1 ? '已通过' : '已驳回')
        await this.loadItems()
        await this.loadStats()
      } catch (err) {
        console.error('审核失败：', err)
        alert('审核失败，请重试')
      } finally {
        this.processing = false
      }
    },
    goBack() {
      this.$emit('back')
    }
  }
}
</script>

<style scoped>
.admin-page {
  min-height: 100vh;
  background: #f5f7fb;
  padding: 16px;
  padding-bottom: 90px;
}

.admin-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.admin-title {
  font-size: 24px;
  font-weight: 700;
  color: #0f172a;
  margin: 0;
}

.back-button {
  border: none;
  background: none;
  color: #1677ff;
  font-size: 14px;
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 8px;
  transition: background 0.2s;
}

.back-button:hover {
  background: #f0f8ff;
}

.review-section,
.past-section {
  margin-bottom: 24px;
}

.stats-section {
  margin-bottom: 20px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.stats-card {
  background: #fff;
  border-radius: 12px;
  padding: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  border: 1px solid #eef2f7;
}

.stats-card.warn {
  border-color: #fde68a;
  background: #fffbeb;
}

.stats-card.success {
  border-color: #bbf7d0;
  background: #f0fdf4;
}

.stats-card.danger {
  border-color: #fecaca;
  background: #fef2f2;
}

.stats-card.info {
  border-color: #bfdbfe;
  background: #eff6ff;
}

.stats-value {
  font-size: 24px;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.1;
}

.stats-label {
  margin-top: 6px;
  font-size: 12px;
  color: #64748b;
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

.section-title {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 12px;
}

.count-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #ef4444;
  color: #fff;
  font-size: 12px;
  font-weight: 600;
}

.review-card {
  background: #fff;
  border-radius: 16px;
  padding: 16px;
  margin-bottom: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  border-left: 4px solid #f59e0b;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.item-title {
  margin: 0;
  font-size: 16px;
  color: #0f172a;
  flex: 1;
}

.type-tag {
  padding: 4px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  color: #fff;
}

.type-tag.tag-lost {
  background: #1677ff;
}

.type-tag.tag-found {
  background: #13c2c2;
}

.card-body {
  background: #f9fafb;
  border-radius: 12px;
  padding: 12px;
  margin-bottom: 12px;
}

.item-desc {
  margin: 0 0 8px;
  color: #475569;
  font-size: 14px;
  line-height: 1.5;
}

.meta-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 13px;
  color: #64748b;
  margin-bottom: 8px;
}

.category-info {
  display: flex;
  gap: 12px;
  font-size: 13px;
  color: #64748b;
}

.card-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.reason-select {
  flex: 1.2;
  height: 40px;
  border: 1px solid #dbe3ee;
  border-radius: 10px;
  background: #fff;
  color: #475569;
  font-size: 13px;
  padding: 0 10px;
}

.custom-reason-row {
  margin-top: 10px;
}

.custom-reason-input {
  width: 100%;
  box-sizing: border-box;
  height: 38px;
  border: 1px solid #dbe3ee;
  border-radius: 10px;
  background: #fff;
  color: #475569;
  font-size: 13px;
  padding: 0 10px;
}

.action-btn {
  flex: 1;
  padding: 10px 16px;
  border: none;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.action-btn.approve {
  background: #d1fae5;
  color: #059669;
}

.action-btn.approve:hover:not(:disabled) {
  background: #a7f3d0;
}

.action-btn.reject {
  background: #fee2e2;
  color: #dc2626;
}

.action-btn.reject:hover:not(:disabled) {
  background: #fecaca;
}

.action-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.past-section {
  background: #fff;
  border-radius: 16px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.past-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.past-item {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 12px;
  background: #f9fafb;
  border-radius: 8px;
  gap: 10px;
}

.past-main {
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex: 1;
}

.item-title-small {
  font-size: 14px;
  color: #475569;
  flex: 1;
}

.reject-reason {
  font-size: 12px;
  color: #b45309;
}

.status-tag {
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.status-tag.approved {
  background: #d1fae5;
  color: #059669;
}

.status-tag.rejected {
  background: #fee2e2;
  color: #dc2626;
}

.status-tag.completed {
  background: #e0e7ff;
  color: #3730a3;
}

.empty-card {
  padding: 30px;
  text-align: center;
  border: 1px dashed #cbd5e1;
  border-radius: 16px;
  color: #64748b;
  background: #f8fafc;
}
</style>
