// Shim of next/navigation for the React island. FilmPage only uses router.prefetch,
// plus push/replace as fallbacks via full navigation.
export function useRouter() {
  return {
    prefetch: (_url?: string) => {},
    push: (url: string) => {
      window.location.href = url
    },
    replace: (url: string) => {
      window.location.replace(url)
    },
    back: () => {
      window.history.back()
    },
  }
}
