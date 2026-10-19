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