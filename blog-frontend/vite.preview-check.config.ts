import { defineConfig, mergeConfig } from 'vite'
import base from './vite.config'

export default mergeConfig(base, defineConfig({
  preview: {
    port: 4173,
    proxy: {
      '/api': { target: 'http://www.xiaolai.cyou', changeOrigin: true },
      '/uploads': { target: 'http://www.xiaolai.cyou', changeOrigin: true },
    },
  },
}))
