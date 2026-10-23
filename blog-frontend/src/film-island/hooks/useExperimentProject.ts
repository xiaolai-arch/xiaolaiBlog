// Shim of the site's experiment-project hook: the island always uses the fallback
// project passed by the page (no Sanity backend here). 类型与原版 hook 保持一致
// （比 InfoButton 的 ProjectInfo 多一个 backgroundColor）。
import type { ReactNode } from 'react'
import type { ToolCategory } from '../components/shared/InfoButton'

export type ProjectInfo = {
  id: string
  title: string
  year: string
  description: ReactNode
  imageSrc: string
  videoSrc?: string
  xLink?: string
  tryItOutHref: string
  backgroundColor?: string
  toolCategories?: ToolCategory[]
}

export function useExperimentProject(
  _projectId: string,
  defaultProject: ProjectInfo,
): ProjectInfo {
  return defaultProject
}
