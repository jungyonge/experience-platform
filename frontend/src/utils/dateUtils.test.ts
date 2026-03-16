import { describe, it, expect, vi, afterEach } from 'vitest'
import { getDdayText, formatDate, formatPeriod } from './dateUtils'

describe('getDdayText', () => {
  afterEach(() => {
    vi.useRealTimers()
  })

  it('null → null', () => {
    expect(getDdayText(null)).toBeNull()
  })

  it('미래 날짜 → D-N', () => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-03-15'))

    expect(getDdayText('2026-03-18')).toBe('D-3')
  })

  it('오늘 → D-Day', () => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-03-15'))

    expect(getDdayText('2026-03-15')).toBe('D-Day')
  })

  it('과거 날짜 → 마감', () => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-03-15'))

    expect(getDdayText('2026-03-10')).toBe('마감')
  })

  it('내일 → D-1', () => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-03-15'))

    expect(getDdayText('2026-03-16')).toBe('D-1')
  })
})

describe('formatDate', () => {
  it('null → null', () => {
    expect(formatDate(null)).toBeNull()
  })

  it('날짜 문자열 → YYYY.MM.DD 형식', () => {
    expect(formatDate('2026-03-10')).toBe('2026.03.10')
  })

  it('한 자리 월/일 → 0 패딩', () => {
    expect(formatDate('2026-01-05')).toBe('2026.01.05')
  })
})

describe('formatPeriod', () => {
  it('둘 다 null → null', () => {
    expect(formatPeriod(null, null)).toBeNull()
  })

  it('둘 다 있으면 → 시작 ~ 종료 형식', () => {
    expect(formatPeriod('2026-03-10', '2026-03-25')).toBe('2026.03.10 ~ 2026.03.25')
  })

  it('시작만 있으면 → 시작 ~', () => {
    expect(formatPeriod('2026-03-10', null)).toBe('2026.03.10 ~')
  })

  it('종료만 있으면 → ~ 종료', () => {
    expect(formatPeriod(null, '2026-03-25')).toBe('~ 2026.03.25')
  })
})
