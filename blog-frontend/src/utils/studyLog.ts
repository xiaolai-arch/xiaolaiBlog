// 学习活动日志：给学习仪表盘的热力图/连击统计用。
// 每条记录 = { d: 'YYYY-MM-DD', k: 唯一条目标识 }，存 localStorage。
const KEY = 'study:log'

export interface StudyEntry {
  d: string
  k: string
  /** 可选：该条目对应的时长（秒），供仪表盘统计 */
  s?: number
}

function today(): string {
  const d = new Date()
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
}

export function getStudyLog(): StudyEntry[] {
  try {
    const arr = JSON.parse(localStorage.getItem(KEY) || '[]')
    return Array.isArray(arr) ? arr : []
  } catch (_) {
    return []
  }
}

function save(entries: StudyEntry[]) {
  localStorage.setItem(KEY, JSON.stringify(entries))
}

export function logStudy(k: string, s?: number) {
  const entries = getStudyLog()
  const d = today()
  if (entries.some(e => e.k === k)) return
  entries.push({ d, k, s })
  save(entries)
}

export function unlogStudy(k: string) {
  save(getStudyLog().filter(e => e.k !== k))
}
