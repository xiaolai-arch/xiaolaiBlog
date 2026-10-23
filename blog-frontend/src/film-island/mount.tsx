// React island entry: mounts the original FilmPage component (from michelletliu/michelle-liu, MIT)
// into a DOM node provided by the Vue shell.
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import FilmPage from './components/film/FilmPage'
import './film-tailwind.css'

export function mountFilmPage(el: HTMLElement) {
  const root = createRoot(el)
  root.render(
    <StrictMode>
      <FilmPage />
    </StrictMode>,
  )
  return () => root.unmount()
}
