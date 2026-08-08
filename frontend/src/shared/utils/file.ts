/**
 * 净化用户提供的文件名，避免路径穿越、非法字符导致下载失败或生成坏文件。
 * - 去掉控制字符（按字符码过滤，规避正则里写控制字符字面量）
 * - 去掉路径分隔符与 Windows/Unix 文件系统非法字符
 * - 去掉开头的点（避免隐藏文件 / 仅扩展名）
 * - 限长，空结果回退到默认名
 */
export function sanitizeFileName(raw: string, fallback = '合同'): string {
  const noControl = Array.from(raw || '')
    .filter((ch) => ch.charCodeAt(0) >= 0x20)
    .join('');
  const cleaned = noControl
    .replace(/[\\/:*?"<>|]/g, '_') // 文件系统非法字符
    .replace(/\s+/g, ' ')
    .trim()
    .replace(/^\.+/, '') // 去掉开头的点
    .trim()
    .slice(0, 100);
  return cleaned || fallback;
}
