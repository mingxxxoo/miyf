/**
 * 后端读取接口为 {@code GET /r/{fileId}}（Nginx 反代到应用）。
 *
 * 上传/签名 URL 常见问题：
 * - baseUrl 误配成 {@code https://host/media} → {@code /media/r/123}（无 /media 路由时落到 SPA，HTML 200 裂图）
 * - 带 MIME 后缀 {@code /r/123.jpg}（PathVariable 只认纯数字）
 *
 * 统一改成同站相对路径 {@code /r/{id}?exp=&sig=}。
 */
export function toResourceUrl(url?: string | null): string | undefined {
  if (url == null) return undefined;
  const trimmed = String(url).trim();
  if (!trimmed) return undefined;

  const m = trimmed.match(
    /(?:https?:\/\/[^/?#]+)?(?:\/media)?(\/r\/\d+)(?:\.(?:jpe?g|png|gif|webp|bmp|svg))?([?#].*)?$/i,
  );
  if (!m) return trimmed;

  return m[1] + (m[2] ?? '');
}

export function toResourceUrlOrEmpty(url?: string | null): string {
  return toResourceUrl(url) ?? '';
}
