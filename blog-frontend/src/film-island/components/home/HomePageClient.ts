// Shim of the site's home page client bundle: in FilmPage it is only warm-imported
// on hover of the back button (bundle prefetch). Nothing to warm in this SPA.
export default function HomePageClient() {
  return null
}
