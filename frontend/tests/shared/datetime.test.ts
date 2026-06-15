import { describe, expect, it } from 'vitest';
import { formatRelativeTime } from '@/shared/utils/datetime';

const NOW = new Date('2026-06-15T12:00:00Z').getTime();

describe('formatRelativeTime', () => {
  it('空字符串返回占位符', () => {
    expect(formatRelativeTime('', NOW)).toBe('—');
  });

  it('非法时间原样返回', () => {
    expect(formatRelativeTime('not-a-date', NOW)).toBe('not-a-date');
  });

  it('一分钟内显示「刚刚」', () => {
    const iso = new Date(NOW - 30_000).toISOString();
    expect(formatRelativeTime(iso, NOW)).toBe('刚刚');
  });

  it('显示 N 分钟前', () => {
    const iso = new Date(NOW - 5 * 60_000).toISOString();
    expect(formatRelativeTime(iso, NOW)).toBe('5 分钟前');
  });

  it('显示 N 小时前', () => {
    const iso = new Date(NOW - 3 * 60 * 60_000).toISOString();
    expect(formatRelativeTime(iso, NOW)).toBe('3 小时前');
  });

  it('显示 N 天前', () => {
    const iso = new Date(NOW - 2 * 24 * 60 * 60_000).toISOString();
    expect(formatRelativeTime(iso, NOW)).toBe('2 天前');
  });

  it('超过一周显示具体日期', () => {
    const iso = new Date(NOW - 10 * 24 * 60 * 60_000).toISOString();
    const out = formatRelativeTime(iso, NOW);
    expect(out).not.toMatch(/前|刚刚/);
    expect(out).toMatch(/\d/);
  });
});
