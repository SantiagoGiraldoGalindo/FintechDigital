import { useCallback, useEffect, useState } from 'react'
import { actualizarUsuario, crearUsuario, eliminarUsuario, listarUsuarios, mensajeError } from '../services/fintechService.js'
import { buttonClass, DataTable, EmptyState, ErrorMessage, inputClass, labelClass, Loading, Notice, PageHeading } from '../components/Feedback.jsx'

const formularioInicial = { nombre: '', correo: '', puntos: 0 }

export default function Usuarios() {
  const [usuarios, setUsuarios] = useState([])
  const [formulario, setFormulario] = useState(formularioInicial)
  const [editando, setEditando] = useState(null)
  const [cargando, setCargando] = useState(true)
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState('')
  const [aviso, setAviso] = useState('')

  const cargar = useCallback(async () => {
    setCargando(true)
    setError('')
    try {
      setUsuarios(await listarUsuarios())
    } catch (e) {
      setError(mensajeError(e))
    } finally {
      setCargando(false)
    }
  }, [])

  useEffect(() => { cargar() }, [cargar])

  const cambiar = (event) => setFormulario({ ...formulario, [event.target.name]: event.target.value })
  const cancelarEdicion = () => {
    setEditando(null)
    setFormulario(formularioInicial)
  }
  const guardar = async (event) => {
    event.preventDefault()
    setGuardando(true)
    setError('')
    setAviso('')
    try {
      const datos = { nombre: formulario.nombre.trim(), correo: formulario.correo.trim() }
      if (editando) {
        await actualizarUsuario(editando, { ...datos, puntos: Number(formulario.puntos) })
        setAviso('Los datos del usuario se actualizaron.')
      } else {
        await crearUsuario(datos)
        setAviso('Usuario registrado correctamente.')
      }
      cancelarEdicion()
      await cargar()
    } catch (e) {
      setError(mensajeError(e))
    } finally {
      setGuardando(false)
    }
  }
  const editar = (usuario) => {
    setEditando(usuario.id)
    setFormulario({ nombre: usuario.nombre, correo: usuario.correo, puntos: usuario.puntos })
    setAviso('')
  }
  const eliminar = async (usuario) => {
    if (!window.confirm(`¿Eliminar a ${usuario.nombre}? Esta acción no se puede deshacer.`)) return
    setError('')
    setAviso('')
    try {
      await eliminarUsuario(usuario.id)
      setAviso('Usuario eliminado.')
      await cargar()
    } catch (e) {
      setError(mensajeError(e))
    }
  }

  const columnas = [
    { key: 'nombre', label: 'Usuario', render: (u) => <><span className="font-semibold text-slate-900">{u.nombre}</span><span className="block text-xs text-slate-500">{u.correo}</span></> },
    { key: 'saldo', label: 'Saldo', render: (u) => `$${Number(u.saldo).toLocaleString('es-CO')}` },
    { key: 'puntos', label: 'Puntos' },
    { key: 'nivel', label: 'Nivel', render: (u) => <span className="rounded-full bg-emerald-50 px-2.5 py-1 text-xs font-semibold text-emerald-800">{u.nivel}</span> },
    { key: 'acciones', label: 'Acciones', render: (u) => <div className="flex gap-3"><button className="font-semibold text-emerald-700 hover:underline" onClick={() => editar(u)}>Editar</button><button className="font-semibold text-red-600 hover:underline" onClick={() => eliminar(u)}>Eliminar</button></div> },
  ]

  return (
    <section>
      <PageHeading eyebrow="Gestión" title="Usuarios" description="Administra perfiles, datos de contacto y puntos de fidelidad." />
      <ErrorMessage onRetry={cargar}>{error}</ErrorMessage>
      <Notice>{aviso}</Notice>
      <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_340px]">
        <div className="min-w-0">
          {cargando ? <Loading label="Cargando usuarios…" /> : usuarios.length
            ? <DataTable columns={columnas} rows={usuarios} />
            : <EmptyState>Aún no hay usuarios registrados.</EmptyState>}
        </div>
        <form onSubmit={guardar} className="h-fit rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
          <div className="mb-5">
            <h2 className="font-semibold text-slate-900">{editando ? 'Editar usuario' : 'Nuevo usuario'}</h2>
            <p className="mt-1 text-xs text-slate-500">{editando ? 'Actualiza los datos del perfil.' : 'Registra un perfil en la plataforma.'}</p>
          </div>
          <label className={labelClass}>Nombre<input className={inputClass} name="nombre" value={formulario.nombre} onChange={cambiar} required maxLength={100} /></label>
          <label className={`${labelClass} mt-4`}>Correo electrónico<input className={inputClass} type="email" name="correo" value={formulario.correo} onChange={cambiar} required /></label>
          {editando && <label className={`${labelClass} mt-4`}>Puntos<input className={inputClass} type="number" min="0" name="puntos" value={formulario.puntos} onChange={cambiar} required /></label>}
          <div className="mt-5 flex gap-2">
            <button className={`${buttonClass} flex-1`} disabled={guardando}>{guardando ? 'Guardando…' : editando ? 'Guardar cambios' : 'Crear usuario'}</button>
            {editando && <button type="button" className="rounded-lg border border-slate-300 px-3 text-sm font-medium text-slate-700" onClick={cancelarEdicion}>Cancelar</button>}
          </div>
        </form>
      </div>
    </section>
  )
}
