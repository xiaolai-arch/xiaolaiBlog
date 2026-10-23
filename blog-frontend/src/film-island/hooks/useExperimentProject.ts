// Shim of the site's experiment-project hook: the island always uses the fallback
// project passed by the page (no Sanity backend here).
import type { ProjectInfo } from '../components/shared/InfoButton'

export function useExperimentProject(_projectId: string, defaultProject: ProjectInfo): ProjectInfo {
  return defaultProject
}
