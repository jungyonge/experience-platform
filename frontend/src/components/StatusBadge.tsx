interface StatusBadgeProps {
  status: string
  statusDisplayName: string
}

export default function StatusBadge({ status, statusDisplayName }: StatusBadgeProps) {
  const colorClass =
    status === 'RECRUITING'
      ? 'bg-orange-100 text-orange-700'
      : 'bg-gray-100 text-gray-500'

  return (
    <span className={`text-xs font-medium px-2 py-0.5 rounded ${colorClass}`}>
      {statusDisplayName}
    </span>
  )
}
