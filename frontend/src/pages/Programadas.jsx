import { useCallback, useEffect, useState } from 'react'
import api from '../services/api.js'
import { listarUsuarios, listarBilleteras, mensajeError } from '../services/fintechService.js'
import { buttonClass, DataTable, EmptyState, ErrorMessage, inputClass, labelClass, Loading, Notice, PageHeading } from '../components/Feedback.jsx'

const tipos = ['TRANSFERENCIA_PERIODICA', 'RECARGA_AUTOMATICA', 'PAGO_RECURRENTE', 'AHORRO_AUTOMATICO_SEMANAL']
const ahoraLocal = () => {
  const fecha = new Date(Date.now() - new Date().getTimezoneOffset() * 60000)
  return fecha.toISOString().slice(0, 16)
}

export default function Programadas() {
  const [usuarios, setUsuarios] = useState([])
  const [billeteras, setBilleteras] = useState([])
  const [idUsuario, setIdUsuario] = useState('')
  const [programadas, setProgramadas] = useState([])
  const [form, setForm] = useState({ tipo: tipos[0], codigoOrigen: '', codigoDestino: '', monto: '', horaProgramada: ahoraLocal(), periodicidad: 'P1D' })
  const [cargando, setCargando] = useState(true)
  const [guardando, setGuardando] = useState(false)
  const [simulando, setSimulando] = useState(false)
  const [avance, setAvance] = useState('PT1H')
  const [horaActual, setHoraActual] = useState('')
  const [ejecutadas, setEjecutadas] = useState([])
  const [error, setError] = useState('')
  const [aviso, setAviso] = useState('')

  const cargarCola = useCallback(async (id = idUsuario) => {
    if (!id) return
    try {
      const [cola, hora] = await Promise.all([
        api.get(`/programadas/${id}`).then(({ data }) => data),
        api.get('/simulacion/hora').then(({ data }) => data),
      ])
      setProgramadas(cola)
      setHoraActual(hora)
    } catch (e) { setError(mensajeError(e)) }
  }, [idUsuario])

  useEffect(() => {
    let activo = true
    listarUsuarios()
      .then(async (data) => {
        const todas = (await Promise.all(data.map((u) => listarBilleteras(u.id)))).flat()
        if (!activo) return
        setUsuarios(data)
        setBilleteras(todas)
        if (data.length) setIdUsuario(String(data[0].id))
      })
      .catch((e) => { if (activo) setError(mensajeError(e)) })
      .finally(() => { if (activo) setCargando(false) })
    return () => { activo = false }
  }, [])
  useEffect(() => { if (idUsuario) cargarCola(idUsuario) }, [idUsuario, cargarCola])

  const crear = async (event) => {
    event.preventDefault()
    setGuardando(true)
    setError('')
    setAviso('')
    try {
      await api.post('/programadas', {
        tipo: form.tipo,
        codigoOrigen: form.codigoOrigen || null,
        codigoDestino: form.codigoDestino,
        monto: Number(form.monto),
        horaProgramada: form.horaProgramada,
        periodicidad: form.periodicidad,
      })
      setAviso('La operación se agregó a la cola de ejecución.')
      await cargarCola()
    } catch (e) { setError(mensajeError(e)) }
    finally { setGuardando(false) }
  }

  const simular = async () => {
    setSimulando(true)
    setError('')
    setAviso('')
    try {
      const { data } = await api.post('/simulacion/avanzar', { avanzar: avance })
      setHoraActual(data.horaActual)
      setEjecutadas(data.ejecutadas || [])
      setAviso(`Reloj avanzado. ${data.ejecutadas?.length || 0} operación(es) ejecutada(s).`)
      await cargarCola()
    } catch (e) { setError(mensajeError(e)) }
    finally { setSimulando(false) }
  }

  const columnas = [
    { key: 'id', label: 'ID', render: (p) => `#${p.id}` },
    { key: 'tipo', label: 'Tipo', render: (p) => p.tipo?.replaceAll('_', ' ') },
    { key: 'horaProgramada', label: 'Próxima ejecución', render: (p) => p.horaProgramada ? new Date(p.horaProgramada).toLocaleString() : '—' },
    { key: 'codigoOrigen', label: 'Origen', render: (p) => p.codigoOrigen || '—' },
    { key: 'codigoDestino', label: 'Destino' },
    { key: 'monto', label: 'Monto', render: (p) => `$${Number(p.monto).toLocaleString('es-CO')}` },
    { key: 'estado', label: 'Estado' },
  ]

  return (
    <section>
      <PageHeading eyebrow="Automatización" title="Operaciones programadas" description="Crea planes recurrentes, consulta la cola y avanza el reloj simulado." />
      <ErrorMessage onRetry={() => cargarCola()}>{error}</ErrorMessage>
      <Notice>{aviso}</Notice>
      {cargando ? <Loading label="Cargando billeteras y usuarios…" /> : !usuarios.length || !billeteras.length ? <EmptyState>Se necesitan usuarios con billeteras para programar operaciones.</EmptyState> : (
        <div className="space-y-6">
          <div className="grid gap-6 xl:grid-cols-[minmax(0,1.1fr)_minmax(320px,.9fr)]">
            <form onSubmit={crear} className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm sm:p-6">
              <h2 className="mb-5 text-lg font-semibold text-slate-900">Crear programación</h2>
              <div className="grid gap-4 sm:grid-cols-2">
                <label className={labelClass}>Tipo<select className={inputClass} value={form.tipo} onChange={(e) => setForm({ ...form, tipo: e.target.value })}>{tipos.map((tipo) => <option key={tipo} value={tipo}>{tipo.replaceAll('_', ' ')}</option>)}</select></label>
                <label className={labelClass}>Usuario de la cola<select className={inputClass} value={idUsuario} onChange={(e) => setIdUsuario(e.target.value)}>{usuarios.map((u) => <option key={u.id} value={u.id}>{u.nombre}</option>)}</select></label>
                <label className={labelClass}>Billetera origen<select className={inputClass} value={form.codigoOrigen} onChange={(e) => setForm({ ...form, codigoOrigen: e.target.value })}><option value="">Sin origen (recarga)</option>{billeteras.map((b) => <option key={b.codigo} value={b.codigo}>{b.codigo} · {b.nombre}</option>)}</select></label>
                <label className={labelClass}>Billetera destino<select className={inputClass} value={form.codigoDestino} onChange={(e) => setForm({ ...form, codigoDestino: e.target.value })} required><option value="">Selecciona destino</option>{billeteras.map((b) => <option key={b.codigo} value={b.codigo}>{b.codigo} · {b.nombre}</option>)}</select></label>
                <label className={labelClass}>Monto<input className={inputClass} type="number" min="0.01" step="0.01" value={form.monto} onChange={(e) => setForm({ ...form, monto: e.target.value })} required /></label>
                <label className={labelClass}>Periodicidad ISO-8601<input className={inputClass} value={form.periodicidad} onChange={(e) => setForm({ ...form, periodicidad: e.target.value })} placeholder="P1D, PT12H, P7D" required /><span className="mt-1 block text-xs font-normal text-slate-500">Ejemplos: P1D (diario), P7D (semanal).</span></label>
                <label className={`${labelClass} sm:col-span-2`}>Primera ejecución<input className={inputClass} type="datetime-local" value={form.horaProgramada} onChange={(e) => setForm({ ...form, horaProgramada: e.target.value })} required /></label>
              </div>
              <button className={`${buttonClass} mt-5 w-full sm:w-auto`} disabled={guardando}>{guardando ? 'Guardando…' : 'Agregar a la cola'}</button>
            </form>

            <div className="flex flex-col justify-between rounded-2xl bg-slate-950 p-6 text-white">
              <div>
                <p className="text-xs font-bold uppercase tracking-[0.18em] text-emerald-300">Simulación</p>
                <h2 className="mt-2 text-xl font-semibold">Reloj del sistema</h2>
                <p className="mt-2 text-sm text-slate-300">La simulación ejecuta las operaciones vencidas en orden de prioridad y actualiza las recurrentes.</p>
                <div className="mt-6 rounded-xl bg-white/10 p-4">
                  <p className="text-xs text-slate-400">Hora actual</p>
                  <p className="mt-1 text-lg font-semibold">{horaActual ? new Date(horaActual).toLocaleString() : '—'}</p>
                </div>
              </div>
              <div className="mt-6 flex flex-col gap-3 sm:flex-row">
                <input aria-label="Duración para avanzar" className="w-full rounded-lg border border-white/20 bg-white/10 px-3 py-2.5 text-sm text-white placeholder:text-slate-400" value={avance} onChange={(e) => setAvance(e.target.value)} placeholder="PT1H" />
                <button className="shrink-0 rounded-lg bg-emerald-500 px-4 py-2.5 text-sm font-semibold text-slate-950 hover:bg-emerald-400 disabled:opacity-60" onClick={simular} disabled={simulando}>{simulando ? 'Ejecutando…' : 'Simular ejecución'}</button>
              </div>
            </div>
          </div>

          <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <div className="mb-4 flex flex-wrap items-end justify-between gap-3">
              <div><h2 className="text-lg font-semibold text-slate-900">Cola de operaciones</h2><p className="mt-1 text-sm text-slate-500">Planes asociados al usuario seleccionado.</p></div>
              <label className={`${labelClass} min-w-56`}>Usuario<select className={inputClass} value={idUsuario} onChange={(e) => setIdUsuario(e.target.value)}>{usuarios.map((u) => <option key={u.id} value={u.id}>{u.nombre}</option>)}</select></label>
            </div>
            {programadas.length ? <DataTable columns={columnas} rows={programadas} /> : <EmptyState>La cola de este usuario está vacía.</EmptyState>}
            {!!ejecutadas.length && <div className="mt-5 border-t border-slate-100 pt-4"><h3 className="mb-2 text-sm font-semibold">Operaciones ejecutadas en la última simulación</h3><div className="flex flex-wrap gap-2">{ejecutadas.map((t) => <span key={t.id} className="rounded-lg bg-emerald-50 px-3 py-2 text-xs text-emerald-900">#{t.id} · {t.tipo} · ${Number(t.valor).toLocaleString('es-CO')}</span>)}</div></div>}
          </div>
        </div>
      )}
    </section>
  )
}
