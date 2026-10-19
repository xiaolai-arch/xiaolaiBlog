import { defineStore } from 'pinia'
import { ref } from 'vue'

import lightHljsUrl from 'highlight.js/styles/github.css?url'
import darkHljsUrl from 'highlight.js/styles/github-dark.css?url'

export const useThemeStore = defineStore('theme', () => {
  const isDark = ref(false)

  function init() {
    // 默认浅色，不跟随系统偏好；仅尊重用户手动切换过的选择
    isDark.value = localStorage.getItem('theme') === 'dark'
    applyTheme()
    // 同源其他窗口（工具箱的 iframe 实时预览、其他标签页）跟随主题变化
    window.addEventListener('storage', onStorageTheme)
  }

  function onStorageTheme(e: StorageEvent) {
    if (e.key !== 'theme' || !e.newValue) return
    const dark = e.newValue === 'dark'
    if (dark !== isDark.value) {
      isDark.value = dark
      applyTheme()
    }
  }

  function toggle() {
    isDark.value = !isDark.value
    localStorage.setItem('theme', isDark.value ? 'dark' : 'light')
    applyTheme()
  }

  function applyTheme() {
    document.documentElement.setAttribute('data-theme', isDark.value ? 'dark' : 'light')
    switchHljsTheme(isDark.value)
  }

  let hljsLink: HTMLLinkElement | null = null

  function switchHljsTheme(dark: boolean) {
    if (!hljsLink) {
      hljsLink = document.createElement('link')
      hljsLink.rel = 'stylesheet'
      hljsLink.id = 'hljs-theme'
      document.head.appendChild(hljsLink)
    }
    hljsLink.href = dark ? darkHljsUrl : lightHljsUrl
  }

  return { isDark, init, toggle }
})