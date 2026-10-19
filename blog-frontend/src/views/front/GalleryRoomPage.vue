<template>
  <div
    ref="rootEl"
    class="gallery-shell"
    @pointerdown="camera.onPointerDown"
    @pointermove="camera.onPointerMove"
    @pointerup="camera.onPointerUp"
    @pointercancel="camera.onPointerCancel"
  >
    <!--
      The site-wide top gradient sits under this shell, so the same desktop
      vignette is recreated here: above the room (z-10), below chrome (z-50).
    -->
    <div aria-hidden="true" class="gallery-vignette" />

    <div data-gallery-no-drag class="chrome-top-right">
      <button
        v-if="canDownload"
        type="button"
        class="ghost-btn"
        aria-label="下载当前画作"
        @click="onDownload"
      >
        <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
          <path
            d="M12 4v11m0 0l-4.5-4.5M12 15l4.5-4.5M5 19h14"
            stroke="currentColor"
            stroke-width="1.5"
            stroke-linecap="round"
            stroke-linejoin="round"
          />
        </svg>
      </button>
      <GalleryInfoButton />
    </div>

    <GalleryRoom
      :pose="camera.pose.value"
      :zoom="camera.zoom.value"
      :focused-id="camera.focusedId.value"
      :paintings="paintings"
      @select-painting="camera.selectPainting"
    />

    <GalleryThumbstick
      :focused-id="camera.focusedId.value"
      :on-select="camera.selectPainting"
      :on-zoom-by="camera.zoomBy"
      :paintings="paintings"
    />
  </div>
</template>

<script setup lang="ts">
/*
 * Ported from michelletliu/michelle-liu (MIT) — src/components/gallery/GalleryPage.tsx
 *
 * Fullscreen immersive room. The reference's AI composer is replaced by an
 * artwork picker fed from this site's own admin uploads, so nothing here calls
 * a generation service.
 */
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import GalleryRoom from '@/components/gallery/GalleryRoom.vue'
import GalleryThumbstick from '@/components/gallery/GalleryThumbstick.vue'
import GalleryInfoButton from '@/components/gallery/GalleryInfoButton.vue'
import { useGalleryCamera } from '@/gallery/useGalleryCamera'
import {
  GALLERY_PAINTINGS,
  type GalleryPainting,
} from '@/gallery/galleryPaintings'
import { downloadImage, generatedImageFilename } from '@/gallery/downloadImage'
import { galleryApi } from '@/api'

/** Artwork hung on a frame — label feeds the download filename. */
export type PaintingSelection = {
  label: string
  imageUrl: string
}

const props = withDefaults(
  defineProps<{
    /** Preloaded hang images keyed by painting id. */
    initialImageById?: Record<string, string>
  }>(),
  {
    initialImageById: undefined,
  },
)

const camera = useGalleryCamera()

const imageById = ref<Record<string, string>>({ ...(props.initialImageById ?? {}) })
const selectionById = ref<Record<string, PaintingSelection>>({})

const paintings = computed<GalleryPainting[]>(() =>
  GALLERY_PAINTINGS.map((painting) => ({
    ...painting,
    imageUrl: imageById.value[painting.id] ?? painting.imageUrl,
  })),
)

const filledHangCount = computed(() => Object.keys(imageById.value).length)

const canDownload = computed(() => Boolean(imageById.value[camera.focusedId.value]))

function onDownload() {
  const imageUrl = imageById.value[camera.focusedId.value]
  if (!imageUrl) return
  void downloadImage(
    imageUrl,
    generatedImageFilename({
      inspirationTitle: selectionById.value[camera.focusedId.value]?.label ?? null,
      imageUrl,
    }),
  )
}

/**
 * Hard nav home — a soft router push waits on WebGL dispose and
 * forceContextLoss on the main thread, which reads as a broken click.
 */
function goHome() {
  window.location.assign('/')
}

/**
 * The exhibition is curated from the backend only: images this site's admin
 * has uploaded hang first (newest first), then the bundled public-domain
 * collection fills the frames the uploads don't cover. Visitors browse and
 * download — nothing on the front end can swap a painting.
 */
interface UploadMedia {
  url: string
  title?: string | null
  originalName?: string | null
}

async function loadUploads(): Promise<PaintingSelection[]> {
  try {
    const res = await galleryApi.getImages({ page: 1, size: GALLERY_PAINTINGS.length })
    if (res.data.code === 200) {
      const page = res.data.data
      // PageResult 的列表字段是 list（兼容 records 形态）
      const list = Array.isArray(page) ? page : (page?.list ?? page?.records ?? [])
      if (Array.isArray(list) && list.length) {
        return (list as UploadMedia[]).map((it) => ({
          label: it.title || it.originalName || '未命名',
          imageUrl: it.url,
        }))
      }
    }
  } catch (_) {}
  return []
}

async function loadMetCollection(): Promise<PaintingSelection[]> {
  try {
    const res = await fetch('/met-art/manifest.json')
    const list = (await res.json()) as Array<{
      file: string
      title: string
      artist: string
    }>
    return list.map((art) => ({
      label: art.artist ? `${art.title} — ${art.artist}` : art.title,
      imageUrl: '/met-art/' + art.file,
    }))
  } catch (_) {
    return []
  }
}

async function hangOpeningCollection() {
  if (filledHangCount.value > 0) return
  const [uploads, met] = await Promise.all([loadUploads(), loadMetCollection()])
  const arts = [...uploads, ...met]
  if (!arts.length) return
  const nextImages: Record<string, string> = { ...imageById.value }
  const nextSelections: Record<string, PaintingSelection> = { ...selectionById.value }
  GALLERY_PAINTINGS.forEach((painting, i) => {
    const art = arts[i % arts.length]
    nextImages[painting.id] = art.imageUrl
    nextSelections[painting.id] = art
  })
  imageById.value = nextImages
  selectionById.value = nextSelections
}

function onKeyDown(e: KeyboardEvent) {
  if (e.key === 'Escape' || e.code === 'Escape') {
    e.preventDefault()
    goHome()
  }
}

onMounted(() => {
  window.addEventListener('keydown', onKeyDown)
  void hangOpeningCollection()
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKeyDown)
})
</script>

<style scoped>
.gallery-shell {
  position: fixed;
  inset: 0;
  z-index: 50;
  touch-action: none;
  overflow: hidden;
  background: #e4e4e4;
  color: #18181b;
}

.gallery-vignette {
  pointer-events: none;
  position: absolute;
  left: 0;
  right: 0;
  top: 0;
  z-index: 20;
  display: none;
  height: 8rem;
  background: linear-gradient(
    180deg,
    hsla(0, 0%, 100%, 0.5) 0%,
    hsla(0, 0%, 100%, 0.369) 19%,
    hsla(0, 0%, 100%, 0.271) 34%,
    hsla(0, 0%, 100%, 0.191) 47%,
    hsla(0, 0%, 100%, 0.139) 56.5%,
    hsla(0, 0%, 100%, 0.097) 65%,
    hsla(0, 0%, 100%, 0.063) 73%,
    hsla(0, 0%, 100%, 0.038) 80.2%,
    hsla(0, 0%, 100%, 0.021) 86.1%,
    hsla(0, 0%, 100%, 0.011) 91%,
    hsla(0, 0%, 100%, 0.004) 95.2%,
    hsla(0, 0%, 100%, 0.001) 98.2%,
    hsla(0, 0%, 100%, 0) 100%
  );
}

@media (min-width: 768px) {
  .gallery-vignette {
    display: block;
  }
}


.chrome-top-right {
  position: fixed;
  top: 32px;
  right: 24px;
  z-index: 50;
  display: flex;
  align-items: center;
  gap: 8px;
}

@media (min-width: 768px) {
  .chrome-top-right {
    right: 64px;
  }
}

.ghost-btn {
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  padding: 0;
  border: 0;
  border-radius: 9999px;
  background: none;
  color: #a1a1aa;
  cursor: pointer;
  transition: background-color 150ms, color 150ms;
}

.ghost-btn svg {
  width: 20px;
  height: 20px;
}

.ghost-btn:hover {
  color: #52525b;
}

.ghost-btn.is-open {
  background: rgba(24, 24, 27, 0.05);
}

.ghost-btn:focus-visible {
  outline: none;
  box-shadow: 0 0 0 2px #d4d4d8;
}

</style>
