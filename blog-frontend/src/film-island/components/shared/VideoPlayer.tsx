// Simplified stand-in for the original mux/hls VideoPlayer (no mux-embed analytics).
// HLS streams (mux) render nothing; plain files render a native <video>.
import { useEffect, useRef } from 'react'

export default function VideoPlayer(props: Record<string, any>) {
  const { src, poster, className = '', onLoaded, ...rest } = props
  const ref = useRef<HTMLVideoElement>(null)
  useEffect(() => {
    const v = ref.current
    if (v && onLoaded) {
      const h = () => onLoaded()
      v.addEventListener('loadeddata', h)
      return () => v.removeEventListener('loadeddata', h)
    }
  }, [onLoaded])
  if (!src || String(src).endsWith('.m3u8')) return null
  return (
    <video
      ref={ref}
      src={src}
      poster={poster}
      className={className}
      playsInline
      {...rest}
    />
  )
}
