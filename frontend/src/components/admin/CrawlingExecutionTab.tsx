import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { useAdminCrawling } from '@/hooks/admin/useAdminCrawling'

export default function CrawlingExecutionTab() {
  const {
    activeSources,
    isSourcesLoading,
    executionResult,
    isExecuting,
    executingSource,
    logs,
    isLogsLoading,
    handleExecuteAll,
    handleExecuteBySource,
  } = useAdminCrawling()

  function formatDuration(ms: number) {
    if (ms < 1000) return `${ms}ms`
    return `${(ms / 1000).toFixed(1)}s`
  }

  function formatDate(dateStr: string) {
    return new Date(dateStr).toLocaleString('ko-KR')
  }

  function statusColor(status: string) {
    switch (status) {
      case 'SUCCESS':
        return 'text-green-600'
      case 'PARTIAL':
        return 'text-yellow-600'
      case 'FAILED':
        return 'text-red-600'
      default:
        return 'text-gray-600'
    }
  }

  return (
    <div className="space-y-6">
      {/* Execution Buttons */}
      <Card className="p-6">
        <h2 className="text-lg font-semibold mb-4">크롤링 실행</h2>
        <div className="flex flex-wrap gap-2">
          <Button
            onClick={handleExecuteAll}
            disabled={isExecuting}
          >
            {isExecuting && !executingSource ? '실행 중...' : '전체 실행'}
          </Button>
          {isSourcesLoading ? (
            <span className="text-sm text-gray-500 self-center">소스 로딩 중...</span>
          ) : (
            activeSources.map((source) => (
              <Button
                key={source.code}
                variant="outline"
                onClick={() => handleExecuteBySource(source.code)}
                disabled={isExecuting}
              >
                {isExecuting && executingSource === source.code
                  ? '실행 중...'
                  : source.name}
              </Button>
            ))
          )}
        </div>
      </Card>

      {/* Execution Results */}
      {executionResult && (
        <Card className="p-6">
          <h2 className="text-lg font-semibold mb-4">실행 결과</h2>
          <div className="mb-3 text-sm text-gray-600">
            실행 시각: {formatDate(executionResult.executedAt)} | 총 소요:
            {' '}{formatDuration(executionResult.totalDurationMs)}
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-sm border-collapse">
              <thead>
                <tr className="bg-gray-50">
                  <th className="text-left p-2 border-b font-medium">소스</th>
                  <th className="text-center p-2 border-b font-medium">상태</th>
                  <th className="text-center p-2 border-b font-medium">총 수집</th>
                  <th className="text-center p-2 border-b font-medium">신규</th>
                  <th className="text-center p-2 border-b font-medium">갱신</th>
                  <th className="text-center p-2 border-b font-medium">실패</th>
                  <th className="text-center p-2 border-b font-medium">소요</th>
                  <th className="text-left p-2 border-b font-medium">오류</th>
                </tr>
              </thead>
              <tbody>
                {executionResult.results.map((result, index) => (
                  <tr key={index} className="hover:bg-gray-50">
                    <td className="p-2 border-b">{result.sourceDisplayName}</td>
                    <td className={`p-2 border-b text-center font-medium ${statusColor(result.status)}`}>
                      {result.status}
                    </td>
                    <td className="p-2 border-b text-center">{result.totalCrawled}</td>
                    <td className="p-2 border-b text-center text-green-600">
                      {result.newCount}
                    </td>
                    <td className="p-2 border-b text-center text-blue-600">
                      {result.updatedCount}
                    </td>
                    <td className="p-2 border-b text-center text-red-600">
                      {result.failedCount}
                    </td>
                    <td className="p-2 border-b text-center">
                      {formatDuration(result.durationMs)}
                    </td>
                    <td className="p-2 border-b text-red-500 text-xs max-w-[200px] truncate">
                      {result.errorMessage || '-'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      )}

      {/* Crawling Logs */}
      <Card className="p-6">
        <h2 className="text-lg font-semibold mb-4">크롤링 로그</h2>
        {isLogsLoading ? (
          <div className="flex items-center justify-center py-8">
            <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-primary" />
          </div>
        ) : logs.length === 0 ? (
          <p className="text-center text-gray-500 py-8">크롤링 로그가 없습니다.</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm border-collapse">
              <thead>
                <tr className="bg-gray-50">
                  <th className="text-left p-2 border-b font-medium">ID</th>
                  <th className="text-left p-2 border-b font-medium">소스</th>
                  <th className="text-center p-2 border-b font-medium">상태</th>
                  <th className="text-center p-2 border-b font-medium">총 수집</th>
                  <th className="text-center p-2 border-b font-medium">신규</th>
                  <th className="text-center p-2 border-b font-medium">갱신</th>
                  <th className="text-center p-2 border-b font-medium">실패</th>
                  <th className="text-center p-2 border-b font-medium">소요</th>
                  <th className="text-left p-2 border-b font-medium">실행 시각</th>
                  <th className="text-left p-2 border-b font-medium">오류</th>
                </tr>
              </thead>
              <tbody>
                {logs.map((log) => (
                  <tr key={log.id} className="hover:bg-gray-50">
                    <td className="p-2 border-b text-gray-600">{log.id}</td>
                    <td className="p-2 border-b">{log.sourceDisplayName}</td>
                    <td className={`p-2 border-b text-center font-medium ${statusColor(log.status)}`}>
                      {log.status}
                    </td>
                    <td className="p-2 border-b text-center">{log.totalCrawled}</td>
                    <td className="p-2 border-b text-center text-green-600">
                      {log.newCount}
                    </td>
                    <td className="p-2 border-b text-center text-blue-600">
                      {log.updatedCount}
                    </td>
                    <td className="p-2 border-b text-center text-red-600">
                      {log.failedCount}
                    </td>
                    <td className="p-2 border-b text-center">
                      {formatDuration(log.durationMs)}
                    </td>
                    <td className="p-2 border-b text-gray-600 text-xs">
                      {formatDate(log.executedAt)}
                    </td>
                    <td className="p-2 border-b text-red-500 text-xs max-w-[200px] truncate">
                      {log.errorMessage || '-'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>
    </div>
  )
}
