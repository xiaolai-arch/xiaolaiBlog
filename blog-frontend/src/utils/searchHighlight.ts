/** Text fragments are rendered by Vue interpolation, including untrusted search terms. */
export function highlight(value: string, keyword: string): { text: string; match: boolean }[] {
  const text = value || '', term = keyword.trim().toLowerCase()
  if (!term) return [{ text, match: false }]
  const lower = text.toLowerCase(), parts: { text: string; match: boolean }[] = []
  let from = 0, at = lower.indexOf(term)
  while (at >= 0) {
    if (at > from) parts.push({ text: text.slice(from, at), match: false })
    parts.push({ text: text.slice(at, at + term.length), match: true })
    from = at + term.length; at = lower.indexOf(term, from)
  }
  if (from < text.length) parts.push({ text: text.slice(from), match: false })
  return parts
}
