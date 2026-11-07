export interface ProgressItem { done: boolean; at: number }
export interface ProgressState { version: 2; items: Record<string, ProgressItem> }

export function parseProgress(raw: string | null): ProgressState {
  const items: Record<string, ProgressItem> = Object.create(null)
  try {
    const value = JSON.parse(raw || '[]')
    if (Array.isArray(value)) {
      for (const id of value) items[String(id)] = { done: true, at: 0 }
    } else if (value?.version === 2 && value.items && typeof value.items === 'object') {
      for (const [id, event] of Object.entries(value.items) as [string, ProgressItem][]) {
        if (typeof event?.done === 'boolean' && Number.isSafeInteger(event.at) && event.at >= 0) items[id] = { ...event }
      }
    }
  } catch (_) {}
  return { version: 2, items }
}

export function mergeProgress(left: ProgressState, right: ProgressState): ProgressState {
  const items: Record<string, ProgressItem> = Object.assign(Object.create(null), left.items)
  for (const [id, event] of Object.entries(right.items)) {
    const old = items[id]
    // On equal timestamps, deletion wins. Old array formats always have timestamp zero.
    if (!old || event.at > old.at || (event.at === old.at && !event.done)) items[id] = { ...event }
  }
  return { version: 2, items }
}

export function completed(state: ProgressState): Set<string> {
  return new Set(Object.entries(state.items).filter(([, item]) => item.done).map(([id]) => id))
}

export function setCompleted(state: ProgressState, id: string, done: boolean, now = Date.now()): ProgressState {
  const next = mergeProgress(state, { version: 2, items: {} })
  next.items[id] = { done, at: Math.max(now, (state.items[id]?.at ?? 0) + 1) }
  return next
}

export interface Submission { pid: string; date: string }
export function mergeSubmissions(...sources: Submission[][]): Submission[] {
  const entries = new Map<string, Submission>()
  for (const list of sources) for (const entry of list) {
    if (typeof entry?.pid === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(entry.date)) entries.set(`${entry.pid}@${entry.date}`, entry)
  }
  return [...entries.values()].sort((a, b) => a.date.localeCompare(b.date) || a.pid.localeCompare(b.pid))
}
