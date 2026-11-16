import { useCallback, useEffect, useState } from 'react'
import api from '../services/api.js'
import { listarUsuarios, mensajeError } from '../services/fintechService.js'
import { buttonClass, DataTable, EmptyState, ErrorMessage, inputClass, labelClass, Loading, Notice, PageHeading } from '../components/Feedback.jsx'

export default function Recompensas() {
  const [usuarios, setUsuarios] = useState([])
  const [ranking, setRanking] = useState([])
  const [beneficios, setBeneficios] = useState([])
  const [canjes, setCanjes] = useState([])
  const [idUsuario, setIdUsuario] = useState('')
  const [rango, setRango] = useState({ minimo: '0', maximo: '1000' })
  const [usuariosRango, setUsuariosRango] = useState([])
  const [cargando, setCargando] = useState(true)
  const [cargandoRango, setCargandoRango] = useState(false)
  const [canjeando, setCanjeando] = useState('')
  const [error, setError] = useState('')
  const [aviso, setAviso] = useState('')

  const cargarBase = useCallback(async () => {
    setCargando(true)
    setError('')
    try {
      const [listaUsuarios, listaRanking, catalogo] = await Promise.all([
        listarUsuarios(),
        api.get('/puntos/ranking').then(({ data }) => data),
        api.get('/beneficios').then(({ data }) => data),
      ])
      setUsuarios(listaUsuarios)
      setRanking(listaRanking)
      setBeneficios(catalogo)
      if (!idUsuario && listaUsuarios.length) setIdUsuario(String(listaUsuarios[0].id))
    } catch (e) { setError(mensajeError(e)) }
    finally { setCargando(false) }
  }, [idUsuario])
  useEffect(() => { cargarBase() }, [cargarBase])

  const cargarCanjes = useCallback(async () => {
    if (!idUsuario) return
    try { setCanjes(await api.get(`/usuarios/${idUsuario}/canjes`).then(({ data }) => data)) }
    catch (e) { setError(mensajeError(e)) }
  }, [idUsuario])
  useEffect(() => { if (idUsuario) cargarCanjes() }, [idUsuario, cargarCanjes])

  const buscarRango = async (event) => {
    event.preventDefault()
    setCargandoRango(true)
    setError('')
    try {
      setUsuariosRango(await api.get('/puntos/rango', { params: { minimo: Number(rango.minimo), maximo: Number(rango.maximo) } }).then(({ data }) => data))
    } catch (e) { setError(mensajeError(e)) }
    finally { setCargandoRango(false) }
  }
  const canjear = async (beneficio) => {
    setCanjeando(String(beneficio.id))
    setError('')
    setAviso('')
    try {
      await api.post(`/usuarios/${idUsuario}/canjes`, { idBeneficio: beneficio.id })
      setAviso(`Canjeaste “${beneficio.nombre}”.`)
      await Promise.all([cargarCanjes(), cargarBase()])
    } catch (e) { setError(mensajeError(e)) }
    finally { setCanjeando('') }
  }

  const columnasRanking = [
    { key: 'posicion', label: '#', render: (u) => u.posicion ?? '—' },
    { key: 'nombre', label: 'Usuario', render: (u) => <><span className="font-semibold text-slate-900">{u.nombre}</span><span className="block text-xs text-slate-500">{u.correo}</span></> },
    { key: 'puntos', label: 'Puntos' },
    { key: 'nivel', label: 'Nivel', render: (u) => <span className="rounded-full bg-emerald-50 px-2.5 py-1 text-xs font-semibold text-emerald-800">{u.nivel}</span> },
  ]
  const columnasCanjes = [
    { key: 'id', label: 'Canje', render: (c) => `#${c.id}` },
    { key: 'beneficio', label: 'Beneficio' },
    { key: 'puntosDescontados', label: 'Puntos' },
    { key: 'fecha', label: 'Fecha', render: (c) => c.fecha ? new Date(c.fecha).toLocaleString() : '—' },
  ]

  return (
    <section>
      <PageHeading eyebrow="Fidelidad" title="Recompensas y niveles" description="Consulta puntos, explora el ranking y canjea beneficios disponibles." />
      <ErrorMessage onRetry={cargarBase}>{error}</ErrorMessage>
      <Notice>{aviso}</Notice>
      {cargando ? <Loading label="Cargando recompensas…" /> : (
        <div className="space-y-6">
          <div className="grid gap-6 lg:grid-cols-2">
            <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
              <div className="mb-4 flex flex-wrap items-end justify-between gap-3"><div><h2 className="text-lg font-semibold">Ranking de puntos</h2><p className="text-sm text-slate-500">Usuarios ordenados por nivel y puntos.</p></div><label className={`${labelClass} min-w-56`}>Usuario para canjes<select className={inputClass} value={idUsuario} onChange={(e) => setIdUsuario(e.target.value)}>{usuarios.map((u) => <option key={u.id} value={u.id}>{u.nombre} · {u.puntos} pts</option>)}</select></label></div>
              <DataTable columns={columnasRanking} rows={ranking.map((u, index) => ({ ...u, posicion: index + 1 }))} />
            </div>
            <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
              <h2 className="text-lg font-semibold">Buscar por rango</h2>
              <p className="mb-4 text-sm text-slate-500">Encuentra usuarios entre un mínimo y un máximo de puntos.</p>
              <form className="mb-4 flex flex-wrap items-end gap-3" onSubmit={buscarRango}>
                <label className={`${labelClass} flex-1`}>Mínimo<input className={inputClass} type="number" min="0" value={rango.minimo} onChange={(e) => setRango({ ...rango, minimo: e.target.value })} required /></label>
                <label className={`${labelClass} flex-1`}>Máximo<input className={inputClass} type="number" min="0" value={rango.maximo} onChange={(e) => setRango({ ...rango, maximo: e.target.value })} required /></label>
                <button className={buttonClass} disabled={cargandoRango}>{cargandoRango ? 'Buscando…' : 'Buscar'}</button>
              </form>
              {usuariosRango.length ? <DataTable columns={columnasRanking} rows={usuariosRango} /> : <EmptyState>{cargandoRango ? 'Consultando…' : 'Realiza una búsqueda para ver usuarios por rango.'}</EmptyState>}
            </div>
          </div>

          <div>
            <div className="mb-4"><h2 className="text-lg font-semibold text-slate-900">Catálogo de beneficios</h2><p className="text-sm text-slate-500">Los puntos se descuentan al confirmar cada canje.</p></div>
            {beneficios.length ? <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
              {beneficios.map((beneficio) => {
                const usuario = usuarios.find((u) => String(u.id) === idUsuario)
                return <article key={beneficio.id} className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
                  <div className="flex items-start justify-between gap-3"><h3 className="font-semibold text-slate-900">{beneficio.nombre}</h3><span className="shrink-0 rounded-full bg-amber-50 px-2.5 py-1 text-xs font-bold text-amber-800">{beneficio.costoPuntos} pts</span></div>
                  <p className="mt-2 min-h-10 text-sm text-slate-500">{beneficio.descripcion}</p>
                  <button className={`${buttonClass} mt-4 w-full`} disabled={!idUsuario || canjeando === String(beneficio.id) || usuario?.puntos < beneficio.costoPuntos} onClick={() => canjear(beneficio)}>{canjeando === String(beneficio.id) ? 'Canjeando…' : usuario?.puntos < beneficio.costoPuntos ? 'Puntos insuficientes' : 'Canjear beneficio'}</button>
                </article>
              })}
            </div> : <EmptyState>No hay beneficios disponibles en el catálogo.</EmptyState>}
          </div>

          <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <h2 className="mb-4 text-lg font-semibold text-slate-900">Canjes recientes</h2>
            {canjes.length ? <DataTable columns={columnasCanjes} rows={canjes} /> : <EmptyState>El usuario seleccionado aún no tiene canjes.</EmptyState>}
          </div>
        </div>
      )}
    </section>
  )
}
