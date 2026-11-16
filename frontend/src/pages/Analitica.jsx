import { useCallback, useEffect, useState } from 'react'
import api from '../services/api.js'
import { mensajeError } from '../services/fintechService.js'
import { DataTable, EmptyState, ErrorMessage, inputClass, labelClass, Loading, PageHeading } from '../components/Feedback.jsx'

const fechaLocal = (dias = 0) => new Date(Date.now() - new Date().getTimezoneOffset() * 60000 - dias * 86400000).toISOString().slice(0, 16)

export default function Analitica() {
  const [rango, setRango] = useState({ desde: fechaLocal(30), hasta: fechaLocal() })
  const [resumen, setResumen] = useState(null)
  const [transacciones, setTransacciones] = useState([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')

  const cargar = useCallback(async (event) => {
    event?.preventDefault()
    setCargando(true)
    setError('')
    try {
      const params = { desde: rango.desde, hasta: rango.hasta, k: 10 }
      const [informe, historial] = await Promise.all([
        api.get('/analitica', { params }).then(({ data }) => data),
        api.get('/historial', { params: { desde: rango.desde, hasta: rango.hasta } }).then(({ data }) => data),
      ])
      setResumen(informe)
      setTransacciones(historial)
    } catch (e) { setError(mensajeError(e)) }
    finally { setCargando(false) }
  }, [rango])
  useEffect(() => { cargar() }, [])

  const porTipo = transacciones.reduce((acum, t) => {
    const tipo = t.tipo || 'OTRO'
    acum[tipo] = (acum[tipo] || 0) + 1
    return acum
  }, {})
  const frecuencia = Object.entries(porTipo).sort((a, b) => b[1] - a[1])
  const maxFrecuencia = Math.max(1, ...frecuencia.map(([, total]) => total))
  const porEstado = transacciones.reduce((acum, t) => {
    acum[t.estado] = (acum[t.estado] || 0) + 1
    return acum
  }, {})
  const montoTotal = Number(resumen?.montoTotal || 0)
  const promedio = transacciones.length ? montoTotal / transacciones.length : 0
  const top = [...transacciones].sort((a, b) => Number(b.valor) - Number(a.valor)).slice(0, 5)
  const columnas = [
    { key: 'id', label: 'ID', render: (t) => `#${t.id}` },
    { key: 'fecha', label: 'Fecha', render: (t) => t.fecha ? new Date(t.fecha).toLocaleString() : '—' },
    { key: 'tipo', label: 'Tipo' },
    { key: 'valor', label: 'Monto', render: (t) => `$${Number(t.valor).toLocaleString('es-CO')}` },
    { key: 'estado', label: 'Estado' },
    { key: 'nivelRiesgo', label: 'Riesgo', render: (t) => t.nivelRiesgo || '—' },
  ]

  return (
    <section>
      <PageHeading eyebrow="Insights" title="Analítica" description="Métricas de actividad, distribución de operaciones y transacciones de mayor valor." />
      <ErrorMessage onRetry={() => cargar()}>{error}</ErrorMessage>
      <form className="mb-6 flex flex-wrap items-end gap-4 rounded-2xl border border-slate-200 bg-white p-5" onSubmit={cargar}>
        <label className={labelClass}>Desde<input className={inputClass} type="datetime-local" value={rango.desde} onChange={(e) => setRango({ ...rango, desde: e.target.value })} required /></label>
        <label className={labelClass}>Hasta<input className={inputClass} type="datetime-local" value={rango.hasta} onChange={(e) => setRango({ ...rango, hasta: e.target.value })} required /></label>
        <button className="rounded-lg bg-slate-900 px-4 py-2.5 text-sm font-semibold text-white disabled:opacity-60" disabled={cargando}>{cargando ? 'Calculando…' : 'Actualizar analítica'}</button>
      </form>
      {cargando && !resumen ? <Loading label="Calculando indicadores…" /> : resumen && (
        <div className="space-y-6">
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <MetricCard title="Monto total" value={`$${montoTotal.toLocaleString('es-CO', { maximumFractionDigits: 2 })}`} detail="Operaciones registradas en el rango" />
            <MetricCard title="Operaciones" value={transacciones.length.toLocaleString('es-CO')} detail="Transacciones del período" />
            <MetricCard title="Ticket promedio" value={`$${promedio.toLocaleString('es-CO', { maximumFractionDigits: 2 })}`} detail="Monto total / operaciones" />
            <MetricCard title="Usuario más activo" value={resumen.usuarioMayorActividad?.nombre || '—'} detail={resumen.usuarioMayorActividad?.operaciones ? `${resumen.usuarioMayorActividad.operaciones} operaciones` : 'Sin actividad en el período'} />
          </div>

          <div className="grid gap-6 lg:grid-cols-2">
            <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
              <h2 className="font-semibold text-slate-900">Frecuencia por tipo</h2><p className="mb-5 text-sm text-slate-500">Número de operaciones por categoría.</p>
              {frecuencia.length ? <div className="space-y-4">{frecuencia.map(([tipo, cantidad]) => <div key={tipo}>
                <div className="mb-1 flex justify-between text-sm"><span className="font-medium text-slate-700">{tipo.replaceAll('_', ' ')}</span><span className="tabular-nums text-slate-500">{cantidad}</span></div>
                <div className="h-2.5 overflow-hidden rounded-full bg-slate-100"><div className="h-full rounded-full bg-emerald-600 transition-all" style={{ width: `${(cantidad / maxFrecuencia) * 100}%` }} /></div>
              </div>)}</div> : <EmptyState>Sin transacciones en el período.</EmptyState>}
            </div>
            <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
              <h2 className="font-semibold text-slate-900">Estado de operaciones</h2><p className="mb-5 text-sm text-slate-500">Distribución por estado registrado.</p>
              {Object.keys(porEstado).length ? <div className="grid gap-3 sm:grid-cols-2">{Object.entries(porEstado).map(([estado, cantidad]) => <div key={estado} className="rounded-xl bg-slate-50 p-4"><p className="text-xs font-bold uppercase tracking-wide text-slate-500">{estado}</p><p className="mt-1 text-2xl font-bold text-slate-900">{cantidad}</p></div>)}</div> : <EmptyState>Sin estados para mostrar.</EmptyState>}
              <div className="mt-5 rounded-xl bg-emerald-50 p-4"><p className="text-xs font-bold uppercase tracking-wide text-emerald-800">Riesgo y patrones</p><p className="mt-1 text-sm leading-6 text-emerald-950">{resumen.informe || 'No hay patrones descritos para este período.'}</p></div>
            </div>
          </div>

          <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <h2 className="mb-1 font-semibold text-slate-900">Top transacciones por monto</h2><p className="mb-4 text-sm text-slate-500">Las cinco operaciones de mayor valor del período.</p>
            {top.length ? <DataTable columns={columnas} rows={top} /> : <EmptyState>No hay transacciones para ordenar.</EmptyState>}
          </div>
        </div>
      )}
    </section>
  )
}

function MetricCard({ title, value, detail }) {
  return <article className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm"><p className="text-sm text-slate-500">{title}</p><p className="mt-2 truncate text-2xl font-bold text-slate-950">{value}</p><p className="mt-1 text-xs text-slate-400">{detail}</p></article>
}
