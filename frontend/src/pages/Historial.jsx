import { useCallback, useEffect, useState } from 'react'
import api from '../services/api.js'
import { consultarHistorial, listarUsuarios, mensajeError } from '../services/fintechService.js'
import { buttonClass, DataTable, EmptyState, ErrorMessage, inputClass, labelClass, Loading, PageHeading } from '../components/Feedback.jsx'

const filtrosVacios = { idUsuario: '', codigoBilletera: '', desde: '', hasta: '' }

export default function Historial() {
  const [usuarios, setUsuarios] = useState([])
  const [filtros, setFiltros] = useState(filtrosVacios)
  const [transacciones, setTransacciones] = useState([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')

  const cargarUsuarios = useCallback(async () => {
    try { setUsuarios(await listarUsuarios()) } catch (e) { setError(mensajeError(e)) }
  }, [])
  useEffect(() => { cargarUsuarios() }, [cargarUsuarios])

  const buscar = useCallback(async (event) => {
    event?.preventDefault()
    if (Boolean(filtros.desde) !== Boolean(filtros.hasta)) {
      setError('Para filtrar por fechas, completa tanto el inicio como el fin.')
      return
    }
    if (filtros.desde && new Date(filtros.desde) > new Date(filtros.hasta)) {
      setError('La fecha inicial debe ser anterior a la fecha final.')
      return
    }
    setCargando(true)
    setError('')
    try {
      const params = {}
      if (filtros.desde) {
        params.desde = filtros.desde
        params.hasta = filtros.hasta
      } else if (filtros.idUsuario) {
        params.idUsuario = filtros.idUsuario
      } else if (filtros.codigoBilletera.trim()) {
        params.codigoBilletera = filtros.codigoBilletera.trim()
      }
      let resultado = await consultarHistorial(params)
      if (filtros.idUsuario) {
        const usuarioId = Number(filtros.idUsuario)
        const billeteras = await api.get('/billeteras', { params: { idUsuario: usuarioId } }).then(({ data }) => data)
        const ids = new Set(billeteras.map((b) => b.id))
        resultado = resultado.filter((t) => ids.has(t.idOrigen) || ids.has(t.idDestino))
      }
      if (filtros.codigoBilletera.trim()) {
        const billetera = await api.get(`/billeteras/${encodeURIComponent(filtros.codigoBilletera.trim())}`).then(({ data }) => data)
        resultado = resultado.filter((t) => t.idOrigen === billetera.id || t.idDestino === billetera.id)
      }
      setTransacciones(resultado)
    } catch (e) {
      setError(mensajeError(e))
    } finally {
      setCargando(false)
    }
  }, [filtros])

  useEffect(() => { buscar() }, []) // Load the unfiltered history on entry.

  const limpiar = () => {
    setFiltros(filtrosVacios)
    setCargando(true)
    setError('')
    consultarHistorial({})
      .then(setTransacciones)
      .catch((e) => setError(mensajeError(e)))
      .finally(() => setCargando(false))
  }
  const columnas = [
    { key: 'id', label: 'ID', render: (t) => `#${t.id}` },
    { key: 'fecha', label: 'Fecha', render: (t) => t.fecha ? new Date(t.fecha).toLocaleString() : '—' },
    { key: 'tipo', label: 'Tipo' },
    { key: 'valor', label: 'Monto', render: (t) => `$${Number(t.valor).toLocaleString('es-CO')}` },
    { key: 'estado', label: 'Estado', render: (t) => <span className={`rounded-full px-2.5 py-1 text-xs font-semibold ${t.estado === 'COMPLETADA' ? 'bg-emerald-50 text-emerald-800' : 'bg-amber-50 text-amber-800'}`}>{t.estado}</span> },
    { key: 'puntosGenerados', label: 'Puntos' },
  ]

  return (
    <section>
      <PageHeading eyebrow="Consulta" title="Historial de operaciones" description="Filtra transacciones por usuario, billetera o intervalo de fechas." />
      <ErrorMessage onRetry={() => buscar()}>{error}</ErrorMessage>
      <form onSubmit={buscar} className="mb-6 grid gap-4 rounded-2xl border border-slate-200 bg-white p-5 sm:grid-cols-2 lg:grid-cols-5">
        <label className={labelClass}>Usuario
          <select className={inputClass} value={filtros.idUsuario} onChange={(e) => setFiltros({ ...filtros, idUsuario: e.target.value })}>
            <option value="">Todos</option>{usuarios.map((u) => <option key={u.id} value={u.id}>{u.nombre}</option>)}
          </select>
        </label>
        <label className={labelClass}>Código de billetera<input className={inputClass} value={filtros.codigoBilletera} onChange={(e) => setFiltros({ ...filtros, codigoBilletera: e.target.value })} placeholder="DEMO-ANA-A" /></label>
        <label className={labelClass}>Desde<input className={inputClass} type="datetime-local" value={filtros.desde} onChange={(e) => setFiltros({ ...filtros, desde: e.target.value })} /></label>
        <label className={labelClass}>Hasta<input className={inputClass} type="datetime-local" value={filtros.hasta} onChange={(e) => setFiltros({ ...filtros, hasta: e.target.value })} /></label>
        <div className="flex items-end gap-2"><button className={`${buttonClass} flex-1`} disabled={cargando}>{cargando ? 'Buscando…' : 'Filtrar'}</button><button type="button" className="rounded-lg border border-slate-300 px-3 py-2.5 text-sm font-semibold text-slate-700 hover:bg-slate-50" onClick={limpiar}>Limpiar</button></div>
      </form>
      {cargando ? <Loading label="Consultando historial…" /> : transacciones.length ? <DataTable columns={columnas} rows={transacciones} /> : <EmptyState>No hay operaciones que coincidan con los filtros.</EmptyState>}
    </section>
  )
}
