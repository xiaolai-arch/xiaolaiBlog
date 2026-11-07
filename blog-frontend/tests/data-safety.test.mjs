import { test } from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import ts from 'typescript'

async function importSource(name) {
  const source = await readFile(new URL(`../src/utils/${name}.ts`, import.meta.url), 'utf8')
  const { outputText } = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2020 } })
  return import(`data:text/javascript;base64,${Buffer.from(outputText).toString('base64')}`)
}
const { createNoteSaveQueue } = await importSource('noteSaveQueue')
const { accountKey, accountScope } = await importSource('accountStorage')
const { completed, mergeProgress, mergeSubmissions, parseProgress, setCompleted } = await importSource('progressModel')

const storage = () => {
  const data = new Map()
  return { getItem: key => data.get(key) ?? null, setItem: (key, value) => data.set(key, value) }
}
const deferred = () => {
  let resolve
  const promise = new Promise(done => { resolve = done })
  return { promise, resolve }
}

test('switching notes cannot change a queued snapshot', async () => {
  const saved = []
  const queue = createNoteSaveQueue({ storage: storage(), key: 'outbox', save: async draft => saved.push(draft) })
  const a = { id: 1, title: 'A', content: 'body A' }
  queue.enqueue(a)
  a.content = 'editor now displays B'
  queue.enqueue({ id: 2, title: 'B', content: 'body B' })
  await queue.flush()
  assert.deepEqual(saved.map(d => d.content), ['body A', 'body B'])
  assert.deepEqual(queue.pending(), [])
})

test('in-flight saves are serialized and cannot erase newer edits', async () => {
  const gate = deferred()
  const saved = []
  const queue = createNoteSaveQueue({ storage: storage(), key: 'outbox', save: async draft => {
    saved.push(draft.content)
    if (saved.length === 1) await gate.promise
  } })
  queue.enqueue({ id: 1, title: 'A', content: 'old' })
  const flush = queue.flush()
  queue.enqueue({ id: 1, title: 'A', content: 'latest' })
  gate.resolve()
  await flush
  assert.deepEqual(saved, ['old', 'latest'])
  assert.deepEqual(queue.pending(), [])
})

test('failed saves survive a page restart and can be retried', async () => {
  const disk = storage()
  const failed = createNoteSaveQueue({ storage: disk, key: 'outbox', save: async () => { throw new Error('offline') } })
  failed.enqueue({ id: 1, title: 'A', content: 'unsent' })
  await failed.flush()
  const saved = []
  const restarted = createNoteSaveQueue({ storage: disk, key: 'outbox', save: async draft => saved.push(draft.content) })
  await restarted.flush()
  assert.deepEqual(saved, ['unsent'])
  assert.deepEqual(restarted.pending(), [])
})

test('private caches are isolated by account and never fall back to guest for malformed sessions', () => {
  const disk = storage()
  assert.equal(accountScope(disk), 'guest')
  disk.setItem('token', 'test')
  assert.equal(accountScope(disk), 'invalid-session')
  disk.setItem('user', JSON.stringify({ id: 7 }))
  const first = accountKey('leetcode:done', accountScope(disk))
  disk.setItem('user', JSON.stringify({ id: 8 }))
  assert.notEqual(first, accountKey('leetcode:done', accountScope(disk)))
})

test('cancellation survives stale device uploads and legacy array migration', () => {
  const old = parseProgress('["1", "2"]')
  const cancelled = setCompleted(old, '1', false, 100)
  const merged = mergeProgress(cancelled, old)
  assert.deepEqual([...completed(merged)], ['2'])
  assert.equal(mergeProgress(merged, parseProgress('["1"]')).items['1'].done, false)
})

test('independent device edits merge and equal-time deletion wins in either order', () => {
  const initial = parseProgress('[]')
  const a = setCompleted(initial, 'a', true, 100)
  const b = setCompleted(initial, 'b', true, 100)
  assert.deepEqual([...completed(mergeProgress(a, b))].sort(), ['a', 'b'])
  const deleted = setCompleted(initial, 'a', false, 100)
  assert.equal(mergeProgress(a, deleted).items.a.done, false)
  assert.equal(mergeProgress(deleted, a).items.a.done, false)
})

test('submission merge is idempotent and retains local-only logs for first upload', () => {
  const local = [{ pid: '1', date: '2026-11-07' }]
  assert.deepEqual(mergeSubmissions(local, []), local)
  assert.deepEqual(mergeSubmissions(local, local), local)
})

const { highlight } = await importSource('searchHighlight')
test('highlight treats HTML and regex characters as plain text', () => {
  const text = '<img onerror=alert(1)> a+b A+B'
  const fragments = highlight(text, 'a+b')
  assert.equal(fragments.map(p => p.text).join(''), text)
  assert.deepEqual(fragments.filter(p => p.match).map(p => p.text), ['a+b', 'A+B'])
  assert.deepEqual(highlight(text, ''), [{ text, match: false }])
})
test('folder metadata survives a failed note save and restart', async () => {
  const disk = storage()
  const failed = createNoteSaveQueue({ storage: disk, key: 'outbox', save: async () => { throw new Error('offline') } })
  failed.enqueue({ id: 1, title: 'A', content: 'body', folder: 'Java' })
  await failed.flush()
  const saved = []
  const restarted = createNoteSaveQueue({ storage: disk, key: 'outbox', save: async d => saved.push(d) })
  await restarted.flush()
  assert.equal(saved[0].folder, 'Java')
})
