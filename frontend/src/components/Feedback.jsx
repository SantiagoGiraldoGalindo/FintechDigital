export function Loading({ label = 'Cargando…' }) {
  return (
    <div className="flex items-center gap-3 py-8 text-sm text-slate-500" role="status">
      <span className="h-5 w-5 animate-spin rounded-full border-2 border-emerald-600 border-t-transparent" />
      {label}
    </div>
  )
}

export function ErrorMessage({ children, onRetry }) {
  if (!children) return null
  return (
    <div className="mb-5 flex flex-wrap items-center justify-between gap-3 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800" role="alert">
      <span>{children}</span>
      {onRetry && <button className="font-semibold underline" onClick={onRetry}>Reintentar</button>}
    </div>
  )
}

export function Notice({ children }) {
  if (!children) return null
  return <div className="mb-5 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-800" role="status">{children}</div>
}

export function PageHeading({ eyebrow, title, description, children }) {
  return (
    <div className="mb-7 flex flex-wrap items-end justify-between gap-4">
      <div>
        {eyebrow && <p className="mb-1 text-xs font-bold uppercase tracking-[0.18em] text-emerald-700">{eyebrow}</p>}
        <h1 className="text-3xl font-bold tracking-tight text-slate-950">{title}</h1>
        {description && <p className="mt-2 max-w-2xl text-sm text-slate-500">{description}</p>}
      </div>
      {children}
    </div>
  )
}

export const inputClass = 'mt-1 w-full rounded-lg border border-slate-300 bg-white px-3 py-2.5 text-sm text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-emerald-600 focus:ring-2 focus:ring-emerald-100'
export const labelClass = 'block text-sm font-medium text-slate-700'
export const buttonClass = 'inline-flex items-center justify-center rounded-lg bg-emerald-700 px-4 py-2.5 text-sm font-semibold text-white transition hover:bg-emerald-800 disabled:cursor-not-allowed disabled:opacity-60'

export function EmptyState({ children }) {
  return <div className="rounded-xl border border-dashed border-slate-300 bg-slate-50 px-5 py-10 text-center text-sm text-slate-500">{children}</div>
}

export function DataTable({ columns, rows, empty = 'No hay resultados para mostrar.' }) {
  return (
    <div className="overflow-x-auto rounded-xl border border-slate-200 bg-white">
      <table className="w-full min-w-[640px] text-left text-sm">
        <thead className="bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
          <tr>{columns.map((column) => <th key={column.key} className="px-4 py-3 font-semibold">{column.label}</th>)}</tr>
        </thead>
        <tbody className="divide-y divide-slate-100">
          {rows.length ? rows.map((row, index) => (
            <tr key={row.id ?? row.codigo ?? index} className="hover:bg-slate-50/70">
              {columns.map((column) => <td key={column.key} className="px-4 py-3.5 text-slate-700">{column.render ? column.render(row) : (row[column.key] ?? '—')}</td>)}
            </tr>
          )) : <tr><td className="px-4 py-8 text-center text-slate-500" colSpan={columns.length}>{empty}</td></tr>}
        </tbody>
      </table>
    </div>
  )
}
