interface FilterChipProps {
  label: string
  active: boolean
  onClick: () => void
}

export default function FilterChip({ label, active, onClick }: FilterChipProps) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`
        px-3 py-1.5 rounded-full text-sm font-medium transition-colors border
        ${active
          ? 'bg-primary text-white border-primary'
          : 'bg-white text-gray-600 border-gray-200 hover:border-gray-400'
        }
      `}
    >
      {label}
    </button>
  )
}
