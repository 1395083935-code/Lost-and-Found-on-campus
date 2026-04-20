import Toast from '../components/Toast.vue'

let currentToastComponent = null
let toastContainer = null

export const showToast = (message, options = {}) => {
  const {
    type = 'info',
    duration = 3000,
    position = 'center'
  } = options

  // 创建容器
  if (!toastContainer) {
    toastContainer = document.createElement('div')
    document.body.appendChild(toastContainer)
  }

  // 移除旧的 toast
  if (currentToastComponent) {
    currentToastComponent.$destroy?.()
    currentToastComponent?.$unmount?.()
  }

  // 创建新的 toast
  const app = {
    setup() {
      return {
        message,
        type,
        duration,
        position
      }
    },
    template: `
      <Toast
        :message="message"
        :type="type"
        :duration="duration"
        :position="position"
      />
    `,
    components: { Toast }
  }

  // Vue 3 简化方式：使用函数式组件
  const div = document.createElement('div')
  const container = document.body.appendChild(div)
  
  return {
    container,
    close: () => {
      if (container.parentNode) {
        container.parentNode.removeChild(container)
      }
    }
  }
}

export const successToast = (message, duration = 3000) => {
  return showToast(message, { type: 'success', duration })
}

export const errorToast = (message, duration = 3000) => {
  return showToast(message, { type: 'error', duration })
}

export const warningToast = (message, duration = 3000) => {
  return showToast(message, { type: 'warning', duration })
}

export const infoToast = (message, duration = 3000) => {
  return showToast(message, { type: 'info', duration })
}
