/*
 * Ported from michelletliu/michelle-liu (MIT) — src/components/gallery/useGalleryCamera.ts
 * React hooks became Vue composables; the easing, thresholds and guards are
 * unchanged so the walk feels identical to the reference room.
 */
import { computed, onBeforeUnmount, ref, shallowRef, watch, type Ref } from 'vue'
import {
  GALLERY_PAINTINGS,
  GALLERY_ZOOM_DEFAULT,
  GALLERY_ZOOM_MAX,
  GALLERY_ZOOM_MIN,
  GALLERY_ZOOM_STEP,
  adjacentPaintingId,
  clampGalleryZoom,
  easeWithPanel,
  framedRoomPose,
  lerpRoomPose,
  progressForPainting,
  roomPoseForPainting,
  type GalleryFraming,
  type GalleryPainting,
  type GalleryRoomPose,
} from './galleryPaintings'
import { dragPastDeadzone, isGalleryNoDragTarget } from './galleryPointer'

const EASE_MS = 780
/** Zoom steps are small and arrive in bursts, so they settle much faster. */
const ZOOM_EASE_MS = 220
/**
 * The action bar's own expand/collapse duration. Reframing for it is a
 * response to that movement, so it runs on its span and its curve.
 */
const FRAMING_EASE_MS = 320
const SWITCH_THRESHOLD = 48
/** Pinch / ctrl+wheel sensitivity (trackpad reports pinch as wheel+ctrl on macOS). */
const PINCH_ZOOM_SCALE = 0.01

const easeOutCubic = (t: number) => 1 - Math.pow(1 - t, 3)

const prefersReducedMotion = () =>
  typeof window !== 'undefined' &&
  typeof window.matchMedia === 'function' &&
  window.matchMedia('(prefers-reduced-motion: reduce)').matches

/**
 * Guards the ends of the range against float drift, so a control disables
 * exactly when the last step has landed rather than one step late.
 */
const ZOOM_EPSILON = 1e-6

/**
 * Middle back hang: the camera stands square to whichever hang is focused, so
 * only the middle of an end wall opens on the room's centerline — the centered
 * one-point view the entrance is meant to give.
 */
const DEFAULT_FOCUS = 'back-2'

/**
 * Tracks an element's rendered height.
 *
 * The gallery's bottom bar changes height in three ways — folded to its pen,
 * open on the prompt row, open with the results grid above it — and each of
 * those hides a different amount of the room. Measuring is what lets the
 * camera answer the panel that is actually on screen instead of the tallest
 * one it might be.
 */
export function useMeasuredHeight(): {
  setNode: (el: HTMLElement | null) => void
  height: Ref<number>
} {
  const height = ref(0)
  let node: HTMLElement | null = null
  let observer: ResizeObserver | null = null

  const measure = () => {
    if (node) height.value = node.getBoundingClientRect().height
  }

  const setNode = (next: HTMLElement | null) => {
    if (node === next) return
    observer?.disconnect()
    observer = null
    node = next
    if (!node) {
      height.value = 0
      return
    }
    measure()
    if (typeof ResizeObserver === 'undefined') return
    observer = new ResizeObserver(measure)
    observer.observe(node)
  }

  onBeforeUnmount(() => {
    observer?.disconnect()
    observer = null
    node = null
  })

  return { setNode, height }
}

export type GalleryCameraOptions = {
  /**
   * Height in CSS px of the UI band covering the bottom of the viewport. The
   * camera drops by however much of the focused frame that band is hiding.
   */
  bottomOcclusionPx?: () => number
  /** Hang list for focus stepping and poses. Defaults to the blank 12-hang room. */
  paintings?: () => GalleryPainting[]
  /** Initial focused hang id. Defaults to the middle back hang. */
  initialFocusId?: string
  /** Initial zoom factor. Clamped; defaults to GALLERY_ZOOM_DEFAULT. */
  initialZoom?: number
}

export type GalleryCamera = {
  /** Tour progress of the focused hang, in [0, 1]. */
  progress: Ref<number>
  focusedId: Ref<string>
  pose: Ref<GalleryRoomPose>
  /** Current zoom factor, always within [GALLERY_ZOOM_MIN, GALLERY_ZOOM_MAX]. */
  zoom: Ref<number>
  /**
   * True while the camera is easing between hangs after a focus change.
   * Zoom-only and framing reframes leave this false so UI (e.g. plaques)
   * can stay put during those moves.
   */
  isFocusEasing: Ref<boolean>
  /** False once zoom sits at its limit, so a control can dim instead of no-op. */
  canZoomIn: Ref<boolean>
  canZoomOut: Ref<boolean>
  selectPainting: (id: string) => void
  /**
   * Zoom to an absolute factor. Clamped, so any continuous input — a
   * thumbstick, a slider — can push raw values at it every frame.
   */
  setZoom: (zoom: number) => void
  /** Zoom by an increment from wherever zoom currently sits. Clamped too. */
  zoomBy: (delta: number) => void
  /** Bind to the room's root element; the room fills this box. */
  setRootNode: (el: HTMLDivElement | null) => void
  onPointerDown: (e: PointerEvent) => void
  onPointerMove: (e: PointerEvent) => void
  onPointerUp: (e: PointerEvent) => void
  onPointerCancel: (e: PointerEvent) => void
}

export function useGalleryCamera({
  bottomOcclusionPx,
  paintings: paintingsSource,
  initialFocusId,
  initialZoom,
}: GalleryCameraOptions = {}): GalleryCamera {
  const readPaintings = () => paintingsSource?.() ?? GALLERY_PAINTINGS
  const readOcclusion = () => bottomOcclusionPx?.() ?? 0

  const startList = readPaintings()
  const startFocus =
    initialFocusId && startList.some((p) => p.id === initialFocusId)
      ? initialFocusId
      : startList.find((p) => p.id === DEFAULT_FOCUS)?.id ??
        startList[0]?.id ??
        DEFAULT_FOCUS

  const startZoom = clampGalleryZoom(initialZoom ?? GALLERY_ZOOM_DEFAULT)

  const focusedId = ref(startFocus)
  const pose = shallowRef<GalleryRoomPose>(
    roomPoseForPainting(startFocus, startZoom, startList),
  )
  const zoom = ref(startZoom)
  const isFocusEasing = ref(false)
  const viewportHeight = ref(0)
  const viewportWidth = ref(0)

  let rootNode: HTMLDivElement | null = null
  let resizeObserver: ResizeObserver | null = null
  let focusEasing = false
  let pendingDrag = false
  let dragging = false
  let pointerId: number | null = null
  let startX = 0
  let startY = 0
  let lastY = 0
  let switchAccum = 0
  let animFrame: number | null = null

  /**
   * Read live rather than captured. The React original recomputed this object on
   * every render; a getter is the same value at call time.
   */
  const currentFraming = (): GalleryFraming | null =>
    viewportHeight.value > 0
      ? {
          viewportHeightPx: viewportHeight.value,
          viewportWidthPx: viewportWidth.value,
          occlusionPx: readOcclusion(),
        }
      : null

  const cancelEase = () => {
    if (animFrame !== null) {
      cancelAnimationFrame(animFrame)
      animFrame = null
    }
  }

  const setFocusEasing = (next: boolean) => {
    if (focusEasing === next) return
    focusEasing = next
    isFocusEasing.value = next
  }

  const easePoseTo = (
    id: string,
    zoomLevel: number,
    durationMs: number = EASE_MS,
    ease: (t: number) => number = easeOutCubic,
    options?: { focusChange?: boolean },
  ) => {
    cancelEase()
    const focusChange = options?.focusChange === true
    // Zoom / framing eases clear any in-flight focus transition so plaques
    // do not stay hidden after a cancelled hang-to-hang move.
    setFocusEasing(focusChange)

    const from = pose.value
    // Every target the camera is ever given already carries the framing
    // offset, so a reframe blends into a focus change or a zoom rather than
    // arriving as a second animation on top of one.
    const to = framedRoomPose(id, zoomLevel, currentFraming(), readPaintings())

    // Guards the reduced-motion path, where `(now - start) / 0` is Infinity on
    // any later frame but NaN on a frame that lands on the same millisecond —
    // and a NaN pose spreads into the camera matrix and renders nothing at all.
    if (durationMs <= 0) {
      pose.value = to
      if (focusChange) setFocusEasing(false)
      return
    }

    const start = performance.now()

    const tick = (now: number) => {
      const t = Math.min(1, (now - start) / durationMs)
      pose.value = lerpRoomPose(from, to, ease(t))
      if (t < 1) {
        animFrame = requestAnimationFrame(tick)
      } else {
        animFrame = null
        if (focusChange) setFocusEasing(false)
      }
    }
    animFrame = requestAnimationFrame(tick)
  }

  /**
   * The single way zoom ever changes, for the trackpad, the keyboard and any
   * on-screen control alike: clamp first, then dolly the eye to match, because
   * zoom moves the viewer rather than only narrowing the lens.
   */
  const setZoom = (next: number) => {
    const z = clampGalleryZoom(next)
    if (z === zoom.value) return
    zoom.value = z
    easePoseTo(focusedId.value, z, ZOOM_EASE_MS)
  }

  const zoomBy = (delta: number) => setZoom(zoom.value + delta)

  const selectPainting = (id: string) => {
    if (id === focusedId.value) {
      easePoseTo(id, zoom.value)
      return
    }
    focusedId.value = id
    switchAccum = 0
    easePoseTo(id, zoom.value, EASE_MS, easeOutCubic, { focusChange: true })
  }

  const stepFocus = (direction: -1 | 1) => {
    const next = adjacentPaintingId(focusedId.value, direction, readPaintings())
    if (next === focusedId.value) return
    selectPainting(next)
  }

  /** Scroll/drag accumulates until a threshold, then snaps to the next work. */
  const accumulateSwitch = (delta: number) => {
    switchAccum += delta
    if (switchAccum >= SWITCH_THRESHOLD) {
      switchAccum = 0
      stepFocus(1)
    } else if (switchAccum <= -SWITCH_THRESHOLD) {
      switchAccum = 0
      stepFocus(-1)
    }
  }

  const onKeyDown = (e: KeyboardEvent) => {
    /*
     * The room's fields also stop propagation before this listener is reached,
     * so this is the second of two guards. Worth keeping both now that zoom
     * answers to bare punctuation.
     */
    const target = e.target as HTMLElement | null
    if (target?.closest?.('input, textarea, [contenteditable=true]')) return

    const isStep =
      e.key === 'ArrowRight' ||
      e.key === 'ArrowDown' ||
      e.key === 'ArrowLeft' ||
      e.key === 'ArrowUp'

    if (isStep) {
      e.preventDefault()
      // Held arrows auto-repeat ~30x/sec, which fires many steps inside one
      // EASE_MS pose transition — focus races ahead and the camera never
      // arrives at the hangs in between, so they look skipped.
      if (e.repeat) return
      stepFocus(e.key === 'ArrowRight' || e.key === 'ArrowDown' ? 1 : -1)
      return
    }

    /*
     * Zoom: bare + / − / 0, and ⌘/Ctrl + = / − / 0. Claiming the modifier
     * chords stops browser page-zoom from blowing up the HTML chrome while the
     * canvas stays the same size — in the room, those keys mean camera dolly.
     */
    if (e.altKey) return

    const withZoomMod = e.metaKey || e.ctrlKey
    const zoomIn =
      e.key === '+' ||
      e.key === '=' ||
      e.code === 'NumpadAdd' ||
      (withZoomMod && e.code === 'Equal')
    const zoomOut =
      e.key === '-' ||
      e.key === '_' ||
      e.code === 'NumpadSubtract' ||
      (withZoomMod && e.code === 'Minus')
    const zoomReset =
      e.key === '0' || e.code === 'Numpad0' || (withZoomMod && e.code === 'Digit0')

    // Ignore unrelated ⌘/Ctrl chords (copy, reload, etc.).
    if (withZoomMod && !zoomIn && !zoomOut && !zoomReset) return

    if (zoomIn) {
      e.preventDefault()
      zoomBy(GALLERY_ZOOM_STEP)
    } else if (zoomOut) {
      e.preventDefault()
      zoomBy(-GALLERY_ZOOM_STEP)
    } else if (zoomReset) {
      e.preventDefault()
      setZoom(GALLERY_ZOOM_DEFAULT)
    }
  }
  window.addEventListener('keydown', onKeyDown)

  const onWheel = (e: WheelEvent) => {
    e.preventDefault()
    // Trackpad pinch is reported as wheel + ctrlKey in Chromium / Safari.
    if (e.ctrlKey || e.metaKey) {
      zoomBy(-e.deltaY * PINCH_ZOOM_SCALE)
      return
    }
    accumulateSwitch(e.deltaY)
  }

  /** The room fills this element, so its box is the viewport's. */
  const setRootNode = (el: HTMLDivElement | null) => {
    if (rootNode === el) return
    if (rootNode) {
      rootNode.removeEventListener('wheel', onWheel)
      resizeObserver?.disconnect()
      resizeObserver = null
    }
    rootNode = el
    if (!rootNode) return

    rootNode.addEventListener('wheel', onWheel, { passive: false })

    const measure = () => {
      viewportHeight.value = rootNode!.clientHeight
      viewportWidth.value = rootNode!.clientWidth
    }
    measure()
    if (typeof ResizeObserver === 'undefined') {
      window.addEventListener('resize', measure)
    } else {
      resizeObserver = new ResizeObserver(measure)
      resizeObserver.observe(rootNode)
    }
  }

  /*
   * Reframe when the bottom bar or the viewport changes size, and only then.
   *
   * Keyed on the two measurements rather than on the offset they produce, which
   * also moves with focus and zoom — and those already run eases of their own
   * that a second one starting in the same frame would cut short.
   */
  let framingSettled = false
  watch([viewportHeight, viewportWidth, () => readOcclusion()], () => {
    if (!framingSettled) {
      framingSettled = true
      return
    }
    easePoseTo(
      focusedId.value,
      zoom.value,
      prefersReducedMotion() ? 0 : FRAMING_EASE_MS,
      easeWithPanel,
    )
  })

  onBeforeUnmount(() => {
    cancelEase()
    window.removeEventListener('keydown', onKeyDown)
    if (rootNode) {
      rootNode.removeEventListener('wheel', onWheel)
    }
    resizeObserver?.disconnect()
    resizeObserver = null
  })

  const endPointer = (el: HTMLDivElement | null, id: number) => {
    pendingDrag = false
    dragging = false
    pointerId = null
    if (!el) return
    try {
      if (el.hasPointerCapture(id)) {
        el.releasePointerCapture(id)
      }
    } catch {
      /* ignore */
    }
  }

  return {
    progress: computed(() => progressForPainting(focusedId.value, readPaintings())),
    focusedId,
    pose,
    zoom,
    isFocusEasing,
    canZoomIn: computed(() => zoom.value < GALLERY_ZOOM_MAX - ZOOM_EPSILON),
    canZoomOut: computed(() => zoom.value > GALLERY_ZOOM_MIN + ZOOM_EPSILON),
    selectPainting,
    setZoom,
    zoomBy,
    setRootNode,
    onPointerDown: (e: PointerEvent) => {
      if (isGalleryNoDragTarget(e.target)) return
      pendingDrag = true
      dragging = false
      pointerId = e.pointerId
      startX = e.clientX
      startY = e.clientY
      lastY = e.clientY
    },
    onPointerMove: (e: PointerEvent) => {
      if (pointerId !== e.pointerId) return

      if (pendingDrag && !dragging) {
        const dx = e.clientX - startX
        const dy = e.clientY - startY
        if (!dragPastDeadzone(dx, dy)) return
        pendingDrag = false
        dragging = true
        lastY = e.clientY
        ;(e.currentTarget as HTMLDivElement | null)?.setPointerCapture(e.pointerId)
      }

      if (!dragging) return
      const dy = e.clientY - lastY
      lastY = e.clientY
      accumulateSwitch(dy)
    },
    onPointerUp: (e) => {
      endPointer(e.currentTarget as HTMLDivElement | null, e.pointerId)
    },
    onPointerCancel: (e) => {
      endPointer(e.currentTarget as HTMLDivElement | null, e.pointerId)
    },
  }
}
