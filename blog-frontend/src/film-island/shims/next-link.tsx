// Shim of next/link for the React island (plain anchor; site is SPA-navigated via mount shell).
import type { AnchorHTMLAttributes, ReactNode } from 'react'

export default function Link({
  href,
  children,
  prefetch,
  ...rest
}: {
  href: string
  children: ReactNode
  /** next/link prop; bundle prefetch is meaningless in this SPA, accepted and ignored. */
  prefetch?: boolean
} & AnchorHTMLAttributes<HTMLAnchorElement>) {
  return (
    <a href={href} {...rest}>
      {children}
    </a>
  )
}
