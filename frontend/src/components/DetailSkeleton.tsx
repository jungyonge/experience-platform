export default function DetailSkeleton() {
  return (
    <div className="max-w-4xl mx-auto px-4 py-6 animate-pulse">
      {/* Back link */}
      <div className="h-4 w-32 bg-gray-200 rounded mb-6" />

      {/* Header area */}
      <div className="flex flex-col md:flex-row gap-6 mb-8">
        {/* Thumbnail */}
        <div className="w-full md:w-[400px] aspect-[3/2] bg-gray-200 rounded-lg shrink-0" />

        {/* Meta */}
        <div className="flex-1 space-y-4">
          <div className="h-7 w-3/4 bg-gray-200 rounded" />
          <div className="flex gap-2">
            <div className="h-5 w-12 bg-gray-200 rounded" />
            <div className="h-5 w-12 bg-gray-200 rounded" />
            <div className="h-5 w-12 bg-gray-200 rounded" />
          </div>
          <div className="space-y-2">
            <div className="h-4 w-40 bg-gray-200 rounded" />
            <div className="h-4 w-48 bg-gray-200 rounded" />
            <div className="h-4 w-32 bg-gray-200 rounded" />
          </div>
        </div>
      </div>

      {/* Body area */}
      <div className="space-y-6">
        <div className="h-4 w-24 bg-gray-200 rounded" />
        <div className="space-y-2">
          <div className="h-4 w-full bg-gray-200 rounded" />
          <div className="h-4 w-5/6 bg-gray-200 rounded" />
          <div className="h-4 w-4/6 bg-gray-200 rounded" />
        </div>
        <div className="h-4 w-24 bg-gray-200 rounded" />
        <div className="h-4 w-2/3 bg-gray-200 rounded" />
      </div>
    </div>
  )
}
