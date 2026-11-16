import { Outlet } from 'react-router-dom'
import Navbar from '../components/Navbar.jsx'

export default function MainLayout() {
  return (
    <div className="min-h-screen flex flex-col">
      <Navbar />
      <main className="flex-1 mx-auto w-full max-w-6xl px-4 py-8">
        <Outlet />
      </main>
      <footer className="border-t border-slate-200 py-4 text-center text-sm text-slate-500">
        FintechDigital · Proyecto universitario
      </footer>
    </div>
  )
}
