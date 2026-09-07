/** 读取 CSS 变量；SSR/测试环境回退到 fallback。 */
export function getCssVar(name: string, fallback: string): string {
  if (typeof window === 'undefined' || !window.getComputedStyle) {
    return fallback;
  }
  const value = getComputedStyle(document.documentElement).getPropertyValue(name).trim();
  return value || fallback;
}
