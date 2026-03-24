import { useCampaignSearch } from '@/hooks/useCampaignSearch'
import { useCampaignFilters } from '@/hooks/useCampaignFilters'
import FilterBar from '@/components/FilterBar'
import CampaignGrid from '@/components/CampaignGrid'
import Pagination from '@/components/Pagination'
import EmptyState from '@/components/EmptyState'
import LoadingSkeleton from '@/components/LoadingSkeleton'
import { Button } from '@/components/ui/button'

export default function HomePage() {
  const {
    campaigns,
    totalCount,
    totalPages,
    currentPage,
    isLoading,
    error,
    sourceTypes,
    categories,
    status,
    region,
    sort,
    toggleSourceType,
    toggleCategory,
    setStatus,
    setRegion,
    setSort,
    setPage,
    resetFilters,
  } = useCampaignSearch()

  const { filters } = useCampaignFilters()

  return (
    <div className="max-w-6xl mx-auto px-4 pb-8">
      {/* 필터 바 */}
      {filters && (
        <FilterBar
          filters={filters}
          selectedSourceTypes={sourceTypes}
          selectedCategories={categories}
          selectedStatus={status}
          selectedRegion={region}
          selectedSort={sort}
          onToggleSourceType={toggleSourceType}
          onToggleCategory={toggleCategory}
          onSetStatus={setStatus}
          onSetRegion={setRegion}
          onSetSort={setSort}
        />
      )}

      {/* 콘텐츠 */}
      {error ? (
        <div className="flex flex-col items-center justify-center py-20 text-center">
          <p className="text-lg text-gray-500 mb-4">{error}</p>
          <Button onClick={() => window.location.reload()}>재시도</Button>
        </div>
      ) : isLoading ? (
        <LoadingSkeleton />
      ) : campaigns.length === 0 ? (
        <EmptyState onReset={resetFilters} />
      ) : (
        <>
          <CampaignGrid campaigns={campaigns} />
          <Pagination
            currentPage={currentPage}
            totalPages={totalPages}
            totalCount={totalCount}
            onPageChange={setPage}
          />
        </>
      )}
    </div>
  )
}
