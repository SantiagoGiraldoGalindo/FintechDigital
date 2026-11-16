import { useEffect, useState } from 'react'
import { ejecutarOperacion, listarUsuarios, listarBilleteras, mensajeError } from '../services/fintechService.js'
import { buttonClass, DataTable, EmptyState, ErrorMessage, inputClass, labelClass, Loading, Notice, PageHeading } from '../components/Feedback.jsx'

const operaciones = [
  { id: 'recarga', titulo: 'Recargar', detalle: 'Agregar fondos a una billetera.' },
  { id: 'retiro', titulo: 'Retirar', detalle: 'Retirar fondos disponibles.' },
  { id: 'transferencia', titulo: 'Transferir', detalle: 'Enviar fondos entre billeteras.' },
]

export default function Operaciones() {
  const [tipo, setTipo] = useState('recarga')
  const [usuarios, setUsuarios] = useState([])
  const [idUsuario, setIdUsuario] = useState('')
  const [billeteras, setBilleteras] = useState([])
  const [todasLasBilleteras, setTodasLasBilleteras] = useState([])
  const [origen, setOrigen] = useState('')
  const [destino, setDestino] = useState('')
  const [monto, setMonto] = useState('')
  const [cargando, setCargando] = useState(true)
  const [procesando, setProcesando] = useState(false)
  const [error, setError] = useState('')
  const [aviso, setAviso] = useState('')
  const [resultado, setResultado] = useState(null)

  useEffect(() => {
    let activo = true
    listarUsuarios()
      .then(async (data) => {
        const billeterasPorUsuario = await Promise.all(data.map((usuario) => listarBilleteras(usuario.id)))
        if (activo) {
          setUsuarios(data)
          setTodasLasBilleteras(billeterasPorUsuario.flat())
          if (data.length) setIdUsuario(String(data[0].id))
        }
      })
      .catch((e) => { if (activo) setError(mensajeError(e)) })
      .finally(() => { if (activo) setCargando(false) })
    return () => { activo = false }
  }, [])

  useEffect(() => {
    if (!idUsuario) return
    const data = todasLasBilleteras.filter((billetera) => String(billetera.idUsuario) === idUsuario)
    setBilleteras(data)
    setOrigen((actual) => data.some((b) => b.codigo === actual) ? actual : (data[0]?.codigo || ''))
    const destinos = tipo === 'transferencia' ? todasLasBilleteras : data
    setDestino((actual) => destinos.some((b) => b.codigo === actual) ? actual : (destinos[0]?.codigo || ''))
  }, [idUsuario, todasLasBilleteras, tipo])

  const enviar = async (event) => {
    event.preventDefault()
    setProcesando(true)
    setError('')
    setAviso('')
    setResultado(null)
    try {
      const datos = { monto: Number(monto) }
      if (tipo === 'recarga') datos.codigoDestino = destino
      if (tipo === 'retiro') datos.codigoOrigen = origen
      if (tipo === 'transferencia') {
        datos.codigoOrigen = origen
        datos.codigoDestino = destino
      }
      const transaccion = await ejecutarOperacion(tipo, datos)
      setResultado(transaccion)
      setAviso('La operación se procesó correctamente.')
      setMonto('')
    } catch (e) {
      setError(mensajeError(e))
    } finally {
      setProcesando(false)
    }
  }
  const opcionesBilletera = (lista, selected, change, label) => (
    <label className={labelClass}>{label}
      <select className={inputClass} value={selected} onChange={(e) => change(e.target.value)} required>
        <option value="" disabled>Selecciona una billetera</option>
        {lista.map((b) => <option key={b.codigo} value={b.codigo}>{b.codigo} · {b.nombre} (${Number(b.saldo).toLocaleString('es-CO')})</option>)}
      </select>
    </label>
  )

  return (
    <section>
      <PageHeading eyebrow="Movimiento de fondos" title="Operaciones" description="Realiza recargas, retiros y transferencias con validación en tiempo real." />
      <ErrorMessage>{error}</ErrorMessage>
      <Notice>{aviso}</Notice>
      {cargando ? <Loading label="Preparando operación…" /> : !usuarios.length ? <EmptyState>Registra un usuario y una billetera antes de realizar operaciones.</EmptyState> : (
        <div className="grid gap-6 lg:grid-cols-[minmax(0,1.1fr)_minmax(300px,.9fr)]">
          <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm sm:p-7">
            <label className={`${labelClass} mb-5 max-w-md`}>Usuario de referencia
              <select className={inputClass} value={idUsuario} onChange={(e) => setIdUsuario(e.target.value)}>
                {usuarios.map((u) => <option key={u.id} value={u.id}>{u.nombre} · {u.correo}</option>)}
              </select>
            </label>
            <div className="mb-6 grid grid-cols-3 gap-2">
              {operaciones.map((op) => <button key={op.id} type="button" onClick={() => { setTipo(op.id); setError(''); setAviso(''); setResultado(null) }} className={`rounded-xl border px-3 py-3 text-left transition ${tipo === op.id ? 'border-emerald-700 bg-emerald-50 text-emerald-900' : 'border-slate-200 text-slate-600 hover:bg-slate-50'}`}><span className="block text-sm font-semibold">{op.titulo}</span><span className="mt-1 hidden text-xs sm:block">{op.detalle}</span></button>)}
            </div>
            {!billeteras.length ? <EmptyState>El usuario seleccionado no tiene billeteras disponibles.</EmptyState> : (
              <form onSubmit={enviar} className="space-y-4">
                {(tipo === 'retiro' || tipo === 'transferencia') && opcionesBilletera(billeteras, origen, setOrigen, 'Billetera de origen')}
                {(tipo === 'recarga' || tipo === 'transferencia') && (
                  <>
                    {tipo === 'recarga' && <p className="text-xs text-slate-500">Recarga a una billetera del usuario seleccionado.</p>}
                    {opcionesBilletera(tipo === 'transferencia' ? todasLasBilleteras : billeteras, destino, setDestino, 'Billetera de destino')}
                  </>
                )}
                <label className={labelClass}>Monto
                  <div className="relative mt-1"><span className="absolute left-3 top-2.5 text-slate-400">$</span><input className={`${inputClass} mt-0 pl-7`} type="number" min="0.01" step="0.01" value={monto} onChange={(e) => setMonto(e.target.value)} placeholder="0.00" required /></div>
                </label>
                <button className={`${buttonClass} w-full`} disabled={procesando}>{procesando ? 'Procesando…' : `${operaciones.find((op) => op.id === tipo)?.titulo} operación`}</button>
              </form>
            )}
          </div>
          <aside className="rounded-2xl bg-slate-950 p-6 text-white">
            <p className="text-xs font-bold uppercase tracking-[0.18em] text-emerald-300">Actividad reciente</p>
            <h2 className="mt-2 text-xl font-semibold">Movimiento seguro</h2>
            <p className="mt-2 text-sm leading-6 text-slate-300">Las operaciones se registran en el historial y actualizan los saldos de las billeteras. Las reglas de negocio del servidor verifican saldo y estado antes de aceptar.</p>
            {resultado && <div className="mt-6 rounded-xl border border-white/10 bg-white/5 p-4">
              <p className="text-xs text-slate-400">Transacción #{resultado.id}</p>
              <div className="mt-2 flex items-center justify-between"><span className="font-medium">{resultado.tipo}</span><span className="font-semibold">${Number(resultado.valor).toLocaleString('es-CO')}</span></div>
              <p className="mt-2 text-xs text-emerald-300">{resultado.estado} · {resultado.fecha ? new Date(resultado.fecha).toLocaleString() : ''}</p>
            </div>}
          </aside>
        </div>
      )}
    </section>
  )
}
