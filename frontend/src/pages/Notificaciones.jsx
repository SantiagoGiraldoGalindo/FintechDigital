import { useCallback, useEffect, useState } from 'react'
import api from '../services/api.js'
import { listarNotificaciones, listarUsuarios, mensajeError } from '../services/fintechService.js'
import { buttonClass, EmptyState, ErrorMessage, inputClass, labelClass, Loading, Notice, PageHeading } from '../components/Feedback.jsx'

export default function Notificaciones() {
  const [usuarios, setUsuarios] = useState([])
  const [idUsuario, setIdUsuario] = useState('')
  const [notificaciones, setNotificaciones] = useState([])
  const [pendientes, setPendientes] = useState(0)
  const [cargando, setCargando] = useState(true)
  const [despachando, setDespachando] = useState(false)
  const [error, setError] = useState('')
  const [aviso, setAviso] = useState('')

  const cargar = useCallback(async () => {
    if (!idUsuario) return
    setCargando(true)
    setError('')
    try {
      const [recientes, cola] = await Promise.all([
        listarNotificaciones(idUsuario),
        api.get('/notificaciones/pendientes').then(({ data }) => data),
      ])
      setNotificaciones(recientes)
      setPendientes(cola.cantidad)
    } catch (e) {
      setError(mensajeError(e))
    } finally {
      setCargando(false)
    }
  }, [idUsuario])

  useEffect(() => {
    let activo = true
    listarUsuarios()
      .then((data) => {
        if (activo) {
          setUsuarios(data)
          if (data.length) setIdUsuario(String(data[0].id))
        }
      })
      .catch((e) => { if (activo) setError(mensajeError(e)) })
      .finally(() => { if (activo) setCargando(false) })
    return () => { activo = false }
  }, [])
  useEffect(() => { if (idUsuario) cargar() }, [idUsuario, cargar])

  const despachar = async () => {
    setDespachando(true)
    setError('')
    setAviso('')
    try {
      const { data } = await api.post('/notificaciones/despachar')
      setAviso(`Se despacharon ${data.length} notificaciones pendientes.`)
      await cargar()
    } catch (e) {
      setError(mensajeError(e))
    } finally {
      setDespachando(false)
    }
  }

  return (
    <section>
      <PageHeading eyebrow="Centro de actividad" title="Notificaciones" description="Revisa los avisos recientes de un usuario y despacha los pendientes." />
      <ErrorMessage onRetry={cargar}>{error}</ErrorMessage>
      <Notice>{aviso}</Notice>
      <div className="mb-6 flex flex-wrap items-end justify-between gap-4 rounded-2xl border border-slate-200 bg-white p-5">
        <label className={`${labelClass} w-full max-w-md`}>Usuario
          <select className={inputClass} value={idUsuario} onChange={(e) => setIdUsuario(e.target.value)}>
            {usuarios.map((u) => <option key={u.id} value={u.id}>{u.nombre} · {u.correo}</option>)}
          </select>
        </label>
        <div className="flex items-center gap-4">
          <span className="text-sm text-slate-600"><strong className="text-slate-950">{pendientes}</strong> pendientes</span>
          <button className={buttonClass} onClick={despachar} disabled={despachando}>{despachando ? 'Despachando…' : 'Despachar pendientes'}</button>
        </div>
      </div>
      {cargando ? <Loading label="Cargando notificaciones…" /> : !notificaciones.length ? <EmptyState>Este usuario aún no tiene notificaciones recientes.</EmptyState> : (
        <ol className="space-y-3">
          {notificaciones.map((n) => (
            <li key={n.id} className="flex gap-4 rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
              <span className="mt-1 flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-emerald-50 text-emerald-700" aria-hidden="true">●</span>
              <div className="min-w-0 flex-1">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <span className="text-xs font-bold uppercase tracking-wide text-emerald-800">{n.tipo}</span>
                  <time className="text-xs text-slate-500">{n.fecha ? new Date(n.fecha).toLocaleString() : '—'}</time>
                </div>
                <p className="mt-1 text-sm leading-6 text-slate-800">{n.mensaje}</p>
              </div>
            </li>
          ))}
        </ol>
      )}
    </section>
  )
}
