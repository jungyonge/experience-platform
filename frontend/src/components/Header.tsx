import { useState, useEffect, useRef, type FormEvent } from 'react'
import { Link, useNavigate, useLocation, useSearchParams } from 'react-router-dom'
import { useAuth } from '@/hooks/useAuth'
import { Button } from '@/components/ui/button'
import UserDropdown from '@/components/UserDropdown'

export default function Header() {
  const { isAuthenticated } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [searchParams] = useSearchParams()
  const isHome = location.pathname === '/'

  const [searchValue, setSearchValue] = useState('')
  const initialized = useRef(false)

  // Home에서 URL keyword와 동기화
  useEffect(() => {
    if (isHome) {
      setSearchValue(searchParams.get('keyword') || '')
      initialized.current = true
    } else if (!initialized.current) {
      setSearchValue('')
    }
  }, [isHome, searchParams])

  const handleSearch = (e: FormEvent) => {
    e.preventDefault()
    if (isHome) {
      const params = new URLSearchParams(searchParams)
      if (searchValue.trim()) {
        params.set('keyword', searchValue.trim())
      } else {
        params.delete('keyword')
      }
      params.set('page', '0')
      navigate(`/?${params.toString()}`)
    } else {
      const params = new URLSearchParams()
      if (searchValue.trim()) {
        params.set('keyword', searchValue.trim())
      }
      navigate(`/?${params.toString()}`)
    }
  }

  return (
    <header className="border-b bg-white sticky top-0 z-50">
      <div className="max-w-6xl mx-auto flex items-center justify-between h-14 px-4 gap-4">
        <Link to="/" className="text-lg font-bold text-primary shrink-0">
          체험단 플랫폼
        </Link>

        <form onSubmit={handleSearch} className="flex-1 max-w-md">
          <input
            type="text"
            placeholder="체험단 검색..."
            value={searchValue}
            onChange={(e) => {
              setSearchValue(e.target.value)
              if (isHome) {
                const params = new URLSearchParams(searchParams)
                if (e.target.value.trim()) {
                  params.set('keyword', e.target.value.trim())
                } else {
                  params.delete('keyword')
                }
                params.set('page', '0')
                navigate(`/?${params.toString()}`, { replace: true })
              }
            }}
            className="w-full h-9 px-3 text-sm rounded-md border border-gray-200 focus:outline-none focus:ring-2 focus:ring-primary/30 focus:border-primary"
          />
        </form>

        <nav className="flex items-center gap-3 shrink-0">
          {isAuthenticated ? (
            <UserDropdown />
          ) : (
            <>
              <Link to="/login">
                <Button variant="ghost" size="sm">로그인</Button>
              </Link>
              <Link to="/signup">
                <Button size="sm">회원가입</Button>
              </Link>
            </>
          )}
        </nav>
      </div>
    </header>
  )
}
