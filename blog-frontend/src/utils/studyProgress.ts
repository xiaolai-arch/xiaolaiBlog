import { ElMessageBox } from 'element-plus'
import { progressApi } from '@/api'
import { accountKey, accountScope } from './accountStorage'
import { completed, mergeProgress, mergeSubmissions, parseProgress, setCompleted } from './progressModel'

const imports = new Map<string, Promise<void>>()
export async function offerGuestProgressImport() {
  const scope = accountScope()
  if (scope === 'guest' || scope === 'invalid-session') return
  if (imports.has(scope)) return imports.get(scope)
  const work = (async () => {
    const marker = accountKey('study:guest-imported', scope)
    if (localStorage.getItem(marker)) return
    const keys = ['leetcode:done', 'cet4:done', 'agent:done', 'lc-log', 'study:log']
    if (!keys.some(key => { const raw = localStorage.getItem(key); return raw && raw !== '[]' })) return
    try {
      await ElMessageBox.confirm('浏览器中有未绑定账号的学习记录，是否导入当前账号？请确认这些记录属于你。', '导入学习记录', {
        confirmButtonText: '导入当前账号', cancelButtonText: '不导入', type: 'info',
      })
      if (accountScope() !== scope) return
      for (const key of keys) {
        const target = accountKey(key, scope)
        if (key.endsWith(':done')) {
          localStorage.setItem(target, JSON.stringify(mergeProgress(parseProgress(localStorage.getItem(target)), parseProgress(localStorage.getItem(key)))))
        } else {
          const old = JSON.parse(localStorage.getItem(target) || '[]')
          const guest = JSON.parse(localStorage.getItem(key) || '[]')
          localStorage.setItem(target, JSON.stringify(key === 'lc-log' ? mergeSubmissions(old, guest) : [...old, ...guest]))
        }
      }
    } catch (_) { /* Declining keeps unbound records available for a later explicit import. */ }
    localStorage.setItem(marker, '1')
  })()
  imports.set(scope, work)
  try { await work } finally { imports.delete(scope) }
}

export function readCompleted(key: string) {
  return completed(parseProgress(localStorage.getItem(accountKey(`${key}:done`))))
}

export function createStudyProgress(key: string) {
  const scope = accountScope()
  const token = localStorage.getItem('token')
  const storageKey = accountKey(`${key}:done`, scope)
  const ownSession = () => accountScope() === scope && localStorage.getItem('token') === token
  const read = () => parseProgress(localStorage.getItem(storageKey))
  let chain = Promise.resolve()
  async function sync() {
    if (!token || !ownSession()) return completed(read())
    const request = chain.catch(() => {}).then(async () => {
      if (!ownSession()) return
      const response = await progressApi.get(key)
      if (!ownSession()) return
      const cloud = parseProgress(response.data.data.value)
      const merged = mergeProgress(cloud, read())
      localStorage.setItem(storageKey, JSON.stringify(merged))
      if (JSON.stringify(cloud) !== JSON.stringify(merged)) {
        const result = await progressApi.put(key, JSON.stringify(merged))
        if (ownSession() && result.data.data?.value) {
          localStorage.setItem(storageKey, JSON.stringify(mergeProgress(parseProgress(result.data.data.value), read())))
        }
      }
    })
    chain = request
    await request
    return completed(read())
  }
  return {
    read: () => completed(read()),
    toggle(id: string, done: boolean) {
      if (!ownSession()) return completed(read())
      const next = setCompleted(read(), id, done)
      localStorage.setItem(storageKey, JSON.stringify(next))
      return completed(next)
    },
    sync,
  }
}
