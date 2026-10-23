// React island entry: mounts the original PolaroidPage component (from michelletliu/michelle-liu, MIT)
// into a DOM node provided by the Vue shell.
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import PolaroidPage from './components/polaroid/PolaroidPage'
import './film-tailwind.css'

export function mountPolaroidPage(el: HTMLElement) {
  const root = createRoot(el)
  root.render(
    <StrictMode>
      <PolaroidPage />
    </StrictMode>,
  )
  return () => root.unmount()
}
