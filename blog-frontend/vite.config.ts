import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import path from 'path'

export default defineConfig({
  plugins: [
    vue(),
    tailwindcss(),
    // Import only the <el-*> components/directives actually used; the full CSS is still loaded in main.ts.
    Components({
      resolvers: [ElementPlusResolver({ importStyle: false })],
      dts: false,
    }),
  ],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src'),
      // React 岛（/film 原版组件）依赖的 Next.js 模块垫片
      'next/link': path.resolve(__dirname, 'src/film-island/shims/next-link.tsx'),
      'next/navigation': path.resolve(__dirname, 'src/film-island/shims/next-navigation.ts'),
    },
  },
  esbuild: {
    // 仅影响 .tsx/.jsx（React 岛）的 JSX 转换
    jsx: 'automatic',
  },
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true
      },
      '/uploads': {
        target: 'http://localhost:8081',
        changeOrigin: true
      }
    }
  }
})
