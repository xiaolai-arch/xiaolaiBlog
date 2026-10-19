<template>
  <div class="gallery-room">
    <div ref="mountEl" class="gallery-room-mount" aria-label="3D gallery room" />
  </div>
</template>

<script setup lang="ts">
/*
 * Vue wrapper around the ported three.js room (createGalleryScene). It owns the
 * WebGL lifetime and pushes prop changes into the scene handle; all the room
 * geometry, materials and the render loop live in galleryScene.ts.
 */
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { createGalleryScene, type GallerySceneHandle } from '@/gallery/galleryScene'
import type { GalleryPainting, GalleryRoomPose } from '@/gallery/galleryPaintings'
import type {
  GalleryFrameFit,
  GalleryFrameStyle,
} from '@/gallery/galleryFrameGeometry'
import type { ShimmerHues } from '@/gallery/shimmerPalette'

const props = withDefaults(
  defineProps<{
    pose: GalleryRoomPose
    focusedId: string
    zoom?: number
    paintings?: GalleryPainting[]
    generatingIds?: ReadonlySet<string>
    shimmerHuesById?: Record<string, ShimmerHues | null>
    imageFit?: GalleryFrameFit
    frameStyle?: GalleryFrameStyle
  }>(),
  {
    zoom: 1,
    paintings: undefined,
    generatingIds: undefined,
    shimmerHuesById: undefined,
    imageFit: 'cover',
    frameStyle: 'dark',
  },
)

const emit = defineEmits<{
  (e: 'select-painting', id: string): void
  (e: 'open-composer'): void
  (e: 'hang-texture-load', id: string): void
}>()

const mountEl = ref<HTMLElement | null>(null)
let scene: GallerySceneHandle | null = null

onMounted(() => {
  if (!mountEl.value) return
  scene = createGalleryScene(mountEl.value, {
    pose: props.pose,
    zoom: props.zoom,
    focusedId: props.focusedId,
    paintings: props.paintings,
    generatingIds: props.generatingIds,
    shimmerHuesById: props.shimmerHuesById,
    imageFit: props.imageFit,
    frameStyle: props.frameStyle,
    onSelectPainting: (id) => emit('select-painting', id),
    onOpenComposer: () => emit('open-composer'),
    onHangTextureLoad: (id) => emit('hang-texture-load', id),
  })
})

watch(
  () => props.pose,
  (next) => scene?.setPose(next),
)
watch(
  () => props.zoom,
  (next) => scene?.setZoom(next ?? 1),
)
watch(
  () => props.focusedId,
  (next) => scene?.setFocusedId(next),
)
watch(
  () => props.paintings,
  (next) => scene?.setPaintings(next ?? []),
)
watch(
  () => props.generatingIds,
  (next) => scene?.setGeneratingIds(next ?? new Set()),
)
watch(
  () => props.shimmerHuesById,
  (next) => scene?.setShimmerHues(next ?? {}),
)

onBeforeUnmount(() => {
  scene?.dispose()
  scene = null
})
</script>

<style scoped>
.gallery-room {
  position: absolute;
  inset: 0;
  z-index: 10;
  height: 100%;
  width: 100%;
}

.gallery-room-mount {
  position: absolute;
  inset: 0;
  height: 100%;
  width: 100%;
}
</style>
