import { useCallback, useEffect, useMemo, useState } from 'react'
import api from '../services/api.js'
import { listarUsuarios, mensajeError } from '../services/fintechService.js'
import { DataTable, EmptyState, ErrorMessage, Loading, PageHeading } from '../components/Feedback.jsx'

const colores = ['#047857', '#0f766e', '#0369a1', '#7c3aed', '#c2410c', '#be123c', '#4f46e5']

export default function Grafo() {
  const [tipo, setTipo] = useState('usuarios')
  const [vertices, setVertices] = useState([])
  const [aristas, setAristas] = useState([])
  const [ciclos, setCiclos] = useState([])
  const [etiquetas, setEtiquetas] = useState({})
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')

  const cargar = useCallback(async () => {
    setCargando(true)
    setError('')
    try {
      const base = `/grafo/${tipo}`
      const [nodos, enlaces, ciclosEncontrados, referencias] = await Promise.all([
        api.get(`${base}/vertices`).then(({ data }) => data),
        api.get(`${base}/aristas`, { params: { limite: 100 } }).then(({ data }) => data),
        api.get(`${base}/ciclos`).then(({ data }) => data),
        tipo === 'usuarios' ? listarUsuarios() : api.get('/usuarios').then(async ({ data }) => {
          const billeteras = (await Promise.all(data.map((u) => api.get('/billeteras', { params: { idUsuario: u.id } }).then(({ data: bs }) => bs)))).flat()
          return billeteras
        }),
      ])
      const mapa = Object.fromEntries(referencias.map((item) => [item.id, tipo === 'usuarios' ? item.nombre : item.codigo]))
      setVertices(nodos)
      setAristas(enlaces)
      setCiclos(ciclosEncontrados)
      setEtiquetas(mapa)
    } catch (e) { setError(mensajeError(e)) }
    finally { setCargando(false) }
  }, [tipo])
  useEffect(() => { cargar() }, [cargar])

  const posiciones = useMemo(() => {
    const ancho = 820
    const alto = Math.max(430, vertices.length > 10 ? 560 : 430)
    const centroX = ancho / 2
    const centroY = alto / 2
    const radio = Math.min(centroX, centroY) - 65
    const coords = {}
    vertices.forEach((id, index) => {
      const angulo = -Math.PI / 2 + (2 * Math.PI * index) / Math.max(vertices.length, 1)
      coords[id] = { x: centroX + radio * Math.cos(angulo), y: centroY + radio * Math.sin(angulo) }
    })
    return { coords, ancho, alto }
  }, [vertices])
  const enlacesCiclo = useMemo(() => {
    const pares = new Set()
    ciclos.forEach((ciclo) => ciclo.forEach((origen, index) => {
      const destino = ciclo[(index + 1) % ciclo.length]
      pares.add(`${origen}->${destino}`)
    }))
    return pares
  }, [ciclos])
  const columnas = [
    { key: 'origen', label: 'Origen', render: (a) => etiquetas[a.origen] || a.origen },
    { key: 'destino', label: 'Destino', render: (a) => etiquetas[a.destino] || a.destino },
    { key: 'montoTotal', label: 'Monto acumulado', render: (a) => `$${Number(a.montoTotal).toLocaleString('es-CO')}` },
    { key: 'veces', label: 'Frecuencia' },
  ]

  return (
    <section>
      <PageHeading eyebrow="Transferencias" title="Grafo de relaciones" description="Explora conexiones, volúmenes y ciclos entre usuarios o billeteras." />
      <ErrorMessage onRetry={cargar}>{error}</ErrorMessage>
      <div className="mb-5 flex flex-wrap items-center justify-between gap-3 rounded-xl border border-slate-200 bg-white p-4">
        <div className="flex gap-2">
          {[['usuarios', 'Usuarios'], ['billeteras', 'Billeteras']].map(([value, label]) => <button key={value} onClick={() => setTipo(value)} className={`rounded-lg px-4 py-2 text-sm font-semibold transition ${tipo === value ? 'bg-emerald-700 text-white' : 'bg-slate-100 text-slate-600 hover:bg-slate-200'}`}>{label}</button>)}
        </div>
        <div className="flex items-center gap-4 text-xs text-slate-600"><span className="flex items-center gap-2"><i className="h-2.5 w-2.5 rounded-full bg-sky-600" />Arista</span><span className="flex items-center gap-2"><i className="h-2.5 w-2.5 rounded-full bg-red-600" />Ciclo detectado</span><span>{vertices.length} nodos · {aristas.length} conexiones · {ciclos.length} ciclos</span></div>
      </div>
      {cargando ? <Loading label="Calculando grafo…" /> : !vertices.length ? <EmptyState>No hay vértices en este grafo todavía.</EmptyState> : (
        <div className="space-y-6">
          <div className="overflow-x-auto rounded-2xl border border-slate-200 bg-white p-3 shadow-sm">
            <svg viewBox={`0 0 ${posiciones.ancho} ${posiciones.alto}`} className="mx-auto min-w-[700px] max-w-full" role="img" aria-label={`Grafo dirigido de ${tipo}`}>
              <defs><marker id="arrow" markerWidth="10" markerHeight="10" refX="8" refY="3" orient="auto" markerUnits="strokeWidth"><path d="M0,0 L0,6 L9,3 z" fill="#64748b" /></marker><marker id="arrowCycle" markerWidth="10" markerHeight="10" refX="8" refY="3" orient="auto" markerUnits="strokeWidth"><path d="M0,0 L0,6 L9,3 z" fill="#dc2626" /></marker></defs>
              {aristas.map((arista, index) => {
                const desde = posiciones.coords[arista.origen]
                const hasta = posiciones.coords[arista.destino]
                if (!desde || !hasta) return null
                const ciclo = enlacesCiclo.has(`${arista.origen}->${arista.destino}`)
                const medioX = (desde.x + hasta.x) / 2
                const medioY = (desde.y + hasta.y) / 2
                return <g key={`${arista.origen}-${arista.destino}-${index}`}>
                  <line x1={desde.x} y1={desde.y} x2={hasta.x} y2={hasta.y} stroke={ciclo ? '#dc2626' : '#94a3b8'} strokeWidth={ciclo ? 3 : 1.7} markerEnd={ciclo ? 'url(#arrowCycle)' : 'url(#arrow)'} opacity={ciclo ? 0.9 : 0.65} />
                  <text x={medioX} y={medioY - 5} textAnchor="middle" className="fill-slate-600" fontSize="11">{arista.veces}×</text>
                </g>
              })}
              {vertices.map((id, index) => {
                const punto = posiciones.coords[id]
                return <g key={id}>
                  <circle cx={punto.x} cy={punto.y} r="23" fill={colores[index % colores.length]} stroke="#fff" strokeWidth="3" />
                  <text x={punto.x} y={punto.y + 4} textAnchor="middle" fill="#fff" fontSize="11" fontWeight="700">{String(id).slice(-4)}</text>
                  <text x={punto.x} y={punto.y + 39} textAnchor="middle" className="fill-slate-700" fontSize="11">{(etiquetas[id] || `#${id}`).slice(0, 18)}</text>
                </g>
              })}
            </svg>
          </div>
          <div className="grid gap-6 lg:grid-cols-[minmax(0,1.3fr)_minmax(260px,.7fr)]">
            <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm"><h2 className="mb-4 font-semibold text-slate-900">Aristas más frecuentes</h2>{aristas.length ? <DataTable columns={columnas} rows={aristas} /> : <EmptyState>No hay aristas para mostrar.</EmptyState>}</div>
            <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm"><h2 className="mb-3 font-semibold text-slate-900">Ciclos detectados</h2>{ciclos.length ? <ol className="space-y-3">{ciclos.map((ciclo, index) => <li key={`${ciclo.join('-')}-${index}`} className="rounded-xl border border-red-100 bg-red-50 p-3"><span className="text-xs font-bold uppercase tracking-wide text-red-700">Ciclo {index + 1}</span><p className="mt-1 break-words text-sm text-red-950">{[...ciclo, ciclo[0]].map((id) => etiquetas[id] || `#${id}`).join(' → ')}</p></li>)}</ol> : <EmptyState>No se encontraron ciclos.</EmptyState>}</div>
          </div>
        </div>
      )}
    </section>
  )
}
