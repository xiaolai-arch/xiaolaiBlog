<template>
  <div
    data-gallery-no-drag
    data-gallery-keep-bar-open
    class="stick-wrap"
    :class="{ 'stick-wrap--hide-mobile': hideOnMobile }"
  >
    <div
      ref="baseRef"
      role="group"
      aria-label="Thumbstick: drag left or right to move between paintings, up or down to zoom"
      class="stick-base"
      :class="{ 'is-active': active, 'stick-base--mobile-gated': hideOnMobile }"
      :style="{ width: BASE_RADIUS * 2 + 'px', height: BASE_RADIUS * 2 + 'px' }"
      @pointerdown="onPointerDown"
      @pointermove="onPointerMove"
      @pointerup="endDrag"
      @pointercancel="endDrag"
      @lostpointercapture="endDrag"
    >
      <button
        v-for="axis in AXES"
        :key="axis.name"
        type="button"
        :aria-label="axis.label"
        class="stick-axis"
        :style="axisBoxStyle(axis.name)"
        @pointerdown.stop
        @click="axis.onPress()"
      >
        <span
          aria-hidden="true"
          class="stick-glyph"
          :class="`stick-glyph--${axis.name}`"
          :style="axisGlyphStyle(axis.name)"
        >
          <svg
            v-if="axis.name === 'top'"
            class="glyph-cross"
            viewBox="0 0 24 24"
            fill="none"
          >
            <path
              d="M12 2V22M2 12H22"
              stroke="currentColor"
              stroke-width="1.5"
              stroke-linecap="round"
              vector-effect="non-scaling-stroke"
            />
          </svg>
          <svg
            v-else-if="axis.name === 'bottom'"
            class="glyph-cross"
            viewBox="0 0 24 24"
            fill="none"
          >
            <path
              d="M2 12H22"
              stroke="currentColor"
              stroke-width="1.5"
              stroke-linecap="round"
              vector-effect="non-scaling-stroke"
            />
          </svg>
          <svg
            v-else
            class="glyph-chevron"
            viewBox="0 0 24 24"
            fill="none"
          >
            <g :transform="axis.name === 'left' ? 'rotate(180 12 12)' : undefined">
              <path
                d="M9 18L15 12L9 6"
                stroke="currentColor"
                stroke-width="1.5"
                stroke-linecap="round"
                stroke-linejoin="round"
                vector-effect="non-scaling-stroke"
              />
            </g>
          </svg>
        </span>
      </button>

      <div
        ref="knobRef"
        aria-hidden="true"
        class="stick-knob"
        :style="{
          width: KNOB_RADIUS * 2 + 'px',
          height: KNOB_RADIUS * 2 + 'px',
          marginLeft: -KNOB_RADIUS + 'px',
          marginTop: -KNOB_RADIUS + 'px',
        }"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
/*
 * Ported from michelletliu/michelle-liu (MIT) — src/components/gallery/GalleryThumbstick.tsx
 *
 * Roblox-style thumbstick: push left or right to step between hangs, push up or
 * down to zoom, release to spring back. Drag-only, and since the on-screen arrow
 * and zoom buttons were removed this is the only pointer affordance for either.
 * That is acceptable only because the keyboard reaches both independently —
 * arrow keys step between hangs and ⌘/Ctrl with plus, minus or zero drives zoom.
 */
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import {
  GALLERY_PAINTINGS,
  adjacentPaintingId,
  type GalleryPainting,
} from '@/gallery/galleryPaintings'
import {
  NAV_REPEAT_IDLE,
  ZOOM_STEP,
  type NavRepeatState,
  advanceNavRepeat,
  applyDeadZone,
  clampKnob,
  zoomDeltaFor,
} from '@/gallery/thumbstickInput'

const props = withDefaults(
  defineProps<{
    focusedId: string
    onSelect: (id: string) => void
    /**
     * The camera's clamped zoom path, called with a signed delta many times a
     * second. Clamping stays on the camera's side so the stick cannot dolly
     * through a wall or breach the near plane however hard it is pushed.
     */
    onZoomBy: (delta: number) => void
    /**
     * When true, hide on viewports below md (768px) — e.g. while the composer
     * is maximized and would otherwise cover the stick.
     */
    hideOnMobile?: boolean
    /** Hang list for left/right stepping. Defaults to the blank 12-hang room. */
    paintings?: GalleryPainting[]
  }>(),
  { hideOnMobile: false, paintings: undefined },
)

/*
 * Match the collapsed composer actions pill padding (p-1 outside the circular
 * icon buttons). 4px pad + the same wash keeps the stick compact (106px across).
 * +/− share one CSS size so the crossbar and minus stroke match optically;
 * chevrons run larger.
 */
const GLYPH_BOX = 26
const GLYPH_PAD = 4 // composer actions p-1
const KNOB_RADIUS = 19
/** Ring band between knob edge and outer rim — glyphs sit centered in this. */
const RING_BAND = GLYPH_BOX + GLYPH_PAD * 2
/** Base radius from knob + matched ring. */
const BASE_RADIUS = KNOB_RADIUS + RING_BAND
const MAX_TRAVEL = BASE_RADIUS - KNOB_RADIUS - 5

const AXIS_INSET = RING_BAND
const AXIS_OUTSET = 14

type AxisName = 'left' | 'right' | 'top' | 'bottom'

const AXIS_BOX: Record<AxisName, Record<string, string>> = {
  left: { left: -AXIS_OUTSET + 'px', top: AXIS_INSET + 'px', bottom: AXIS_INSET + 'px', width: AXIS_INSET + AXIS_OUTSET + 'px' },
  right: { right: -AXIS_OUTSET + 'px', top: AXIS_INSET + 'px', bottom: AXIS_INSET + 'px', width: AXIS_INSET + AXIS_OUTSET + 'px' },
  top: { top: -AXIS_OUTSET + 'px', left: AXIS_INSET + 'px', right: AXIS_INSET + 'px', height: AXIS_INSET + AXIS_OUTSET + 'px' },
  bottom: { bottom: -AXIS_OUTSET + 'px', left: AXIS_INSET + 'px', right: AXIS_INSET + 'px', height: AXIS_INSET + AXIS_OUTSET + 'px' },
}

const AXIS_GLYPH_INSET: Record<AxisName, Record<string, string>> = {
  left: { right: GLYPH_PAD + 'px' },
  right: { left: GLYPH_PAD + 'px' },
  top: { bottom: GLYPH_PAD + 'px' },
  bottom: { top: GLYPH_PAD + 'px' },
}

const axisBoxStyle = (axis: AxisName) => AXIS_BOX[axis]
const axisGlyphStyle = (axis: AxisName) => ({
  ...AXIS_GLYPH_INSET[axis],
  width: GLYPH_BOX + 'px',
  height: GLYPH_BOX + 'px',
})

const baseRef = ref<HTMLDivElement | null>(null)
const knobRef = ref<HTMLDivElement | null>(null)
const active = ref(false)

/*
 * Everything the animation loop touches lives outside reactive state. The loop
 * outlives any one render, and routing a 60Hz pointer stream through reactivity
 * would re-render the page every frame to move a knob a few pixels.
 */
let pointerId: number | null = null
let vector = { x: 0, y: 0 }
let frame: number | null = null
let lastTick = 0
let nav: NavRepeatState = NAV_REPEAT_IDLE

const paintings = computed(() => props.paintings ?? GALLERY_PAINTINGS)

const AXES = computed(() => [
  {
    name: 'left' as AxisName,
    label: 'Previous painting',
    onPress: () =>
      props.onSelect(adjacentPaintingId(props.focusedId, -1, paintings.value)),
  },
  {
    name: 'right' as AxisName,
    label: 'Next painting',
    onPress: () =>
      props.onSelect(adjacentPaintingId(props.focusedId, 1, paintings.value)),
  },
  { name: 'top' as AxisName, label: 'Zoom in', onPress: () => props.onZoomBy(ZOOM_STEP) },
  { name: 'bottom' as AxisName, label: 'Zoom out', onPress: () => props.onZoomBy(-ZOOM_STEP) },
])

function moveKnob(x: number, y: number, animate: boolean) {
  const knob = knobRef.value
  if (!knob) return
  knob.style.transition = animate
    ? 'transform 220ms cubic-bezier(0.22, 1, 0.36, 1)'
    : 'none'
  knob.style.transform = `translate3d(${x}px, ${y}px, 0)`
}

/**
 * Return to rest.
 *
 * Wired to release, cancel, lost capture, window blur and tab hide alike. Each
 * of those on its own leaves a path where the pointer is gone but the loop keeps
 * walking through paintings, and a control that carries on navigating after you
 * let go is the one failure this must not have.
 */
function endDrag() {
  if (frame !== null) {
    cancelAnimationFrame(frame)
    frame = null
  }
  vector = { x: 0, y: 0 }
  nav = NAV_REPEAT_IDLE

  const base = baseRef.value
  const id = pointerId
  if (base && id !== null) {
    try {
      if (base.hasPointerCapture(id)) base.releasePointerCapture(id)
    } catch {
      /* the pointer is already gone; nothing to release */
    }
  }
  pointerId = null
  moveKnob(0, 0, true)
  active.value = false
}

const tick = (now: number) => {
  // Clamped so a backgrounded tab resuming after seconds does not cash in one
  // enormous frame as a burst of navigation.
  const deltaMs = Math.min(now - lastTick, 100)
  lastTick = now
  const { x, y } = vector

  if (y !== 0) props.onZoomBy(zoomDeltaFor(y, deltaMs))

  const advanced = advanceNavRepeat(nav, x, deltaMs)
  nav = advanced.state
  if (advanced.step !== 0) {
    props.onSelect(
      adjacentPaintingId(props.focusedId, advanced.step, paintings.value),
    )
  }

  frame = requestAnimationFrame(tick)
}

function onPointerDown(e: PointerEvent) {
  if (pointerId !== null) return
  pointerId = e.pointerId
  // Capture so the drag survives the pointer leaving the base, which it will
  // constantly: the whole gesture is about pushing past the edge.
  ;(e.currentTarget as HTMLDivElement).setPointerCapture(e.pointerId)
  active.value = true
  moveKnob(0, 0, false)
  lastTick = performance.now()
  frame = requestAnimationFrame(tick)
}

function onPointerMove(e: PointerEvent) {
  if (pointerId !== e.pointerId) return
  const rect = (e.currentTarget as HTMLDivElement).getBoundingClientRect()
  const knob = clampKnob(
    e.clientX - (rect.left + rect.width / 2),
    e.clientY - (rect.top + rect.height / 2),
    MAX_TRAVEL,
  )
  moveKnob(knob.x, knob.y, false)
  vector = {
    x: applyDeadZone(knob.x / MAX_TRAVEL),
    y: applyDeadZone(knob.y / MAX_TRAVEL),
  }
}

const onWindowBlur = () => endDrag()
const onVisibilityChange = () => {
  if (document.hidden) endDrag()
}
window.addEventListener('blur', onWindowBlur)
document.addEventListener('visibilitychange', onVisibilityChange)

watch(
  () => props.hideOnMobile,
  (hidden) => {
    if (hidden) endDrag()
  },
)

onBeforeUnmount(() => {
  endDrag()
  window.removeEventListener('blur', onWindowBlur)
  document.removeEventListener('visibilitychange', onVisibilityChange)
})
</script>

<style scoped>
/*
 * Right edge on wide screens, and never a right-hand corner: the info button
 * holds the top one. Wide, the stick sits in its corner squarely at the same
 * 4rem inset the info button and the logo already use; narrow, it becomes
 * nearby room navigation just above the composer.
 */
.stick-wrap {
  pointer-events: none;
  position: absolute;
  right: 24px;
  bottom: calc(env(safe-area-inset-bottom) + 7.25rem);
  z-index: 30;
  transition: opacity 200ms ease-out;
}

.stick-wrap--hide-mobile {
  opacity: 0;
}

@media (min-width: 768px) {
  .stick-wrap {
    right: 64px;
    bottom: 64px;
  }

  .stick-wrap--hide-mobile {
    opacity: 1;
  }
}

@media (prefers-reduced-motion: reduce) {
  .stick-wrap {
    transition: none;
  }
}

.stick-base {
  position: relative;
  /* wrap 是 pointer-events: none 的穿透层，.pad 自己必须恢复接收事件，
     否则 hideOnMobile=false（选画器收起）时整个摇杆继承 none 而失灵 */
  pointer-events: auto;
  touch-action: none;
  user-select: none;
  border-radius: 9999px;
  border: 1px solid rgba(0, 0, 0, 0.1);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  backdrop-filter: blur(12px);
  background: rgba(255, 255, 255, 0.6);
  transition: background-color 150ms;
}

.stick-base.is-active {
  background: rgba(255, 255, 255, 0.85);
}

.stick-base--mobile-gated {
  pointer-events: none;
}

@media (min-width: 768px) {
  .stick-base--mobile-gated {
    pointer-events: auto;
  }
}

@media (prefers-reduced-motion: reduce) {
  .stick-base {
    transition: none;
  }
}

.stick-base:focus-visible {
  outline: none;
  box-shadow:
    0 8px 24px rgba(0, 0, 0, 0.12),
    0 0 0 2px #d4d4d8;
}

/*
 * The hit area is invisible and larger than the mark; the wash on hover is
 * sized to the glyph instead, so the feedback lands where the eye is rather
 * than exposing how far the target really reaches.
 */
.stick-axis {
  position: absolute;
  padding: 0;
  border: 0;
  background: none;
  cursor: pointer;
}

.stick-axis:focus-visible {
  outline: none;
}

.stick-glyph {
  position: absolute;
  display: grid;
  place-items: center;
  border-radius: 9999px;
  color: #a1a1aa;
  transition: color 150ms, background-color 150ms;
}

.stick-glyph--left,
.stick-glyph--right {
  top: 50%;
  transform: translateY(-50%);
}

.stick-glyph--top,
.stick-glyph--bottom {
  left: 50%;
  transform: translateX(-50%);
}

.stick-axis:hover .stick-glyph {
  background: rgba(24, 24, 27, 0.06);
  color: #52525b;
}

.stick-axis:active .stick-glyph {
  background: rgba(24, 24, 27, 0.12);
}

.stick-axis:focus-visible .stick-glyph {
  box-shadow: 0 0 0 2px #d4d4d8;
}

.glyph-cross {
  display: block;
  width: 13px;
  height: 13px;
  flex-shrink: 0;
}

.glyph-chevron {
  display: block;
  width: 20px;
  height: 20px;
  flex-shrink: 0;
}

@media (prefers-reduced-motion: reduce) {
  .stick-glyph {
    transition: none;
  }
}

.stick-knob {
  position: absolute;
  left: 50%;
  top: 50%;
  border-radius: 9999px;
  border: 1px solid rgba(0, 0, 0, 0.1);
  background: #fff;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.16);
}
</style>
