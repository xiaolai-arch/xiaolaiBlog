export function accountScope(storage: Pick<Storage, 'getItem'> = localStorage): string {
  if (!storage.getItem('token')) return 'guest'
  try {
    const user = JSON.parse(storage.getItem('user') || 'null')
    if (user?.id != null) return `user:${user.id}`
    if (user?.username) return `username:${encodeURIComponent(user.username)}`
  } catch (_) {}
  // An invalid session must never fall back to the shared guest space.
  return 'invalid-session'
}

export function accountKey(key: string, scope = accountScope()): string {
  return scope === 'guest' ? key : `${scope}:${key}`
}
