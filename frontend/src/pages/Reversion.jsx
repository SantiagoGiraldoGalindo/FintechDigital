import { useCallback, useEffect, useState } from 'react'
import api from '../services/api.js'
import { listarUsuarios, mensajeError } from '../services/fintechService.js'
import { buttonClass, DataTable, EmptyState, ErrorMessage, inputClass, labelClass, Loading, Notice, PageHeading } from '../components/Feedback.jsx'

export default function Reversion() {
  const [usuarios, setUsuarios] = useState([])
  const [idUsuario, setIdUsuario] = useState('')
  const [transacciones, setTransacciones] = useState([])
  const [reversiones, setReversiones] = useState([])
  const [idTransaccion, setIdTransaccion] = useState('')
  const [motivo, setMotivo] = useState('')
  const [cargando, setCargando] = useState(true)
  const [revirtiendo, setRevirtiendo] = useState(false)
  const [error, setError] = useState('')
  const [aviso, setAviso] = useState('')

  const cargarTransacciones = useCallback(async () => {
    try {
      const lista = await api.get('/historial').then(({ data }) => data)
      const elegibles = lista.filter((t) => t.estado === 'COMPLETADA'
        && (['TRANSFERENCIA', 'RECARGA'].includes(t.tipo) || (t.tipo === 'PAGO_PROGRAMADO' && t.duplicada)))
      setTransacciones(elegibles)
      if (elegibles.length && !elegibles.some((t) => String(t.id) === idTransaccion)) setIdTransaccion(String(elegibles[0].id))
    } catch (e) { setError(mensajeError(e)) }
  }, [idTransaccion])
  const cargarReversiones = useCallback(async () => {
    if (!idUsuario) return
    try { setReversiones(await api.get(`/usuarios/${idUsuario}/reversiones`).then(({ data }) => data)) }
    catch (e) { setError(mensajeError(e)) }
  }, [idUsuario])

  useEffect(() => {
    let activo = true
    Promise.all([listarUsuarios(), api.get('/historial').then(({ data }) => data)])
      .then(([listaUsuarios, listaTransacciones]) => {
        if (!activo) return
        setUsuarios(listaUsuarios)
        const elegibles = listaTransacciones.filter((t) => t.estado === 'COMPLETADA'
          && (['TRANSFERENCIA', 'RECARGA'].includes(t.tipo) || (t.tipo === 'PAGO_PROGRAMADO' && t.duplicada)))
        setTransacciones(elegibles)
        if (elegibles.length) setIdTransaccion(String(elegibles[0].id))
        if (listaUsuarios.length) setIdUsuario(String(listaUsuarios[0].id))
      })
      .catch((e) => { if (activo) setError(mensajeError(e)) })
      .finally(() => { if (activo) setCargando(false) })
    return () => { activo = false }
  }, [])
  useEffect(() => { if (idUsuario) cargarReversiones() }, [idUsuario, cargarReversiones])

  const revertir = async (event) => {
    event.preventDefault()
    setRevirtiendo(true)
    setError('')
    setAviso('')
    try {
      const { data } = await api.post('/reversiones', { idTransaccion: Number(idTransaccion), motivo: motivo.trim() })
      setAviso(`Transacción #${data.idTransaccion} revertida correctamente.`)
      setMotivo('')
      await Promise.all([cargarTransacciones(), cargarReversiones()])
    } catch (e) { setError(mensajeError(e)) }
    finally { setRevirtiendo(false) }
  }

  const columnas = [
    { key: 'id', label: 'Transacción', render: (t) => `#${t.id}` },
    { key: 'fecha', label: 'Fecha', render: (t) => t.fecha ? new Date(t.fecha).toLocaleString() : '—' },
    { key: 'tipo', label: 'Tipo' },
    { key: 'valor', label: 'Monto', render: (t) => `$${Number(t.valor).toLocaleString('es-CO')}` },
    { key: 'estado', label: 'Estado' },
  ]
  const columnasReversion = [
    { key: 'id', label: 'Reversión', render: (r) => `#${r.id}` },
    { key: 'idTransaccion', label: 'Transacción', render: (r) => `#${r.idTransaccion}` },
    { key: 'fecha', label: 'Fecha', render: (r) => r.fecha ? new Date(r.fecha).toLocaleString() : '—' },
    { key: 'motivo', label: 'Motivo' },
  ]

  return (
    <section>
      <PageHeading eyebrow="Control de operaciones" title="Reversión" description="Revierte recargas o transferencias cuando las reglas de saldo y estado lo permiten." />
      <ErrorMessage>{error}</ErrorMessage>
      <Notice>{aviso}</Notice>
      {cargando ? <Loading label="Consultando transacciones…" /> : (
        <div className="space-y-6">
          <div className="grid gap-6 lg:grid-cols-[minmax(0,1.1fr)_minmax(320px,.9fr)]">
            <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm"><h2 className="mb-1 text-lg font-semibold">Operaciones elegibles</h2><p className="mb-4 text-sm text-slate-500">Las transacciones revertidas dejan de estar disponibles para una segunda reversión.</p>{transacciones.length ? <DataTable columns={columnas} rows={transacciones} /> : <EmptyState>No hay recargas o transferencias completadas para revertir.</EmptyState>}</div>
            <form onSubmit={revertir} className="h-fit rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
              <h2 className="mb-5 text-lg font-semibold">Revertir una operación</h2>
              <label className={labelClass}>Transacción<select className={inputClass} value={idTransaccion} onChange={(e) => setIdTransaccion(e.target.value)} required>
                {!transacciones.length && <option value="">No hay operaciones elegibles</option>}
                {transacciones.map((t) => <option key={t.id} value={t.id}>#{t.id} · {t.tipo} · ${Number(t.valor).toLocaleString('es-CO')} · {new Date(t.fecha).toLocaleString()}</option>)}
              </select></label>
              <label className={`${labelClass} mt-4`}>Motivo<textarea className={`${inputClass} min-h-24 resize-y`} value={motivo} onChange={(e) => setMotivo(e.target.value)} required maxLength={300} placeholder="Describe por qué debe revertirse" /></label>
              <p className="mt-3 text-xs leading-5 text-slate-500">La reversión puede rechazarse si ya fue aplicada, si la operación no es reversible o si el saldo actual no permite compensarla.</p>
              <button className={`${buttonClass} mt-5 w-full`} disabled={revirtiendo || !transacciones.length}>{revirtiendo ? 'Revirtiendo…' : 'Confirmar reversión'}</button>
            </form>
          </div>
          <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <div className="mb-4 flex flex-wrap items-end justify-between gap-3"><div><h2 className="text-lg font-semibold">Historial de reversiones</h2><p className="text-sm text-slate-500">Reversiones asociadas al usuario seleccionado.</p></div><label className={`${labelClass} min-w-56`}>Usuario<select className={inputClass} value={idUsuario} onChange={(e) => setIdUsuario(e.target.value)}>{usuarios.map((u) => <option key={u.id} value={u.id}>{u.nombre}</option>)}</select></label></div>
            {reversiones.length ? <DataTable columns={columnasReversion} rows={reversiones} /> : <EmptyState>Este usuario aún no tiene reversiones.</EmptyState>}
          </div>
        </div>
      )}
    </section>
  )
}
