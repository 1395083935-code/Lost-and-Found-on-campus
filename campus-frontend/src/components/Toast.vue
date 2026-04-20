<template>
  <div v-if="visible" class="toast" :class="[`toast-${type}`, `toast-${position}`]">
    <span class="toast-icon">{{ iconMap[type] }}</span>
    <span class="toast-message">{{ message }}</span>
    <button v-if="showClose" type="button" class="toast-close" @click="close">×</button>
  </div>
</template>

<script>
export default {
  name: 'Toast',
  props: {
    message: {
      type: String,
      default: ''
    },
    type: {
      type: String,
      enum: ['success', 'error', 'warning', 'info'],
      default: 'info'
    },
    duration: {
      type: Number,
      default: 3000  // 毫秒
    },
    position: {
      type: String,
      enum: ['top', 'center', 'bottom'],
      default: 'center'
    },
    showClose: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      visible: false,
      iconMap: {
        success: '✓',
        error: '✕',
        warning: '⚠',
        info: 'ℹ'
      },
      timer: null
    }
  },
  watch: {
    message(newVal) {
      if (newVal) {
        this.show()
      }
    }
  },
  methods: {
    show() {
      this.visible = true
      this.clearTimer()
      if (this.duration > 0) {
        this.timer = setTimeout(() => {
          this.close()
        }, this.duration)
      }
    },
    close() {
      this.visible = false
      this.clearTimer()
    },
    clearTimer() {
      if (this.timer) {
        clearTimeout(this.timer)
        this.timer = null
      }
    }
  },
  beforeUnmount() {
    this.clearTimer()
  }
}
</script>

<style scoped>
.toast {
  position: fixed;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 20px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  z-index: 9999;
  animation: slideIn 0.3s ease-out;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  max-width: 90%;
  word-break: break-word;
}

.toast-top {
  top: 20px;
  left: 50%;
  transform: translateX(-50%);
}

.toast-center {
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}

.toast-bottom {
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
}

/* 类型样式 */
.toast-success {
  background: #f0fdf4;
  color: #166534;
  border: 1px solid #bbf7d0;
}

.toast-error {
  background: #fef2f2;
  color: #991b1b;
  border: 1px solid #fecaca;
}

.toast-warning {
  background: #fffbeb;
  color: #92400e;
  border: 1px solid #fde68a;
}

.toast-info {
  background: #eff6ff;
  color: #164e63;
  border: 1px solid #bae6fd;
}

.toast-icon {
  font-size: 16px;
  flex-shrink: 0;
  font-weight: 600;
}

.toast-message {
  flex: 1;
}

.toast-close {
  border: none;
  background: none;
  color: inherit;
  font-size: 18px;
  cursor: pointer;
  padding: 0;
  width: 20px;
  height: 20px;
  flex-shrink: 0;
  opacity: 0.7;
  transition: opacity 0.2s;
}

.toast-close:hover {
  opacity: 1;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateX(-50%) translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateX(-50%) translateY(0);
  }
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateX(-50%) translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateX(-50%) translateY(0);
  }
}
</style>
