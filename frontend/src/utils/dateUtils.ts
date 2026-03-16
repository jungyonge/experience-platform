export function getDdayText(endDate: string | null): string | null {
  if (!endDate) return null

  const today = new Date()
  today.setHours(0, 0, 0, 0)

  const end = new Date(endDate)
  end.setHours(0, 0, 0, 0)

  const diffTime = end.getTime() - today.getTime()
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24))

  if (diffDays > 0) return `D-${diffDays}`
  if (diffDays === 0) return 'D-Day'
  return '마감'
}

export function formatDate(date: string | null): string | null {
  if (!date) return null

  const d = new Date(date)
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')

  return `${year}.${month}.${day}`
}

export function formatPeriod(start: string | null, end: string | null): string | null {
  const formattedStart = formatDate(start)
  const formattedEnd = formatDate(end)

  if (formattedStart && formattedEnd) {
    return `${formattedStart} ~ ${formattedEnd}`
  }
  if (formattedStart) return `${formattedStart} ~`
  if (formattedEnd) return `~ ${formattedEnd}`
  return null
}
