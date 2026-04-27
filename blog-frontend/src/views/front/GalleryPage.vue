<template>
  <div class="gallery-page">
    <header class="gallery-header">
      <h1>我的图库</h1>
      <p class="gallery-subtitle">用镜头记录生活中的美好瞬间</p>
    </header>

    <!-- Category filter pills -->
    <div class="category-filter" v-if="categories.length > 0">
      <button
        v-for="cat in categories"
        :key="cat.id"
        class="category-pill"
        :class="{ active: activeCategory === cat.id }"
        @click="switchCategory(cat.id)"
      >
        {{ cat.name }}
        <span class="category-count">{{ cat.count }}</span>
      </button>
    </div>

    <!-- Image grid -->
    <div v-if="loading" class="gallery-loading">
      <div class="loading-spinner"></div>
    </div>

    <div v-else-if="images.length === 0" class="gallery-empty">
      <div class="empty-icon">
        <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round">
          <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
          <circle cx="8.5" cy="8.5" r="1.5"/>
          <polyline points="21 15 16 10 5 21"/>
        </svg>
      </div>
      <p>暂无图片</p>
    </div>

    <div v-else class="gallery-grid">
      <div
        v-for="image in images"
        :key="image.id"
        class="gallery-card"
        @click="openLightbox(image)"
      >
        <div class="card-image-wrapper">
          <img :src="image.url" :alt="image.title || image.originalName" loading="lazy" />
          <div class="card-overlay">
            <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2">
              <circle cx="11" cy="11" r="8"/>
              <line x1="21" y1="21" x2="16.65" y2="16.65"/>
              <line x1="11" y1="8" x2="11" y2="14"/>
              <line x1="8" y1="11" x2="14" y2="11"/>
            </svg>
          </div>
        </div>
        <div class="card-caption">
          <p class="caption-title">{{ image.title || image.originalName }}</p>
        </div>
      </div>
    </div>

    <!-- Load more -->
    <div v-if="hasMore && images.length > 0" class="load-more">
      <button class="load-more-btn" @click="loadMore" :disabled="loadingMore">
        {{ loadingMore ? '加载中...' : '加载更多' }}
      </button>
    </div>

    <!-- Lightbox -->
    <Teleport to="body">
      <div v-if="lightboxImage" class="lightbox" @click.self="closeLightbox">
        <button class="lightbox-close" @click="closeLightbox">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
          </svg>
        </button>
        <button class="lightbox-prev" @click.stop="prevImage" :disabled="lightboxIndex <= 0">
          <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15 18 9 12 15 6"/></svg>
        </button>
        <div class="lightbox-content">
          <img :src="lightboxImage.url" :alt="lightboxImage.title || lightboxImage.originalName" />
          <p class="lightbox-caption">{{ lightboxImage.title || lightboxImage.originalName }}</p>
        </div>
        <button class="lightbox-next" @click.stop="nextImage" :disabled="lightboxIndex >= lightboxImages.length - 1">
          <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="9 18 15 12 9 6"/></svg>
        </button>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { galleryApi } from '@/api'

interface GalleryImage {
  id: number
  originalName: string
  title: string | null
  url: string
  categoryId: number | null
  createTime: string
}

interface Category {
  id: number
  name: string
  slug: string
  count: number
}

const categories = ref<Category[]>([])
const images = ref<GalleryImage[]>([])
const activeCategory = ref<number | null>(null)
const loading = ref(false)
const loadingMore = ref(false)

let currentPage = 1
const pageSize = 20
let totalCount = 0

const hasMore = ref(false)

// Lightbox state
const lightboxImage = ref<GalleryImage | null>(null)
const lightboxImages = ref<GalleryImage[]>([])
const lightboxIndex = ref(0)

onMounted(async () => {
  await loadCategories()
  await loadImages()
})

async function loadCategories() {
  try {
    const res = await galleryApi.getCategories()
    if (res.data.code === 200) {
      categories.value = res.data.data || []
    }
  } catch (_) {}
}

async function loadImages(reset = true) {
  if (reset) {
    loading.value = true
    currentPage = 1
  }
  try {
    const res = await galleryApi.getImages({
      categoryId: activeCategory.value || undefined,
      page: currentPage,
      size: pageSize,
    })
    if (res.data.code === 200) {
      const data = res.data.data
      if (reset) {
        images.value = data.list || []
      } else {
        images.value.push(...(data.list || []))
      }
      totalCount = data.total
      hasMore.value = images.value.length < totalCount
    }
  } catch (_) {}
  loading.value = false
  loadingMore.value = false
}

function switchCategory(catId: number | null) {
  if (activeCategory.value === catId) {
    activeCategory.value = null
  } else {
    activeCategory.value = catId
  }
  loadImages()
}

function loadMore() {
  loadingMore.value = true
  currentPage++
  loadImages(false)
}

function openLightbox(image: GalleryImage) {
  lightboxImages.value = images.value
  lightboxIndex.value = images.value.findIndex(i => i.id === image.id)
  lightboxImage.value = image
  document.body.style.overflow = 'hidden'
}

function closeLightbox() {
  lightboxImage.value = null
  document.body.style.overflow = ''
}

function prevImage() {
  if (lightboxIndex.value > 0) {
    lightboxIndex.value--
    lightboxImage.value = lightboxImages.value[lightboxIndex.value]
  }
}

function nextImage() {
  if (lightboxIndex.value < lightboxImages.value.length - 1) {
    lightboxIndex.value++
    lightboxImage.value = lightboxImages.value[lightboxIndex.value]
  }
}
</script>

<style scoped>
.gallery-page {
  max-width: 900px;
  margin: 0 auto;
  padding: 60px 24px 40px;
}

.gallery-header {
  text-align: center;
  margin-bottom: 40px;
}

.gallery-header h1 {
  font-size: 32px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 8px;
  letter-spacing: -0.5px;
}

.gallery-subtitle {
  font-size: 15px;
  color: var(--text-muted);
  margin: 0;
}

/* Category filter */
.category-filter {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 10px;
  margin-bottom: 36px;
}

.category-pill {
  padding: 8px 18px;
  border-radius: 20px;
  border: 1px solid var(--border-color);
  background: var(--card-bg);
  color: var(--text-secondary);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.25s ease;
  display: flex;
  align-items: center;
  gap: 6px;
}

.category-pill:hover {
  border-color: var(--accent-color);
  color: var(--accent-color);
}

.category-pill.active {
  background: var(--accent-color);
  border-color: var(--accent-color);
  color: #fff;
}

.category-count {
  font-size: 12px;
  opacity: 0.7;
}

/* Gallery grid */
.gallery-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 20px;
}

.gallery-card {
  cursor: pointer;
  border-radius: 12px;
  overflow: hidden;
  background: var(--card-bg);
  border: 1px solid var(--border-color);
  transition: transform 0.25s ease, box-shadow 0.25s ease;
}

.gallery-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.12);
}

.card-image-wrapper {
  position: relative;
  width: 100%;
  padding-top: 75%;
  overflow: hidden;
  background: var(--bg-secondary);
}

.card-image-wrapper img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.4s ease;
}

.gallery-card:hover .card-image-wrapper img {
  transform: scale(1.05);
}

.card-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.3s ease;
  opacity: 0;
}

.gallery-card:hover .card-overlay {
  background: rgba(0, 0, 0, 0.35);
  opacity: 1;
}

.card-overlay svg {
  transform: scale(0.8);
  transition: transform 0.3s ease;
}

.gallery-card:hover .card-overlay svg {
  transform: scale(1);
}

.card-caption {
  padding: 14px 16px;
}

.caption-title {
  font-size: 14px;
  color: var(--text-primary);
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 500;
}

/* States */
.gallery-loading {
  display: flex;
  justify-content: center;
  padding: 80px 0;
}

.loading-spinner {
  width: 36px;
  height: 36px;
  border: 3px solid var(--border-color);
  border-top-color: var(--accent-color);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.gallery-empty {
  text-align: center;
  padding: 80px 0;
  color: var(--text-muted);
}

.empty-icon {
  margin-bottom: 16px;
  opacity: 0.4;
}

.gallery-empty p {
  font-size: 15px;
  margin: 0;
}

/* Load more */
.load-more {
  text-align: center;
  margin-top: 40px;
}

.load-more-btn {
  padding: 12px 40px;
  border: 1px solid var(--border-color);
  border-radius: 24px;
  background: var(--card-bg);
  color: var(--text-secondary);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.25s ease;
}

.load-more-btn:hover {
  border-color: var(--accent-color);
  color: var(--accent-color);
}

.load-more-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* Lightbox */
.lightbox {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: rgba(0, 0, 0, 0.92);
  display: flex;
  align-items: center;
  justify-content: center;
  animation: fadeIn 0.2s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.lightbox-close {
  position: absolute;
  top: 20px;
  right: 20px;
  background: rgba(255,255,255,0.1);
  border: none;
  color: #fff;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
}

.lightbox-close:hover {
  background: rgba(255,255,255,0.25);
}

.lightbox-prev,
.lightbox-next {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  background: rgba(255,255,255,0.1);
  border: none;
  color: #fff;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
}

.lightbox-prev { left: 20px; }
.lightbox-next { right: 20px; }

.lightbox-prev:hover,
.lightbox-next:hover {
  background: rgba(255,255,255,0.25);
}

.lightbox-prev:disabled,
.lightbox-next:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

.lightbox-content {
  max-width: 85vw;
  max-height: 88vh;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.lightbox-content img {
  max-width: 100%;
  max-height: 78vh;
  object-fit: contain;
  border-radius: 4px;
}

.lightbox-caption {
  color: #ccc;
  font-size: 14px;
  margin-top: 16px;
  text-align: center;
}

@media (max-width: 640px) {
  .gallery-page {
    padding: 40px 16px 32px;
  }

  .gallery-header h1 {
    font-size: 26px;
  }

  .gallery-grid {
    grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
    gap: 12px;
  }

  .card-caption {
    padding: 10px 12px;
  }

  .caption-title {
    font-size: 13px;
  }

  .lightbox-prev { left: 8px; }
  .lightbox-next { right: 8px; }
  .lightbox-prev,
  .lightbox-next {
    width: 40px;
    height: 40px;
  }
}
</style>