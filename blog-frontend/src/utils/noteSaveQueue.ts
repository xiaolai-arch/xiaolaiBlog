export interface NoteDraft { id: string | number; title: string; content: string }

/** Durable per-note outbox. A save never reads from the currently displayed editor. */
export function createNoteSaveQueue(options: {
  storage: Pick<Storage, 'getItem' | 'setItem'>
  key: string
  save: (draft: NoteDraft) => Promise<unknown>
  onState?: (state: 'pending' | 'saved' | 'failed') => void
  delay?: number
}) {
  let drafts: Record<string, NoteDraft> = {}
  try { drafts = JSON.parse(options.storage.getItem(options.key) || '{}') } catch (_) {}
  const running = new Map<string, Promise<void>>()
  const timers = new Map<string, ReturnType<typeof setTimeout>>()
  const persist = () => options.storage.setItem(options.key, JSON.stringify(drafts))

  async function flushOne(id: string): Promise<void> {
    clearTimeout(timers.get(id))
    timers.delete(id)
    const existing = running.get(id)
    if (existing) return existing
    const work = (async () => {
      while (drafts[id]) {
        const snapshot = drafts[id]
        try {
          await options.save({ ...snapshot })
          if (drafts[id] === snapshot) { delete drafts[id]; persist() }
        } catch (_) {
          options.onState?.('failed')
          return
        }
      }
      options.onState?.(Object.keys(drafts).length ? 'pending' : 'saved')
    })()
    running.set(id, work)
    try { await work } finally { running.delete(id) }
  }

  return {
    pending: () => Object.values(drafts).map(d => ({ ...d })),
    enqueue(draft: NoteDraft) {
      const id = String(draft.id)
      drafts[id] = { ...draft }
      persist()
      options.onState?.('pending')
      clearTimeout(timers.get(id))
      timers.set(id, setTimeout(() => { void flushOne(id) }, options.delay ?? 600))
    },
    async flush() { await Promise.all(Object.keys(drafts).map(flushOne)) },
    async discard(id: string | number) {
      const key = String(id)
      clearTimeout(timers.get(key))
      timers.delete(key)
      await running.get(key)
      delete drafts[key]
      persist()
    },
  }
}
