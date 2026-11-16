import { useCallback, useEffect, useState } from 'react'
import { crearBilletera, listarBilleteras, listarUsuarios, mensajeError } from '../services/fintechService.js'
import { buttonClass, DataTable, EmptyState, ErrorMessage, inputClass, labelClass, Loading, Notice, PageHeading } from '../components/Feedback.jsx'

const tipos = ['GASTOS_DIARIOS', 'AHORRO', 'TRANSPORTE', 'COMPRAS', 'SUSCRIPCIONES', 'INVERSION']

export default function Billeteras() {
  const [usuarios, setUsuarios] = useState([])
  const [idUsuario, setIdUsuario] = useState('')
  const [billeteras, setBilleteras] = useState([])
  const [form, setForm] = useState({ codigo: '', nombre: '', tipo: 'AHORRO' })
  const [cargando, setCargando] = useState(true)
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState('')
  const [aviso, setAviso] = useState('')

  const cargarBilleteras = useCallback(async (id = idUsuario) => {
    if (!id) { setBilleteras([]); return }
    try {
      setError('')
      setBilleteras(await listarBilleteras(id))
    } catch (e) {
      setError(mensajeError(e))
    }
  }, [idUsuario])

  useEffect(() => {
    let activo = true
    listarUsuarios()
      .then((data) => {
        if (!activo) return
        setUsuarios(data)
        if (data.length) setIdUsuario(String(data[0].id))
      })
      .catch((e) => { if (activo) setError(mensajeError(e)) })
      .finally(() => { if (activo) setCargando(false) })
    return () => { activo = false }
  }, [])

  useEffect(() => {
    if (idUsuario) cargarBilleteras(idUsuario)
  }, [idUsuario, cargarBilleteras])

  const crear = async (event) => {
    event.preventDefault()
    setGuardando(true)
    setError('')
    setAviso('')
    try {
      await crearBilletera({ ...form, idUsuario: Number(idUsuario) })
      setForm({ codigo: '', nombre: '', tipo: 'AHORRO' })
      setAviso('Billetera creada correctamente.')
      await cargarBilleteras()
    } catch (e) {
      setError(mensajeError(e))
    } finally {
      setGuardando(false)
    }
  }

  const columnas = [
    { key: 'codigo', label: 'Código', render: (b) => <span className="font-semibold text-slate-900">{b.codigo}</span> },
    { key: 'nombre', label: 'Billetera' },
    { key: 'tipo', label: 'Categoría' },
    { key: 'saldo', label: 'Saldo', render: (b) => `$${Number(b.saldo).toLocaleString('es-CO')}` },
    { key: 'estado', label: 'Estado', render: (b) => <span className={`rounded-full px-2.5 py-1 text-xs font-semibold ${b.estado === 'ACTIVO' ? 'bg-emerald-50 text-emerald-800' : 'bg-slate-100 text-slate-600'}`}>{b.estado}</span> },
  ]

  return (
    <section>
      <PageHeading eyebrow="Cuentas" title="Billeteras" description="Consulta las billeteras vinculadas a cada usuario y crea nuevas." />
      <ErrorMessage onRetry={() => cargarBilleteras()}>{error}</ErrorMessage>
      <Notice>{aviso}</Notice>
      {cargando ? <Loading label="Cargando usuarios…" /> : !usuarios.length ? <EmptyState>Registra un usuario antes de crear billeteras.</EmptyState> : (
        <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_340px]">
          <div>
            <label className={`${labelClass} mb-4 max-w-sm`}>Usuario
              <select className={inputClass} value={idUsuario} onChange={(e) => setIdUsuario(e.target.value)}>
                {usuarios.map((u) => <option key={u.id} value={u.id}>{u.nombre} · {u.correo}</option>)}
              </select>
            </label>
            {billeteras.length ? <DataTable columns={columnas} rows={billeteras} /> : <EmptyState>Este usuario aún no tiene billeteras.</EmptyState>}
          </div>
          <form onSubmit={crear} className="h-fit rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <h2 className="mb-1 font-semibold text-slate-900">Nueva billetera</h2>
            <p className="mb-5 text-xs text-slate-500">La billetera se asociará al usuario seleccionado.</p>
            <label className={labelClass}>Código<input className={inputClass} value={form.codigo} onChange={(e) => setForm({ ...form, codigo: e.target.value })} required /></label>
            <label className={`${labelClass} mt-4`}>Nombre<input className={inputClass} value={form.nombre} onChange={(e) => setForm({ ...form, nombre: e.target.value })} required /></label>
            <label className={`${labelClass} mt-4`}>Categoría
              <select className={inputClass} value={form.tipo} onChange={(e) => setForm({ ...form, tipo: e.target.value })}>
                {tipos.map((tipo) => <option key={tipo} value={tipo}>{tipo.replaceAll('_', ' ')}</option>)}
              </select>
            </label>
            <button className={`${buttonClass} mt-5 w-full`} disabled={guardando}>{guardando ? 'Creando…' : 'Crear billetera'}</button>
          </form>
        </div>
      )}
    </section>
  )
}
