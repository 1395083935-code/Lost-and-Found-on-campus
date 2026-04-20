<template>
  <div class="publish-page">
    <header class="page-header">
      <button class="back-button" @click="goBack">
        <span class="back-icon">←</span>
        返回
      </button>
      <h1 class="page-title">{{ pageTitle }}</h1>
    </header>

    <form class="publish-form" @submit.prevent="handleSubmit">

      <!-- 信息类型（优先选择，影响后续字段） -->
      <div class="form-group">
        <label class="form-label">我想发布 *</label>
        <div class="type-options">
          <button
            v-for="type in types"
            :key="type.value"
            type="button"
            :class="['type-button', form.type === type.value ? 'selected' : '']"
            @click="form.type = type.value"
          >
            {{ type.label }}
          </button>
        </div>
      </div>

      <!-- 物品名称 -->
      <div class="form-group">
        <label class="form-label">物品名称 *</label>
        <input
          v-model="form.title"
          type="text"
          class="form-input"
          :placeholder="form.type === 1 ? '如：黑色钥匙串、蓝牙耳机' : '如：校园卡、红色背包'"
          maxlength="20"
        />
        <span class="input-counter">{{ form.title.length }}/20</span>
      </div>

      <!-- 时间选择 -->
      <div class="form-group">
        <label class="form-label">{{ form.type === 1 ? '拾获时间' : '丢失时间' }} *</label>
        <input
          v-model="form.eventTime"
          type="datetime-local"
          class="form-input"
          :max="todayMax"
        />
      </div>

      <!-- 地点 -->
      <div class="form-group">
        <label class="form-label">{{ form.type === 1 ? '拾获地点' : '丢失地点' }} *</label>
        <div class="quick-locations">
          <button
            v-for="loc in quickLocations"
            :key="loc"
            type="button"
            :class="['loc-pill', form.location === loc ? 'selected' : '']"
            @click="form.location = loc"
          >
            {{ loc }}
          </button>
        </div>
        <input
          v-model="form.location"
          type="text"
          class="form-input"
          placeholder="或手动输入具体地点"
        />
      </div>

      <!-- 物品分类 -->
      <div class="form-group">
        <label class="form-label">物品分类 *</label>
        <div class="category-options">
          <button
            v-for="category in categories"
            :key="category.value"
            type="button"
            :class="['category-button', form.category === category.value ? 'selected' : '']"
            @click="form.category = category.value"
          >
            {{ category.label }}
          </button>
        </div>
      </div>

      <!-- 存放地点（仅拾物） -->
      <div v-if="form.type === 1" class="form-group">
        <label class="form-label">目前存放地点 *</label>
        <div class="quick-locations">
          <button
            v-for="loc in storagePlaces"
            :key="loc"
            type="button"
            :class="['loc-pill', form.storageLocation === loc ? 'selected' : '']"
            @click="form.storageLocation = loc"
          >
            {{ loc }}
          </button>
        </div>
        <input
          v-model="form.storageLocation"
          type="text"
          class="form-input"
          placeholder="如：本人宿舍、图书馆前台、XX办公室"
        />
      </div>

      <!-- 物品描述 -->
      <div class="form-group">
        <label class="form-label">物品特征描述</label>
        <textarea
          v-model="form.description"
          class="form-textarea"
          :placeholder="form.type === 1 ? '描述物品外观、颜色、品牌等特征，便于失主认领' : '描述物品特征，如颜色、品牌、有无标记等，提高找回率'"
          rows="4"
          maxlength="500"
        ></textarea>
        <span class="input-counter">{{ form.description.length }}/500</span>
      </div>

      <!-- 联系方式 -->
      <div class="form-group">
        <label class="form-label">联系方式 *</label>
        <input
          v-model="form.contactInfo"
          type="text"
          class="form-input"
          placeholder="请输入手机号码或微信号"
        />
      </div>

      <!-- 匿名发布 -->
      <div class="form-group toggle-group">
        <div class="toggle-info">
          <span class="form-label" style="margin-bottom:0">匿名发布</span>
          <span class="toggle-hint">开启后隐藏昵称，仅保留联系方式</span>
        </div>
        <button
          type="button"
          :class="['toggle-btn', form.anonymous ? 'on' : '']"
          @click="form.anonymous = !form.anonymous"
        >
          <span class="toggle-thumb"></span>
        </button>
      </div>

      <!-- 物品图片（最多3张） -->
      <div class="form-group">
        <label class="form-label">物品图片 <span class="label-hint">（最多3张）</span></label>
        <input
          ref="imageInput"
          type="file"
          accept="image/*"
          multiple
          @change="handleImageSelect"
          style="display: none"
        />
        <div class="image-grid">
          <div
            v-for="(preview, idx) in imagePreviews"
            :key="idx"
            class="image-thumb"
          >
            <img :src="preview" alt="预览" />
            <button type="button" class="remove-image" @click="removeImage(idx)">×</button>
          </div>
          <button
            v-if="imagePreviews.length < 3"
            type="button"
            class="upload-button"
            @click="$refs.imageInput.click()"
          >
            <span class="upload-icon">📷</span>
            <span>添加图片</span>
          </button>
        </div>
      </div>

      <div class="form-actions">
        <button type="button" class="cancel-button" @click="goBack" :disabled="submitting">取消</button>
        <button type="submit" class="submit-button" :disabled="submitting">
          {{ submitText }}
        </button>
      </div>
    </form>

    <LoadingSpinner v-if="submitting" :message="`${isEditMode ? '提交' : '发布'}中...`" />
  </div>
</template>

<script>
import { itemApi, fileApi } from './api/index.js'
import LoadingSpinner from './components/Spinner.vue'

export default {
  name: 'PublishPage',
  components: {
    LoadingSpinner
  },
  props: {
    draftItem: {
      type: Object,
      default: null
    },
    currentUser: {
      type: Object,
      default: null
    }
  },
  data() {
    const now = new Date()
    const pad = n => String(n).padStart(2, '0')
    const localNow = `${now.getFullYear()}-${pad(now.getMonth()+1)}-${pad(now.getDate())}T${pad(now.getHours())}:${pad(now.getMinutes())}`
    return {
      form: {
        title: '',
        description: '',
        category: '',
        type: 0, // 0: 失物寻主, 1: 拾物招领
        location: '',
        storageLocation: '',
        eventTime: localNow,
        contactInfo: '',
        anonymous: false
      },
      imageFiles: [],
      imagePreviews: [],
      submitting: false,
      todayMax: localNow,
      quickLocations: ['教学楼', '食堂', '图书馆', '操场', '宿舍', '体育馆'],
      storagePlaces: ['本人宿舍', '失物招领处', '图书馆前台', '教务处'],
      categories: [
        { label: '证件', value: '证件' },
        { label: '电子产品', value: '电子产品' },
        { label: '文具饰品', value: '文具饰品' },
        { label: '衣物箱包', value: '衣物箱包' },
        { label: '钥匙卡包', value: '钥匙卡包' },
        { label: '其他', value: '其他' }
      ],
      types: [
        { label: '失物寻主（我丢了）', value: 0 },
        { label: '拾物招领（我捡到了）', value: 1 }
      ]
    }
  },
  computed: {
    isEditMode() {
      return !!(this.draftItem && this.draftItem.id)
    },
    pageTitle() {
      if (this.isEditMode) {
        return '编辑并重新提交'
      }
      return this.form.type === 1 ? '发布拾物招领' : '发布失物寻主'
    },
    submitText() {
      if (this.submitting) {
        return this.isEditMode ? '提交中...' : '发布中...'
      }
      return this.isEditMode ? '保存并重提审核' : '提交审核'
    }
  },
  watch: {
    draftItem: {
      handler(newVal) {
        this.applyDraftItem(newVal)
      },
      immediate: true
    }
  },
  methods: {
    normalizeCategory(category) {
      const categoryMap = {
        certificate: '证件',
        electronic: '电子产品',
        stationery: '文具饰品',
        clothing: '衣物箱包',
        other: '其他'
      }
      return categoryMap[category] || category || ''
    },
    isValidContactInfo(contactInfo) {
      return /^1[3-9]\d{9}$/.test(contactInfo)
    },
    applyDraftItem(item) {
      if (!item || !item.id) {
        return
      }
      const now = new Date()
      const pad = n => String(n).padStart(2, '0')
      const fallbackNow = `${now.getFullYear()}-${pad(now.getMonth()+1)}-${pad(now.getDate())}T${pad(now.getHours())}:${pad(now.getMinutes())}`
      const rawTime = item.rawCreateTime || item.createTime
      let eventTime = fallbackNow
      if (rawTime) {
        const dt = new Date(rawTime)
        if (!Number.isNaN(dt.getTime())) {
          eventTime = `${dt.getFullYear()}-${pad(dt.getMonth()+1)}-${pad(dt.getDate())}T${pad(dt.getHours())}:${pad(dt.getMinutes())}`
        }
      }

      this.form = {
        title: item.title || '',
        description: item.description || '',
        category: this.normalizeCategory(item.category),
        type: item.type === 1 ? 1 : 0,
        location: item.location || '',
        storageLocation: item.storageLocation || '',
        eventTime,
        contactInfo: item.contactInfo || '',
        anonymous: !!item.anonymous
      }

      this.imageFiles = []
      this.imagePreviews = item.imageUrl ? String(item.imageUrl).split(',').map(s => s.trim()).filter(Boolean) : []
    },
    goBack() {
      this.$emit('back')
    },
    handleImageSelect(event) {
      const files = Array.from(event.target.files || [])
      if (files.length === 0) return

      const allowedTypes = ['image/jpeg', 'image/png', 'image/webp', 'image/gif', 'image/bmp']
      for (const file of files) {
        if (this.imagePreviews.length >= 3) {
          alert('最多只能上传 3 张图片')
          break
        }
        if (!allowedTypes.includes(file.type)) {
          alert(`不支持的图片格式：${file.name}`)
          continue
        }
        if (file.size > 2 * 1024 * 1024) {
          alert(`图片 ${file.name} 超过 2MB，请压缩后重试`)
          continue
        }
        this.imageFiles.push(file)
        const reader = new FileReader()
        reader.onload = (e) => {
          this.imagePreviews.push(e.target.result)
        }
        reader.readAsDataURL(file)
      }

      // 清空 input 以允许重复选同一文件
      this.$refs.imageInput.value = ''
    },
    async uploadWithRetry(file, maxRetry = 1) {
      let lastError = null
      for (let i = 0; i <= maxRetry; i++) {
        try {
          return await fileApi.upload(file)
        } catch (e) {
          lastError = e
        }
      }
      throw lastError
    },
    removeImage(idx) {
      const existingCount = this.imagePreviews.filter(url => /^https?:\/\//.test(url)).length
      if (idx >= existingCount) {
        const fileIdx = idx - existingCount
        if (fileIdx >= 0 && fileIdx < this.imageFiles.length) {
          this.imageFiles.splice(fileIdx, 1)
        }
      }
      this.imagePreviews.splice(idx, 1)
    },
    async handleSubmit() {
      if (this.submitting) return

      if (!this.currentUser || !this.currentUser.id) {
        alert('请先登录后再发布')
        this.goBack()
        return
      }

      const title = this.form.title.trim()
      const description = this.form.description.trim()
      const location = this.form.location.trim()
      const storageLocation = this.form.storageLocation.trim()
      const contactInfo = this.form.contactInfo.trim()
      const category = this.normalizeCategory(this.form.category)
      const eventTime = this.form.eventTime ? new Date(this.form.eventTime) : null

      // ========== 详细的表单验证 ==========
      
      // 1. 验证信息类型
      if (this.form.type !== 0 && this.form.type !== 1) {
        alert('请选择信息类型（失物寻主或拾物招领）')
        return
      }

      // 2. 验证物品名称
      if (!title) {
        alert('请输入物品名称')
        return
      }
      if (title.length < 2) {
        alert('物品名称至少 2 个字')
        return
      }
      if (title.length > 20) {
        alert('物品名称不能超过 20 字')
        return
      }

      // 3. 验证时间
      if (!eventTime || Number.isNaN(eventTime.getTime())) {
        alert('请选择有效的时间')
        return
      }
      if (eventTime.getTime() > Date.now()) {
        alert('时间不能晚于当前时间')
        return
      }

      // 4. 验证地点
      if (!location) {
        alert('请填写地点（可从快捷选项中选择）')
        return
      }

      // 5. 验证分类
      if (!category || category.trim() === '') {
        alert('请选择物品分类')
        return
      }

      // 6. 验证存放地点（仅拾物）
      if (this.form.type === 1 && !storageLocation) {
        alert('拾物招领必须填写目前存放地点')
        return
      }

      // 7. 验证描述
      if (description.length > 500) {
        alert('物品描述不能超过 500 字')
        return
      }

      // 8. 验证联系方式
      if (!contactInfo) {
        alert('请输入联系方式（手机号）')
        return
      }
      if (!this.isValidContactInfo(contactInfo)) {
        alert('请输入有效的手机号（11位，以1开头）')
        return
      }

      // 9. 验证图片
      if (this.imagePreviews.length < 1) {
        alert('请至少上传 1 张物品图片')
        return
      }
      if (this.imagePreviews.length > 3) {
        alert('最多只能上传 3 张物品图片')
        return
      }

      // ========== 通过所有验证，开始提交 ==========

      this.submitting = true
      try {
        let imageUrl = this.isEditMode && this.draftItem.imageUrl ? this.draftItem.imageUrl : ''
        if (this.imageFiles.length > 0) {
          const uploadedUrls = []
          for (const file of this.imageFiles) {
            try {
              const uploadResult = await this.uploadWithRetry(file, 1)
              const url = uploadResult.data || uploadResult.url || ''
              if (url) uploadedUrls.push(url)
            } catch (error) {
              alert(`图片上传失败，请检查网络连接后重试：${file.name}`)
              this.submitting = false
              return
            }
          }
          imageUrl = uploadedUrls.join(',')
        } else if (this.isEditMode) {
          // 编辑模式下，若未新增图片则以当前预览列表为准
          imageUrl = this.imagePreviews.filter(url => /^https?:\/\//.test(url)).join(',')
        }

        const itemData = {
          title,
          description,
          category,
          type: this.form.type,
          location,
          storageLocation,
          contactInfo,
          anonymous: this.form.anonymous,
          imageUrl,
          status: this.isEditMode ? 2 : 2,  // 新发和重提均需审核
          createTime: eventTime.toISOString()
        }

        if (this.isEditMode) {
          await itemApi.update(this.draftItem.id, itemData)
        } else {
          await itemApi.publish(itemData)
        }

        this.$emit('back', {
          isEditMode: this.isEditMode,
          targetPage: this.isEditMode ? 'profile' : 'home',
          message: this.isEditMode ? '修改成功，已重新提交审核' : '发布成功，正在等待审核'
        })
      } catch (error) {
        console.error('提交出错：', error)
        alert(this.isEditMode ? '重提失败，请重试' : '发布失败，请重试')
      } finally {
        this.submitting = false
      }
    }
  }
}
</script>

<style scoped>
.publish-page {
  min-height: 100vh;
  background: #f5f7fb;
  padding: 16px;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
  padding: 16px 0;
}

.back-button {
  display: flex;
  align-items: center;
  gap: 8px;
  border: none;
  background: none;
  color: #1677ff;
  font-size: 16px;
  cursor: pointer;
  padding: 8px;
}

.back-icon {
  font-size: 18px;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
}

.publish-form {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  width: min(100%, 760px);
  margin: 0 auto;
}

.form-group {
  margin-bottom: 24px;
}

.form-label {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 8px;
}

.form-input,
.form-textarea {
  width: min(100%, 560px);
  box-sizing: border-box;
  border: 1px solid #dbe3ee;
  border-radius: 10px;
  padding: 10px 14px;
  font-size: 14px;
  line-height: 1.4;
  outline: none;
  transition: border-color 0.2s;
}

.form-input:focus,
.form-textarea:focus {
  border-color: #1677ff;
}

.form-textarea {
  resize: vertical;
  min-height: 80px;
}

.category-options,
.type-options {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.category-button,
.type-button {
  padding: 8px 16px;
  border: 1px solid #dbe3ee;
  border-radius: 20px;
  background: #fff;
  color: #6b7280;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.category-button.selected,
.type-button.selected {
  border-color: #1677ff;
  background: #1677ff;
  color: #fff;
}

.image-upload {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* 多图上传网格 */
.image-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: flex-start;
}

.image-thumb {
  position: relative;
  width: 88px;
  height: 88px;
  border-radius: 12px;
  overflow: hidden;
}

.image-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.upload-button {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 88px;
  height: 88px;
  border: 1.5px dashed #dbe3ee;
  border-radius: 12px;
  background: #f9fafb;
  color: #9ca3af;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.upload-button:hover {
  border-color: #1677ff;
  background: #f0f8ff;
  color: #1677ff;
}

.upload-icon {
  font-size: 22px;
}

.remove-image {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 20px;
  height: 20px;
  border: none;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.55);
  color: #fff;
  font-size: 14px;
  font-weight: bold;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
}

/* 地点快捷选项 */
.quick-locations {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 10px;
}

.loc-pill {
  padding: 6px 14px;
  border: 1px solid #dbe3ee;
  border-radius: 20px;
  background: #fff;
  color: #6b7280;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.loc-pill.selected {
  border-color: #1677ff;
  background: #eff6ff;
  color: #1677ff;
}

/* 字数计数 */
.input-counter {
  display: block;
  text-align: right;
  font-size: 12px;
  color: #9ca3af;
  margin-top: 4px;
}

.label-hint {
  font-weight: 400;
  color: #9ca3af;
  font-size: 12px;
}

/* 匿名发布开关 */
.toggle-group {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.toggle-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.toggle-hint {
  font-size: 12px;
  color: #9ca3af;
}

.toggle-btn {
  width: 44px;
  height: 24px;
  border-radius: 12px;
  border: none;
  background: #d1d5db;
  position: relative;
  cursor: pointer;
  transition: background 0.2s;
  flex-shrink: 0;
}

.toggle-btn.on {
  background: #1677ff;
}

.toggle-thumb {
  position: absolute;
  top: 2px;
  left: 2px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #fff;
  transition: transform 0.2s;
  box-shadow: 0 1px 4px rgba(0,0,0,0.2);
}

.toggle-btn.on .toggle-thumb {
  transform: translateX(20px);
}

.form-actions {
  display: flex;
  gap: 12px;
  margin-top: 32px;
}

.cancel-button,
.submit-button {
  flex: 1;
  height: 48px;
  border: none;
  border-radius: 24px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.cancel-button {
  background: #f3f4f6;
  color: #6b7280;
}

.cancel-button:hover {
  background: #e5e7eb;
}

.submit-button {
  background: linear-gradient(135deg, #1677ff 0%, #13c2c2 100%);
  color: #fff;
  box-shadow: 0 4px 12px rgba(22, 119, 255, 0.3);
}

.submit-button:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(22, 119, 255, 0.4);
}

.submit-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}
</style>