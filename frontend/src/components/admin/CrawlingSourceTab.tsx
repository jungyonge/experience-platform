import { useState } from 'react'
import { Button } from '@/components/ui/button'
import { useAdminCrawlingSources } from '@/hooks/admin/useAdminCrawlingSources'
import CrawlingSourceFormModal from '@/components/admin/CrawlingSourceFormModal'
import CrawlingTestResultModal from '@/components/admin/CrawlingTestResultModal'
import type { CrawlingSourceItem } from '@/types/admin'

export default function CrawlingSourceTab() {
  const {
    sources,
    availableCrawlerTypes,
    isLoading,
    error,
    testResult,
    isTestLoading,
    isSaving,
    reload,
    handleCreate,
    handleUpdate,
    handleToggleActive,
    handleTest,
    clearTestResult,
  } = useAdminCrawlingSources()

  const [showForm, setShowForm] = useState(false)
  const [editingSource, setEditingSource] = useState<CrawlingSourceItem | null>(null)

  function openCreateForm() {
    setEditingSource(null)
    setShowForm(true)
  }

  function openEditForm(source: CrawlingSourceItem) {
    setEditingSource(source)
    setShowForm(true)
  }

  function closeForm() {
    setShowForm(false)
    setEditingSource(null)
  }

  function formatDate(dateStr: string | null) {
    if (!dateStr) return '-'
    return new Date(dateStr).toLocaleString('ko-KR')
  }

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-12">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary" />
      </div>
    )
  }

  if (error) {
    return (
      <div className="text-center py-12">
        <p className="text-red-500 mb-4">{error}</p>
        <Button onClick={reload} variant="outline">
          다시 시도
        </Button>
      </div>
    )
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-lg font-semibold">크롤링 소스 목록</h2>
        <Button onClick={openCreateForm}>소스 추가</Button>
      </div>

      <div className="overflow-x-auto">
        <table className="w-full text-sm border-collapse">
          <thead>
            <tr className="bg-gray-50">
              <th className="text-left p-3 border-b font-medium">ID</th>
              <th className="text-left p-3 border-b font-medium">코드</th>
              <th className="text-left p-3 border-b font-medium">이름</th>
              <th className="text-left p-3 border-b font-medium">URL</th>
              <th className="text-left p-3 border-b font-medium">크롤러</th>
              <th className="text-center p-3 border-b font-medium">캠페인 수</th>
              <th className="text-left p-3 border-b font-medium">마지막 크롤링</th>
              <th className="text-center p-3 border-b font-medium">상태</th>
              <th className="text-center p-3 border-b font-medium">액션</th>
            </tr>
          </thead>
          <tbody>
            {sources.length === 0 ? (
              <tr>
                <td colSpan={9} className="text-center p-8 text-gray-500">
                  등록된 소스가 없습니다.
                </td>
              </tr>
            ) : (
              sources.map((source) => (
                <tr key={source.id} className="hover:bg-gray-50">
                  <td className="p-3 border-b text-gray-600">{source.id}</td>
                  <td className="p-3 border-b font-mono text-xs">{source.code}</td>
                  <td className="p-3 border-b">{source.name}</td>
                  <td className="p-3 border-b max-w-[200px] truncate text-gray-600">
                    {source.baseUrl}
                  </td>
                  <td className="p-3 border-b text-gray-600">{source.crawlerType}</td>
                  <td className="p-3 border-b text-center">{source.campaignCount}</td>
                  <td className="p-3 border-b text-gray-600 text-xs">
                    {formatDate(source.lastCrawledAt)}
                  </td>
                  <td className="p-3 border-b text-center">
                    <span
                      className={`inline-block px-2 py-1 rounded-full text-xs font-medium ${
                        source.active
                          ? 'bg-green-100 text-green-700'
                          : 'bg-gray-100 text-gray-500'
                      }`}
                    >
                      {source.active ? '활성' : '비활성'}
                    </span>
                  </td>
                  <td className="p-3 border-b text-center">
                    <div className="flex items-center justify-center gap-1">
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => openEditForm(source)}
                      >
                        수정
                      </Button>
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => handleTest(source.id)}
                        disabled={isTestLoading}
                      >
                        {isTestLoading ? '...' : '테스트'}
                      </Button>
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => handleToggleActive(source.id)}
                      >
                        {source.active ? '비활성화' : '활성화'}
                      </Button>
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {showForm && (
        <CrawlingSourceFormModal
          source={editingSource}
          availableCrawlerTypes={availableCrawlerTypes}
          isSaving={isSaving}
          onSubmitCreate={handleCreate}
          onSubmitUpdate={handleUpdate}
          onClose={closeForm}
        />
      )}

      {testResult && (
        <CrawlingTestResultModal
          result={testResult}
          onClose={clearTestResult}
        />
      )}
    </div>
  )
}
