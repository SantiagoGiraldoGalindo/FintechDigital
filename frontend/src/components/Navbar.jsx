import { NavLink } from 'react-router-dom'
import logo from '../assets/logo.svg'

const linkClass = ({ isActive }) =>
  `px-3 py-2 rounded-md text-sm font-medium transition ${
    isActive ? 'bg-emerald-100 text-emerald-800' : 'text-slate-600 hover:bg-slate-100'
  }`

export default function Navbar() {
  return (
    <header className="bg-white border-b border-slate-200">
      <nav className="mx-auto flex max-w-6xl items-center justify-between px-4 py-3">
        <div className="flex items-center gap-2">
          <img src={logo} alt="FintechDigital" className="h-8 w-8" />
          <span className="text-lg font-semibold text-slate-900">FintechDigital</span>
        </div>
        <div className="flex flex-1 gap-1 overflow-x-auto py-1 pl-4">
          <NavLink to="/" end className={linkClass}>Inicio</NavLink>
          <NavLink to="/dashboard" className={linkClass}>Dashboard</NavLink>
          <NavLink to="/usuarios" className={linkClass}>Usuarios</NavLink>
          <NavLink to="/billeteras" className={linkClass}>Billeteras</NavLink>
          <NavLink to="/operaciones" className={linkClass}>Operaciones</NavLink>
          <NavLink to="/historial" className={linkClass}>Historial</NavLink>
          <NavLink to="/notificaciones" className={linkClass}>Notificaciones</NavLink>
          <NavLink to="/programadas" className={linkClass}>Programadas</NavLink>
          <NavLink to="/recompensas" className={linkClass}>Recompensas</NavLink>
          <NavLink to="/reversion" className={linkClass}>Reversión</NavLink>
          <NavLink to="/seguridad" className={linkClass}>Alertas/Auditoría</NavLink>
          <NavLink to="/analitica" className={linkClass}>Analítica</NavLink>
          <NavLink to="/grafo" className={linkClass}>Grafo</NavLink>
          <NavLink to="/benchmark" className={linkClass}>Benchmark</NavLink>
        </div>
      </nav>
    </header>
  )
}
