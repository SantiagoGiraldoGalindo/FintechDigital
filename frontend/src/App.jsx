import { Route, Routes } from 'react-router-dom'
import MainLayout from './layouts/MainLayout.jsx'
import Welcome from './pages/Welcome.jsx'
import Dashboard from './pages/Dashboard.jsx'
import NotFound from './pages/NotFound.jsx'
import Usuarios from './pages/Usuarios.jsx'
import Billeteras from './pages/Billeteras.jsx'
import Operaciones from './pages/Operaciones.jsx'
import Historial from './pages/Historial.jsx'
import Notificaciones from './pages/Notificaciones.jsx'
import Programadas from './pages/Programadas.jsx'
import Recompensas from './pages/Recompensas.jsx'
import Reversion from './pages/Reversion.jsx'
import AlertasAuditoria from './pages/AlertasAuditoria.jsx'
import Analitica from './pages/Analitica.jsx'
import Grafo from './pages/Grafo.jsx'
import Benchmark from './pages/Benchmark.jsx'

export default function App() {
  return (
    <Routes>
      <Route element={<MainLayout />}>
        <Route path="/" element={<Welcome />} />
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/usuarios" element={<Usuarios />} />
        <Route path="/billeteras" element={<Billeteras />} />
        <Route path="/operaciones" element={<Operaciones />} />
        <Route path="/historial" element={<Historial />} />
        <Route path="/notificaciones" element={<Notificaciones />} />
        <Route path="/programadas" element={<Programadas />} />
        <Route path="/recompensas" element={<Recompensas />} />
        <Route path="/reversion" element={<Reversion />} />
        <Route path="/seguridad" element={<AlertasAuditoria />} />
        <Route path="/analitica" element={<Analitica />} />
        <Route path="/grafo" element={<Grafo />} />
        <Route path="/benchmark" element={<Benchmark />} />
        <Route path="*" element={<NotFound />} />
      </Route>
    </Routes>
  )
}
