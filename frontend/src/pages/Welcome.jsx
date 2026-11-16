import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { obtenerEstadoApi } from '../services/healthService.js'

export default function Welcome() {
  const [estado, setEstado] = useState('comprobando')

  useEffect(() => {
    obtenerEstadoApi()
      .then(() => setEstado('conectado'))
      .catch(() => setEstado('desconectado'))
  }, [])

  const estilos = {
    comprobando: 'bg-slate-100 text-slate-600',
    conectado: 'bg-emerald-100 text-emerald-800',
    desconectado: 'bg-red-100 text-red-700',
  }
  const textos = {
    comprobando: 'Comprobando conexión con la API…',
    conectado: 'API conectada',
    desconectado: 'API no disponible (¿está corriendo el backend?)',
  }

  return (
    <section className="py-12 text-center">
      <h1 className="text-4xl font-bold text-slate-900">Bienvenido a FintechDigital</h1>
      <p className="mx-auto mt-4 max-w-xl text-slate-600">
        Plataforma para gestionar billeteras digitales, transacciones, recompensas y analítica financiera.
      </p>
      <p className={`mt-6 inline-block rounded-full px-4 py-1 text-sm ${estilos[estado]}`}>
        {textos[estado]}
      </p>
      <div className="mt-8">
        <Link
          to="/dashboard"
          className="rounded-lg bg-emerald-600 px-6 py-3 font-medium text-white shadow hover:bg-emerald-700"
        >
          Ir al dashboard
        </Link>
      </div>
    </section>
  )
}
