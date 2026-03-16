import FilterChip from '@/components/FilterChip'
import type { FilterOptionsResponse } from '@/types/campaign'

interface FilterBarProps {
  filters: FilterOptionsResponse
  selectedSourceTypes: string[]
  selectedCategories: string[]
  selectedStatus: string
  selectedSort: string
  onToggleSourceType: (code: string) => void
  onToggleCategory: (code: string) => void
  onSetStatus: (code: string) => void
  onSetSort: (code: string) => void
}

export default function FilterBar({
  filters,
  selectedSourceTypes,
  selectedCategories,
  selectedStatus,
  selectedSort,
  onToggleSourceType,
  onToggleCategory,
  onSetStatus,
  onSetSort,
}: FilterBarProps) {
  return (
    <div className="space-y-3 py-4">
      {/* 소스 필터 */}
      <div className="flex flex-wrap items-center gap-2">
        <span className="text-sm font-medium text-gray-500 w-14 shrink-0">소스</span>
        <FilterChip
          label="전체"
          active={selectedSourceTypes.length === 0}
          onClick={() => {
            if (selectedSourceTypes.length > 0) {
              selectedSourceTypes.forEach(onToggleSourceType)
            }
          }}
        />
        {filters.sourceTypes.map((s) => (
          <FilterChip
            key={s.code}
            label={s.name}
            active={selectedSourceTypes.includes(s.code)}
            onClick={() => onToggleSourceType(s.code)}
          />
        ))}
      </div>

      {/* 카테고리 필터 */}
      <div className="flex flex-wrap items-center gap-2">
        <span className="text-sm font-medium text-gray-500 w-14 shrink-0">카테고리</span>
        <FilterChip
          label="전체"
          active={selectedCategories.length === 0}
          onClick={() => {
            if (selectedCategories.length > 0) {
              selectedCategories.forEach(onToggleCategory)
            }
          }}
        />
        {filters.categories.map((c) => (
          <FilterChip
            key={c.code}
            label={c.name}
            active={selectedCategories.includes(c.code)}
            onClick={() => onToggleCategory(c.code)}
          />
        ))}
      </div>

      {/* 상태 + 정렬 */}
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div className="flex items-center gap-2">
          <span className="text-sm font-medium text-gray-500 w-14 shrink-0">상태</span>
          <FilterChip
            label="전체"
            active={!selectedStatus}
            onClick={() => onSetStatus('')}
          />
          {filters.statuses.map((s) => (
            <FilterChip
              key={s.code}
              label={s.name}
              active={selectedStatus === s.code}
              onClick={() => onSetStatus(selectedStatus === s.code ? '' : s.code)}
            />
          ))}
        </div>

        <select
          value={selectedSort}
          onChange={(e) => onSetSort(e.target.value)}
          className="text-sm border border-gray-200 rounded-md px-3 py-1.5 bg-white"
        >
          {filters.sortOptions.map((s) => (
            <option key={s.code} value={s.code}>
              {s.name}
            </option>
          ))}
        </select>
      </div>
    </div>
  )
}
