import { Link } from 'react-router-dom'
import { Button } from '@/components/ui/button'

export default function NotFoundState() {
  return (
    <div className="max-w-4xl mx-auto px-4 py-20 text-center">
      <div className="text-6xl mb-4">😕</div>
      <h2 className="text-2xl font-bold mb-2">캠페인을 찾을 수 없습니다</h2>
      <p className="text-gray-500 mb-6">
        요청하신 캠페인이 존재하지 않거나 삭제되었을 수 있습니다.
      </p>
      <Link to="/">
        <Button>목록으로 돌아가기</Button>
      </Link>
    </div>
  )
}
