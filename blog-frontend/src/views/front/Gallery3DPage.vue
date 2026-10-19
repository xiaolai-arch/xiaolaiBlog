<template>
  <div class="gx-page">
    <div ref="mountRef" class="gx-canvas-wrap"></div>

    <!-- 暗角 -->
    <div class="gx-vignette" aria-hidden="true"></div>

    <!-- 左上圆形返回 -->
    <router-link to="/" class="gx-logo-back" aria-label="返回首页">咲</router-link>

    <!-- 顶部提示 -->
    <div class="gx-hint">{{ isMobile ? '拖动环视 · 按住按钮移动 · 点画欣赏' : '拖动环视 · WASD / 方向键移动 · 滚轮变焦 · 点击画作欣赏' }}</div>

    <!-- 胶片式加载 -->
    <div v-if="loading" class="gx-loading">
      <p class="gx-loading-text">{{ phrase }}</p>
    </div>

    <!-- 底部操作条 -->
    <div class="gx-action-bar">
      <button class="gx-btn" @click="reshuffle">换一批画作</button>
      <button class="gx-btn" @click="infoOpen = !infoOpen">说明</button>
    </div>

    <!-- 说明气泡 -->
    <Transition name="gx-pop">
      <div v-if="infoOpen" class="gx-info-pop">
        <p><strong>3D 美术馆</strong> — 墙上是大都会艺术博物馆 Open Access（CC0）公有领域名画。灵感与布局参考 michelle-liu 的 gallery（MIT）。</p>
        <button class="gx-pop-close" @click="infoOpen = false">知道了</button>
      </div>
    </Transition>

    <!-- 画作信息卡 -->
    <Transition name="gx-card">
      <div v-if="focused" class="gx-plaque-card">
        <h3>{{ focused.title }}</h3>
        <p class="gx-artist">{{ focused.artist }}</p>
        <p class="gx-meta">{{ focused.date }}<template v-if="focused.medium"> · {{ focused.medium }}</template></p>
        <button class="gx-close" @click="unfocus">继续逛展 →</button>
      </div>
    </Transition>

    <!-- 移动端方向键 -->
    <div v-if="isMobile" class="gx-move-btns">
      <button class="gx-move-btn mv-w" data-dir="w" @pointerdown.prevent="onMoveBtn('w', true)" @pointerup.prevent="onMoveBtn('w', false)" @pointerleave="onMoveBtn('w', false)">▲</button>
      <button class="gx-move-btn mv-a" data-dir="a" @pointerdown.prevent="onMoveBtn('a', true)" @pointerup.prevent="onMoveBtn('a', false)" @pointerleave="onMoveBtn('a', false)">◀</button>
      <button class="gx-move-btn mv-s" data-dir="s" @pointerdown.prevent="onMoveBtn('s', true)" @pointerup.prevent="onMoveBtn('s', false)" @pointerleave="onMoveBtn('s', false)">▼</button>
      <button class="gx-move-btn mv-d" data-dir="d" @pointerdown.prevent="onMoveBtn('d', true)" @pointerup.prevent="onMoveBtn('d', false)" @pointerleave="onMoveBtn('d', false)">▶</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import * as THREE from 'three'

/* ============ 原站规格（galleryPaintings.ts）============ */
const ROOM = { w: 11, d: 12, h: 4.2, eyeY: 2.1, frameInset: 0.04, sideSpan: 6.4, endSpan: 6.6 }
const PORTRAIT = { w: 1.1625, h: 1.55 } // 3:4
const LANDSCAPE = { w: 1.98, h: 1.32 } // 3:2
const LIP = 0.035
const MAT = 0.09
const FRAME_DEPTH = 0.055
const PLAQUE_GAP = 0.12

interface Artwork {
  file: string
  title: string
  artist: string
  date: string
  medium: string
  w: number
  h: number
  ratio: number
}

interface Hang {
  wall: 'left' | 'back' | 'right' | 'front'
  slot: number
  pos: THREE.Vector3
  ry: number
  normal: THREE.Vector3
  size: { w: number; h: number }
}

const mountRef = ref<HTMLElement | null>(null)
const loading = ref(true)
const focused = ref<{ title: string; artist: string; date: string; medium: string } | null>(null)
const infoOpen = ref(false)
const isMobile = window.matchMedia('(max-width: 768px)').matches

const phrases = ['正在布展…', '调整画框高度…', '打开展柜灯…', '校准铭牌…', '欢迎光临']
const phrase = ref(phrases[0])

let renderer: THREE.WebGLRenderer | null = null
let scene: THREE.Scene
let camera: THREE.PerspectiveCamera
let raf = 0
let disposed = false
let artworks: Artwork[] = []

/* 漫游状态 */
const keys: Record<string, boolean> = {}
const moveButtons: Record<string, boolean> = {}
let yaw = 0
let pitch = 0
let dragging = false
let lastX = 0
let lastY = 0
const homePos = new THREE.Vector3(0, ROOM.eyeY, ROOM.d / 2 - 2.2)

/* 聚焦 */
let focusAnim: { from: THREE.Vector3; to: THREE.Vector3; fromYaw: number; toYaw: number; fromPitch: number; toPitch: number; start: number } | null = null
const raycaster = new THREE.Raycaster()
const hits: { mesh: THREE.Mesh; art: Artwork; stand: THREE.Vector3; look: THREE.Vector3 }[] = []
let exhibitGroup: THREE.Group | null = null

/* ============ 挂位布局（hangCentersEqualGap 同款算法）============ */
function equalGapCenters(outerWidths: number[], wallLength: number, sign: 1 | -1): number[] {
  const n = outerWidths.length
  if (n === 0) return []
  if (n === 1) return [0]
  const sumW = outerWidths.reduce((a, b) => a + b, 0)
  const gap = Math.max(0.08, (wallLength - sumW) / (n + 1))
  const centers: number[] = []
  if (sign === 1) {
    let edge = -wallLength / 2 + gap
    for (const w of outerWidths) {
      centers.push(edge + w / 2)
      edge += w + gap
    }
  } else {
    let edge = wallLength / 2 - gap
    for (const w of outerWidths) {
      centers.push(edge - w / 2)
      edge -= w + gap
    }
  }
  return centers
}

function planHangs(portraits: Artwork[], landscapes: Artwork[]): Hang[] {
  const hangs: Hang[] = []
  const wallCfg = [
    { wall: 'left' as const, span: ROOM.sideSpan, sign: -1 as const, size: PORTRAIT, arts: portraits.slice(0, 3) },
    { wall: 'back' as const, span: ROOM.endSpan, sign: 1 as const, size: LANDSCAPE, arts: landscapes.slice(0, 3) },
    { wall: 'right' as const, span: ROOM.sideSpan, sign: 1 as const, size: PORTRAIT, arts: portraits.slice(3, 6) },
    { wall: 'front' as const, span: ROOM.endSpan, sign: -1 as const, size: LANDSCAPE, arts: landscapes.slice(3, 6) },
  ]
  for (const cfg of wallCfg) {
    const outerW = cfg.arts.map(() => cfg.size.w + 2 * (LIP + MAT))
    const centers = equalGapCenters(outerW, cfg.span, cfg.sign)
    cfg.arts.forEach((_, i) => {
      const c = centers[i]
      let pos: THREE.Vector3
      let ry: number
      let normal: THREE.Vector3
      if (cfg.wall === 'left') { pos = new THREE.Vector3(-ROOM.w / 2 + ROOM.frameInset, ROOM.eyeY, c); ry = Math.PI / 2; normal = new THREE.Vector3(1, 0, 0) }
      else if (cfg.wall === 'right') { pos = new THREE.Vector3(ROOM.w / 2 - ROOM.frameInset, ROOM.eyeY, c); ry = -Math.PI / 2; normal = new THREE.Vector3(-1, 0, 0) }
      else if (cfg.wall === 'back') { pos = new THREE.Vector3(c, ROOM.eyeY, -ROOM.d / 2 + ROOM.frameInset); ry = 0; normal = new THREE.Vector3(0, 0, 1) }
      else { pos = new THREE.Vector3(c, ROOM.eyeY, ROOM.d / 2 - ROOM.frameInset); ry = Math.PI; normal = new THREE.Vector3(0, 0, -1) }
      hangs.push({ wall: cfg.wall, slot: i, pos, ry, normal, size: cfg.size })
    })
  }
  return hangs
}

/* ============ 房间 ============ */
function buildRoom() {
  // 地板：暖木
  const fc = document.createElement('canvas')
  fc.width = 512
  fc.height = 512
  const fx = fc.getContext('2d')!
  fx.fillStyle = '#cfc4b0'
  fx.fillRect(0, 0, 512, 512)
  for (let i = 0; i < 8; i++) {
    fx.fillStyle = i % 2 ? '#c9bda8' : '#d4c9b6'
    fx.fillRect(0, i * 64, 512, 61)
    fx.strokeStyle = 'rgba(90,75,55,0.18)'
    fx.beginPath()
    fx.moveTo(0, i * 64)
    fx.lineTo(512, i * 64)
    fx.stroke()
    // 木纹丝
    for (let s = 0; s < 24; s++) {
      fx.strokeStyle = 'rgba(120,100,70,0.06)'
      fx.beginPath()
      const y = i * 64 + Math.random() * 61
      fx.moveTo(0, y)
      fx.bezierCurveTo(170, y + (Math.random() - 0.5) * 6, 340, y + (Math.random() - 0.5) * 6, 512, y)
      fx.stroke()
    }
  }
  const floorTex = new THREE.CanvasTexture(fc)
  floorTex.wrapS = floorTex.wrapT = THREE.RepeatWrapping
  floorTex.repeat.set(3, 3.3)
  floorTex.colorSpace = THREE.SRGBColorSpace
  const floor = new THREE.Mesh(new THREE.PlaneGeometry(ROOM.w, ROOM.d), new THREE.MeshStandardMaterial({ map: floorTex, roughness: 0.85 }))
  floor.rotation.x = -Math.PI / 2
  scene.add(floor)

  // 墙与天花
  const wallMat = new THREE.MeshStandardMaterial({ color: 0xf6f5f1, roughness: 1 })
  const mkWall = (w: number, h: number, pos: THREE.Vector3, ry: number) => {
    const m = new THREE.Mesh(new THREE.PlaneGeometry(w, h), wallMat)
    m.position.copy(pos)
    m.rotation.y = ry
    scene.add(m)
  }
  mkWall(ROOM.w, ROOM.h, new THREE.Vector3(0, ROOM.h / 2, -ROOM.d / 2), 0)
  mkWall(ROOM.w, ROOM.h, new THREE.Vector3(0, ROOM.h / 2, ROOM.d / 2), Math.PI)
  mkWall(ROOM.d, ROOM.h, new THREE.Vector3(-ROOM.w / 2, ROOM.h / 2, 0), Math.PI / 2)
  mkWall(ROOM.d, ROOM.h, new THREE.Vector3(ROOM.w / 2, ROOM.h / 2, 0), -Math.PI / 2)
  const ceil = new THREE.Mesh(new THREE.PlaneGeometry(ROOM.w, ROOM.d), new THREE.MeshStandardMaterial({ color: 0xf8f8f5, roughness: 1 }))
  ceil.rotation.x = Math.PI / 2
  ceil.position.y = ROOM.h
  scene.add(ceil)

  // 踢脚线 + 顶角线
  const trimMat = new THREE.MeshStandardMaterial({ color: 0xfefdf9, roughness: 0.9 })
  const mkTrim = (len: number, pos: THREE.Vector3, ry: number, hh: number, yy: number) => {
    const m = new THREE.Mesh(new THREE.BoxGeometry(len, hh, 0.05), trimMat)
    m.position.copy(pos)
    m.position.y = yy
    m.rotation.y = ry
    scene.add(m)
  }
  for (const [wall, len] of [['back', ROOM.w], ['front', ROOM.w], ['left', ROOM.d], ['right', ROOM.d]] as const) {
    const ry = wall === 'back' ? 0 : wall === 'front' ? Math.PI : wall === 'left' ? Math.PI / 2 : -Math.PI / 2
    const pos = wall === 'back' ? new THREE.Vector3(0, 0, -ROOM.d / 2 + 0.03)
      : wall === 'front' ? new THREE.Vector3(0, 0, ROOM.d / 2 - 0.03)
      : wall === 'left' ? new THREE.Vector3(-ROOM.w / 2 + 0.03, 0, 0)
      : new THREE.Vector3(ROOM.w / 2 - 0.03, 0, 0)
    mkTrim(len, pos, ry, 0.14, 0.07)
    mkTrim(len, pos, ry, 0.1, ROOM.h - 0.05)
  }

  // 天花筒灯（网格发光点）
  const dotMat = new THREE.MeshBasicMaterial({ color: 0xfff6e6 })
  for (let gx = -1; gx <= 1; gx++) {
    for (let gz = -2; gz <= 2; gz++) {
      const dot = new THREE.Mesh(new THREE.CircleGeometry(0.11, 24), dotMat)
      dot.rotation.x = Math.PI / 2
      dot.position.set(gx * 3.2, ROOM.h - 0.01, gz * 2.6)
      scene.add(dot)
      const ring = new THREE.Mesh(new THREE.RingGeometry(0.13, 0.17, 24), new THREE.MeshStandardMaterial({ color: 0xeeece5, roughness: 1 }))
      ring.rotation.x = Math.PI / 2
      ring.position.set(gx * 3.2, ROOM.h - 0.005, gz * 2.6)
      scene.add(ring)
    }
  }
}

/* ============ 布展 ============ */
async function loadArtworks(): Promise<Artwork[]> {
  try {
    const res = await fetch('/met-art/manifest.json')
    const list = (await res.json()) as Artwork[]
    if (Array.isArray(list) && list.length) return list
  } catch (_) {}
  return []
}

function plaqueTexture(title: string, artist: string): THREE.CanvasTexture {
  const c = document.createElement('canvas')
  c.width = 512
  c.height = 160
  const x = c.getContext('2d')!
  x.fillStyle = '#fbfaf6'
  x.fillRect(0, 0, 512, 160)
  x.strokeStyle = '#ddd9cf'
  x.lineWidth = 3
  x.strokeRect(5, 5, 502, 150)
  x.fillStyle = '#26262b'
  x.font = '600 34px "DM Sans", sans-serif'
  x.textAlign = 'center'
  const t = title.length > 26 ? title.slice(0, 25) + '…' : title
  x.fillText(t, 256, 62)
  x.fillStyle = '#8e8e96'
  x.font = '28px "DM Sans", sans-serif'
  const a = artist.length > 30 ? artist.slice(0, 29) + '…' : artist
  x.fillText(a, 256, 108)
  const tex = new THREE.CanvasTexture(c)
  tex.colorSpace = THREE.SRGBColorSpace
  return tex
}

async function hangExhibition() {
  // 清掉旧展
  if (exhibitGroup) {
    scene.remove(exhibitGroup)
    exhibitGroup.traverse((o) => {
      const m = o as THREE.Mesh
      if (m.geometry) m.geometry.dispose()
    })
    exhibitGroup = null
  }
  hits.length = 0
  focused.value = null
  focusAnim = null

  // 选画：竖画 6 + 横画 6（从 24 幅中轮换）
  const portraits = artworks.filter((a) => a.ratio < 1.05)
  const landscapes = artworks.filter((a) => a.ratio >= 1.05)
  shuffleSeed++
  const rot = (arr: Artwork[], n: number) => {
    const s = shuffleSeed % arr.length
    return [...arr.slice(s), ...arr.slice(0, s)].slice(0, n)
  }
  const chosenP = rot(portraits, 6)
  const chosenL = rot(landscapes, 6)
  const hangs = planHangs(chosenP, chosenL)
  const arts = [...chosenP.slice(0, 3), ...chosenL.slice(0, 3), ...chosenP.slice(3, 6), ...chosenL.slice(3, 6)]

  exhibitGroup = new THREE.Group()
  const loader = new THREE.TextureLoader()
  const load = (url: string) =>
    new Promise<THREE.Texture | null>((r) => loader.load(url, (t) => r(t), undefined, () => r(null)))

  for (let i = 0; i < hangs.length; i++) {
    const hg = hangs[i]
    const art = arts[i]
    const tex = await load('/met-art/' + art.file)
    if (!tex) continue
    tex.colorSpace = THREE.SRGBColorSpace
    addFrame(exhibitGroup!, hg, tex, art)
  }
  scene.add(exhibitGroup!)
}

let shuffleSeed = 0

function addFrame(group: THREE.Group, hg: Hang, tex: THREE.Texture, art: Artwork) {
  const g = new THREE.Group()
  g.position.copy(hg.pos)
  g.rotation.y = hg.ry

  const apertureW = hg.size.w
  const apertureH = hg.size.h
  const outerW = apertureW + 2 * (LIP + MAT)
  const outerH = apertureH + 2 * (LIP + MAT)

  // 深色外框（lip）
  const lipMat = new THREE.MeshStandardMaterial({ color: 0x232327, roughness: 0.45, metalness: 0.2 })
  const mkBar = (w: number, h: number, x: number, y: number) => {
    const m = new THREE.Mesh(new THREE.BoxGeometry(w, h, FRAME_DEPTH), lipMat)
    m.position.set(x, y, 0)
    g.add(m)
  }
  mkBar(outerW, LIP, 0, apertureH / 2 + MAT + LIP / 2)
  mkBar(outerW, LIP, 0, -apertureH / 2 - MAT - LIP / 2)
  mkBar(LIP, apertureH + 2 * MAT, -outerW / 2 + LIP / 2, 0)
  mkBar(LIP, apertureH + 2 * MAT, outerW / 2 - LIP / 2, 0)

  // 白卡纸
  const matMesh = new THREE.Mesh(
    new THREE.PlaneGeometry(apertureW + 2 * MAT, apertureH + 2 * MAT),
    new THREE.MeshStandardMaterial({ color: 0xfefefa, roughness: 1 })
  )
  matMesh.position.z = FRAME_DEPTH / 2 - 0.004
  g.add(matMesh)

  // 画作（contain 进光圈，白边补齐）
  const imgRatio = art.w / art.h
  const slotRatio = apertureW / apertureH
  let pw = apertureW
  let ph = apertureH
  if (imgRatio > slotRatio) ph = apertureW / imgRatio
  else pw = apertureH * imgRatio
  const artMesh = new THREE.Mesh(
    new THREE.PlaneGeometry(pw, ph),
    new THREE.MeshStandardMaterial({ map: tex, roughness: 0.9, emissive: 0xffffff, emissiveMap: tex, emissiveIntensity: 0.06 })
  )
  artMesh.position.z = FRAME_DEPTH / 2 + 0.002
  g.add(artMesh)

  // 铭牌
  const plaque = new THREE.Mesh(
    new THREE.PlaneGeometry(0.5, 0.156),
    new THREE.MeshStandardMaterial({ map: plaqueTexture(art.title, art.artist), roughness: 0.85 })
  )
  plaque.position.set(0, -outerH / 2 - PLAQUE_GAP - 0.07, 0.045)
  g.add(plaque)

  group.add(g)

  // 点击热区（贴墙外一点）
  const hitMesh = new THREE.Mesh(
    new THREE.PlaneGeometry(outerW, outerH),
    new THREE.MeshBasicMaterial({ transparent: true, opacity: 0 })
  )
  hitMesh.position.copy(hg.pos).add(hg.normal.clone().multiplyScalar(0.02))
  hitMesh.rotation.y = hg.ry
  hitMesh.userData.hitIndex = hits.length
  group.add(hitMesh)

  // 聚焦站位
  const longEdge = Math.max(outerW, outerH)
  const stand = hg.pos.clone().add(hg.normal.clone().multiplyScalar(Math.max(1.5, longEdge * 1.15)))
  stand.y = ROOM.eyeY
  hits.push({ mesh: hitMesh, art, stand, look: hg.pos.clone() })
}

let reshufflePending = false
async function reshuffle() {
  if (reshufflePending || loading.value) return
  reshufflePending = true
  loading.value = true
  phrase.value = '正在换展…'
  await hangExhibition()
  loading.value = false
  reshufflePending = false
}

/* ============ 交互 ============ */
function onKeyDown(e: KeyboardEvent) {
  const t = e.target as HTMLElement | null
  if (t?.closest?.('input, textarea')) return
  keys[e.key.toLowerCase()] = true
  if (['arrowup', 'arrowdown', 'arrowleft', 'arrowright'].includes(e.key.toLowerCase())) e.preventDefault()
  if (e.key === 'Escape') unfocus()
}
function onKeyUp(e: KeyboardEvent) {
  keys[e.key.toLowerCase()] = false
}
function onMoveBtn(d: string, on: boolean) {
  moveButtons[d] = on
}

function bindPointer(el: HTMLElement) {
  el.addEventListener('pointerdown', (e) => {
    dragging = true
    lastX = e.clientX
    lastY = e.clientY
    el.setPointerCapture(e.pointerId)
  })
  el.addEventListener('pointermove', (e) => {
    if (!dragging) {
      hoverCheck(e)
      return
    }
    yaw -= (e.clientX - lastX) * 0.0042
    pitch -= (e.clientY - lastY) * 0.0042
    pitch = Math.max(-1.2, Math.min(1.2, pitch))
    lastX = e.clientX
    lastY = e.clientY
  })
  el.addEventListener('pointerup', (e) => {
    const moved = Math.abs(e.clientX - lastX) + Math.abs(e.clientY - lastY)
    dragging = false
    if (moved < 6) clickCheck(e)
  })
  el.addEventListener('wheel', (e) => {
    e.preventDefault()
    camera.fov = Math.max(32, Math.min(75, camera.fov + e.deltaY * 0.03))
    camera.updateProjectionMatrix()
  }, { passive: false })
}

function pickAt(e: Event) {
  const el = renderer!.domElement
  const rect = el.getBoundingClientRect()
  const ndc = new THREE.Vector2(
    ((e as PointerEvent).clientX - rect.left) / rect.width * 2 - 1,
    -(((e as PointerEvent).clientY - rect.top) / rect.height) * 2 + 1
  )
  raycaster.setFromCamera(ndc, camera)
  const list = raycaster.intersectObjects(hits.map((h) => h.mesh))
  if (!list.length) return null
  const mesh = list[0].object as THREE.Mesh
  return hits[mesh.userData.hitIndex as number]
}

function hoverCheck(e: Event) {
  const hit = pickAt(e)
  renderer!.domElement.style.cursor = hit ? 'pointer' : 'grab'
}

function clickCheck(e: Event) {
  const hit = pickAt(e)
  if (!hit) {
    unfocus()
    return
  }
  focusAnim = {
    from: camera.position.clone(),
    to: hit.stand,
    fromYaw: yaw,
    toYaw: Math.atan2(hit.look.x - hit.stand.x, hit.look.z - hit.stand.z),
    fromPitch: pitch,
    toPitch: 0,
    start: performance.now(),
  }
  focused.value = { title: hit.art.title, artist: hit.art.artist, date: hit.art.date, medium: hit.art.medium }
}

function unfocus() {
  if (!focused.value && !focusAnim) return
  focusAnim = {
    from: camera.position.clone(),
    to: homePos.clone(),
    fromYaw: yaw,
    toYaw: 0,
    fromPitch: pitch,
    toPitch: 0,
    start: performance.now(),
  }
  focused.value = null
}

/* ============ 循环 ============ */
function animate() {
  if (disposed) return
  raf = requestAnimationFrame(animate)

  if (focusAnim) {
    const t = Math.min(1, (performance.now() - focusAnim.start) / 700)
    const ease = 1 - Math.pow(1 - t, 3)
    camera.position.lerpVectors(focusAnim.from, focusAnim.to, ease)
    yaw = focusAnim.fromYaw + (focusAnim.toYaw - focusAnim.fromYaw) * ease
    pitch = focusAnim.fromPitch + (focusAnim.toPitch - focusAnim.fromPitch) * ease
    if (t >= 1) focusAnim = null
  } else {
    const fwd = (keys['w'] || keys['arrowup'] || moveButtons['w'] ? 1 : 0) - (keys['s'] || keys['arrowdown'] || moveButtons['s'] ? 1 : 0)
    const strafe = (keys['d'] || keys['arrowright'] || moveButtons['d'] ? 1 : 0) - (keys['a'] || keys['arrowleft'] || moveButtons['a'] ? 1 : 0)
    if (fwd || strafe) {
      const speed = 3.2 / 60
      const dir = new THREE.Vector3(-Math.sin(yaw) * fwd + Math.cos(yaw) * strafe, 0, -Math.cos(yaw) * fwd - Math.sin(yaw) * strafe)
      camera.position.addScaledVector(dir.normalize(), speed)
      const m = 0.8
      camera.position.x = Math.max(-ROOM.w / 2 + m, Math.min(ROOM.w / 2 - m, camera.position.x))
      camera.position.z = Math.max(-ROOM.d / 2 + m, Math.min(ROOM.d / 2 - m, camera.position.z))
    }
  }

  camera.position.y = ROOM.eyeY
  camera.rotation.set(0, 0, 0)
  camera.rotateY(yaw)
  camera.rotateX(pitch)
  renderer!.render(scene, camera)
}

/* ============ 生命周期 ============ */
onMounted(async () => {
  const el = mountRef.value!
  renderer = new THREE.WebGLRenderer({ antialias: true })
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
  renderer.setSize(el.clientWidth, el.clientHeight)
  el.appendChild(renderer.domElement)
  renderer.domElement.style.cursor = 'grab'
  renderer.domElement.style.touchAction = 'none'

  scene = new THREE.Scene()
  scene.background = new THREE.Color(0xefeeea)

  camera = new THREE.PerspectiveCamera(60, el.clientWidth / el.clientHeight, 0.1, 100)
  camera.position.copy(homePos)

  scene.add(new THREE.HemisphereLight(0xfffdf8, 0xb8b2a4, 1.0))
  const key = new THREE.DirectionalLight(0xffffff, 0.9)
  key.position.set(2.5, 6, 3)
  scene.add(key)
  const fill = new THREE.DirectionalLight(0xfff3e4, 0.35)
  fill.position.set(-3, 5, -4)
  scene.add(fill)

  buildRoom()
  bindPointer(renderer.domElement)
  window.addEventListener('keydown', onKeyDown)
  window.addEventListener('keyup', onKeyUp)
  window.addEventListener('resize', onResize)

  // 胶片式加载文案轮换
  let pi = 0
  const pt = window.setInterval(() => {
    pi = (pi + 1) % phrases.length
    if (loading.value) phrase.value = phrases[pi]
  }, 1400)

  artworks = await loadArtworks()
  await hangExhibition()
  window.clearInterval(pt)
  loading.value = false
  animate()
})

function onResize() {
  const el = mountRef.value
  if (!el || !renderer) return
  renderer.setSize(el.clientWidth, el.clientHeight)
  camera.aspect = el.clientWidth / el.clientHeight
  camera.updateProjectionMatrix()
}

onUnmounted(() => {
  disposed = true
  cancelAnimationFrame(raf)
  window.removeEventListener('keydown', onKeyDown)
  window.removeEventListener('keyup', onKeyUp)
  window.removeEventListener('resize', onResize)
  renderer?.dispose()
  if (renderer?.domElement.parentElement) {
    renderer.domElement.parentElement.removeChild(renderer.domElement)
  }
})
</script>

<style scoped>
.gx-page {
  position: relative;
  width: 100%;
  max-width: 1280px;
  margin: 16px auto 0;
  height: calc(100vh - 140px);
  min-height: 480px;
  border-radius: var(--radius-lg);
  overflow: hidden;
  background: #e4e4e4;
}

.gx-canvas-wrap {
  position: absolute;
  inset: 0;
}

.gx-canvas-wrap :deep(canvas) {
  display: block;
}

.gx-vignette {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background: radial-gradient(ellipse at center, transparent 58%, rgba(20, 18, 14, 0.18) 100%);
  z-index: 2;
}

/* 左上圆形返回 logo */
.gx-logo-back {
  position: absolute;
  top: 16px;
  left: 16px;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: rgba(253, 252, 250, 0.92);
  backdrop-filter: blur(6px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.16);
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-display);
  font-weight: 800;
  font-size: 17px;
  color: #18181b !important;
  z-index: 6;
  transition: transform 0.2s;
}

.gx-logo-back:hover {
  transform: scale(1.08);
}

.gx-hint {
  position: absolute;
  top: 22px;
  left: 50%;
  transform: translateX(-50%);
  color: rgba(255, 255, 255, 0.95);
  font-size: 12.5px;
  background: rgba(20, 20, 24, 0.45);
  backdrop-filter: blur(6px);
  padding: 7px 14px;
  border-radius: 999px;
  z-index: 5;
  pointer-events: none;
  white-space: nowrap;
}

/* 加载 */
.gx-loading {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #e9e8e4;
  z-index: 10;
}

.gx-loading-text {
  color: #71717a;
  font-size: 14.5px;
  letter-spacing: 0.06em;
  animation: gxfade 1.4s ease infinite;
}

@keyframes gxfade {
  0%, 100% { opacity: 0.35; }
  50% { opacity: 1; }
}

/* 底部操作条 */
.gx-action-bar {
  position: absolute;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 10px;
  z-index: 6;
}

.gx-btn {
  border: none;
  background: rgba(253, 252, 250, 0.94);
  backdrop-filter: blur(8px);
  color: #18181b;
  font-size: 13.5px;
  font-weight: 600;
  padding: 11px 20px;
  border-radius: 999px;
  cursor: pointer;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.18);
  transition: transform 0.15s, background 0.15s;
}

.gx-btn:hover {
  transform: translateY(-2px);
  background: #fff;
}

/* 说明气泡 */
.gx-info-pop {
  position: absolute;
  bottom: 84px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(253, 252, 250, 0.97);
  border-radius: 18px;
  padding: 16px 20px;
  max-width: 340px;
  font-size: 13px;
  line-height: 1.7;
  color: #52525b;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
  z-index: 7;
}

.gx-pop-close {
  margin-top: 10px;
  border: none;
  background: #18181b;
  color: #fff;
  font-size: 12.5px;
  font-weight: 600;
  padding: 7px 16px;
  border-radius: 999px;
  cursor: pointer;
}

.gx-pop-enter-active,
.gx-pop-leave-active {
  transition: all 0.25s ease;
}

.gx-pop-enter-from,
.gx-pop-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(10px);
}

/* 铭牌信息卡 */
.gx-plaque-card {
  position: absolute;
  bottom: 84px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(253, 252, 250, 0.96);
  backdrop-filter: blur(10px);
  border-radius: 20px;
  padding: 18px 28px;
  text-align: center;
  box-shadow: 0 14px 40px rgba(0, 0, 0, 0.22);
  z-index: 6;
  min-width: 260px;
  max-width: 420px;
}

.gx-plaque-card h3 {
  font-family: var(--font-display);
  font-size: 16.5px;
  font-weight: 700;
  color: #18181b;
}

.gx-artist {
  margin-top: 4px;
  font-size: 13px;
  font-weight: 600;
  color: #52525b;
}

.gx-meta {
  margin-top: 2px;
  font-size: 12px;
  color: #a1a1aa;
}

.gx-close {
  margin-top: 12px;
  border: none;
  background: #18181b;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  padding: 8px 18px;
  border-radius: 999px;
  cursor: pointer;
}

.gx-close:hover {
  background: #3f3f46;
}

.gx-card-enter-active,
.gx-card-leave-active {
  transition: all 0.3s ease;
}

.gx-card-enter-from,
.gx-card-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(16px);
}

/* 移动端方向键 */
.gx-move-btns {
  position: absolute;
  bottom: 20px;
  right: 18px;
  display: grid;
  grid-template-columns: repeat(3, 46px);
  grid-template-rows: repeat(2, 46px);
  gap: 8px;
  z-index: 6;
}

.gx-move-btn {
  border: none;
  border-radius: 14px;
  background: rgba(253, 252, 250, 0.9);
  color: #18181b;
  font-size: 15px;
  font-weight: 700;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.18);
  cursor: pointer;
  touch-action: none;
}

.gx-move-btn:active {
  background: #18181b;
  color: #fff;
}

.mv-w { grid-column: 2; grid-row: 1; }
.mv-a { grid-column: 1; grid-row: 2; }
.mv-s { grid-column: 2; grid-row: 2; }
.mv-d { grid-column: 3; grid-row: 2; }

@media (max-width: 768px) {
  .gx-page {
    height: calc(100vh - 210px);
    border-radius: 16px;
  }

  .gx-hint {
    top: auto;
    bottom: 84px;
  }
}
</style>
