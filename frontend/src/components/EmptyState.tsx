import { Button } from '@/components/ui/button'

interface EmptyStateProps {
  onReset: () => void
}

export default function EmptyState({ onReset }: EmptyStateProps) {
  return (
    <div className="flex flex-col items-center justify-center py-20 text-center">
      <p className="text-lg text-gray-500 mb-2">검색 결과가 없습니다.</p>
      <p className="text-sm text-gray-400 mb-4">다른 검색어나 필터를 시도해보세요.</p>
      <Button variant="outline" onClick={onReset}>
        필터 초기화
      </Button>
    </div>
  )
}
