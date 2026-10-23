import { ElMessageBox } from 'element-plus'
import router from '@/router'

/** 工具页内使用需要账号的云功能时调用：未登录弹引导（去注册/登录，带原页回跳），已登录返回 true */
export async function requireLoginForAction(action = '使用该功能'): Promise<boolean> {
  if (localStorage.getItem('token')) return true
  try {
    await ElMessageBox.confirm(
      `${action}需要先注册或登录（免费，一分钟搞定），你的进度会云端同步到所有设备。`,
      '注册 / 登录后继续',
      {
        confirmButtonText: '去登录',
        cancelButtonText: '暂不',
        type: 'info',
        customStyle: { maxWidth: '420px' },
      },
    )
    router.push({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
  } catch (_) {
    // 用户选择暂不：留在本页（本地模式仍可用）
  }
  return false
}
