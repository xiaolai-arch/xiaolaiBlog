import { createApp } from 'vue'
import { createPinia } from 'pinia'
import 'element-plus/dist/index.css'
import {
  ArrowDown, ChatDotSquare, DataAnalysis, Document, Expand, Fold, Folder, Headset, Menu, Moon,
  Picture, Plus, PriceTag, Search, Setting, Sunny, Upload, UploadFilled,
} from '@element-plus/icons-vue'
import 'katex/dist/katex.min.css'
import App from './App.vue'
import router from './router'
import './assets/styles/global.scss'

const app = createApp(App)

// Registered globally because some are referenced by name string via <component :is="...">.
// Add new icons here when a template starts using them.
const icons = {
  ArrowDown, ChatDotSquare, DataAnalysis, Document, Expand, Fold, Folder, Headset, Menu, Moon,
  Picture, Plus, PriceTag, Search, Setting, Sunny, Upload, UploadFilled,
}
for (const [key, component] of Object.entries(icons)) {
  app.component(key, component)
}

app.use(createPinia())
app.use(router)
app.mount('#app')
