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

    <div data-gallery-no-drag class="chrome-seal">
      <a
        href="/"
        class="seal"
        aria-label="返回首页"
        @click.prevent="goHome"
      >咲</a>
    </div>

    <div data-gallery-no-drag class="chrome-top-right">
      <button
        v-if="canSave"
        ref="shareButtonEl"
        type="button"
        class="ghost-btn"
        :class="{ 'is-open': saveOpen }"
        aria-label="分享"
        aria-haspopup="dialog"
        :aria-expanded="saveOpen"
        @click="saveOpen = !saveOpen"
      >
        <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
          <path
            d="M4 12L20 4L13 20L11 13L4 12Z"
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
      @open-composer="openComposer"
    />

    <!--
      Ignores pointer events itself so the room stays draggable through the gaps
      either side of the bar.
    -->
    <div ref="bottomStackEl" class="bottom-stack">
      <GalleryArtworkBar
        :focused-id="camera.focusedId.value"
        :can-download="canDownload"
        :open-signal="composerOpenSignal"
        @download="onDownload"
        @expanded-change="(open: boolean) => (composerExpanded = open)"
      />
    </div>

    <GalleryThumbstick
      :focused-id="camera.focusedId.value"
      :on-select="camera.selectPainting"
      :on-zoom-by="camera.zoomBy"
      :hide-on-mobile="composerExpanded"
      :paintings="paintings"
    />

    <GallerySaveDialog
      :open="saveOpen"
      :hangs="saveHangs"
      :anchor="shareButtonEl"
      @close="saveOpen = false"
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
import GalleryArtworkBar from '@/components/gallery/GalleryArtworkBar.vue'
import GallerySaveDialog from '@/components/gallery/GallerySaveDialog.vue'
import {
  useGalleryCamera,
  useMeasuredHeight,
} from '@/gallery/useGalleryCamera'
import {
  GALLERY_PAINTINGS,
  type GalleryPainting,
} from '@/gallery/galleryPaintings'
import { downloadImage, generatedImageFilename } from '@/gallery/downloadImage'

/** Last chosen artwork per canvas — restored when the bar reopens. */
export type PaintingSelection = {
  label: string
  imageUrl: string
}

const props = withDefaults(
  defineProps<{
    mode?: 'edit' | 'view'
    /** Preloaded hang images keyed by painting id. */
    initialImageById?: Record<string, string>
    /** Shared gallery display name (view mode). */
    galleryName?: string
    /** Who made the shared gallery (view mode attribution). */
    galleryCreator?: string
  }>(),
  {
    mode: 'edit',
    initialImageById: undefined,
    galleryName: undefined,
    galleryCreator: undefined,
  },
)

const isView = computed(() => props.mode === 'view')

/*
 * The bottom stack covers the foot of the room, and the action bar inside it
 * changes height as it opens, so how much of the focused frame is hidden is
 * something only the rendered bar can answer. The camera drops by whatever this
 * measures rather than by a number picked for one of its states.
 */
const bottomStackEl = ref<HTMLElement | null>(null)
const bottomStack = useMeasuredHeight()

const camera = useGalleryCamera({
  bottomOcclusionPx: () => bottomStack.height.value,
})

const imageById = ref<Record<string, string>>({ ...(props.initialImageById ?? {}) })
const selectionById = ref<Record<string, PaintingSelection>>({})
const composerOpenSignal = ref(0)
const composerExpanded = ref(!isView.value)
const saveOpen = ref(false)
const shareButtonEl = ref<HTMLButtonElement | null>(null)

const paintings = computed<GalleryPainting[]>(() =>
  GALLERY_PAINTINGS.map((painting) => ({
    ...painting,
    imageUrl: imageById.value[painting.id] ?? painting.imageUrl,
  })),
)

const filledHangCount = computed(() => Object.keys(imageById.value).length)
const canSave = computed(() => !isView.value && filledHangCount.value >= 1)

const saveHangs = computed(() =>
  Object.entries(imageById.value).map(([paintingId, imageUrl]) => ({
    paintingId,
    imageUrl,
    label: selectionById.value[paintingId]?.label,
  })),
)

const canDownload = computed(() => Boolean(imageById.value[camera.focusedId.value]))

/** Hang an image chosen from this site's own uploads on the focused canvas. */
function hangArtwork(selection: PaintingSelection) {
  const paintingId = camera.focusedId.value
  imageById.value = { ...imageById.value, [paintingId]: selection.imageUrl }
  selectionById.value = { ...selectionById.value, [paintingId]: selection }
}

function openComposer() {
  composerOpenSignal.value += 1
}

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

function onKeyDown(e: KeyboardEvent) {
  if (e.key === 'Escape' || e.code === 'Escape') {
    e.preventDefault()
    goHome()
  }
}

onMounted(() => {
  bottomStack.setNode(bottomStackEl.value)
  window.addEventListener('keydown', onKeyDown)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKeyDown)
})

defineExpose({ hangArtwork })
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

.chrome-seal {
  position: relative;
  z-index: 50;
}

.seal {
  position: fixed;
  top: 32px;
  left: 24px;
  z-index: 50;
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  border-radius: 4px;
  background: #c8322b;
  color: #fff;
  font-family: var(--font-display, serif);
  font-size: 16px;
  font-weight: 700;
  line-height: 1;
  text-decoration: none;
  cursor: pointer;
  transition: opacity 300ms ease-out, transform 300ms ease-out;
}

.seal:hover {
  opacity: 0.8;
}

@media (min-width: 768px) {
  .seal {
    left: 64px;
    width: 44px;
    height: 44px;
    font-size: 22px;
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

/*
 * Desktop: lift the composer so its vertical center matches the thumbstick
 * (md:bottom-16 + 106px disc → center at 117px). Expanded single-line bar is
 * ~62px tall, so pb = 117 − 31 = 86. Mobile keeps pb-6 — the stick sits above
 * the bar there on purpose.
 */
.bottom-stack {
  pointer-events: none;
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 40;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 0 16px 24px;
}

@media (min-width: 768px) {
  .bottom-stack {
    padding-bottom: 86px;
  }
}
</style>
