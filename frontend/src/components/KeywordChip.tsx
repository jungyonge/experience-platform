interface KeywordChipProps {
  keyword: string
}

export default function KeywordChip({ keyword }: KeywordChipProps) {
  return (
    <span className="inline-block bg-gray-100 text-gray-600 text-xs px-2.5 py-1 rounded-full">
      #{keyword}
    </span>
  )
}
