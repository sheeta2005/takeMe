// 提交重试复用此标识，随机值不依赖仅在 HTTPS 可用的 randomUUID。
export function newOrderRequestId(): string {
  const bytes = crypto.getRandomValues(new Uint8Array(16))
  return Array.from(bytes, byte => byte.toString(16).padStart(2, '0')).join('')
}
