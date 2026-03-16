interface PaginationProps {
  currentPage: number
  totalPages: number
  totalCount: number
  onPageChange: (page: number) => void
}

export default function Pagination({ currentPage, totalPages, totalCount, onPageChange }: PaginationProps) {
  if (totalPages <= 0) return null

  const handlePageChange = (page: number) => {
    onPageChange(page)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  // 최대 5개 페이지 번호
  const maxVisible = 5
  let start = Math.max(0, currentPage - Math.floor(maxVisible / 2))
  const end = Math.min(totalPages, start + maxVisible)
  if (end - start < maxVisible) {
    start = Math.max(0, end - maxVisible)
  }

  const pages = Array.from({ length: end - start }, (_, i) => start + i)

  return (
    <div className="flex flex-col items-center gap-3 py-6">
      <p className="text-sm text-gray-500">총 {totalCount.toLocaleString()}건</p>
      <div className="flex items-center gap-1">
        <button
          onClick={() => handlePageChange(currentPage - 1)}
          disabled={currentPage === 0}
          className="px-3 py-1.5 text-sm rounded border border-gray-200 disabled:opacity-40 disabled:cursor-not-allowed hover:bg-gray-50"
        >
          이전
        </button>

        {pages.map((p) => (
          <button
            key={p}
            onClick={() => handlePageChange(p)}
            className={`
              w-8 h-8 text-sm rounded border transition-colors
              ${p === currentPage
                ? 'bg-primary text-white border-primary'
                : 'border-gray-200 hover:bg-gray-50'
              }
            `}
          >
            {p + 1}
          </button>
        ))}

        <button
          onClick={() => handlePageChange(currentPage + 1)}
          disabled={currentPage >= totalPages - 1}
          className="px-3 py-1.5 text-sm rounded border border-gray-200 disabled:opacity-40 disabled:cursor-not-allowed hover:bg-gray-50"
        >
          다음
        </button>
      </div>
    </div>
  )
}
