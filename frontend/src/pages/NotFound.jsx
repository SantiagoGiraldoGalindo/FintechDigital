import { Link } from 'react-router-dom'

export default function NotFound() {
  return (
    <section className="py-16 text-center">
      <h1 className="text-3xl font-bold text-slate-900">Página no encontrada</h1>
      <Link to="/" className="mt-4 inline-block text-emerald-700 underline">
        Volver al inicio
      </Link>
    </section>
  )
}
