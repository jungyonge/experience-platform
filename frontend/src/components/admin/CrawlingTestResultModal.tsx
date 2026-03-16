import { Button } from '@/components/ui/button'
import type { CrawlingTestResponse } from '@/types/admin'

interface CrawlingTestResultModalProps {
  result: CrawlingTestResponse
  onClose: () => void
}

export default function CrawlingTestResultModal({
  result,
  onClose,
}: CrawlingTestResultModalProps) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-3xl max-h-[80vh] flex flex-col mx-4">
        <div className="flex items-center justify-between p-6 border-b">
          <h2 className="text-lg font-semibold">
            크롤링 테스트 결과 - {result.sourceName}
          </h2>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600 text-xl leading-none"
          >
            &times;
          </button>
        </div>

        <div className="p-6 overflow-y-auto flex-1">
          <div className="mb-4 p-3 bg-yellow-50 border border-yellow-200 rounded text-sm text-yellow-800">
            DB에는 저장되지 않습니다. 테스트 결과만 표시됩니다.
          </div>

          <div className="grid grid-cols-3 gap-4 mb-6">
            <div className="text-center p-3 bg-gray-50 rounded">
              <div className="text-sm text-gray-500">상태</div>
              <div
                className={`text-lg font-semibold ${
                  result.success ? 'text-green-600' : 'text-red-600'
                }`}
              >
                {result.success ? '성공' : '실패'}
              </div>
            </div>
            <div className="text-center p-3 bg-gray-50 rounded">
              <div className="text-sm text-gray-500">크롤러 타입</div>
              <div className="text-lg font-semibold">{result.crawlerType}</div>
            </div>
            <div className="text-center p-3 bg-gray-50 rounded">
              <div className="text-sm text-gray-500">수집 건수</div>
              <div className="text-lg font-semibold">{result.totalCount}</div>
            </div>
          </div>

          {result.errorMessage && (
            <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded text-sm text-red-700">
              {result.errorMessage}
            </div>
          )}

          {result.items.length > 0 && (
            <div>
              <h3 className="text-sm font-medium text-gray-700 mb-2">
                수집된 항목 미리보기 ({result.items.length}건)
              </h3>
              <div className="overflow-x-auto">
                <table className="w-full text-sm border-collapse">
                  <thead>
                    <tr className="bg-gray-50">
                      <th className="text-left p-2 border-b font-medium">ID</th>
                      <th className="text-left p-2 border-b font-medium">제목</th>
                      <th className="text-left p-2 border-b font-medium">카테고리</th>
                      <th className="text-left p-2 border-b font-medium">URL</th>
                    </tr>
                  </thead>
                  <tbody>
                    {result.items.map((item, index) => (
                      <tr key={index} className="hover:bg-gray-50">
                        <td className="p-2 border-b text-gray-600">
                          {item.originalId}
                        </td>
                        <td className="p-2 border-b max-w-xs truncate">
                          {item.title}
                        </td>
                        <td className="p-2 border-b text-gray-600">
                          {item.category || '-'}
                        </td>
                        <td className="p-2 border-b">
                          <a
                            href={item.originalUrl}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="text-blue-600 hover:underline truncate block max-w-[200px]"
                          >
                            {item.originalUrl}
                          </a>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </div>

        <div className="flex justify-end p-6 border-t">
          <Button onClick={onClose}>닫기</Button>
        </div>
      </div>
    </div>
  )
}
