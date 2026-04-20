<template>
  <div :class="wrapperClass">
    <div class="smart-image__bg" :style="bgStyle"></div>
    <img
      class="smart-image__img"
      :src="src"
      :alt="alt"
      loading="lazy"
      @load="onLoad"
    />
  </div>
</template>

<script>
export default {
  name: 'SmartImage',
  props: {
    src: {
      type: String,
      required: true
    },
    alt: {
      type: String,
      default: '图片'
    },
    variant: {
      type: String,
      default: 'detail' // detail | thumb
    }
  },
  data() {
    return {
      loaded: false,
      orientation: 'landscape'
    }
  },
  computed: {
    wrapperClass() {
      return [
        'smart-image',
        `smart-image--${this.variant}`,
        `is-${this.orientation}`,
        this.loaded ? 'is-loaded' : ''
      ]
    },
    bgStyle() {
      return {
        backgroundImage: `url(${this.src})`
      }
    }
  },
  methods: {
    onLoad(event) {
      const { naturalWidth, naturalHeight } = event.target
      if (naturalWidth > 0 && naturalHeight > 0) {
        if (naturalWidth / naturalHeight > 1.15) {
          this.orientation = 'landscape'
        } else if (naturalHeight / naturalWidth > 1.15) {
          this.orientation = 'portrait'
        } else {
          this.orientation = 'square'
        }
      }
      this.loaded = true
    }
  }
}
</script>

<style scoped>
.smart-image {
  position: relative;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background: #f1f5f9;
}

.smart-image__bg {
  position: absolute;
  inset: 0;
  background-size: cover;
  background-position: center;
  filter: blur(16px) saturate(0.9);
  transform: scale(1.08);
  opacity: 0;
  transition: opacity 0.2s ease;
}

.smart-image.is-loaded .smart-image__bg {
  opacity: 0.55;
}

.smart-image__img {
  position: relative;
  z-index: 1;
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.smart-image--detail.is-portrait .smart-image__img {
  max-width: min(68%, 420px);
}

.smart-image--detail.is-square .smart-image__img {
  max-width: min(78%, 420px);
}

.smart-image--thumb .smart-image__bg {
  opacity: 0.45;
}

.smart-image--thumb .smart-image__img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

@media (max-width: 768px) {
  .smart-image--detail.is-portrait .smart-image__img,
  .smart-image--detail.is-square .smart-image__img {
    max-width: 100%;
  }
}
</style>
