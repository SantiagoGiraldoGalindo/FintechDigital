import { useState } from 'react'
import api from '../services/api.js'
import { mensajeError } from '../services/fintechService.js'
import { buttonClass, DataTable, ErrorMessage, Loading, PageHeading } from '../components/Feedback.jsx'

const aMilisegundos = (nanosegundos) => (Number(nanosegundos) / 1e6).toLocaleString('es-CO', { maximumFractionDigits: 3 })

export default function Benchmark() {
  const [reporte, setReporte] = useState(null)
  const [cargando, setCargando] = useState(false)
  const [error, setError] = useState('')

  const ejecutar = async () => {
    setCargando(true)
    setError('')
    try { setReporte(await api.get('/benchmark', { timeout: 120000 }).then(({ data }) => data)) }
    catch (e) { setError(mensajeError(e)) }
    finally { setCargando(false) }
  }
  const columnas = [
    { key: 'n', label: 'N' },
    { key: 'caso', label: 'Prueba' },
    { key: 'estructuraPropia', label: 'Estructura propia' },
    { key: 'nanosPropia', label: 'Tiempo propio (ms)', render: (m) => aMilisegundos(m.nanosPropia) },
    { key: 'referencia', label: 'Referencia' },
    { key: 'nanosReferencia', label: 'Tiempo ref. (ms)', render: (m) => aMilisegundos(m.nanosReferencia) },
    { key: 'operacionesMedidas', label: 'Operaciones' },
  ]
  const resumen = reporte?.mediciones?.reduce((acc, medicion) => {
    const grupo = acc[medicion.caso] || { caso: medicion.caso, n100k: medicion }
    if (medicion.n === 100000) grupo.n100k = medicion
    acc[medicion.caso] = grupo
    return acc
  }, {})

  return (
    <section>
      <PageHeading eyebrow="Rendimiento" title="Benchmark de estructuras" description="Compara las estructuras propias con implementaciones de referencia bajo distintas cargas." />
      <ErrorMessage>{error}</ErrorMessage>
      <div className="mb-6 flex flex-wrap items-center justify-between gap-4 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
        <div><h2 className="font-semibold text-slate-900">Ejecutar pruebas</h2><p className="mt-1 text-sm text-slate-500">Prueba N=1.000, 10.000 y 100.000. Puede tardar unos segundos.</p></div>
        <button className={buttonClass} onClick={ejecutar} disabled={cargando}>{cargando ? 'Midiendo…' : reporte ? 'Volver a ejecutar' : 'Ejecutar benchmark'}</button>
      </div>
      {cargando ? <Loading label="Ejecutando pruebas de rendimiento; puede tomar un momento…" /> : reporte && (
        <div className="space-y-6">
          <div className="grid gap-4 sm:grid-cols-3">
            <InfoCard titulo="Fecha de ejecución" valor={reporte.fechaEjecucion ? new Date(reporte.fechaEjecucion).toLocaleString() : '—'} />
            <InfoCard titulo="Versión Java" valor={reporte.versionJava || '—'} />
            <InfoCard titulo="Metodología" valor={`${reporte.calentamientos} calentamientos · ${reporte.muestras} muestras · mediana`} />
          </div>
          <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <h2 className="mb-4 font-semibold text-slate-900">Mediciones reales</h2>
            <DataTable columns={columnas} rows={reporte.mediciones || []} />
            <p className="mt-3 text-xs text-slate-500">Tiempos en milisegundos, convertidos desde nanosegundos medidos por el backend. Las mediciones dependen del hardware y de la carga del sistema.</p>
          </div>
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            {Object.values(resumen || {}).map((item) => <InfoCard key={item.caso} titulo={item.caso} valor={item.n100k ? `${item.n100k.estructuraPropia}: ${aMilisegundos(item.n100k.nanosPropia)} ms` : 'Sin mediciones'} detail={item.n100k ? `${item.n100k.referencia}: ${aMilisegundos(item.n100k.nanosReferencia)} ms · N=100.000` : ''} />)}
          </div>
        </div>
      )}
    </section>
  )
}

function InfoCard({ titulo, valor, detail }) {
  return <article className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm"><p className="text-xs font-bold uppercase tracking-wide text-slate-500">{titulo}</p><p className="mt-2 text-lg font-semibold text-slate-950">{valor}</p>{detail && <p className="mt-1 text-xs text-slate-500">{detail}</p>}</article>
}
