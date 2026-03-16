import { useSearchParams } from 'react-router-dom'
import CrawlingExecutionTab from '@/components/admin/CrawlingExecutionTab'
import CrawlingSourceTab from '@/components/admin/CrawlingSourceTab'

type TabType = 'execution' | 'sources'

export default function AdminCrawlingPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const activeTab = (searchParams.get('tab') as TabType) || 'execution'

  function handleTabChange(tab: TabType) {
    setSearchParams({ tab })
  }

  return (
    <div className="max-w-7xl mx-auto px-4 py-8">
      <h1 className="text-2xl font-bold mb-6">크롤링 소스 관리</h1>

      <div className="flex border-b mb-6">
        <button
          onClick={() => handleTabChange('execution')}
          className={`px-4 py-2 text-sm font-medium border-b-2 transition-colors ${
            activeTab === 'execution'
              ? 'border-primary text-primary'
              : 'border-transparent text-gray-500 hover:text-gray-700'
          }`}
        >
          실행 관리
        </button>
        <button
          onClick={() => handleTabChange('sources')}
          className={`px-4 py-2 text-sm font-medium border-b-2 transition-colors ${
            activeTab === 'sources'
              ? 'border-primary text-primary'
              : 'border-transparent text-gray-500 hover:text-gray-700'
          }`}
        >
          소스 관리
        </button>
      </div>

      {activeTab === 'execution' ? <CrawlingExecutionTab /> : <CrawlingSourceTab />}
    </div>
  )
}
