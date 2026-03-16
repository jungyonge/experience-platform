import { useState, useEffect } from 'react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import type {
  CrawlingSourceItem,
  CrawlingSourceCreateRequest,
  CrawlingSourceUpdateRequest,
} from '@/types/admin'

interface CrawlingSourceFormModalProps {
  source: CrawlingSourceItem | null
  availableCrawlerTypes: string[]
  isSaving: boolean
  onSubmitCreate: (data: CrawlingSourceCreateRequest) => Promise<boolean>
  onSubmitUpdate: (id: number, data: CrawlingSourceUpdateRequest) => Promise<boolean>
  onClose: () => void
}

export default function CrawlingSourceFormModal({
  source,
  availableCrawlerTypes,
  isSaving,
  onSubmitCreate,
  onSubmitUpdate,
  onClose,
}: CrawlingSourceFormModalProps) {
  const isEdit = source !== null

  const [code, setCode] = useState('')
  const [name, setName] = useState('')
  const [baseUrl, setBaseUrl] = useState('')
  const [listUrlPattern, setListUrlPattern] = useState('')
  const [description, setDescription] = useState('')
  const [crawlerType, setCrawlerType] = useState('')
  const [displayOrder, setDisplayOrder] = useState(0)
  const [errors, setErrors] = useState<Record<string, string>>({})

  useEffect(() => {
    if (source) {
      setCode(source.code)
      setName(source.name)
      setBaseUrl(source.baseUrl)
      setListUrlPattern(source.listUrlPattern || '')
      setDescription(source.description || '')
      setCrawlerType(source.crawlerType)
      setDisplayOrder(source.displayOrder)
    } else if (availableCrawlerTypes.length > 0) {
      setCrawlerType(availableCrawlerTypes[0])
    }
  }, [source, availableCrawlerTypes])

  function validate(): boolean {
    const newErrors: Record<string, string> = {}
    if (!isEdit && !code.trim()) newErrors.code = '코드를 입력해주세요.'
    if (!name.trim()) newErrors.name = '이름을 입력해주세요.'
    if (!baseUrl.trim()) newErrors.baseUrl = 'URL을 입력해주세요.'
    if (!crawlerType) newErrors.crawlerType = '크롤러 타입을 선택해주세요.'
    setErrors(newErrors)
    return Object.keys(newErrors).length === 0
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!validate()) return

    const commonData = {
      name: name.trim(),
      baseUrl: baseUrl.trim(),
      listUrlPattern: listUrlPattern.trim() || undefined,
      description: description.trim() || undefined,
      crawlerType,
      displayOrder,
    }

    let success: boolean
    if (isEdit && source) {
      success = await onSubmitUpdate(source.id, commonData)
    } else {
      success = await onSubmitCreate({ ...commonData, code: code.trim() })
    }

    if (success) {
      onClose()
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-lg mx-4">
        <div className="flex items-center justify-between p-6 border-b">
          <h2 className="text-lg font-semibold">
            {isEdit ? '소스 수정' : '소스 추가'}
          </h2>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600 text-xl leading-none"
          >
            &times;
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          <div>
            <Label htmlFor="code">코드</Label>
            <Input
              id="code"
              value={code}
              onChange={(e) => setCode(e.target.value)}
              disabled={isEdit}
              placeholder="예: NAVER_BLOG"
              className={`mt-1 ${isEdit ? 'bg-gray-100' : ''}`}
            />
            {errors.code && (
              <p className="text-sm text-red-500 mt-1">{errors.code}</p>
            )}
          </div>

          <div>
            <Label htmlFor="name">이름</Label>
            <Input
              id="name"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="예: 네이버 블로그"
              className="mt-1"
            />
            {errors.name && (
              <p className="text-sm text-red-500 mt-1">{errors.name}</p>
            )}
          </div>

          <div>
            <Label htmlFor="baseUrl">기본 URL</Label>
            <Input
              id="baseUrl"
              value={baseUrl}
              onChange={(e) => setBaseUrl(e.target.value)}
              placeholder="https://example.com"
              className="mt-1"
            />
            {errors.baseUrl && (
              <p className="text-sm text-red-500 mt-1">{errors.baseUrl}</p>
            )}
          </div>

          <div>
            <Label htmlFor="listUrlPattern">목록 URL 패턴 (선택)</Label>
            <Input
              id="listUrlPattern"
              value={listUrlPattern}
              onChange={(e) => setListUrlPattern(e.target.value)}
              placeholder="/list?page={page}"
              className="mt-1"
            />
          </div>

          <div>
            <Label htmlFor="description">설명 (선택)</Label>
            <textarea
              id="description"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="소스에 대한 설명"
              rows={3}
              className="mt-1 flex w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
            />
          </div>

          <div>
            <Label htmlFor="crawlerType">크롤러 타입</Label>
            <select
              id="crawlerType"
              value={crawlerType}
              onChange={(e) => setCrawlerType(e.target.value)}
              className="mt-1 flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
            >
              {availableCrawlerTypes.map((type) => (
                <option key={type} value={type}>
                  {type}
                </option>
              ))}
            </select>
            {errors.crawlerType && (
              <p className="text-sm text-red-500 mt-1">{errors.crawlerType}</p>
            )}
          </div>

          <div>
            <Label htmlFor="displayOrder">표시 순서</Label>
            <Input
              id="displayOrder"
              type="number"
              value={displayOrder}
              onChange={(e) => setDisplayOrder(Number(e.target.value))}
              className="mt-1"
            />
          </div>

          <div className="flex justify-end gap-2 pt-4">
            <Button type="button" variant="outline" onClick={onClose}>
              취소
            </Button>
            <Button type="submit" disabled={isSaving}>
              {isSaving ? '저장 중...' : isEdit ? '수정' : '추가'}
            </Button>
          </div>
        </form>
      </div>
    </div>
  )
}
