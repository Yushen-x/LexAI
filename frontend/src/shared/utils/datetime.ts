/**
 * 时间格式化工具的单一来源。
 * 此前 formatRelative 在 DashboardView、LegalSessionHistoryPanel 中各有一份重复实现，
 * 这里统一收口，便于一致维护与测试。
 */

const MINUTE = 60_000;
const HOUR = 60 * MINUTE;
const DAY = 24 * HOUR;

/**
 * 将 ISO 时间格式化为「相对当前」的中文描述。
 * - 空值返回占位符；非法时间原样返回。
 * - 一周以内显示「刚刚 / N 分钟前 / N 小时前 / N 天前」；更久显示具体日期时间。
 *
 * @param iso  ISO 时间字符串
 * @param now  当前时间戳（默认 Date.now()，可注入便于测试）
 */
export function formatRelativeTime(iso: string, now: number = Date.now()): string {
  if (!iso) return '—';
  const t = new Date(iso).getTime();
  if (Number.isNaN(t)) return iso;

  const diff = now - t;
  if (diff < MINUTE) return '刚刚';
  if (diff < HOUR) return `${Math.floor(diff / MINUTE)} 分钟前`;
  if (diff < DAY) return `${Math.floor(diff / HOUR)} 小时前`;
  if (diff < 7 * DAY) return `${Math.floor(diff / DAY)} 天前`;

  return new Date(iso).toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  });
}

/**
 * 将 ISO 时间格式化为本地「年-月-日 时:分」绝对时间。
 * - 空值返回占位符；非法时间原样返回。
 */
export function formatDateTime(iso: string): string {
  if (!iso) return '—';
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return d.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  });
}
