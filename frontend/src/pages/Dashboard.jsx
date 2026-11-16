import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import StatCard from '../components/StatCard.jsx'
import { DataTable, EmptyState, ErrorMessage, Loading, PageHeading } from '../components/Feedback.jsx'
import api from '../services/api.js'
import { listarUsuarios, mensajeError } from '../services/fintechService.js'

export default function Dashboard() {
  const [resumen, setResumen] = useState(null)
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')

  const cargar = useCallback(async () => {
    setCargando(true)
    setError('')
    try {
      const usuarios = await listarUsuarios()
      const [historial, billeterasPorUsuario] = await Promise.all([
        api.get('/historial').then(({ data }) => data),
        Promise.all(usuarios.map((usuario) => api.get('/billeteras', { params: { idUsuario: usuario.id } }).then(({ data }) => data))),
      ])
      const billeteras = billeterasPorUsuario.flat()
      const completadas = historial.filter((t) => t.estado === 'COMPLETADA')
      const puntos = usuarios.reduce((total, usuario) => total + Number(usuario.puntos || 0), 0)
      const saldo = billeteras.reduce((total, billetera) => total + Number(billetera.saldo || 0), 0)
      const promedio = completadas.length ? completadas.reduce((total, t) => total + Number(t.valor || 0), 0) / completadas.length : 0
      const porTipo = historial.reduce((acc, t) => {
        acc[t.tipo] = (acc[t.tipo] || 0) + 1
        return acc
      }, {})
      setResumen({
        usuarios: usuarios.length,
        billeteras: billeteras.length,
        saldo,
        transacciones: historial.length,
        completadas: completadas.length,
        puntos,
        promedio,
        actividad: Object.entries(porTipo).sort((a, b) => b[1] - a[1]),
        recientes: [...historial].sort((a, b) => new Date(b.fecha) - new Date(a.fecha)).slice(0, 6),
      })
    } catch (e) { setError(mensajeError(e)) }
    finally { setCargando(false) }
  }, [])
  useEffect(() => { cargar() }, [cargar])

  const columnas = [
    { key: 'id', label: 'ID', render: (t) => `#${t.id}` },
    { key: 'fecha', label: 'Fecha', render: (t) => t.fecha ? new Date(t.fecha).toLocaleString() : '—' },
    { key: 'tipo', label: 'Tipo' },
    { key: 'valor', label: 'Monto', render: (t) => `$${Number(t.valor).toLocaleString('es-CO')}` },
    { key: 'estado', label: 'Estado', render: (t) => <span className={`rounded-full px-2.5 py-1 text-xs font-semibold ${t.estado === 'COMPLETADA' ? 'bg-emerald-50 text-emerald-800' : 'bg-amber-50 text-amber-800'}`}>{t.estado}</span> },
  ]
  const maxActividad = Math.max(1, ...(resumen?.actividad || []).map(([, cantidad]) => cantidad))

  return (
    <section>
      <PageHeading eyebrow="Resumen de plataforma" title="Dashboard financiero" description="Indicadores en tiempo real de usuarios, saldos y actividad registrada." />
      <ErrorMessage onRetry={cargar}>{error}</ErrorMessage>
      {cargando && !resumen ? <Loading label="Cargando indicadores desde la API…" /> : resumen && (
        <div className="space-y-7">
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <StatCard titulo="Saldo en billeteras" valor={`$${resumen.saldo.toLocaleString('es-CO', { maximumFractionDigits: 2 })}`} detalle={`${resumen.billeteras} billeteras · ${resumen.usuarios} usuarios`} />
            <StatCard titulo="Transacciones" valor={resumen.transacciones.toLocaleString('es-CO')} detalle={`${resumen.completadas} completadas`} />
            <StatCard titulo="Puntos acumulados" valor={resumen.puntos.toLocaleString('es-CO')} detalle="Suma de puntos de los usuarios" />
            <StatCard titulo="Monto promedio" valor={`$${resumen.promedio.toLocaleString('es-CO', { maximumFractionDigits: 2 })}`} detalle="Promedio de operaciones completadas" />
          </div>

          <div className="grid gap-6 lg:grid-cols-[minmax(0,1.1fr)_minmax(280px,.9fr)]">
            <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
              <div className="mb-5 flex items-center justify-between gap-3"><div><h2 className="font-semibold text-slate-900">Actividad por tipo</h2><p className="mt-1 text-sm text-slate-500">Distribución de operaciones registradas.</p></div><Link className="text-sm font-semibold text-emerald-700 hover:underline" to="/analitica">Ver analítica →</Link></div>
              {resumen.actividad.length ? <div className="space-y-4">{resumen.actividad.map(([tipo, cantidad]) => <div key={tipo}>
                <div className="mb-1 flex justify-between text-sm"><span className="font-medium text-slate-700">{tipo.replaceAll('_', ' ')}</span><span className="text-slate-500">{cantidad}</span></div>
                <div className="h-2.5 overflow-hidden rounded-full bg-slate-100"><div className="h-full rounded-full bg-emerald-600" style={{ width: `${(cantidad / maxActividad) * 100}%` }} /></div>
              </div>)}</div> : <EmptyState>No hay actividad registrada aún.</EmptyState>}
            </div>
            <div className="rounded-2xl bg-slate-950 p-6 text-white">
              <p className="text-xs font-bold uppercase tracking-[0.18em] text-emerald-300">Accesos rápidos</p>
              <h2 className="mt-2 text-xl font-semibold">Gestiona tu operación</h2>
              <div className="mt-5 grid gap-2">
                {[['/operaciones', 'Realizar una operación'], ['/programadas', 'Programar una operación'], ['/recompensas', 'Puntos y beneficios'], ['/grafo', 'Explorar relaciones']].map(([href, label]) => <Link key={href} to={href} className="rounded-lg border border-white/10 bg-white/5 px-4 py-3 text-sm font-medium text-slate-100 transition hover:bg-white/10">{label} <span className="float-right text-emerald-300">→</span></Link>)}
              </div>
            </div>
          </div>

          <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <div className="mb-4 flex flex-wrap items-center justify-between gap-3"><div><h2 className="font-semibold text-slate-900">Últimas operaciones</h2><p className="mt-1 text-sm text-slate-500">Actividad reciente de todos los usuarios.</p></div><Link className="text-sm font-semibold text-emerald-700 hover:underline" to="/historial">Ver historial completo →</Link></div>
            {resumen.recientes.length ? <DataTable columns={columnas} rows={resumen.recientes} /> : <EmptyState>No hay operaciones registradas.</EmptyState>}
          </div>
        </div>
      )}
      {!cargando && resumen && <p className="mt-5 text-right text-xs text-slate-400">Datos obtenidos de la API · <button className="font-semibold text-emerald-700 hover:underline" onClick={cargar}>Actualizar</button></p>}
    </section>
  )
}
