import { useCallback, useEffect, useState } from 'react'
import api from '../services/api.js'
import { listarUsuarios, mensajeError } from '../services/fintechService.js'
import { DataTable, EmptyState, ErrorMessage, inputClass, labelClass, Loading, PageHeading } from '../components/Feedback.jsx'

const fechaLocal = (dias = 0) => {
  const fecha = new Date(Date.now() - new Date().getTimezoneOffset() * 60000 - dias * 86400000)
  return fecha.toISOString().slice(0, 16)
}

export default function AlertasAuditoria() {
  const [usuarios, setUsuarios] = useState([])
  const [idUsuario, setIdUsuario] = useState('')
  const [alertas, setAlertas] = useState([])
  const [auditoria, setAuditoria] = useState([])
  const [rango, setRango] = useState({ desde: fechaLocal(30), hasta: fechaLocal() })
  const [cargando, setCargando] = useState(true)
  const [cargandoAuditoria, setCargandoAuditoria] = useState(false)
  const [error, setError] = useState('')

  const cargarAlertas = useCallback(async (id = idUsuario) => {
    if (!id) { setAlertas([]); return }
    try { setAlertas(await api.get(`/usuarios/${id}/alertas`).then(({ data }) => data)) }
    catch (e) { setError(mensajeError(e)) }
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
  useEffect(() => { if (idUsuario) cargarAlertas(idUsuario) }, [idUsuario, cargarAlertas])

  const buscarAuditoria = async (event) => {
    event?.preventDefault()
    setCargandoAuditoria(true)
    setError('')
    try {
      const params = { desde: rango.desde, hasta: rango.hasta }
      setAuditoria(await api.get('/auditoria', { params }).then(({ data }) => data))
    } catch (e) { setError(mensajeError(e)) }
    finally { setCargandoAuditoria(false) }
  }
  useEffect(() => { buscarAuditoria() }, [])

  const columnasAlertas = [
    { key: 'id', label: 'Alerta' },
    { key: 'fecha', label: 'Fecha', render: (a) => a.fecha ? new Date(a.fecha).toLocaleString() : '—' },
    { key: 'tipo', label: 'Tipo' },
    { key: 'riesgo', label: 'Riesgo', render: (a) => <span className={`rounded-full px-2.5 py-1 text-xs font-semibold ${a.riesgo === 'CRITICO' || a.riesgo === 'ALTO' ? 'bg-red-50 text-red-700' : 'bg-amber-50 text-amber-800'}`}>{a.riesgo}</span> },
    { key: 'mensaje', label: 'Detalle' },
    { key: 'idTransaccion', label: 'Transacción', render: (a) => a.idTransaccion ? `#${a.idTransaccion}` : '—' },
  ]
  const columnasAuditoria = [
    { key: 'fecha', label: 'Fecha', render: (a) => a.fecha ? new Date(a.fecha).toLocaleString() : '—' },
    { key: 'regla', label: 'Regla detectada' },
    { key: 'idTransaccion', label: 'Transacción', render: (a) => `#${a.idTransaccion}` },
    { key: 'idAlerta', label: 'Alerta', render: (a) => `#${a.idAlerta}` },
    { key: 'nivelRiesgo', label: 'Riesgo' },
  ]

  return (
    <section>
      <PageHeading eyebrow="Seguridad" title="Alertas y auditoría" description="Consulta alertas por usuario y revisa eventos de seguridad dentro de un período." />
      <ErrorMessage onRetry={() => { cargarAlertas(); buscarAuditoria() }}>{error}</ErrorMessage>
      {cargando ? <Loading label="Cargando usuarios…" /> : (
        <div className="space-y-6">
          <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <div className="mb-4 flex flex-wrap items-end justify-between gap-4">
              <div><h2 className="text-lg font-semibold">Alertas de usuario</h2><p className="text-sm text-slate-500">Incidentes y señales detectadas para el perfil.</p></div>
              <label className={`${labelClass} min-w-64`}>Usuario<select className={inputClass} value={idUsuario} onChange={(e) => setIdUsuario(e.target.value)}>{usuarios.map((u) => <option key={u.id} value={u.id}>{u.nombre}</option>)}</select></label>
            </div>
            {alertas.length ? <DataTable columns={columnasAlertas} rows={alertas} /> : <EmptyState>El usuario seleccionado no tiene alertas.</EmptyState>}
          </div>
          <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <div className="mb-4 flex flex-wrap items-end justify-between gap-4"><div><h2 className="text-lg font-semibold">Historial de auditoría</h2><p className="text-sm text-slate-500">Eventos analíticos y alertas registradas en el período.</p></div>
              <form className="flex flex-wrap items-end gap-3" onSubmit={buscarAuditoria}>
                <label className={labelClass}>Desde<input className={inputClass} type="datetime-local" value={rango.desde} onChange={(e) => setRango({ ...rango, desde: e.target.value })} required /></label>
                <label className={labelClass}>Hasta<input className={inputClass} type="datetime-local" value={rango.hasta} onChange={(e) => setRango({ ...rango, hasta: e.target.value })} required /></label>
                <button className="rounded-lg bg-slate-900 px-4 py-2.5 text-sm font-semibold text-white disabled:opacity-60" disabled={cargandoAuditoria}>{cargandoAuditoria ? 'Consultando…' : 'Consultar'}</button>
              </form>
            </div>
            {cargandoAuditoria ? <Loading label="Consultando eventos…" /> : auditoria.length ? <DataTable columns={columnasAuditoria} rows={auditoria} /> : <EmptyState>No se encontraron eventos para este intervalo.</EmptyState>}
          </div>
        </div>
      )}
    </section>
  )
}
