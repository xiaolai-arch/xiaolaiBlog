// '@/lib/navigation' 垫片：参考站 React 岛里 import { useNavigate } from '@/lib/navigation'，
// '@' 已映射到 src/，这里把请求转交给岛内 next/navigation 垫片（整页跳转）。
export { useRouter, useNavigate } from '@/film-island/shims/next-navigation'
